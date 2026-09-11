import api from './api'

const BILLING_STATUS_TIMEOUT_MS = 20000
const BILLING_REFRESH_TIMEOUT_MS = 30000
const BILLING_CHECKOUT_TIMEOUT_MS = 45000

const BillingService = {
  status(includePortal = false, forceRefresh = false) {
    return api.get('/billing/status', {
      params: { includePortal, forceRefresh },
      timeout: includePortal || forceRefresh ? BILLING_REFRESH_TIMEOUT_MS : BILLING_STATUS_TIMEOUT_MS,
    })
  },
  plans() { return api.get('/billing/plans') },
  cancel() { return api.post('/billing/cancel') },
  portal() { return api.post('/billing/portal') },
  validatePromo(code) {
    return api.post('/billing/validate-promo', null, {
      params: { code },
      timeout: 10000,
    })
  },
  checkout(plan, termsAccepted, promoCode) {
    return api.post('/billing/checkout', { plan, termsAccepted, promoCode }, { timeout: BILLING_CHECKOUT_TIMEOUT_MS })
  },
}

export default BillingService
