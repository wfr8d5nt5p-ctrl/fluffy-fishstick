<template>
  <div class="page">
    <el-card shadow="never" class="search-card">
      <el-form inline>
        <el-form-item label="员工姓名">
          <el-input
            v-model="query.name"
            placeholder="请输入姓名"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="openAdd">新增员工</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="name" label="姓名" min-width="110" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="sex" label="性别" width="70" align="center">
          <template #default="{ row }">{{ row.sex === '1' ? '男' : row.sex === '0' ? '女' : '未知' }}</template>
        </el-table-column>
        <el-table-column prop="idNumber" label="身份证号" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :disabled="row.id === currentUserId"
              :loading="row._statusLoading"
              @change="(val) => handleStatusChange(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button
              type="danger"
              link
              :disabled="row.id === currentUserId"
              @click="handleDisable(row)"
            >
              禁用
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="load"
        @current-change="load"
      />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑员工' : '新增员工'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="性别" prop="sex">
          <el-radio-group v-model="form.sex">
            <el-radio value="1">男</el-radio>
            <el-radio value="0">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="身份证号" prop="idNumber">
          <el-input v-model="form.idNumber" placeholder="请输入身份证号" maxlength="18" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入初始密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  employeePage,
  employeeSave,
  employeeUpdate,
  employeeStatus
} from '@/api/employee'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const formRef = ref()

const query = reactive({ page: 1, pageSize: 10, name: '' })

const form = reactive({
  id: null,
  username: '',
  name: '',
  phone: '',
  sex: '1',
  idNumber: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }]
}

const currentUserId = ref(0)

function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 19)
}

async function load() {
  loading.value = true
  try {
    const data = await employeePage(query)
    list.value = data.records || []
    total.value = Number(data.total || 0)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  load()
}

function handleReset() {
  query.name = ''
  query.page = 1
  load()
}

function openAdd() {
  Object.assign(form, {
    id: null,
    username: '',
    name: '',
    phone: '',
    sex: '1',
    idNumber: '',
    password: ''
  })
  rules.password[0].required = true
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    username: row.username,
    name: row.name,
    phone: row.phone,
    sex: String(row.sex ?? '1'),
    idNumber: row.idNumber,
    password: ''
  })
  rules.password[0].required = false
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      id: form.id,
      username: form.username,
      name: form.name,
      phone: form.phone,
      sex: form.sex,
      idNumber: form.idNumber
    }
    if (!form.id) payload.password = form.password
    if (form.id) {
      await employeeUpdate(payload)
      ElMessage.success('修改成功')
    } else {
      await employeeSave(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } catch (e) {
    // 表单校验或接口错误已在上层提示
  } finally {
    saving.value = false
  }
}

async function handleStatusChange(row, val) {
  try {
    row._statusLoading = true
    await employeeStatus(val ? 1 : 0, row.id)
    row.status = val ? 1 : 0
    ElMessage.success(val ? '已启用' : '已禁用')
  } catch (e) {
    // 拦截器已提示，无需重复
  } finally {
    row._statusLoading = false
  }
}

function handleDisable(row) {
  ElMessageBox.confirm(`确定禁用员工「${row.name}」吗？禁用后该账号将无法登录。`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      try {
        await employeeStatus(0, row.id)
        row.status = 0
        ElMessage.success('已禁用')
      } catch (e) {}
    })
    .catch(() => {})
}

onMounted(() => {
  try {
    const token = localStorage.getItem('admin_token')
    if (token) {
      const payload = JSON.parse(atob(token.split('.')[1]))
      currentUserId.value = payload.empId || payload.id || 0
    }
  } catch (e) {}
  load()
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.search-card :deep(.el-card__body) {
  padding-bottom: 0;
}
</style>