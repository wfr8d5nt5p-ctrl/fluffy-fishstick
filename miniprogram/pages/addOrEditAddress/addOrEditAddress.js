const app = getApp()
const api = require('../../common/api')

Page({
  data: {
    id: null,
    form: {
      consignee: '', sex: '1', phone: '',
      provinceName: '', cityName: '', districtName: '',
      detail: '', label: '家', isDefault: 0
    },
    region: ['', '', ''],
    labels: ['家', '公司', '学校'],
    saving: false
  },

  onLoad(q) {
    this.setData({ id: q.id || null })
    if (q.id) {
      api.get('/user/addressBook/' + q.id).then(a => {
        this.setData({
          form: {
            consignee: a.consignee, sex: (a.sex || '1'), phone: a.phone,
            provinceName: a.provinceName, cityName: a.cityName, districtName: a.districtName,
            detail: a.detail, label: a.label || '家', isDefault: a.isDefault
          },
          region: [a.provinceName, a.cityName, a.districtName].filter(Boolean)
        })
      }).catch(() => {})
    }
  },

  onInput(e) {
    const key = e.currentTarget.dataset.k
    this.setData({ [`form.${key}`]: e.detail.value })
  },

  onSex(e) { this.setData({ 'form.sex': e.currentTarget.dataset.v }) },

  onRegion(e) {
    const [p, c, d] = e.detail.value
    this.setData({ region: e.detail.value, 'form.provinceName': p, 'form.cityName': c, 'form.districtName': d })
  },

  onLabel(e) { this.setData({ 'form.label': e.currentTarget.dataset.v }) },

  onDefault(e) { this.setData({ 'form.isDefault': e.detail.value ? 1 : 0 }) },

  save() {
    const f = this.data.form
    if (!f.consignee) return wx.showToast({ title: '请填写收货人', icon: 'none' })
    if (!/^1\d{10}$/.test(f.phone)) return wx.showToast({ title: '请填写正确手机号', icon: 'none' })
    if (!f.detail) return wx.showToast({ title: '请填写详细地址', icon: 'none' })
    if (this.data.saving) return
    this.setData({ saving: true })

    const body = {
      consignee: f.consignee, sex: f.sex, phone: f.phone,
      provinceName: f.provinceName, cityName: f.cityName, districtName: f.districtName,
      detail: f.detail, label: f.label, isDefault: f.isDefault
    }
    const p = this.data.id
      ? api.put('/user/addressBook', Object.assign({ id: Number(this.data.id) }, body))
      : api.post('/user/addressBook', body)

    p.then(() => {
      wx.showToast({ title: '已保存', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 600)
    }).catch(err => wx.showToast({ title: err, icon: 'none' }))
      .finally(() => this.setData({ saving: false }))
  }
})