import { ref } from 'vue'
import BillingService from '@/services/BillingService'

const status = ref('unknown') // unknown | active | past_due | canceled | inactive
const portalUrl = ref('')
let inflightBasic = null
let inflightWithPortal = null

async function fetchStatus(force = false, includePortal = false) {
  if (!force && !includePortal && status.value === 'active') return status.value
  const currentInflight = includePortal ? inflightWithPortal : inflightBasic
  if (!force && currentInflight) return currentInflight

  const request = BillingService.status(includePortal)
    .then((res) => {
      status.value = res?.data?.status || 'inactive'
      if (includePortal) {
        portalUrl.value = res?.data?.portalUrl || ''
      }
      return status.value
    })
    .catch(() => {
      status.value = 'inactive'
      if (includePortal) {
        portalUrl.value = ''
      }
      return status.value
    })
    .finally(() => {
      if (includePortal) inflightWithPortal = null
      else inflightBasic = null
    })

  if (includePortal) inflightWithPortal = request
  else inflightBasic = request

  return request
}

function reset() {
  status.value = 'unknown'
  portalUrl.value = ''
}

export function useBillingStore() {
  return { status, portalUrl, fetchStatus, reset }
}
