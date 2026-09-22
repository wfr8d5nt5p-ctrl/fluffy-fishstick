const api = require('../../common/api')

Page({
  data: {
    phone: '',
    code: '',
    countdown: 60,
    counting: false,
    agreed: false,
    loading: false,
    ip: ''
  },

  onLoad() {
    const ui = wx.getStorageSync('userInfo') || {}
    if (ui.phone) this.setData({ phone: ui.phone })
    api.getUserInfo().then(u => {
      if (u && u.lastLoginIp) this.setData({ ip: u.lastLoginIp })
    }).catch(() => {})
  },

  onPhone(e) { this.setData({ phone: e.detail.value.replace(/\D/g, '').slice(0, 11) }) },
  onCode(e) { this.setData({ code: e.detail.value.replace(/\D/g, '').slice(0, 6) }) },

  toggleAgree() { this.setData({ agreed: !this.data.agreed }) },

  // 获取验证码：演示逻辑，校验手机号后开始倒计时
  getCode() {
    if (this.data.counting) return
    if (!/^1\d{10}$/.test(this.data.phone)) {
      return wx.showToast({ title: '请先输入正确的手机号', icon: 'none' })
    }
    wx.showToast({ title: '验证码已发送（演示）', icon: 'none' })
    this.setData({ counting: true, countdown: 60 })
    this._timer = setInterval(() => {
      if (this.data.countdown <= 1) {
        clearInterval(this._timer)
        this.setData({ counting: false, countdown: 60 })
      } else {
        this.setData({ countdown: this.data.countdown - 1 })
      }
    }, 1000)
  },

  submit() {
    if (this.data.loading) return
    if (!this.data.agreed) return wx.showToast({ title: '请先勾选并同意协议', icon: 'none' })
    if (!/^1\d{10}$/.test(this.data.phone)) return wx.showToast({ title: '手机号格式不正确', icon: 'none' })
    if (!/^\d{6}$/.test(this.data.code)) return wx.showToast({ title: '请输入6位验证码', icon: 'none' })

    const ui = wx.getStorageSync('userInfo') || {}
    this.setData({ loading: true })
    api.updateUserInfo({ name: ui.name || '微信用户', phone: this.data.phone, avatar: ui.avatar || '' })
      .then(() => {
        ui.phone = this.data.phone
        wx.setStorageSync('userInfo', ui)
        wx.setStorageSync('profileDone', 1)
        wx.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => wx.reLaunch({ url: '/pages/home/index' }), 600)
      })
      .catch(err => wx.showToast({ title: err || '登录失败', icon: 'none' }))
      .finally(() => this.setData({ loading: false }))
  },

  // 微信一键登录：静默换取手机号
  wxLogin(e) {
    const d = e.detail || {}
    if (!d.code) {
      return wx.showModal({
        title: '未拿到授权码',
        content: (d.errMsg || '') + '。\n若为个人主体/未认证小程序，微信不允许一键获取手机号，请选择手机号验证码登录。',
        showCancel: false
      })
    }
    wx.showLoading({ title: '登录中…' })
    api.getPhoneByCode(d.code)
      .then(p => {
        const ui = wx.getStorageSync('userInfo') || {}
        ui.phone = p || ''
        wx.setStorageSync('userInfo', ui)
        wx.setStorageSync('profileDone', 1)
        wx.hideLoading()
        wx.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => wx.reLaunch({ url: '/pages/home/index' }), 600)
      })
      .catch(err => {
        wx.hideLoading()
        wx.showModal({ title: '请求后端失败', content: String(err || '未知错误'), showCancel: false })
      })
  },

  onUnload() { if (this._timer) clearInterval(this._timer) }
})