import api from './api.js'

const ADMIN_BASE = '/admin'
const ADMINISTRATIVE_BASE = '/administrative'

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

async function assertDownloadableBlob(blob) {
  if (!blob) {
    throw new Error('Aucun fichier recu depuis le backend.')
  }

  if (typeof Blob !== 'undefined' && blob instanceof Blob) {
    if (
      blob.type?.includes('application/json') ||
      blob.type?.includes('text/plain') ||
      blob.type?.includes('text/html')
    ) {
      const text = await blob.text()
      try {
        const payload = JSON.parse(text)
        throw new Error(payload.message || payload.error || 'Erreur backend pendant la generation du document.')
      } catch (error) {
        if (error instanceof SyntaxError) {
          throw new Error(text || 'Erreur backend pendant la generation du document.')
        }
        throw error
      }
    }

    if (blob.size === 0) {
      throw new Error('Le backend a renvoye un document vide.')
    }
  }
}

function filenameFromDisposition(disposition, fallback) {
  if (!disposition) return fallback
  const utf8Match = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
  if (utf8Match?.[1]) return decodeURIComponent(utf8Match[1].replace(/"/g, ''))
  const simpleMatch = /filename="?([^";]+)"?/i.exec(disposition)
  return simpleMatch?.[1] || fallback
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

  async administrativeProfile() {
    const { data } = await api.get(`${ADMINISTRATIVE_BASE}/profile`)
    return data
  },

  async saveAdministrativeProfile(payload) {
    const { data } = await api.put(`${ADMINISTRATIVE_BASE}/profile`, payload)
    return data
  },

  async administrativeSummary(params = {}) {
    const { data } = await api.get(`${ADMINISTRATIVE_BASE}/summary`, { params })
    return data
  },

  async administrativeYearSummary(year) {
    const { data } = await api.get(`${ADMINISTRATIVE_BASE}/year-summary`, {
      params: year ? { year } : {},
    })
    return data
  },

  async administrativeDocuments() {
    const { data } = await api.get(`${ADMINISTRATIVE_BASE}/documents`)
    return normalizeList(data)
  },

  async generateAdministrativeDocument(documentId, payload = {}) {
    const response = await api.post(`${ADMINISTRATIVE_BASE}/documents/${documentId}`, payload, {
      responseType: 'blob',
      headers: {
        Accept: 'application/pdf',
      },
    })
    await assertDownloadableBlob(response.data)
    const stamp = new Date().toISOString().slice(0, 10)
    const filename = filenameFromDisposition(
      response.headers?.['content-disposition'],
      `${documentId}-${stamp}.pdf`,
    )
    downloadBlob(response.data, filename)
    return filename
  },
}

export default AdminService
