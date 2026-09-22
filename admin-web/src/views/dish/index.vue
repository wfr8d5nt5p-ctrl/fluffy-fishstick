<template>
  <div class="page">
    <el-card shadow="never" class="search-card">
      <el-form inline>
        <el-form-item label="菜品名称">
          <el-input v-model="query.name" placeholder="请输入名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.categoryId" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="c in dishCategories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="起售" :value="1" />
            <el-option label="停售" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="openAdd">新增菜品</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column label="图片" width="90" align="center">
          <template #default="{ row }">
            <el-image v-if="row.image" :src="row.image" :preview-src-list="[row.image]" preview-teleported fit="cover" style="width: 50px; height: 50px; border-radius: 4px" />
            <span v-else class="no-img">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="菜品名称" min-width="150" />
        <el-table-column prop="categoryName" label="分类" min-width="120" />
        <el-table-column label="售价" width="100" align="right">
          <template #default="{ row }">￥{{ row.price }}</template>
        </el-table-column>
        <el-table-column label="口味" min-width="160">
          <template #default="{ row }">
            <span v-if="row.flavors && row.flavors.length">{{ row.flavors.map((f) => f.name).join('、') }}</span>
            <span v-else class="no-img">无</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '起售' : '停售' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">修改</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
            <el-switch
              :model-value="row.status === 1"
              :loading="row._statusLoading"
              @change="(val) => handleStatusChange(row, val)"
            />
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
      :title="form.id ? '修改菜品' : '新增菜品'"
      width="640px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="菜品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入菜品名称" />
        </el-form-item>
        <el-form-item label="菜品分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="c in dishCategories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="菜品价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" controls-position="right" style="width: 200px" />
        </el-form-item>
        <el-form-item label="菜品图片" prop="image">
          <el-upload
            class="avatar-uploader"
            :show-file-list="false"
            :http-request="doUpload"
            accept="image/*"
          >
            <img v-if="form.image" :src="form.image" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="up-tip">点击上传图片（支持 jpg/png 等）</div>
        </el-form-item>
        <el-form-item label="菜品描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="菜品描述" />
        </el-form-item>

        <el-form-item label="口味">
          <div class="flavor-wrap">
            <div v-for="(f, i) in form.flavors" :key="f._key" class="flavor-item">
              <el-input v-model="f.name" placeholder="口味名，如辣度" style="width: 120px" />
              <el-select
                v-model="f.value"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="输入选项回车添加"
                style="flex: 1"
              >
              </el-select>
              <el-button type="danger" @click="removeFlavor(i)">删除</el-button>
            </div>
            <el-button type="primary" plain @click="addFlavor">＋ 添加口味</el-button>
          </div>
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
import { Plus } from '@element-plus/icons-vue'
import { dishPage, dishSave, dishUpdate, dishDelete, dishGetById, dishStatus } from '@/api/dish'
import { categoryList } from '@/api/category'
import { uploadImage } from '@/api/common'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const formRef = ref()
const dishCategories = ref([])

const query = reactive({ page: 1, pageSize: 10, name: '', categoryId: null })
const queryStatus = ref(null)

let flavorSeed = 0

const form = reactive(defaultForm())

function defaultForm() {
  return {
    id: null,
    name: '',
    categoryId: null,
    price: 0,
    image: '',
    description: '',
    status: 1,
    flavors: []
  }
}

const rules = {
  name: [{ required: true, message: '请输入菜品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择菜品分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入菜品价格', trigger: 'blur' }],
  image: [{ required: true, message: '请上传菜品图片', trigger: 'change' }]
}

function addFlavor() {
  form.flavors.push({ _key: `f_${++flavorSeed}`, name: '', value: [] })
}

function removeFlavor(i) {
  form.flavors.splice(i, 1)
}

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    if (queryStatus.value != null) params.status = queryStatus.value
    const data = await dishPage(params)
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
  query.categoryId = null
  queryStatus.value = null
  query.page = 1
  load()
}

async function doUpload({ file }) {
  try {
    const url = await uploadImage(file)
    if (!url) throw new Error('上传返回为空')
    form.image = url
    ElMessage.success('图片上传成功')
  } catch (e) {
    ElMessage.error('图片上传失败')
  }
}

function openAdd() {
  Object.assign(form, defaultForm())
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function openEdit(row) {
  Object.assign(form, defaultForm())
  dialogVisible.value = true
  try {
    const detail = await dishGetById(row.id)
    Object.assign(form, {
      id: detail.id,
      name: detail.name,
      categoryId: detail.categoryId,
      price: Number(detail.price),
      image: detail.image,
      description: detail.description
    })
    form.flavors = (detail.flavors || []).map((f) => ({
      _key: `f_${++flavorSeed}`,
      name: f.name,
      value: safeParseArray(f.value)
    }))
  } catch (e) {}
}

function safeParseArray(str) {
  if (Array.isArray(str)) return str
  try {
    return JSON.parse(str || '[]')
  } catch (e) {
    return String(str || '').split(',').filter(Boolean)
  }
}

function buildPayload() {
  const flavors = form.flavors
    .filter((f) => f.name)
    .map((f) => ({
      name: f.name,
      value: JSON.stringify(Array.isArray(f.value) ? f.value : [])
    }))
  return {
    id: form.id,
    name: form.name,
    categoryId: form.categoryId,
    price: form.price,
    image: form.image,
    description: form.description,
    status: 1,
    flavors
  }
}

async function handleSubmit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = buildPayload()
    if (form.id) {
      await dishUpdate(payload)
      ElMessage.success('修改成功')
    } else {
      await dishSave(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } catch (e) {
  } finally {
    saving.value = false
  }
}

async function handleStatusChange(row, val) {
  try {
    row._statusLoading = true
    await dishStatus(val ? 1 : 0, row.id)
    row.status = val ? 1 : 0
    ElMessage.success(val ? '已起售' : '已停售')
  } catch (e) {
  } finally {
    row._statusLoading = false
  }
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除菜品「${row.name}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      try {
        await dishDelete([row.id])
        ElMessage.success('删除成功')
        load()
      } catch (e) {}
    })
    .catch(() => {})
}

onMounted(async () => {
  try {
    dishCategories.value = await categoryList(1)
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
.no-img {
  color: #999;
  font-size: 12px;
}
.avatar-uploader .avatar {
  width: 100px;
  height: 100px;
  border-radius: 6px;
  object-fit: cover;
  display: block;
}
.avatar-uploader :deep(.el-upload) {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  overflow: hidden;
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.2s;
}
.avatar-uploader :deep(.el-upload:hover) {
  border-color: #409eff;
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
}
.up-tip {
  margin-left: 12px;
  color: #999;
  font-size: 12px;
}
.flavor-wrap {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.flavor-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>