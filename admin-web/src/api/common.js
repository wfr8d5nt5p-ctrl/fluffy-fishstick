import request from '@/utils/request'

const BASE = '/admin/common'

// 上传图片，formData: { file }
export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`${BASE}/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}