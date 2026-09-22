const app = getApp()
const api = require('../../common/api')

Page({
  data: {
    statusBarHeight: 20,
    storeId: null,
    store: null,          // 店铺信息 StoreVO
    menus: [],            // 该店分类菜单 CategoryMenu[]
    activeIndex: 0,
    items: [],            // 当前分类下的 菜品+套餐 合并列表 {id,type,name,image,price,desc,cart}
    cartCount: 0,
    cartTotal: 0,         // 购物车实时总金额
    minPrice: 0,          // 起送价
    diff: 0,              // 距离起送还差多少
    reachMin: false,      // 是否达到起送价
    showCart: false,      // 购物车明细弹层
    cartItems: [],        // 弹层明细列表 {key,type,id,name,price,num}
    loading: true
  },

  onLoad(options) {
    const sys = wx.getWindowInfo ? wx.getWindowInfo() : wx.getSystemInfoSync()
    this.setData({ statusBarHeight: sys.statusBarHeight || 20 })
    const storeId = Number(options.storeId)
    this.setData({ storeId })
    app.globalData.currentStoreId = storeId
    app.globalData.currentStore = { id: storeId, name: decodeURIComponent(options.storeName || '') }
    app.ensureLogin().then(() => this._load()).catch(() => this._load())
  },

  onShow() {
    if (this.data.storeId) this._refreshCart()
  },

  _load() {
    this.setData({ loading: true })
    // 若购物车里有其他店铺的商品，先清空（换店点餐）
    return api.get('/user/shoppingCart/list')
      .then(list => {
        const foreign = (list || []).find(it => it.storeId && it.storeId !== this.data.storeId)
        if (foreign) return api.del('/user/shoppingCart/clean')
      })
      .then(() => api.getStoreMenu(this.data.storeId))
      .then(menu => {
        menu = menu || {}
        this.setData({ store: menu.store || null, menus: menu.menus || [] })
        this._computeTotal()
        this.selectCat(0)
      })
      .catch(() => {})
      .finally(() => this.setData({ loading: false }))
  },

  selectCat(e) {
    const idx = typeof e === 'number' ? e : e.currentTarget.dataset.idx
    const menu = this.data.menus[idx]
    if (!menu) { this.setData({ activeIndex: idx, items: [] }); return }
    const items = []
    ;(menu.dishes || []).forEach(d => items.push({
      id: d.id, type: 1, name: d.name, image: api.img(d.image),
      price: d.price, desc: d.description || '', cart: this._cartNum(d.id, 1)
    }))
    ;(menu.setmeals || []).forEach(s => items.push({
      id: s.id, type: 2, name: s.name, image: api.img(s.image),
      price: s.price, desc: s.description || '', cart: this._cartNum(s.id, 2)
    }))
    this.setData({ activeIndex: idx, items })
  },

  _cartNum(id, type) {
    const k = (type === 2 ? 'setmeal' : 'dish') + id
    return (this._cartMap && this._cartMap[k]) ? this._cartMap[k].number : 0
  },

  _refreshCart() {
    return api.get('/user/shoppingCart/list').then(list => {
      const map = {}
      let count = 0
      let total = 0
      ;(list || []).forEach(it => {
        const key = it.dishId ? 'dish' + it.dishId : 'setmeal' + it.setmealId
        map[key] = it
        count += it.number || 0
        total += (Number(it.amount) || 0) * (it.number || 0)
      })
      this._cartMap = map
      const cItems = (list || []).map(it => ({
        key: it.dishId ? 'dish' + it.dishId : 'setmeal' + it.setmealId,
        type: it.dishId ? 1 : 2,
        id: it.dishId || it.setmealId,
        name: it.name,
        price: Number(it.amount) || 0,
        num: it.number || 0
      }))
      this.setData({
        cartCount: count,
        cartTotal: Math.round(total * 100) / 100,
        cartItems: cItems,
        items: this.data.items.map(it => Object.assign({}, it, { cart: this._cartNum(it.id, it.type) }))
      })
      this._computeTotal()
    }).catch(() => {})
  },

  // 依据总金额与起送价，计算差额与是否达标
  _computeTotal() {
    const min = Number(this.data.store && this.data.store.minPrice) || 0
    const total = this.data.cartTotal || 0
    this.setData({
      minPrice: min,
      diff: Math.max(0, Math.round((min - total) * 100) / 100),
      reachMin: total >= min
    })
  },

  add(e) {
    const { id, type } = e.currentTarget.dataset
    const body = type == 2
      ? { setmealId: Number(id), storeId: this.data.storeId }
      : { dishId: Number(id), storeId: this.data.storeId }
    api.post('/user/shoppingCart/add', body)
      .then(() => this._refreshCart())
      .catch(err => wx.showToast({ title: err, icon: 'none' }))
  },

  sub(e) {
    const { id, type } = e.currentTarget.dataset
    const body = type == 2 ? { setmealId: Number(id) } : { dishId: Number(id) }
    api.post('/user/shoppingCart/sub', body)
      .then(() => this._refreshCart())
      .catch(err => wx.showToast({ title: err, icon: 'none' }))
  },

  goCart() {
    if (!this.data.cartCount) return wx.showToast({ title: '购物车还是空的', icon: 'none' })
    this.setData({ showCart: true })
  },

  closeCart() { this.setData({ showCart: false }) },
  noop() {},

  // 弹层内加减：复用 add/sub 的 data-id/data-type 参数
  cartAdd(e) { this.add(e) },
  cartSub(e) { this.sub(e) },

  // 清空购物车
  cartClean() {
    api.del('/user/shoppingCart/clean')
      .then(() => this._refreshCart())
      .catch(err => wx.showToast({ title: err, icon: 'none' }))
  },

  // 弹层内去结算
  goCheckout() {
    if (!this.data.reachMin)
      return wx.showToast({ title: '还差 ¥' + this.data.diff + ' 起送', icon: 'none' })
    this.setData({ showCart: false })
    const name = (this.data.store && this.data.store.name) || ''
    wx.navigateTo({ url: `/pages/order/index?storeId=${this.data.storeId}&storeName=${encodeURIComponent(name)}` })
  },

  goBack() { wx.navigateBack() },

  onPullDownRefresh() { this._load().finally(() => wx.stopPullDownRefresh()) }
})