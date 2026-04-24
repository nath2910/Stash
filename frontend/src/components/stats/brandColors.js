const BASE_BRAND_COLORS = {
  nike: '#3b82f6',
  adidas: '#2563eb',
  jordan: '#1d4ed8',
  puma: '#06b6d4',
  reebok: '#0ea5e9',
  newbalance: '#14b8a6',
  converse: '#22c55e',
  vans: '#84cc16',
  asics: '#eab308',
  saucony: '#f59e0b',
  yeezy: '#f97316',
  supreme: '#ef4444',
  bape: '#ec4899',
  pokemon: '#f59e0b',
  pokemoncard: '#f59e0b',
  cartes: '#f59e0b',
  ticket: '#8b5cf6',
  tickets: '#8b5cf6',
  autre: '#64748b',
  autres: '#64748b',
  other: '#64748b',
  unknown: '#64748b',
}

const FALLBACK_COLORS = [
  '#3b82f6',
  '#06b6d4',
  '#14b8a6',
  '#22c55e',
  '#84cc16',
  '#eab308',
  '#f59e0b',
  '#f97316',
  '#ef4444',
  '#ec4899',
  '#8b5cf6',
  '#6366f1',
]

function normalizeKey(value) {
  return String(value ?? '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]/g, '')
    .trim()
}

function hashString(value) {
  let hash = 0
  for (let i = 0; i < value.length; i += 1) {
    hash = (hash << 5) - hash + value.charCodeAt(i)
    hash |= 0
  }
  return Math.abs(hash)
}

export function getBrandColor(label) {
  const key = normalizeKey(label)
  if (!key) return '#64748b'
  if (BASE_BRAND_COLORS[key]) return BASE_BRAND_COLORS[key]
  const index = hashString(key) % FALLBACK_COLORS.length
  return FALLBACK_COLORS[index]
}

