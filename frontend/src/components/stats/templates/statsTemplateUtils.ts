import { cloneWidgetProps, getWidgetDef } from '../widgetRegistry'
import { getWidgetPaletteMeta } from '../palette/widgetPaletteMeta'
import type {
  EditableTemplateFields,
  StatsTemplateDensity,
  StatsTemplateLayoutWidget,
  StatsTemplateRange,
  StatsTemplateRangeKind,
  StatsTemplateRecord,
  StatsTemplateScaffold,
  StatsTemplateScaffoldTone,
  StatsTemplateScaffoldZone,
  StatsTemplateScene,
  StatsTemplateSort,
  StatsTemplateSource,
  TemplateFilterState,
  TemplatePreviewEntry,
} from './templateTypes'

const FALLBACK_ACCENT = '#0ea5e9'
const FALLBACK_CATEGORY = 'General'
const FALLBACK_NAME = 'Template'
const FALLBACK_THEME_TOKEN = 'executive_cobalt'
const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/

function normalizeText(value: unknown) {
  return String(value ?? '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .trim()
}

function asText(value: unknown) {
  return String(value ?? '').trim()
}

function asDate(value: unknown): string | undefined {
  const parsed = asText(value)
  if (!ISO_DATE_RE.test(parsed)) return undefined
  return parsed
}

function asPositiveNumber(value: unknown, fallback: number): number {
  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed <= 0) return fallback
  return Math.round(parsed)
}

function asFiniteNumber(value: unknown, fallback = 0): number {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return fallback
  return parsed
}

function asNonNegativeNumber(value: unknown, fallback = 0): number {
  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed < 0) return fallback
  return Math.round(parsed)
}

function isSafeBackground(value: string) {
  return !value.includes(';') && !/url\s*\(/i.test(value)
}

function normalizeScaffoldTone(value: unknown): StatsTemplateScaffoldTone {
  const tone = asText(value).toLowerCase()
  if (tone === 'hero' || tone === 'kpi' || tone === 'analysis' || tone === 'actions') return tone
  return 'analysis'
}

function normalizeTemplateScaffold(raw: unknown): StatsTemplateScaffold | undefined {
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) return undefined
  const layoutRaw = asText((raw as { layout?: unknown }).layout).toLowerCase()
  const layout =
    layoutRaw === 'executive' || layoutRaw === 'stock' || layoutRaw === 'sales' || layoutRaw === 'board'
      ? layoutRaw
      : undefined
  if (!layout) return undefined

  const zonesRaw = (raw as { zones?: unknown }).zones
  if (!Array.isArray(zonesRaw) || !zonesRaw.length) return undefined
  const zones: StatsTemplateScaffoldZone[] = []
  for (let idx = 0; idx < zonesRaw.length; idx += 1) {
    const entry = zonesRaw[idx]
    if (!entry || typeof entry !== 'object' || Array.isArray(entry)) continue
    const id = asText((entry as { id?: unknown }).id) || `zone_${idx + 1}`
    const title = asText((entry as { title?: unknown }).title)
    if (!title) continue
    const zone = {
      id,
      title,
      subtitle: asText((entry as { subtitle?: unknown }).subtitle) || undefined,
      x: asNonNegativeNumber((entry as { x?: unknown }).x, 0),
      y: asNonNegativeNumber((entry as { y?: unknown }).y, 0),
      w: asPositiveNumber((entry as { w?: unknown }).w, 320),
      h: asPositiveNumber((entry as { h?: unknown }).h, 160),
      tone: normalizeScaffoldTone((entry as { tone?: unknown }).tone),
    } satisfies StatsTemplateScaffoldZone
    zones.push(zone)
    if (zones.length >= 12) break
  }
  if (!zones.length) return undefined
  return { layout, zones }
}

export function normalizeAccentColor(value: unknown, fallback = FALLBACK_ACCENT): string {
  const color = asText(value)
  if (/^#[0-9a-fA-F]{6}$/.test(color)) return color.toLowerCase()
  if (/^#[0-9a-fA-F]{3}$/.test(color)) {
    return `#${color[1]}${color[1]}${color[2]}${color[2]}${color[3]}${color[3]}`.toLowerCase()
  }
  return fallback
}

export function normalizeTemplateDensity(value: unknown): StatsTemplateDensity {
  const density = asText(value).toLowerCase()
  if (density === 'dense' || density === 'balanced' || density === 'minimal') return density
  return 'balanced'
}

function normalizeTemplateSort(value: unknown): StatsTemplateSort {
  const mode = asText(value).toLowerCase()
  if (mode === 'name' || mode === 'updated' || mode === 'density') return mode
  return 'relevance'
}

function normalizeTags(value: unknown): string[] {
  if (!Array.isArray(value)) return []
  const unique = new Map<string, string>()
  for (const entry of value) {
    const tag = asText(entry)
    if (!tag) continue
    const key = normalizeText(tag)
    if (!key) continue
    if (!unique.has(key)) unique.set(key, tag)
  }
  return Array.from(unique.values()).slice(0, 12)
}

function normalizeTemplateRangeKind(value: unknown): StatsTemplateRangeKind {
  const kind = asText(value).toLowerCase()
  if (kind === 'month' || kind === 'ytd' || kind === 'year' || kind === 'custom') {
    return kind
  }
  return 'month'
}

export function normalizeTemplateRange(raw: unknown): StatsTemplateRange {
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) {
    return { kind: 'month' }
  }
  const kind = normalizeTemplateRangeKind((raw as { kind?: unknown }).kind)
  if (kind !== 'custom') return { kind }

  const from = asDate((raw as { from?: unknown }).from)
  const to = asDate((raw as { to?: unknown }).to)
  if (from && to) {
    return from <= to ? { kind: 'custom', from, to } : { kind: 'custom', from: to, to: from }
  }
  return { kind: 'month' }
}

export function createTemplateId(prefix = 'tpl') {
  const uid =
    globalThis.crypto?.randomUUID?.() ?? `${Date.now()}_${Math.random().toString(16).slice(2)}`
  return `${prefix}_${uid}`
}

export function normalizeTemplateLayout(rawLayout: unknown): StatsTemplateLayoutWidget[] {
  if (!Array.isArray(rawLayout)) return []

  const list: StatsTemplateLayoutWidget[] = []
  for (let index = 0; index < rawLayout.length; index += 1) {
    const raw = rawLayout[index] as Record<string, unknown>
    const type = asText(raw?.type)
    const def = getWidgetDef(type)
    if (!def) continue

    list.push({
      id: asText(raw?.id) || undefined,
      type: def.type,
      title: asText(raw?.title) || def.title,
      x: asFiniteNumber(raw?.x, 0),
      y: asFiniteNumber(raw?.y, 0),
      w: asPositiveNumber(raw?.w, def.defaultSize.w),
      h: asPositiveNumber(raw?.h, def.defaultSize.h),
      z: asPositiveNumber(raw?.z, index + 1),
      props: {
        ...cloneWidgetProps(def.defaultProps ?? {}),
        ...cloneWidgetProps(raw?.props ?? {}),
      },
    })
  }

  return list
}

type DensifyMode = StatsTemplateDensity

type RowGroup = {
  top: number
  bottom: number
  members: Array<{ idx: number; widget: StatsTemplateLayoutWidget }>
}

function clusterRows(layout: StatsTemplateLayoutWidget[], rowThreshold = 140): RowGroup[] {
  const sorted = layout
    .map((widget, idx) => ({ idx, widget }))
    .sort((a, b) => (a.widget.y ?? 0) - (b.widget.y ?? 0) || (a.widget.x ?? 0) - (b.widget.x ?? 0))

  const groups: RowGroup[] = []
  for (const entry of sorted) {
    const y = asFiniteNumber(entry.widget.y, 0)
    const h = asPositiveNumber(entry.widget.h, 240)
    const current = groups[groups.length - 1]
    if (!current || Math.abs(y - current.top) > rowThreshold) {
      groups.push({
        top: y,
        bottom: y + h,
        members: [entry],
      })
      continue
    }

    current.top = Math.min(current.top, y)
    current.bottom = Math.max(current.bottom, y + h)
    current.members.push(entry)
  }
  return groups
}

function tunedWidgetHeight(widget: StatsTemplateLayoutWidget, mode: DensifyMode) {
  const type = widget.type
  const current = asPositiveNumber(widget.h, 220)
  if (mode === 'dense' && (type === 'textBlock' || type === 'textTitle')) {
    return Math.max(64, Math.round(current * 0.94))
  }
  if (mode === 'minimal' && type === 'textBlock') {
    return Math.round(current * 1.1)
  }
  return current
}

export function densifyTemplateLayout(
  rawLayout: StatsTemplateLayoutWidget[],
  mode: DensifyMode = 'balanced',
): StatsTemplateLayoutWidget[] {
  const normalized = normalizeTemplateLayout(rawLayout)
  if (!normalized.length) return []

  const targetGap = mode === 'dense' ? 30 : mode === 'minimal' ? 72 : 46
  const rowThreshold = mode === 'dense' ? 130 : mode === 'minimal' ? 176 : 156
  const rows = clusterRows(normalized, rowThreshold)

  let shift = 0
  let prevBottom: number | null = null
  const byIndex = new Map<number, StatsTemplateLayoutWidget>()
  for (const row of rows) {
    const currentTop = row.top - shift
    if (prevBottom != null) {
      const naturalGap = currentTop - prevBottom
      if (naturalGap > targetGap) {
        shift += naturalGap - targetGap
      }
    }

    for (const member of row.members) {
      const nextY = Math.max(0, asFiniteNumber(member.widget.y, 0) - shift)
      byIndex.set(member.idx, {
        ...member.widget,
        y: nextY,
        h: tunedWidgetHeight(member.widget, mode),
      })
    }

    const rowHeight = row.bottom - row.top
    prevBottom = row.top - shift + rowHeight
  }

  const out = normalized.map((widget, idx) => byIndex.get(idx) ?? widget)
  return out
}

function normalizeIsoInstant(value: unknown): string | undefined {
  const parsed = asText(value)
  if (!parsed) return undefined
  const timestamp = Date.parse(parsed)
  if (!Number.isFinite(timestamp)) return undefined
  return new Date(timestamp).toISOString()
}

export function normalizeTemplateScene(raw: unknown): StatsTemplateScene | undefined {
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) return undefined

  const id = asText((raw as { id?: unknown }).id)
  const boardBackground = asText((raw as { boardBackground?: unknown }).boardBackground)
  if (!id || !boardBackground || !isSafeBackground(boardBackground)) return undefined

  const boardBackgroundLight = asText((raw as { boardBackgroundLight?: unknown }).boardBackgroundLight)
  const boardBackgroundDark = asText((raw as { boardBackgroundDark?: unknown }).boardBackgroundDark)
  const overlayPattern = asText((raw as { overlayPattern?: unknown }).overlayPattern)
  const overlayGradient = asText((raw as { overlayGradient?: unknown }).overlayGradient)
  const themeTokenId = asText((raw as { themeTokenId?: unknown }).themeTokenId)
  const scaffold = normalizeTemplateScaffold((raw as { scaffold?: unknown }).scaffold)

  return {
    id,
    boardBackground,
    boardBackgroundLight: boardBackgroundLight && isSafeBackground(boardBackgroundLight) ? boardBackgroundLight : undefined,
    boardBackgroundDark: boardBackgroundDark && isSafeBackground(boardBackgroundDark) ? boardBackgroundDark : undefined,
    overlayPattern: overlayPattern && isSafeBackground(overlayPattern) ? overlayPattern : undefined,
    overlayGradient: overlayGradient && isSafeBackground(overlayGradient) ? overlayGradient : undefined,
    themeTokenId: themeTokenId || undefined,
    scaffold,
  }
}

export function normalizeTemplateRecord(
  raw: unknown,
  options: { source: StatsTemplateSource; idPrefix?: string } = { source: 'custom' },
): StatsTemplateRecord | null {
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) return null
  const obj = raw as Record<string, unknown>
  const source = options.source
  const layout = normalizeTemplateLayout(obj.layout)
  if (!layout.length) return null

  const name = asText(obj.name) || FALLBACK_NAME
  const id = asText(obj.id) || createTemplateId(options.idPrefix ?? (source === 'system' ? 'sys' : 'custom'))
  const density = normalizeTemplateDensity(obj.density)
  const scene = normalizeTemplateScene(obj.scene)
  const themeTokenId = asText(obj.themeTokenId) || scene?.themeTokenId || FALLBACK_THEME_TOKEN

  return {
    id,
    source,
    name,
    description: asText(obj.description),
    category: asText(obj.category) || FALLBACK_CATEGORY,
    tags: normalizeTags(obj.tags),
    accent: normalizeAccentColor(obj.accent, FALLBACK_ACCENT),
    density,
    sortRank: Number.isFinite(Number(obj.sortRank)) ? Number(obj.sortRank) : undefined,
    recommendedRange: normalizeTemplateRange(obj.recommendedRange),
    themeTokenId,
    scene,
    layout,
    createdAt: normalizeIsoInstant(obj.createdAt),
    updatedAt: normalizeIsoInstant(obj.updatedAt),
  }
}

export function normalizeTemplateCollection(
  raw: unknown,
  options: { source: StatsTemplateSource; idPrefix?: string } = { source: 'custom' },
): StatsTemplateRecord[] {
  if (!Array.isArray(raw)) return []
  const unique = new Map<string, StatsTemplateRecord>()
  for (const entry of raw) {
    const normalized = normalizeTemplateRecord(entry, options)
    if (!normalized) continue
    if (!unique.has(normalized.id)) unique.set(normalized.id, normalized)
  }
  return Array.from(unique.values())
}

function compareDensity(a: StatsTemplateDensity, b: StatsTemplateDensity) {
  const rank = { dense: 0, balanced: 1, minimal: 2 } as const
  return rank[a] - rank[b]
}

export function sortTemplates(
  templates: StatsTemplateRecord[],
  mode: StatsTemplateSort = 'relevance',
  query = '',
): StatsTemplateRecord[] {
  const normalizedQuery = normalizeText(query)
  const list = [...templates]
  if (mode === 'name') {
    return list.sort((a, b) => a.name.localeCompare(b.name, 'fr'))
  }
  if (mode === 'updated') {
    return list.sort((a, b) => {
      const aTime = Date.parse(a.updatedAt || a.createdAt || '') || 0
      const bTime = Date.parse(b.updatedAt || b.createdAt || '') || 0
      return bTime - aTime
    })
  }
  if (mode === 'density') {
    return list.sort((a, b) => compareDensity(a.density ?? 'balanced', b.density ?? 'balanced'))
  }

  return list.sort((a, b) => {
    if (a.source !== b.source) return a.source === 'system' ? -1 : 1
    const ar = Number.isFinite(Number(a.sortRank)) ? Number(a.sortRank) : Number.MAX_SAFE_INTEGER
    const br = Number.isFinite(Number(b.sortRank)) ? Number(b.sortRank) : Number.MAX_SAFE_INTEGER
    if (ar !== br) return ar - br
    if (normalizedQuery) {
      const aIndex = normalizeText([a.name, a.description, a.tags.join(' ')].join(' '))
      const bIndex = normalizeText([b.name, b.description, b.tags.join(' ')].join(' '))
      const aStarts = aIndex.startsWith(normalizedQuery)
      const bStarts = bIndex.startsWith(normalizedQuery)
      if (aStarts !== bStarts) return aStarts ? -1 : 1
    }
    return a.name.localeCompare(b.name, 'fr')
  })
}

export function filterTemplates(
  templates: StatsTemplateRecord[],
  state: TemplateFilterState,
): StatsTemplateRecord[] {
  const query = normalizeText(state.query)
  const tags = (state.tags ?? []).map((tag) => normalizeText(tag)).filter(Boolean)
  const density = state.density ?? 'all'
  const sorted = sortTemplates(templates, normalizeTemplateSort(state.sort), query)

  return sorted
    .filter((template) => state.source === 'all' || template.source === state.source)
    .filter((template) => state.category === 'all' || template.category === state.category)
    .filter((template) => density === 'all' || (template.density ?? 'balanced') === density)
    .filter((template) => {
      if (!tags.length) return true
      const index = new Set((template.tags ?? []).map((tag) => normalizeText(tag)))
      return tags.every((tag) => index.has(tag))
    })
    .filter((template) => {
      if (!query) return true
      const index = normalizeText([
        template.name,
        template.description,
        template.category,
        ...(template.tags ?? []),
      ].join(' '))
      return index.includes(query)
    })
}

export function mapTemplatePreview(template: StatsTemplateRecord, maxItems = 5): TemplatePreviewEntry[] {
  const previewEntries: TemplatePreviewEntry[] = []
  const seenTypes = new Set<string>()

  for (const widget of template.layout) {
    if (seenTypes.has(widget.type)) continue
    const def = getWidgetDef(widget.type)
    if (!def) continue
    seenTypes.add(def.type)
    previewEntries.push({
      type: def.type,
      title: widget.title || def.title,
      preview: getWidgetPaletteMeta(def.type).preview,
    })
    if (previewEntries.length >= maxItems) break
  }

  if (!previewEntries.length) {
    previewEntries.push({
      type: 'fallback',
      title: 'Apercu',
      preview: { kind: 'fallback' },
    })
  }
  return previewEntries
}

export function applyTemplateLayout(
  template: StatsTemplateRecord,
  createWidgetId: (type: string) => string,
  options: { density?: StatsTemplateDensity } = {},
) {
  const density = options.density ?? template.density ?? 'balanced'
  const normalized = normalizeTemplateLayout(template.layout)
  const densified = densifyTemplateLayout(normalized, density)

  return densified.map((widget, index) => ({
    id: createWidgetId(widget.type),
    type: widget.type,
    title: widget.title || getWidgetDef(widget.type)?.title || widget.type,
    x: asFiniteNumber(widget.x, 0),
    y: asFiniteNumber(widget.y, 0),
    w: asPositiveNumber(widget.w, getWidgetDef(widget.type)?.defaultSize?.w ?? 480),
    h: asPositiveNumber(widget.h, getWidgetDef(widget.type)?.defaultSize?.h ?? 320),
    props: cloneWidgetProps(widget.props ?? {}),
    z: index + 1,
  }))
}

function formatDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function subMonthsClamp(date: Date, months: number) {
  const day = date.getDate()
  const firstOfTargetMonth = new Date(date.getFullYear(), date.getMonth() - months, 1)
  const lastDayOfTargetMonth = new Date(
    firstOfTargetMonth.getFullYear(),
    firstOfTargetMonth.getMonth() + 1,
    0,
  ).getDate()
  const clampedDay = Math.min(day, lastDayOfTargetMonth)
  return new Date(firstOfTargetMonth.getFullYear(), firstOfTargetMonth.getMonth(), clampedDay)
}

function subYearsClamp(date: Date, years: number) {
  const targetYear = date.getFullYear() - years
  const month = date.getMonth()
  const day = date.getDate()
  const lastDayOfTargetMonth = new Date(targetYear, month + 1, 0).getDate()
  const clampedDay = Math.min(day, lastDayOfTargetMonth)
  return new Date(targetYear, month, clampedDay)
}

export function resolveTemplateRange(range: StatsTemplateRange, now = new Date()) {
  const today = new Date(now)
  today.setHours(0, 0, 0, 0)

  if (range.kind === 'month') {
    return {
      from: formatDate(subMonthsClamp(today, 1)),
      to: formatDate(today),
    }
  }
  if (range.kind === 'ytd') {
    return {
      from: formatDate(new Date(today.getFullYear(), 0, 1)),
      to: formatDate(today),
    }
  }
  if (range.kind === 'year') {
    return {
      from: formatDate(subYearsClamp(today, 1)),
      to: formatDate(today),
    }
  }

  const from = asDate(range.from)
  const to = asDate(range.to)
  if (!from || !to) return null
  return from <= to ? { from, to } : { from: to, to: from }
}

export function createCustomTemplateFromCanvas(args: {
  fields: EditableTemplateFields
  widgets: StatsTemplateLayoutWidget[]
  scene?: StatsTemplateScene
  now?: Date
}): StatsTemplateRecord | null {
  const now = args.now ?? new Date()
  const raw = {
    id: createTemplateId('custom'),
    name: asText(args.fields.name) || FALLBACK_NAME,
    description: asText(args.fields.description),
    category: asText(args.fields.category) || FALLBACK_CATEGORY,
    tags: args.fields.tags ?? [],
    accent: normalizeAccentColor(args.fields.accent, FALLBACK_ACCENT),
    density: 'balanced',
    recommendedRange: normalizeTemplateRange(args.fields.recommendedRange),
    themeTokenId: FALLBACK_THEME_TOKEN,
    scene: args.scene,
    layout: args.widgets,
    createdAt: now.toISOString(),
    updatedAt: now.toISOString(),
  }
  return normalizeTemplateRecord(raw, { source: 'custom', idPrefix: 'custom' })
}
