import StatsServices from './StatsServices.js'

function isObject(value) {
  return !!value && typeof value === 'object' && !Array.isArray(value)
}

function normalizeLayoutEnvelope(layoutPayload) {
  const base = isObject(layoutPayload) ? layoutPayload : {}
  const profiles = isObject(base.profiles) ? base.profiles : {}
  return {
    ...base,
    version: Number(base.version || 1),
    profiles,
  }
}

function normalizeAccentColor(value, fallback = '#0ea5e9') {
  const color = String(value ?? '').trim()
  if (/^#[0-9a-fA-F]{6}$/.test(color)) return color
  if (/^#[0-9a-fA-F]{3}$/.test(color)) {
    return `#${color[1]}${color[1]}${color[2]}${color[2]}${color[3]}${color[3]}`.toLowerCase()
  }
  return fallback
}

function normalizeTags(tags) {
  if (!Array.isArray(tags)) return []
  const unique = new Map()
  for (const tag of tags) {
    const text = String(tag ?? '').trim()
    if (!text) continue
    const key = text.toLowerCase()
    if (!unique.has(key)) unique.set(key, text)
  }
  return Array.from(unique.values()).slice(0, 10)
}

function normalizeRange(raw) {
  const kind = String(raw?.kind ?? '').trim().toLowerCase()
  if (kind === 'month' || kind === 'ytd' || kind === 'year') return { kind }
  if (kind === 'custom') {
    const from = String(raw?.from ?? '').trim()
    const to = String(raw?.to ?? '').trim()
    if (/^\d{4}-\d{2}-\d{2}$/.test(from) && /^\d{4}-\d{2}-\d{2}$/.test(to)) {
      return from <= to ? { kind: 'custom', from, to } : { kind: 'custom', from: to, to: from }
    }
  }
  return { kind: 'month' }
}

function normalizeLayout(layout) {
  if (!Array.isArray(layout)) return []
  return layout
    .filter((entry) => entry && typeof entry === 'object' && typeof entry.type === 'string')
    .map((entry) => ({ ...entry }))
}

function normalizeTemplateCollection(rawTemplates) {
  if (!Array.isArray(rawTemplates)) return []
  return rawTemplates
    .map((entry) => {
      if (!entry || typeof entry !== 'object') return null
      const layout = normalizeLayout(entry.layout)
      if (!layout.length) return null
      return {
        ...entry,
        id: String(entry.id ?? '').trim(),
        source: 'custom',
        name: String(entry.name ?? '').trim() || 'Template',
        description: String(entry.description ?? '').trim(),
        category: String(entry.category ?? '').trim() || 'General',
        tags: normalizeTags(entry.tags),
        accent: normalizeAccentColor(entry.accent),
        recommendedRange: normalizeRange(entry.recommendedRange),
        layout,
      }
    })
    .filter((entry) => entry && entry.id)
}

function extractCustomTemplates(layoutPayload) {
  const layout = normalizeLayoutEnvelope(layoutPayload)
  return normalizeTemplateCollection(layout.customTemplates)
}

function mergeCustomTemplates(layoutPayload, templates) {
  const layout = normalizeLayoutEnvelope(layoutPayload)
  return {
    ...layout,
    customTemplates: normalizeTemplateCollection(templates),
  }
}

async function loadCustomTemplates() {
  const response = await StatsServices.getLayout()
  return extractCustomTemplates(response?.data?.layout)
}

async function saveCustomTemplates(templates, baseLayout) {
  let layout = baseLayout
  if (!isObject(layout)) {
    const response = await StatsServices.getLayout()
    layout = response?.data?.layout
  }
  const payload = mergeCustomTemplates(layout, templates)
  return StatsServices.saveLayout(payload)
}

export default {
  extractCustomTemplates,
  mergeCustomTemplates,
  loadCustomTemplates,
  saveCustomTemplates,
}
