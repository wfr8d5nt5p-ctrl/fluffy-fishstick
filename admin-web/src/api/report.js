import request from '@/utils/request'

const BASE = '/admin/report'

// 营业额统计，params: { begin, end }
export function turnoverStatistics(params) {
  return request.get(`${BASE}/turnoverStatistics`, { params })
}

// 用户统计
export function userStatistics(params) {
  return request.get(`${BASE}/userStatistics`, { params })
}

// 订单统计
export function orderStatistics(params) {
  return request.get(`${BASE}/orderStatistics`, { params })
}

// 导出运营数据报表（返回 blob，前端触发下载）
export function exportBusiness(params) {
  return request.get(`${BASE}/export`, { params, responseType: 'blob' })
}