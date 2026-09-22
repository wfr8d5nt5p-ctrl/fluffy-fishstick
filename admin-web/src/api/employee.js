import request from '@/utils/request'

const BASE = '/admin/employee'

export function employeeLogin(data) {
  return request.post(`${BASE}/login`, data)
}

export function employeeLogout() {
  return request.post(`${BASE}/logout`)
}

export function employeePage(params) {
  return request.get(`${BASE}/page`, { params })
}

export function employeeSave(data) {
  return request.post(BASE, data)
}

export function employeeUpdate(data) {
  return request.put(BASE, data)
}

export function employeeGetById(id) {
  return request.get(`${BASE}/${id}`)
}

export function employeeStatus(status, id) {
  return request.post(`${BASE}/status/${status}`, null, { params: { id } })
}