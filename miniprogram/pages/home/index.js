const app = getApp()
const api = require('../../common/api')

const CATS = [
  { name: '全部', ic: '🏠' },
  { name: '快餐', ic: '🍔' },
  { name: '甜点', ic: '🍰' },
  { name: '饮品', ic: '🥤' },
  { name: '炸鸡', ic: '🍗' },
  { name: '烧烤', ic: '🍢' },
  { name: '轻食', ic: '🥗' }
]

// Haversine 计算两点球面距离（公里）
function distanceKm(lat1, lng1, lat2, lng2) {
  const R = 6371
  const rad = d => d * Math.PI / 180
  const dLat = rad(lat2 - lat1)
  const dLng = rad(lng2 - lng1)
  const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2)
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

Page({
  data: {
    statusBarHeight: 20,
    keyword: '',
    cats: CATS,
    activeCat: '全部',
    stores: [],
    loading: true,
    showAuth: false,
    authKick: false,
    locationName: '天津科技大学',
    auth: { avatar: '/static/logo.png', nick: '', phone: '', loading: false },
    showLoc: false,
    isLocating: false,
    locInput: '',
    cities: [
      { name: '天津科技大学', lng: 117.2440, lat: 39.0280 },
      { name: '天津市', lng: 117.2000, lat: 39.0800 },
      { name: '北京市', lng: 116.4074, lat: 39.9042 },
      { name: '上海市', lng: 121.4737, lat: 31.2304 },
      { name: '广州市', lng: 113.2644, lat: 23.1291 },
      { name: '深圳市', lng: 114.0579, lat: 22.5431 }
    ]
  },

  onLoad() {
    const sys = wx.getWindowInfo ? wx.getWindowInfo() : wx.getSystemInfoSync()
    this.setData({ statusBarHeight: sys.statusBarHeight || 20 })
    const geo = wx.getStorageSync('geo')
    if (geo && geo.addressName) this.setData({ locationName: geo.addressName })
    app.ensureLogin().then(() => {
      this.checkProfile()
      this.locate()
      this.loadStores()
    }).catch(() => {
      this.checkProfile()
      this.locate()
      this.loadStores()
    })
  },

  // 通过 wx.getLocation 触发微信定位授权弹窗，用户点允许后获取经纬度并存到后端
  locate() {
    wx.getLocation({
      type: 'gcj02',
      success: res => {
        const geo = { longitude: res.longitude, latitude: res.latitude }
        wx.setStorageSync('geo', geo)
        api.updateLocation(res.longitude, res.latitude).catch(() => {})
        this.loadStores()
      },
      fail: () => {} // 用户拒绝/未开通定位，静默跳过，可在服务中引导开启
    })
  },

  // 点击左上角地址：优先微信地图选点；失败(权限未开通)则降级为自定义地址面板
  onChooseLocation() {
    wx.chooseLocation({
      success: res => {
        const name = res.address || res.name || '已选择位置'
        const geo = { longitude: res.longitude, latitude: res.latitude, addressName: name }
        wx.setStorageSync('geo', geo)
        this.setData({ locationName: name })
        api.updateLocation(res.longitude, res.latitude).catch(() => {})
        this.loadStores()
      },
      fail: () => this.openLocPanel()
    })
  },

  // 自定义地址选择面板（保证一定能弹）
  openLocPanel() {
    this.setData({ showLoc: true, locInput: '' })
  },
  closeLocPanel() { this.setData({ showLoc: false }) },
  onLocInput(e) { this.setData({ locInput: e.detail.value }) },
  noop() {},
  tapCity(e) {
    const c = e.currentTarget.dataset
    this.applyLoc(c.name, Number(c.lng), Number(c.lat))
  },
  // 用当前定位
  useGps() {
    this.setData({ isLocating: true })
    wx.getLocation({
      type: 'gcj02',
      success: res => {
        wx.reverseGeocoder({
          location: { latitude: res.latitude, longitude: res.longitude },
          success: r => this.applyLoc(r.result.address, res.longitude, res.latitude),
          fail: () => this.applyLoc('当前位置', res.longitude, res.latitude)
        })
      },
      fail: () => { this.setData({ isLocating: false }); wx.showToast({ title: '无法获取当前位置', icon: 'none' }) }
    })
  },
  // 手动输入地址
  confirmLoc() {
    const v = (this.data.locInput || '').trim()
    if (!v) { wx.showToast({ title: '请输入地址', icon: 'none' }); return }
    this.applyLoc(v, 0, 0)
  },
  applyLoc(name, lng, lat) {
    const geo = { longitude: lng, latitude: lat, addressName: name }
    wx.setStorageSync('geo', geo)
    this.setData({ locationName: name, showLoc: false, isLocating: false })
    if (lng && lat) {
      api.updateLocation(lng, lat).catch(() => {})
      this.loadStores()
    } else {
      wx.showToast({ title: '已切换地址', icon: 'success' })
    }
  },

  // 首次进入未完善资料时，进入原型风格的登录页（不再弹自定义授权框）
  checkProfile() {
    const ui = wx.getStorageSync('userInfo') || {}
    const needLogin = !wx.getStorageSync('profileDone') || !ui.name || !ui.phone
    if (needLogin && !this.data.authKick) {
      this.setData({ authKick: true })
      wx.navigateTo({ url: '/pages/login/login' })
    }
  },

  // 选择微信头像后自动上传，得到真实URL
  onChooseAvatar(e) {
    const path = e.detail.avatarUrl
    this.setData({ 'auth.avatar': path })
    api.uploadAvatar(path).then(url => {
      if (url) this.setData({ 'auth.avatar': url })
    }).catch(() => {})
  },

  onNick(e) { this.setData({ 'auth.nick': e.detail.value }) },
  onPhone(e) { this.setData({ 'auth.phone': e.detail.value }) },

  // 一键获取手机号：个人/未认证主体拿不到授权，回落手动填写
  onGetPhone(e) {
    const d = e.detail || {}
    if (!d.code) {
      wx.showModal({ title: '未拿到授权码', content: (d.errMsg || '') + '。\n若为个人主体/未认证小程序，微信不允许一键获取手机号，请手动填写。', showCancel: false })
      return
    }
    api.getPhoneByCode(d.code).then(p => {
      if (p) this.setData({ 'auth.phone': p })
      else wx.showModal({ title: '后端未返回手机号', content: '可能 code 无效。请手动填写。', showCancel: false })
    }).catch(err => wx.showModal({ title: '请求后端失败', content: String(err || '未知错误'), showCancel: false }))
  },

  submitAuth() {
    const name = (this.data.auth.nick || '').trim()
    const phone = (this.data.auth.phone || '').trim()
    if (!name) { wx.showToast({ title: '请填写昵称', icon: 'none' }); return }
    if (phone && !/^1\d{10}$/.test(phone)) { wx.showToast({ title: '手机号格式不正确', icon: 'none' }); return }
    if (this.data.auth.loading) return

    let avatar = this.data.auth.avatar || ''
    if (/^(wxfile|wxbridge|http:\/\/tmp)/.test(avatar)) avatar = ''
    this.setData({ 'auth.loading': true })

    api.updateUserInfo({ name, phone: phone || null, avatar })
      .then(() => {
        const ui = wx.getStorageSync('userInfo') || {}
        ui.name = name; ui.phone = phone; if (avatar) ui.avatar = avatar
        wx.setStorageSync('userInfo', ui)
        wx.setStorageSync('profileDone', 1)
        this.setData({ showAuth: false, 'auth.loading': false })
        wx.showToast({ title: '授权成功', icon: 'success' })
      })
      .catch(err => { this.setData({ 'auth.loading': false }); wx.showToast({ title: err || '保存失败', icon: 'none' }) })
  },

  skipAuth() {
    wx.setStorageSync('profileDone', 1)
    this.setData({ showAuth: false })
  },

  onPullDownRefresh() {
    this.loadStores().finally(() => wx.stopPullDownRefresh())
  },

  loadStores() {
    return api.getStoreList(this.data.keyword, this.data.activeCat === '全部' ? '' : this.data.activeCat)
      .then(list => {
        const geo = wx.getStorageSync('geo') || {}
        const myLng = Number(geo.longitude), myLat = Number(geo.latitude)
        const hasGeo = !!(myLng && myLat)
        let stores = (list || []).map((s, i) => {
          const tags = []
          tags.push({ t: '满' + (s.minPrice || 20) + '减' + (s.bonus || 8) + '', c: 'orange' })
          if (s.monthlySales >= 1000) tags.push({ t: '月售千单', c: 'green' })
          else if (s.monthlySales > 0) tags.push({ t: '回头客多', c: 'green' })
          else if (i % 2 === 0) tags.push({ t: '新客立减', c: 'orange' })
          const n = Object.assign({}, s, {
            img: api.img(s.photo),
            closed: s.status === 0,
            tags
          })
          const slng = Number(s.longitude), slat = Number(s.latitude)
          if (hasGeo && slng && slat) {
            n.distanceKm = Math.round(distanceKm(myLat, myLng, slat, slng) * 100) / 100
          }
          return n
        })
        if (hasGeo) stores.sort((a, b) => (a.distanceKm || 0) - (b.distanceKm || 0))
        this.setData({ stores, loading: false })
      })
      .catch(() => this.setData({ stores: [], loading: false }))
  },

  onSearch(e) { this.setData({ keyword: e.detail.value }, () => this.loadStores()) },
  clearSearch() { this.setData({ keyword: '' }, () => this.loadStores()) },
  tapCat(e) { this.setData({ activeCat: e.currentTarget.dataset.c }, () => this.loadStores()) },
  coupon() {
    wx.showToast({ title: '已领取，下单自动抵扣', icon: 'none' })
  },
  goMore() { wx.showToast({ title: '正在精选更多好店', icon: 'none' }) },
  goStore(e) {
    const { id, name } = e.currentTarget.dataset
    wx.navigateTo({ url: `/pages/index/index?storeId=${id}&storeName=${encodeURIComponent(name || '')}` })
  }
})