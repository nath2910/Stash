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
    return api.post('/snkVente/import', items)
  }
}

export default new SnkVenteServices()
