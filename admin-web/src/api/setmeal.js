import request from '@/utils/request'

const BASE = '/admin/setmeal'

export function setmealPage(params) {
  return request.get(`${BASE}/page`, { params })
}

export function setmealSave(data) {
  return request.post(BASE, data)
}

export function setmealUpdate(data) {
  return request.put(BASE, data)
}

export function setmealDelete(ids) {
  return request.delete(BASE, { params: { ids: ids.join(',') } })
}

export function setmealGetById(id) {
  return request.get(`${BASE}/${id}`)
}

export function setmealStatus(status, id) {
  return request.post(`${BASE}/status/${status}`, null, { params: { id } })
}