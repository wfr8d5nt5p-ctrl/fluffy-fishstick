<template>
  <div class="page">
    <el-card shadow="never" class="search-card">
      <el-form inline>
        <el-form-item label="套餐名称">
          <el-input v-model="query.name" placeholder="请输入名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.categoryId" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="c in setmealCategories" :key="c.id" :label="c.name" :value="c.id" />
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
          <el-button type="success" @click="openAdd">新增套餐</el-button>
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
        <el-table-column prop="name" label="套餐名称" min-width="150" />
        <el-table-column prop="categoryName" label="分类" min-width="120" />
        <el-table-column label="套餐价" width="100" align="right">
          <template #default="{ row }">￥{{ row.price }}</template>
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
      :title="form.id ? '修改套餐' : '新增套餐'"
      width="760px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="套餐名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入套餐名称" />
        </el-form-item>
        <el-form-item label="套餐分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="c in setmealCategories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="套餐价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" controls-position="right" style="width: 200px" />
        </el-form-item>
        <el-form-item label="套餐图片" prop="image">
          <el-upload
            class="avatar-uploader"
            :show-file-list="false"
            :http-request="doUpload"
            accept="image/*"
          >
            <img v-if="form.image" :src="form.image" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="up-tip">点击上传图片</div>
        </el-form-item>
        <el-form-item label="套餐描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="套餐描述" />
        </el-form-item>

        <el-form-item label="包含菜品">
          <div class="setmeal-dish-wrap">
            <div v-for="(sd, i) in form.setmealDishes" :key="sd.dishId" class="setmeal-dish-item">
              <span class="dish-name">{{ sd.name }}</span>
              <el-input-number v-model="sd.copies" :min="1" size="small" />
              <el-button type="danger" size="small" @click="removeDish(i)">删除</el-button>
            </div>
            <el-button type="primary" plain @click="openDishDialog">＋ 选择菜品</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dishDialogVisible" title="选择菜品" width="640px" append-to-body destroy-on-close>
      <el-tabs v-model="activeDishCategory">
        <el-tab-pane v-for="c in dishCategories" :key="c.id" :label="c.name" :name="String(c.id)">
          <el-checkbox-group v-model="selectedDishIds" class="dish-check-group">
            <el-checkbox v-for="d in dishesMap[c.id] || []" :key="d.id" :label="d.id" :value="d.id">
              <span class="dish-check-item">{{ d.name }} ￥{{ d.price }}</span>
            </el-checkbox>
          </el-checkbox-group>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="dishDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSelectDish">确定选择</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { setmealPage, setmealSave, setmealUpdate, setmealDelete, setmealGetById, setmealStatus } from '@/api/setmeal'
import { categoryList } from '@/api/category'
import { dishListByCategory } from '@/api/dish'
import { uploadImage } from '@/api/common'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const formRef = ref()
const setmealCategories = ref([])
const dishCategories = ref([])
const dishesMap = ref({})

const dishDialogVisible = ref(false)
const activeDishCategory = ref('1')
const selectedDishIds = ref([])

const query = reactive({ page: 1, pageSize: 10, name: '', categoryId: null })
const queryStatus = ref(null)

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
    setmealDishes: []
  }
}

const rules = {
  name: [{ required: true, message: '请输入套餐名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择套餐分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入套餐价格', trigger: 'blur' }],
  image: [{ required: true, message: '请上传套餐图片', trigger: 'change' }]
}

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    if (queryStatus.value != null) params.status = queryStatus.value
    const data = await setmealPage(params)
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
    if (!url) throw new Error('空')
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
    const detail = await setmealGetById(row.id)
    Object.assign(form, {
      id: detail.id,
      name: detail.name,
      categoryId: detail.categoryId,
      price: Number(detail.price),
      image: detail.image,
      description: detail.description,
      setmealDishes: (detail.setmealDishes || []).map((sd) => ({
        dishId: sd.dishId,
        name: sd.name,
        copies: sd.copies
      }))
    })
  } catch (e) {}
}

async function openDishDialog() {
  await loadDishData()
  const existing = new Set(form.setmealDishes.map((d) => d.dishId))
  selectedDishIds.value = form.setmealDishes.map((d) => d.dishId)
  dishDialogVisible.value = true
  // 默认切到第一个分类
  if (dishCategories.value.length) {
    activeDishCategory.value = String(dishCategories.value[0].id)
  }
}

async function loadDishData() {
  if (dishCategories.value.length) return
  try {
    dishCategories.value = await categoryList(1)
    const map = {}
    for (const c of dishCategories.value) {
      map[c.id] = await dishListByCategory(c.id)
    }
    dishesMap.value = map
  } catch (e) {}
}

function removeDish(i) {
  form.setmealDishes.splice(i, 1)
}

function confirmSelectDish() {
  const selected = []
  for (const c of dishCategories.value) {
    const dishes = dishesMap.value[c.id] || []
    for (const d of dishes) {
      if (selectedDishIds.value.includes(d.id)) {
        selected.push({ dishId: d.id, name: d.name, copies: 1 })
      }
    }
  }
  // 保留已选但分类中未展示的（如已有回显）
  const idSet = new Set(selected.map((s) => s.dishId))
  form.setmealDishes.forEach((d) => {
    if (selectedDishIds.value.includes(d.dishId) && !idSet.has(d.dishId)) {
      selected.push(d)
    }
  })
  form.setmealDishes = selected
  dishDialogVisible.value = false
}

async function handleSubmit() {
  await formRef.value.validate()
  if (!form.setmealDishes.length) {
    ElMessage.warning('请选择包含菜品')
    return
  }
  saving.value = true
  try {
    const payload = {
      id: form.id,
      name: form.name,
      categoryId: form.categoryId,
      price: form.price,
      image: form.image,
      description: form.description,
      status: 1,
      setmealDishes: form.setmealDishes
    }
    if (form.id) {
      await setmealUpdate(payload)
      ElMessage.success('修改成功')
    } else {
      await setmealSave(payload)
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
    await setmealStatus(val ? 1 : 0, row.id)
    row.status = val ? 1 : 0
    ElMessage.success(val ? '已起售' : '已停售')
  } catch (e) {
  } finally {
    row._statusLoading = false
  }
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除套餐「${row.name}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      try {
        await setmealDelete([row.id])
        ElMessage.success('删除成功')
        load()
      } catch (e) {}
    })
    .catch(() => {})
}

onMounted(async () => {
  try {
    setmealCategories.value = await categoryList(2)
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
  width: 90px;
  height: 90px;
  border-radius: 6px;
  object-fit: cover;
  display: block;
}
.avatar-uploader :deep(.el-upload) {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  overflow: hidden;
  width: 90px;
  height: 90px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.2s;
}
.avatar-uploader :deep(.el-upload:hover) {
  border-color: #409eff;
}
.avatar-uploader-icon {
  font-size: 26px;
  color: #8c939d;
}
.up-tip {
  margin-left: 12px;
  color: #999;
  font-size: 12px;
}
.setmeal-dish-wrap {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.setmeal-dish-item {
  display: flex;
  align-items: center;
  gap: 12px;
}
.dish-name {
  width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dish-check-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.dish-check-item {
  font-size: 14px;
}
</style>