<template>
  <el-container class="layout">
    <el-aside width="200px" class="aside">
      <div class="logo">
        <span>ConeEats</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#263445"
        text-color="#bfcbd9"
        active-text-color="#42b883"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item index="/employee">
          <el-icon><User /></el-icon>
          <span>员工管理</span>
        </el-menu-item>
        <el-menu-item index="/category">
          <el-icon><Menu /></el-icon>
          <span>分类管理</span>
        </el-menu-item>
        <el-menu-item index="/dish">
          <el-icon><Food /></el-icon>
          <span>菜品管理</span>
        </el-menu-item>
        <el-menu-item index="/setmeal">
          <el-icon><Dish /></el-icon>
          <span>套餐管理</span>
        </el-menu-item>
        <el-menu-item index="/report">
          <el-icon><TrendCharts /></el-icon>
          <span>数据统计</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <!-- 营业状态 -->
          <el-tag :type="open ? 'success' : 'danger'" effect="dark" size="large">
            {{ open ? '营业中' : '已打烊' }}
          </el-tag>
          <el-switch v-model="open" @change="handleShopChange" style="margin-left: 12px" />
        </div>
        <div class="header-right">
          <span class="store-name" v-if="storeName">{{ storeName }}</span>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><Avatar /></el-icon>
              <span>{{ userName || '管理员' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { shopStatus, setShopStatus } from '@/api/shop'
import { employeeLogout } from '@/api/employee'
import { getToken, removeToken, removeStoreId } from '@/utils/auth'

const route = useRoute()
const router = useRouter()

const open = ref(true)
const userName = ref('')

// 多商家：从登录响应存的 storeId 显示店铺标识（简化，可在实际中拉取店铺名）
const storeName = computed(() => localStorage.getItem('store_id') ? '本店模式' : '平台管理员')

const activeMenu = computed(() => route.path)

onMounted(async () => {
  // 解析 token 中的用户名（简化，实际可扩展解码）
  try {
    const token = getToken()
    if (token) {
      const payload = JSON.parse(atob(token.split('.')[1]))
      userName.value = payload.name || payload.empId || '管理员'
    }
  } catch (e) {
    // ignore
  }
  await loadShopStatus()
  // 平台管理员才能看到营业状态（商家按店铺维度的营业状态可能不同，这里统一展示）
})

async function loadShopStatus() {
  try {
    const s = await shopStatus()
    open.value = String(s).trim() === '1' || s === true
  } catch (e) {
    // 接口不可用时默认营业中
  }
}

async function handleShopChange(val) {
  try {
    await setShopStatus(val ? 1 : 0)
    ElMessage.success(val ? '已开启营业' : '已打烊')
  } catch (e) {
    open.value = !val
  }
}

async function handleCommand(cmd) {
  if (cmd === 'logout') {
    try { await employeeLogout() } catch (e) { /* ignore */ }
    removeToken()
    removeStoreId()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
}
.aside {
  background: #263445;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  background: #1f2d3d;
}
.aside :deep(.el-menu) {
  border-right: none;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}
.header-left,
.header-right {
  display: flex;
  align-items: center;
}
.store-name {
  margin-right: 16px;
  color: #606266;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: #333;
  outline: none;
}
.main {
  background: #f0f2f5;
  padding: 20px;
}
</style>