import api from './api'

const BillingService = {
  status(includePortal = false) {
    return api.get('/billing/status', { params: { includePortal } })
  },
  checkout(promoCode, discord) {
    return api.post('/billing/checkout', { promoCode, discord })
  },
}

export default BillingService
