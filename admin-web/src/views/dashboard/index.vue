<template>
  <div class="page">
    <el-card shadow="never" class="welcome">
      <div class="welcome-title">{{ greeting }}，{{ userName }}</div>
      <div class="welcome-sub">
        <span v-if="storeName">{{ storeName }}</span>
        <span v-else>平台管理员（可查看全平台数据）</span>
        ，祝你工作顺利！🛵
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col v-for="item in quickCards" :key="item.title" :span="6">
        <el-card shadow="hover" class="quick-card" @click="go(item.path)">
          <el-icon :size="30" :color="item.color"><component :is="item.icon" /></el-icon>
          <div class="quick-title">{{ item.title }}</div>
          <div class="quick-desc">{{ item.desc }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header><span>近期数据一览</span></template>
      <el-row :gutter="16">
        <el-col :span="6">
          <div class="mini-stat">
            <div class="mini-label">今日营业额</div>
            <div class="mini-value primary">￥{{ todayTurnover.toFixed(2) }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="mini-stat">
            <div class="mini-label">今日订单数</div>
            <div class="mini-value info">{{ todayOrders }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="mini-stat">
            <div class="mini-label">今日有效订单</div>
            <div class="mini-value success">{{ todayValidOrders }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="mini-stat">
            <div class="mini-label">订单完成率</div>
            <div class="mini-value neutral">
              {{ todayOrders ? ((todayValidOrders / todayOrders) * 100).toFixed(1) : '0.0' }}%
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, Menu, Food, TrendCharts, Odometer, Dish } from '@element-plus/icons-vue'
import { turnoverStatistics, orderStatistics } from '@/api/report'

const router = useRouter()
const userName = ref('管理员')
const storeName = ref('')
const todayTurnover = ref(0)
const todayOrders = ref(0)
const todayValidOrders = ref(0)

function todayStr() {
  const d = new Date()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const quickCards = [
  { title: '员工管理', desc: '账号 / 角色 / 状态', icon: User, color: '#409eff', path: '/employee' },
  { title: '分类管理', desc: '菜品 / 套餐分类', icon: Menu, color: '#67c23a', path: '/category' },
  { title: '菜品管理', desc: '菜品 / 口味 / 价格', icon: Food, color: '#e6a23c', path: '/dish' },
  { title: '套餐管理', desc: '组合套餐 / 选菜', icon: Dish, color: '#f56c6c', path: '/setmeal' }
]

function go(path) {
  router.push(path)
}

onMounted(async () => {
  try {
    const token = localStorage.getItem('admin_token')
    if (token) {
      const payload = JSON.parse(atob(token.split('.')[1]))
      userName.value = payload.name || payload.empId || '管理员'
    }
  } catch (e) {}
  const sid = localStorage.getItem('admin_store_id')
  storeName.value = sid ? '本店模式' : ''

  try {
    const t = todayStr()
    const [to, o] = await Promise.all([
      turnoverStatistics({ begin: t, end: t }),
      orderStatistics({ begin: t, end: t })
    ])
    const turn = String(to.turnoverList || '').split(',').filter((x) => x !== '')
    todayTurnover.value = turn.length ? Number(turn[0] || 0) : 0
    todayOrders.value = Number(o.totalOrderCount || 0)
    todayValidOrders.value = Number(o.validOrderCount || 0)
  } catch (e) {}
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.welcome-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 6px;
}
.welcome-sub {
  color: #909399;
}
.quick-card {
  cursor: pointer;
  text-align: center;
  transition: transform 0.2s, box-shadow 0.2s;
}
.quick-card:hover {
  transform: translateY(-4px);
}
.quick-title {
  font-size: 16px;
  font-weight: 600;
  margin: 10px 0 4px;
}
.quick-desc {
  color: #909399;
  font-size: 13px;
}
.mini-stat {
  text-align: center;
  padding: 8px 0;
}
.mini-label {
  color: #909399;
  margin-bottom: 8px;
}
.mini-value {
  font-size: 24px;
  font-weight: 700;
}
.primary {
  color: #409eff;
}
.info {
  color: #909399;
}
.success {
  color: #67c23a;
}
.neutral {
  color: #606266;
}
</style>