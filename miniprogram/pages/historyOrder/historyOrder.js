const app = getApp()
const api = require('../../common/api')

const STATUS_TEXT = { 1: '待付款', 2: '待接单', 3: '待配送', 4: '配送中', 5: '已完成', 6: '已取消' }
const COLORS = ['#FF7A35', '#4A90D9', '#2FB98A', '#F5A623', '#E6573C', '#8A6BBE', '#3DB9A8', '#5B8DEF', '#E86E8A']

Page({
  data: {
    tabs: [
      { key: '', name: '全部' },
      { key: '1', name: '待付款' },
      { key: '2', name: '待接单' },
      { key: '3', name: '待配送' },
      { key: '5', name: '已完成' },
      { key: '6', name: '已取消' }
    ],
    activeKey: '',
    list: [],
    loading: true
  },

  onShow() {
    app.ensureLogin().then(() => this._load())
  },

  switchTab(e) {
    const key = e.currentTarget.dataset.key
    this.setData({ activeKey: key })
    this._load()
  },

  goAgain(e) {
    const { storeid, name } = e.currentTarget.dataset
    if (!storeid) return
    wx.navigateTo({ url: '/pages/index/index?storeId=' + storeid + '&storeName=' + encodeURIComponent(name || '') })
  },

  _load() {
    this.setData({ loading: true })
    const status = this.data.activeKey || ''
    Promise.all([
      api.get('/user/order/history' + (status ? '?status=' + status : '')),
      api.getStoreList('', '').catch(() => [])
    ]).then(([list, stores]) => {
      const storeMap = {}
      ;(stores || []).forEach((s, i) => {
        storeMap[s.id] = {
          name: s.name || 'ConeEats',
          photo: s.photo ? api.img(s.photo) : '',
          color: COLORS[i % COLORS.length]
        }
      })
      const items = (list || []).map(o => {
        const st = storeMap[o.storeId]
        const name = (st && st.name) || 'ConeEats'
        return {
          id: o.id,
          number: o.number,
          status: o.status,
          statusText: STATUS_TEXT[o.status] || '未知',
          time: (o.orderTime || '').replace('T', ' ').slice(0, 16),
          amount: (o.amount || 0).toFixed(2),
          address: o.address || '',
          storeId: o.storeId,
          storeName: name,
          storeImage: (st && st.photo) || '',
          storeColor: (st && st.color) || '#FF7A35',
          storeInitial: (name[0] || '蛋')
        }
      })
      this.setData({ list: items })
    }).catch(() => this.setData({ list: [] }))
      .finally(() => this.setData({ loading: false }))
  },

  goOrder() { wx.switchTab({ url: '/pages/index/index' }) },

  onPullDownRefresh() {
    this._load().finally(() => wx.stopPullDownRefresh())
  }
})