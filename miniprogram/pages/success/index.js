Page({
  data: { orderNumber: '', amount: '0.00' },
  onLoad(q) { this.setData({ orderNumber: q.orderNumber || '', amount: q.amount || '0.00' }) },
  viewOrder() { wx.switchTab({ url: '/pages/historyOrder/historyOrder' }) },
  backHome() { wx.switchTab({ url: '/pages/index/index' }) }
})