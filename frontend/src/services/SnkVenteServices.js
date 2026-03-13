// src/services/SnkVenteServices.js
import api from './api'

class SnkVenteServices {
  // liste complete de l'utilisateur courant
  getSnkVente() {
    return api.get('/snkVente')
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
    return api.post('/snkVente/add', vente)
  }

  supprimer(id) {
    return api.delete(`/snkVente/${id}`)
  }

  supprimerEnMasse(ids) {
    return api.post('/snkVente/bulk-delete', ids)
  }

  topVentes() {
    return api.get('/snkVente/topVentes')
  }

  update(id, vente) {
    return api.put(`/snkVente/${id}`, vente)
  }

  importBulk(items) {
    return api.post('/snkVente/import', items, {
      timeout: 120000,
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
