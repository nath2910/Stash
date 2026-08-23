// src/services/SnkVenteServices.js
import api from './api'
import { invalidateStatsCache } from './StatsServices.js'
import { notifyInventoryChanged } from '@/utils/inventoryEvents.js'

const LIST_CACHE_TTL_MS = 60_000
const listCache = new Map()
const listInflight = new Map()
let listCacheGeneration = 0

function normalizeOptions(options = {}) {
  if (typeof options === 'number') return { limit: options }
  return options && typeof options === 'object' ? { ...options } : {}
}

function cacheKeyFor(options = {}) {
  return JSON.stringify(
    Object.keys(options)
      .sort((a, b) => a.localeCompare(b))
      .reduce((acc, key) => {
        const value = options[key]
        if (value !== undefined && value !== null && value !== '') acc[key] = value
        return acc
      }, {}),
  )
}

function invalidateListCache() {
  listCacheGeneration += 1
  listCache.clear()
  listInflight.clear()
}

function syncAfterInventoryMutation(detail = {}) {
  invalidateListCache()
  invalidateStatsCache()
  notifyInventoryChanged(detail)
}

class SnkVenteServices {
  // liste complete de l'utilisateur courant
  getSnkVente(options = {}) {
    const params = normalizeOptions(options)
    const bypassCache = params.forceRefresh || params.noCache
    delete params.forceRefresh
    delete params.noCache

    const key = cacheKeyFor(params)
    const requestGeneration = listCacheGeneration
    const now = Date.now()
    const cached = listCache.get(key)
    if (!bypassCache && cached && cached.expiresAt > now) {
      return Promise.resolve(cached.response)
    }

    const pending = listInflight.get(key)
    if (!bypassCache && pending) return pending

    const request = api
      .get('/snkVente', { params })
      .then((response) => {
        if (requestGeneration !== listCacheGeneration) return response
        listCache.set(key, {
          response,
          expiresAt: Date.now() + LIST_CACHE_TTL_MS,
        })
        return response
      })
      .finally(() => {
        listInflight.delete(key)
      })

    if (!bypassCache) listInflight.set(key, request)
    return request
  }

  getGroupedSnkVente(options = {}) {
    const params = normalizeOptions(options)
    return api.get('/snkVente/grouped', { params })
  }

  recent(limit = 8) {
    return api.get('/snkVente/recent', { params: { limit } })
  }
  totalBenef() {
    return api.get('/snkVente/total')
  }

  totalBenefAnnee(year) {
    return api.get('/snkVente/total', { params: { year } })
  }

  chiffreAffaire() {
    return api.get('/snkVente/ca')
  }

  marque() {
    return api.get('/snkVente/marque')
  }

  ajouter(vente) {
    return api.post('/snkVente/add', vente).then((response) => {
      syncAfterInventoryMutation({ source: 'inventory-service', action: 'add' })
      return response
    })
  }

  create(vente) {
    return api.post('/snkVente', vente).then((response) => {
      syncAfterInventoryMutation({ source: 'inventory-service', action: 'create' })
      return response
    })
  }

  createMany(vente, quantity = 1) {
    return api
      .post('/snkVente/bulk-create', {
        ...vente,
        quantity,
      })
      .then((response) => {
        syncAfterInventoryMutation({
          source: 'inventory-service',
          action: 'bulk-create',
          quantity,
        })
        return response
      })
  }

  supprimer(id) {
    return api.delete(`/snkVente/${id}`).then((response) => {
      syncAfterInventoryMutation({ source: 'inventory-service', action: 'delete', id })
      return response
    })
  }

  supprimerEnMasse(ids) {
    return api.post('/snkVente/bulk-delete', ids).then((response) => {
      syncAfterInventoryMutation({
        source: 'inventory-service',
        action: 'bulk-delete',
        count: Array.isArray(ids) ? ids.length : undefined,
      })
      return response
    })
  }

  topVentes() {
    return api.get('/snkVente/topVentes')
  }

  update(id, vente) {
    return api.put(`/snkVente/${id}`, vente).then((response) => {
      syncAfterInventoryMutation({ source: 'inventory-service', action: 'update', id })
      return response
    })
  }

  importBulk(items) {
    return api
      .post('/snkVente/import', items, {
        timeout: 120000,
      })
      .then((response) => {
        syncAfterInventoryMutation({
          source: 'inventory-service',
          action: 'import',
          count: Array.isArray(items) ? items.length : undefined,
        })
        return response
      })
  }

  // Attachments
  listAttachments(id) {
    return api.get(`/snkVente/${id}/attachments`)
  }

  uploadAttachment(id, file) {
    const form = new FormData()
    form.append('file', file)
    return api.post(`/snkVente/${id}/attachments`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  }

  deleteAttachment(id, attachmentId) {
    return api.delete(`/snkVente/${id}/attachments/${attachmentId}`)
  }

  downloadAttachment(id, attachmentId) {
    return api.get(`/snkVente/${id}/attachments/${attachmentId}/download`, {
      responseType: 'blob',
    })
  }
}

export default new SnkVenteServices()
