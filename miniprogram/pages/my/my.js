const app = getApp()
const api = require('../../common/api')

Page({
  data: { name: '微信用户', phone: '', ip: '', avatar: '/static/avatar.png', frequentStores: [] },

  onShow() {
    // 先确保登录状态有效（token 失效时自动换新），再拉资料
    app.ensureLogin()
      .then(() => { this.loadUser(); this.loadFrequent() })
      .catch(() => { this.loadUser(); this.loadFrequent() })
  },

  loadUser() {
    const userInfo = wx.getStorageSync('userInfo')
    if (userInfo && (userInfo.phone || userInfo.name)) {
      this.setData({
        name: userInfo.name || '微信用户',
        phone: userInfo.phone || '',
        avatar: userInfo.avatar ? api.img(userInfo.avatar) : '/static/avatar.png'
      })
    }
    // 拉取最新资料（含最近登录IP）
    api.getUserInfo().then(u => {
      if (!u) return
      this.setData({
        name: u.name || this.data.name,
        phone: u.phone || '',
        ip: u.lastLoginIp || '',
        avatar: u.avatar ? api.img(u.avatar) : this.data.avatar
      })
      wx.setStorageSync('userInfo', u)
    }).catch(() => {})
  },

  orders() { wx.switchTab({ url: '/pages/historyOrder/historyOrder' }) },
  address() { wx.navigateTo({ url: '/pages/address/address' }) },
  edit() { wx.navigateTo({ url: '/pages/login/login' }) },

  coupon() { wx.showToast({ title: '暂无可用优惠券', icon: 'none' }) },

  service() {
    wx.showModal({ title: '客服中心', content: '在线客服时间 09:00 - 22:00\n欢迎咨询订单、退款等任何问题。', showCancel: false })
  },

  security() {
    wx.showModal({ title: '账号与安全', content: '当前为微信快捷登录\n支持更换手机号、解绑等操作。', showCancel: false })
  },

  about() {
    wx.showModal({ title: 'ConeEats', content: 'ConeEats · 美食到家\n让每道好味从出餐到入口都不失温度。', showCancel: false })
  },

  // 常点店铺：取商家列表前3家作演示
  loadFrequent() {
    api.getStoreList('', '').then(list => {
      const items = (list || []).slice(0, 3).map((s, i) => ({
        id: s.id,
        name: s.name || 'ConeEats',
        img: s.photo ? api.img(s.photo) : '',
        last: ['上次点过：冰火两重天', '上次点过：当季果切拼盘', '上次点过：招牌套餐'][i] || '常点好店'
      }))
      this.setData({ frequentStores: items })
    }).catch(() => {})
  },

  goStore(e) {
    const { id, name } = e.currentTarget.dataset
    if (!id) return
    wx.navigateTo({ url: '/pages/index/index?storeId=' + id + '&storeName=' + encodeURIComponent(name || '') })
  },
  moreStores() { wx.switchTab({ url: '/pages/home/index' }) },

  logout() {
    wx.showModal({
      title: '提示', content: '确定退出登录吗？',
      success: r => {
        if (!r.confirm) return
        wx.removeStorageSync('token')
        wx.removeStorageSync('userInfo')
        wx.removeStorageSync('profileDone')
        this.setData({ name: '微信用户', phone: '', ip: '', avatar: '/static/avatar.png' })
        app.globalData.token = ''
        wx.showToast({ title: '已退出', icon: 'success' })
      }
    })
  }
})