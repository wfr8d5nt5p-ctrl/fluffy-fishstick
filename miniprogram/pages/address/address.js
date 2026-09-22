const app = getApp()
const api = require('../../common/api')

Page({
  data: { list: [], select: false, loading: true },

  onLoad(q) { this.setData({ select: q.select == 1 }) },

  onShow() {
    app.ensureLogin().then(() => this._load())
  },

  _load() {
    this.setData({ loading: true })
    api.get('/user/addressBook/list').then(list => this.setData({ list: list || [] }))
      .catch(() => {})
      .finally(() => this.setData({ loading: false }))
  },

  choose(e) {
    if (!this.data.select) return
    const addr = this.data.list[e.currentTarget.dataset.idx]
    wx.setStorageSync('selectedAddress', addr)
    wx.navigateBack()
  },

  edit(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/addOrEditAddress/addOrEditAddress?id=' + id })
  },

  del(e) {
    const { idx, id } = e.currentTarget.dataset
    wx.showModal({
      title: '提示', content: '确定删除该地址吗？',
      success: (r) => r.confirm && api.del('/user/addressBook/' + id)
        .then(() => this.setData({ list: this.data.list.filter((_, i) => i !== idx) }))
        .catch(err => wx.showToast({ title: err, icon: 'none' }))
    })
  },

  setDefault(e) {
    const { idx, id } = e.currentTarget.dataset
    api.put('/user/addressBook/default', { id })
      .then(() => {
        const list = this.data.list.map((a, i) => ({ ...a, isDefault: i === idx }))
        this.setData({ list })
      })
      .catch(err => wx.showToast({ title: err, icon: 'none' }))
  },

  add() { wx.navigateTo({ url: '/pages/addOrEditAddress/addOrEditAddress' }) }
})