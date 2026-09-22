// ConeEats - 请求封装与接口定义
const BASE_URL = 'http://localhost:8080'   // TODO: 上线时改为服务器域名

// 统一请求
function request(method, url, data = {}, header = {}) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token')
    wx.request({
      url: BASE_URL + url,
      method,
      data,
      header: Object.assign({ 'Content-Type': 'application/json' }, token ? { authentication: token } : {}, header),
      success(res) {
        if (res.statusCode === 200) {
          const body = res.data
          if (body.code === 1) return resolve(body.data)
          // 未登录/令牌失效：清掉旧 token，标记需重新登录
          if (body.code === 0 && /401|token|未登录|登录/i.test(body.msg || '')) {
            wx.removeStorageSync('token')
            wx.setStorageSync('tokenInvalid', 1)
          }
          reject(body.msg || '请求失败')
        } else if (res.statusCode === 401) {
          wx.removeStorageSync('token')
          wx.setStorageSync('tokenInvalid', 1)
          reject('登录已失效，请重新登录')
        } else {
          reject('服务异常(' + res.statusCode + ')')
        }
      },
      fail() { reject('网络连接失败，请检查后端服务') }
    })
  })
}

const get = (u, d) => request('GET', u, d)
const post = (u, d) => request('POST', u, d)
const put = (u, d) => request('PUT', u, d)
const del = (u, d) => request('DELETE', u, d)

// 微信登录：本地 token 有效则复用；失效/缺失则重新 wx.login 换新
function login() {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token')
    const invalid = wx.getStorageSync('tokenInvalid')
    if (token && !invalid) return resolve(token)
    wx.login({
      success(res) {
        post('/user/user/login', { code: res.code })
          .then(user => {
            wx.setStorageSync('token', user.token)
            wx.setStorageSync('userInfo', user)
            wx.removeStorageSync('tokenInvalid')
            resolve(user.token)
          })
          .catch(reject)
      },
      fail: reject
    })
  })
}

// 商家列表（附近好店，支持关键词/主营分类筛选）
function getStoreList(keyword, category) {
  const q = []
  if (keyword) q.push('keyword=' + encodeURIComponent(keyword))
  if (category) q.push('category=' + encodeURIComponent(category))
  return get('/user/store/list' + (q.length ? '?' + q.join('&') : ''))
}

// 店铺详情及菜单（进店点单）
function getStoreMenu(id) { return get('/user/store/' + id + '/menu') }

// 拼接图片完整地址（后端返回相对路径）
function img(u) {
  if (!u) return '/static/logo.png'
  if (/^https?:\/\//.test(u)) return u
  return BASE_URL + (u[0] === '/' ? '' : '/') + u
}

// 完善用户资料（昵称/手机号/头像）
function updateUserInfo(user) { return put('/user/user/info', user) }

// 获取当前用户资料（含最近登录IP）
function getUserInfo() { return get('/user/user/info') }

// 通过微信code换取手机号
function getPhoneByCode(code) { return post('/user/user/phone', { code }) }

// 上传头像，返回完整可访问地址
function uploadAvatar(filePath) {
  const token = wx.getStorageSync('token')
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: BASE_URL + '/user/common/upload',
      filePath,
      name: 'file',
      header: token ? { authentication: token } : {},
      success(res) {
        try {
          const body = JSON.parse(res.data)
          if (body.code === 1) return resolve(body.data)
          reject(body.msg || '上传失败')
        } catch (e) { reject('上传返回异常') }
      },
      fail() { reject('上传失败，请检查后端服务') }
    })
  })
}

// 保存用户当前定位（经纬度）
function updateLocation(longitude, latitude) { return put('/user/user/location', { longitude: longitude || 0, latitude: latitude || 0 }) }

module.exports = {
  BASE_URL, login, get, post, put, del, img, getStoreList, getStoreMenu, updateUserInfo, getUserInfo, getPhoneByCode, uploadAvatar, updateLocation
}