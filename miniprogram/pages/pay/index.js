const app = getApp()
const api = require('../../common/api')

Page({
  data: {
    orderNumber: '',
    amount: '0.00',
    paying: false
  },

  onLoad(q) {
    this.setData({ orderNumber: q.orderNumber || '', amount: q.amount || '0.00' })
  },

  // 模拟微信支付：调用支付下单 + 支付成功回调（触发来单提醒）
  pay() {
    if (this.data.paying) return
    this.setData({ paying: true })
    wx.showLoading({ title: '支付中…', mask: true })
    api.post('/user/order/payment', { orderNumber: this.data.orderNumber, payMethod: 1 })
      .then(() => api.post('/user/order/notify', { orderNumber: this.data.orderNumber }))
      .then(() => {
        wx.hideLoading()
        // 清空已支付的购物车
        api.del('/user/shoppingCart/clean').catch(() => {})
        app.syncCartCount()
        wx.redirectTo({ url: `/pages/success/index?orderNumber=${this.data.orderNumber}&amount=${this.data.amount}` })
      })
      .catch(err => {
        wx.hideLoading()
        wx.showModal({ title: '支付失败', content: err, showCancel: false })
      })
      .finally(() => this.setData({ paying: false }))
  },

  laterPay() {
    wx.showModal({ title: '提示', content: '可在「我的-订单」中继续支付', showCancel: false, success: () => {
      wx.switchTab({ url: '/pages/historyOrder/historyOrder' })
    }})
  }
})