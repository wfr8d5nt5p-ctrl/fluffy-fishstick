// ConeEats - 全局
const api = require('./common/api')

App({
  globalData: {
    token: '',
    userInfo: null,
    shopStatus: 1,     // 1营业 0打烊
    cartCount: 0,
    currentStoreId: null,   // 当前进店点单的店铺id
    currentStore: null      // 当前店铺信息
  },

  onLaunch() {
    // 尝试静默登录（仅首次取 token）
    api.login().then(token => {
      this.globalData.token = token
      api.get('/user/shop/status').then(status => {
        this.globalData.shopStatus = status
      }).catch(() => {})
      this.syncCartCount()
    }).catch(() => {})
  },

  // 登录（供页面显式调用）
  ensureLogin() {
    return api.login().then(token => (this.globalData.token = token))
  },

  // 同步购物车数量
  syncCartCount() {
    api.get('/user/shoppingCart/list').then(list => {
      const count = (list || []).reduce((s, it) => s + (it.number || 0), 0)
      this.globalData.cartCount = count
    }).catch(() => {})
  }
})