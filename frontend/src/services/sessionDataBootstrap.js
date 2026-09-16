import SnkVenteServices from './SnkVenteServices.js'
import StatsServices from './StatsServices.js'
import { readAuthToken, readStoredUser } from '@/utils/authStorage.js'
import { getCurrentYearRange } from '@/utils/homeDashboard.js'

const warmedSessions = new Set()
const inFlightSessions = new Map()

function sessionKey() {
  const token = readAuthToken()
  const user = readStoredUser()
  const userId = user?.id == null ? '' : String(user.id)
  if (!token || !userId) return ''
  return `${userId}::${token}`
}

export function resetSessionDataBootstrap() {
  warmedSessions.clear()
  inFlightSessions.clear()
}

export function warmSessionData({ force = false } = {}) {
  const key = sessionKey()
  if (!key) return Promise.resolve()
  if (!force && warmedSessions.has(key)) return Promise.resolve()
  if (!force && inFlightSessions.has(key)) return inFlightSessions.get(key)

  const yearRange = getCurrentYearRange(new Date())
  const request = Promise.allSettled([
    SnkVenteServices.getSnkVente(),
    SnkVenteServices.getGroupedSnkVente(),
    StatsServices.summary(yearRange.from, yearRange.to),
    StatsServices.dateBounds(),
  ])
    .then(() => {
      warmedSessions.add(key)
    })
    .finally(() => {
      inFlightSessions.delete(key)
    })

  inFlightSessions.set(key, request)
  return request
}
