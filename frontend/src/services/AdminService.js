import api from './api'

const ADMIN_BASE = '/admin'

function normalizeList(payload, keys = ['items', 'data', 'content', 'results']) {
  if (Array.isArray(payload)) return payload
  if (!payload || typeof payload !== 'object') return []
  for (const key of keys) {
    if (Array.isArray(payload[key])) return payload[key]
  }
  return []
}

function downloadBlob(blob, filename) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}

const AdminService = {
  async dashboard(params = {}) {
    const { data } = await api.get(`${ADMIN_BASE}/dashboard`, { params })
    return data
  },

  async settings() {
    const { data } = await api.get(`${ADMIN_BASE}/settings`)
    return data
  },

  async saveSettings(payload) {
    const { data } = await api.put(`${ADMIN_BASE}/settings`, payload)
    return data
  },

  async state() {
    const { data } = await api.get(`${ADMIN_BASE}/state`)
    return data
  },

  async saveState(payload) {
    const { data } = await api.put(`${ADMIN_BASE}/state`, payload)
    return data
  },

  async invoices(params = {}) {
    const { data } = await api.get(`${ADMIN_BASE}/invoices`, { params })
    return normalizeList(data, ['invoices', 'items', 'data', 'content'])
  },

  async createInvoice(payload) {
    const { data } = await api.post(`${ADMIN_BASE}/invoices`, payload)
    return data
  },

  async markInvoicePaid(invoiceId) {
    const { data } = await api.patch(`${ADMIN_BASE}/invoices/${invoiceId}/paid`)
    return data
  },

  async cancelInvoice(invoiceId, reason = '') {
    const { data } = await api.patch(`${ADMIN_BASE}/invoices/${invoiceId}/cancel`, { reason })
    return data
  },

  async exportCsv(params = {}) {
    const response = await api.get(`${ADMIN_BASE}/export.csv`, {
      params,
      responseType: 'blob',
    })
    const stamp = new Date().toISOString().slice(0, 10)
    downloadBlob(response.data, `export-administratif-${stamp}.csv`)
  },
}

export default AdminService
