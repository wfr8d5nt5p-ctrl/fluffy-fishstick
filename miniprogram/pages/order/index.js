const app = getApp()
const api = require('../../common/api')

Page({
  data: {
    items: [],
    address: null,
    storeId: null,
    storeName: '',
    remark: '',
    tableware: 1,           // 1按餐量 0自定义
    foodCount: 0,
    total: '0.00',
    loading: false
  },

  onLoad(options) {
    this.setData({ storeId: Number(options.storeId), storeName: decodeURIComponent(options.storeName || '') })
  },

  onShow() {
    app.ensureLogin()
      .then(() => Promise.all([this._loadCart(), this._loadAddress()]))
  },

  _loadCart() {
    return api.get('/user/shoppingCart/list').then(list => {
      const items = (list || []).map(it => ({
        id: it.dishId || it.setmealId,
        kind: it.dishId ? 'dish' : 'setmeal',
        dishId: it.dishId, setmealId: it.setmealId,
        name: it.name, image: api.img(it.image), price: it.amount || it.price,
        number: it.number || 0, flavor: it.dishFlavor || ''
      }))
      this._recalc(items)
    }).catch(() => this._recalc([]))
  },

  _recalc(items) {
    const foodCount = items.reduce((s, it) => s + it.number, 0)
    const total = items.reduce((s, it) => s + (it.price || 0) * it.number, 0)
    this.setData({ items, foodCount, total: total.toFixed(2) })
  },

  _loadAddress() {
    // 优先取刚选择的地址
    const selected = wx.getStorageSync('selectedAddress')
    if (selected) { this.setData({ address: selected }); return Promise.resolve() }
    return api.get('/user/addressBook/list').then(list => {
      const def = (list || []).find(a => a.isDefault) || (list || [])[0]
      this.setData({ address: def || null })
    }).catch(() => {})
  },

  sub(e) { this._change(e.currentTarget.dataset, -1) },
  add(e) { this._change(e.currentTarget.dataset, 1) },

  _change(item, delta) {
    const { dishid, setmealid, n } = item
    if (delta < 0 && Number(n) <= 1) {
      const body = dishid ? { dishId: Number(dishid) } : { setmealId: Number(setmealid) }
      return api.post('/user/shoppingCart/sub', body).then(() => this._loadCart())
    }
    const body = (dishid ? { dishId: Number(dishid), number: 1 } : { setmealId: Number(setmealid), number: 1 })
    api.post('/user/shoppingCart/add', body).then(() => this._loadCart())
      .catch(err => wx.showToast({ title: err, icon: 'none' }))
  },

  clearCart() {
    wx.showModal({ title: '提示', content: '确定清空购物车吗？', success: (r) => {
      if (r.confirm) api.del('/user/shoppingCart/clean').then(() => this._loadCart())
    }})
  },

  chooseAddress() {
    wx.navigateTo({ url: '/pages/address/address?select=1' })
  },

  onRemark(e) { this.setData({ remark: e.detail.value }) },
  onTableware(e) { this.setData({ tableware: e.currentTarget.dataset.v }) },

  submit() {
    if (this.data.loading) return
    if (!this.data.address) return wx.showToast({ title: '请先选择收货地址', icon: 'none' })
    if (!this.data.items.length) return wx.showToast({ title: '购物车是空的', icon: 'none' })

    const now = new Date(Date.now() + 30 * 60000)
    const pad = n => (n < 10 ? '0' + n : '' + n)
    const estimatedDeliveryTime = `${now.getFullYear()}-${pad(now.getMonth()+1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`

    this.setData({ loading: true })
    api.post('/user/order/submit', {
      addressBookId: this.data.address.id,
      storeId: this.data.storeId,
      payMethod: 1,
      remark: this.data.remark,
      estimatedDeliveryTime,
      deliveryStatus: 1,
      tablewareStatus: this.data.tableware,
      packAmount: 0,
      amount: Number(this.data.total)
    }).then(res => {
      wx.removeStorageSync('selectedAddress')
      wx.navigateTo({ url: `/pages/pay/index?orderNumber=${res.orderNumber}&amount=${this.data.total}` })
    }).catch(err => wx.showToast({ title: err, icon: 'none' }))
      .finally(() => this.setData({ loading: false }))
  }
})