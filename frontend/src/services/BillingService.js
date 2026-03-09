import api from './api'

const BillingService = {
  status() {
    return api.get('/billing/status')
  },
  checkout(promoCode, discord) {
    return api.post('/billing/checkout', { promoCode, discord })
  },
}

export default BillingService
