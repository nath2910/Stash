import api from './api'

const BillingService = {
  status(includePortal = false, forceRefresh = false) {
    return api.get('/billing/status', { params: { includePortal, forceRefresh } })
  },
  checkout(promoCode, discord) {
    return api.post('/billing/checkout', { promoCode, discord })
  },
}

export default BillingService
