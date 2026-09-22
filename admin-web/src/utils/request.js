import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken, removeStoreId } from '@/utils/auth'
import router from '@/router'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/',
  timeout: 15000
})

// 请求拦截器：自动携带管理端 JWT
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers['token'] = token
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一处理 code
service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端统一返回 { code, msg, data }，仅供参考，若结构不同可调整
    if (res && typeof res === 'object' && 'code' in res) {
      if (res.code === 1) {
        return res.data
      }
      // 401 或未认证：清理凭证并回登录
      if (res.code === 0 && (res.msg || '').includes('未登录') || res.code === 401) {
        removeToken()
        removeStoreId()
        router.push('/login')
      }
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || 'Error'))
    }
    return res
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      removeToken()
      removeStoreId()
      router.push('/login')
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default service