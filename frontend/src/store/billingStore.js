import { ref } from 'vue'
import BillingService from '@/services/BillingService'

const status = ref('unknown') // unknown | active | past_due | canceled | inactive
const portalUrl = ref('')
let inflight = null

async function fetchStatus(force = false, includePortal = false) {
  if (!force && !includePortal && status.value === 'active') return status.value
  if (inflight) return inflight
  inflight = BillingService.status(includePortal)
    .then((res) => {
      status.value = res?.data?.status || 'inactive'
      portalUrl.value = res?.data?.portalUrl || ''
      return status.value
    })
    .catch(() => {
      status.value = 'inactive'
      portalUrl.value = ''
      return status.value
    })
    .finally(() => {
      inflight = null
    })
  return inflight
}

function reset() {
  status.value = 'unknown'
  portalUrl.value = ''
}

export function useBillingStore() {
  return { status, portalUrl, fetchStatus, reset }
}
