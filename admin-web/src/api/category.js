import request from '@/utils/request'

const BASE = '/admin/category'

export function categoryPage(params) {
  return request.get(`${BASE}/page`, { params })
}

export function categorySave(data) {
  return request.post(BASE, data)
}

export function categoryUpdate(data) {
  return request.put(BASE, data)
}

export function categoryDelete(id) {
  return request.delete(BASE, { params: { id } })
}

export function categoryStatus(status, id) {
  return request.post(`${BASE}/status/${status}`, null, { params: { id } })
}

// 根据类型查询分类（1 菜品分类，2 套餐分类）
export function categoryList(type) {
  return request.get(`${BASE}/list`, { params: { type } })
}