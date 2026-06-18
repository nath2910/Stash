// src/services/StatsServices.js
import api from './api.js'

const DEFAULT_CACHE_TTL_MS = 20_000
const inFlightRequests = new Map()
const responseCache = new Map()

function asStableString(value) {
  if (value == null) return ''
  if (Array.isArray(value)) {
    return value
      .map((entry) => String(entry ?? '').trim())
      .filter(Boolean)
      .sort((a, b) => a.localeCompare(b))
      .join(',')
  }
  return String(value).trim()
}

function buildCacheKey(path, params = {}) {
  const keys = Object.keys(params).sort((a, b) => a.localeCompare(b))
  const pairs = []
  for (const key of keys) {
    const raw = params[key]
    if (raw === undefined || raw === null || raw === '') continue
    if (Array.isArray(raw)) {
      const arr = raw
        .map((entry) => String(entry ?? '').trim())
        .filter(Boolean)
        .sort((a, b) => a.localeCompare(b))
      for (const entry of arr) {
        pairs.push(`${encodeURIComponent(key)}=${encodeURIComponent(entry)}`)
      }
      continue
    }
    const stable = asStableString(raw)
    if (!stable) continue
    pairs.push(`${encodeURIComponent(key)}=${encodeURIComponent(stable)}`)
  }
  return `${path}?${pairs.join('&')}`
}

function cachedGet(path, params, opts = {}) {
  const ttlMs =
    Number.isFinite(opts.ttlMs) && Number(opts.ttlMs) >= 0
      ? Number(opts.ttlMs)
      : DEFAULT_CACHE_TTL_MS

  if (ttlMs === 0) {
    return api.get(path, { params })
  }

  const key = buildCacheKey(path, params)
  const now = Date.now()
  const cached = responseCache.get(key)
  if (cached && cached.expiresAt > now) {
    return Promise.resolve(cached.response)
  }

  const pending = inFlightRequests.get(key)
  if (pending) return pending

  const req = api
    .get(path, { params })
    .then((response) => {
      responseCache.set(key, {
        response,
        expiresAt: Date.now() + ttlMs,
      })
      return response
    })
    .finally(() => {
      inFlightRequests.delete(key)
    })

  inFlightRequests.set(key, req)
  return req
}

/**
 * Controller accepts: start/end OR from/to.
 * We send both to be robust.
 */
function dateParams(from, to, asOf, categories, types) {
  const params = {
    from,
    to,
    start: from,
    end: to,
  }
  if (asOf !== undefined) params.asOf = asOf
  if (categories !== undefined) params.categories = categories
  if (types !== undefined) params.types = types
  return params
}

/**
 * Supports:
 *  - summary(from, to)
 *  - summary({ from, to })
 */
function resolveRange(a, b, categories, types) {
  if (a && typeof a === 'object') {
    return {
      from: a.from,
      to: a.to,
      asOf: a.asOf,
      categories: a.categories,
      types: a.types,
    }
  }
  return { from: a, to: b, asOf: undefined, categories, types }
}

function nonEmptyArray(value) {
  return Array.isArray(value) && value.length ? value : undefined
}

function resolvedDateParams(a, b, categories, types, { includeAsOf = true } = {}) {
  const { from, to, asOf, categories: cats, types: itemTypes } = resolveRange(a, b, categories, types)
  return dateParams(
    from,
    to,
    includeAsOf ? asOf : undefined,
    nonEmptyArray(cats),
    nonEmptyArray(itemTypes),
  )
}

function resolveGranularity(granularityOrOpts = 'day') {
  if (typeof granularityOrOpts === 'string') return granularityOrOpts
  if (granularityOrOpts && typeof granularityOrOpts === 'object') {
    return granularityOrOpts.granularity || granularityOrOpts.bucket || 'day'
  }
  return 'day'
}

function summary(a, b, categories, types) {
  return cachedGet('/stats/summary', resolvedDateParams(a, b, categories, types))
}

function timeseries(a, b, granularityOrOpts = 'day', categories, types) {
  return cachedGet('/stats/timeseries', {
    ...resolvedDateParams(a, b, categories, types, { includeAsOf: false }),
    granularity: resolveGranularity(granularityOrOpts),
  })
}

function brands(a, b, categories, types) {
  return cachedGet('/stats/brands', resolvedDateParams(a, b, categories, types, { includeAsOf: false }))
}

function topSales(a, b, limit = 3, categories, types) {
  return cachedGet('/stats/top-sales', {
    ...resolvedDateParams(a, b, categories, types, { includeAsOf: false }),
    limit,
  })
}

function kpi(metric, a, b, categories, types) {
  return cachedGet(`/stats/kpi/${metric}`, resolvedDateParams(a, b, categories, types, { includeAsOf: false }))
}

function series(metric, a, b, granularityOrOpts = 'day', categories, types) {
  return cachedGet(`/stats/series/${metric}`, {
    ...resolvedDateParams(a, b, categories, types, { includeAsOf: false }),
    granularity: resolveGranularity(granularityOrOpts),
  })
}

function breakdown(metric, a, b, categories, types) {
  return cachedGet(
    `/stats/breakdown/${metric}`,
    resolvedDateParams(a, b, categories, types, { includeAsOf: false }),
  )
}

function rank(metric, a, b, limit = 10, categories, types) {
  return cachedGet(`/stats/rank/${metric}`, {
    ...resolvedDateParams(a, b, categories, types, { includeAsOf: false }),
    limit,
  })
}

function dateBounds() {
  return cachedGet('/stats/date-bounds', {}, { ttlMs: 60_000 })
}

function categories(from, to) {
  const params = {}
  if (/^\d{4}-\d{2}-\d{2}$/.test(String(from || '')) && /^\d{4}-\d{2}-\d{2}$/.test(String(to || ''))) {
    params.from = from
    params.to = to
  }
  return cachedGet('/stats/categories', params, { ttlMs: 60_000 })
}

function annualDashboard(year) {
  return cachedGet('/stats/annual-dashboard', { year }, { ttlMs: 20_000 })
}

function monthlyDashboard(year, month) {
  return cachedGet('/stats/monthly-dashboard', { year, month }, { ttlMs: 60_000 })
}

// Persistance du layout cote backend (prive / multi-appareil).
function getLayout() {
  return api.get('/stats/layout')
}

function saveLayout(layout) {
  return api.put('/stats/layout', { layout })
}

export default {
  summary,
  timeseries,
  brands,
  topSales,
  kpi,
  series,
  breakdown,
  rank,
  annualDashboard,
  monthlyDashboard,
  dateBounds,
  categories,
  getLayout,
  saveLayout,
}
