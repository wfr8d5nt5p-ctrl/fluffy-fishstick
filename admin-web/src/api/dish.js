import request from '@/utils/request'

const BASE = '/admin/dish'

export function dishPage(params) {
  return request.get(`${BASE}/page`, { params })
}

export function dishSave(data) {
  return request.post(BASE, data)
}

export function dishUpdate(data) {
  return request.put(BASE, data)
}

export function dishDelete(ids) {
  return request.delete(BASE, { params: { ids: ids.join(',') } })
}

export function dishGetById(id) {
  return request.get(`${BASE}/${id}`)
}

export function dishStatus(status, id) {
  return request.post(`${BASE}/status/${status}`, null, { params: { id } })
}

// 按分类查询菜品（套餐选择菜品弹窗）
export function dishListByCategory(categoryId) {
  return request.get(`${BASE}/list`, { params: { categoryId } })
}