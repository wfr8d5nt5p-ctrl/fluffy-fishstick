import request from '@/utils/request'

const BASE = '/admin/shop'

// 获取店铺营业状态
export function shopStatus() {
  return request.get(`${BASE}/status`)
}

// 设置营业状态；status 1 营业 0 打烊；可用 url 路径或 query 传参
export function setShopStatus(status) {
  return request.put(`${BASE}/${status}`)
}