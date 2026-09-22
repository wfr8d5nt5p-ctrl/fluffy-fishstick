<template>
  <div class="page">
    <el-card shadow="never" class="toolbar-card">
      <el-form inline>
        <el-form-item label="统计时间">
          <el-date-picker
            v-model="range"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item label="快捷">
          <el-radio-group v-model="quickRange">
            <el-radio-button value="7">近7天</el-radio-button>
            <el-radio-button value="30">近30天</el-radio-button>
            <el-radio-button value="90">近90天</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button type="success" @click="handleExport">
            <el-icon style="margin-right: 4px"><Download /></el-icon>
            导出运营数据
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">营业额</div>
          <div class="stat-value primary">￥{{ turnoverTotal.toFixed(2) }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">订单 / 有效订单</div>
          <div class="stat-value info">{{ orderTotal }} / {{ validOrderTotal }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">订单完成率</div>
          <div class="stat-value success">{{ (completionRate * 100).toFixed(1) }}%</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">新增用户</div>
          <div class="stat-value warning">{{ newUserTotal }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">累计用户</div>
          <div class="stat-value danger">{{ totalUserTotal }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">统计天数</div>
          <div class="stat-value neutral">{{ dates.length }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>
        <span>每日运营明细</span>
      </template>
      <el-table :data="detailRows" border stripe size="small">
        <el-table-column prop="date" label="日期" width="130" />
        <el-table-column label="营业额" align="right">
          <template #default="{ row }">￥{{ row.turnover.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="订单数" align="center" prop="orderCount" />
        <el-table-column label="有效订单" align="center" prop="validOrderCount" />
        <el-table-column label="新增用户" align="center" prop="newUser" />
        <el-table-column label="累计用户" align="center" prop="totalUser" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { turnoverStatistics, userStatistics, orderStatistics, exportBusiness } from '@/api/report'

const range = ref([])
const quickRange = ref('7')

const turnover = reactive({ dates: [], values: [] })
const user = reactive({ dates: [], newUser: [], totalUser: [] })
const order = reactive({ dates: [], orderCount: [], validOrderCount: [], total: 0, valid: 0, rate: 0 })

const dates = ref([])
const turnoverByDate = ref({})
const newUserByDate = ref({})
const totalUserByDate = ref({})
const orderCountByDate = ref({})
const validOrderCountByDate = ref({})

const turnoverTotal = computed(() =>
  turnDates.value.reduce((s, d) => s + Number(turnoverByDate.value[d] || 0), 0)
)
const orderTotal = computed(() =>
  turnDates.value.reduce((s, d) => s + Number(orderCountByDate.value[d] || 0), 0)
)
const validOrderTotal = computed(() =>
  turnDates.value.reduce((s, d) => s + Number(validOrderCountByDate.value[d] || 0), 0)
)
const newUserTotal = computed(() =>
  turnDates.value.reduce((s, d) => s + Number(newUserByDate.value[d] || 0), 0)
)
const totalUserTotal = computed(() => {
  const tail = turnDates.value.slice(-1)
  if (!tail.length) return 0
  return Number(totalUserByDate.value[tail[0]] || 0)
})
const completionRate = computed(() =>
  orderTotal.value ? validOrderTotal.value / orderTotal.value : 0
)

const turnDates = computed(() => dates.value)

function daysAgo(n) {
  const d = new Date()
  d.setDate(d.getDate() - n)
  return d
}

function fmt(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function applyQuickRange() {
  const n = Number(quickRange.value)
  const end = new Date()
  const begin = daysAgo(n - 1)
  range.value = [fmt(begin), fmt(end)]
}

function splitCsv(str) {
  if (!str) return []
  return String(str).split(',').filter((s) => s !== '')
}

function buildDetailRows() {
  const rows = []
  for (const d of dates.value) {
    rows.push({
      date: d,
      turnover: Number(turnoverByDate.value[d] || 0),
      orderCount: Number(orderCountByDate.value[d] || 0),
      validOrderCount: Number(validOrderCountByDate.value[d] || 0),
      newUser: Number(newUserByDate.value[d] || 0),
      totalUser: Number(totalUserByDate.value[d] || 0)
    })
  }
  return rows
}

const detailRows = computed(() => buildDetailRows())

async function loadData() {
  if (!range.value || range.value.length !== 2) {
    ElMessage.warning('请选择统计时间')
    return
  }
  const [begin, end] = range.value
  const params = { begin, end }

  const [t, u, o] = await Promise.all([
    turnoverStatistics(params),
    userStatistics(params),
    orderStatistics(params)
  ])

  turnover.dates = splitCsv(t.dateList)
  turnover.values = splitCsv(t.turnoverList)
  user.dates = splitCsv(u.dateList)
  user.newUser = splitCsv(u.newUserList)
  user.totalUser = splitCsv(u.totalUserList)
  order.dates = splitCsv(o.dateList)
  order.orderCount = splitCsv(o.orderCountList)
  order.validOrderCount = splitCsv(o.validOrderCountList)
  order.total = Number(o.totalOrderCount || 0)
  order.valid = Number(o.validOrderCount || 0)
  order.rate = Number(o.orderCompletionRate || 0)

  dates.value = turnover.dates

  const tb = {},
    nb = {},
    ub = {},
    ocb = {},
    vcb = {}
  turnover.dates.forEach((d, i) => (tb[d] = turnover.values[i]))
  user.dates.forEach((d, i) => (nb[d] = user.newUser[i]))
  user.dates.forEach((d, i) => (ub[d] = user.totalUser[i]))
  order.dates.forEach((d, i) => (ocb[d] = order.orderCount[i]))
  order.dates.forEach((d, i) => (vcb[d] = order.validOrderCount[i]))
  turnoverByDate.value = tb
  newUserByDate.value = nb
  totalUserByDate.value = ub
  orderCountByDate.value = ocb
  validOrderCountByDate.value = vcb
}

async function handleExport() {
  if (!range.value || range.value.length !== 2) {
    ElMessage.warning('请选择统计时间')
    return
  }
  const [begin, end] = range.value
  try {
    const blob = await exportBusiness({ begin, end })
    const isBlob = blob instanceof Blob
    const name = `运营数据报表_${begin}_${end}.xlsx`
    if (isBlob) {
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = name
      a.click()
      URL.revokeObjectURL(url)
      ElMessage.success('导出成功')
    } else {
      ElMessage.error('导出失败，请稍后再试')
    }
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

onMounted(() => {
  applyQuickRange()
  loadData()
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.toolbar-card :deep(.el-card__body) {
  padding-bottom: 0;
}
.stat-card {
  text-align: center;
}
.stat-title {
  color: #909399;
  font-size: 14px;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 28px;
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
.warning {
  color: #e6a23c;
}
.danger {
  color: #f56c6c;
}
.neutral {
  color: #606266;
}
</style>