import api from './api.js'

class DeliveryTrackingService {
  listMailAccounts() {
    return api.get('/delivery/mail-accounts')
  }

  connectGmail(emailAddress = '') {
    const payload = emailAddress ? { emailAddress } : {}
    return api.post('/delivery/mail-accounts/gmail/connect', payload)
  }

  deleteMailAccount(id) {
    return api.delete(`/delivery/mail-accounts/${id}`)
  }

  scanNow(id) {
    return api.post(`/delivery/mail-accounts/${id}/scan-now`)
  }

  scanAll() {
    return api.post('/delivery/mail-accounts/scan-all')
  }

  listTrackingCandidates() {
    return api.get('/delivery/tracking-candidates')
  }

  confirmTrackingCandidate(id) {
    return api.post(`/delivery/tracking-candidates/${id}/confirm`)
  }

  ignoreTrackingCandidate(id) {
    return api.post(`/delivery/tracking-candidates/${id}/ignore`)
  }

  listParcels() {
    return api.get('/delivery/parcels')
  }

  createParcel(payload) {
    return api.post('/delivery/parcels', payload)
  }

  getParcel(id) {
    return api.get(`/delivery/parcels/${id}`)
  }

  refreshAllParcels() {
    return api.post('/delivery/parcels/refresh-all')
  }

  refreshParcel(id) {
    return api.post(`/delivery/parcels/${id}/refresh`)
  }

  deleteParcel(id) {
    return api.delete(`/delivery/parcels/${id}`)
  }
}

export default new DeliveryTrackingService()
