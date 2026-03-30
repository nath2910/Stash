import type {
  PaletteFilterState,
  PaletteFlatItem,
  WidgetPaletteGroup,
  WidgetPaletteItem,
  WidgetPreviewSpec,
} from './types'

const QUERY_SPLIT_RE = /\s+/g

export function normalizeText(value: unknown): string {
  return String(value ?? '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .trim()
}

function widgetSearchIndex(item: WidgetPaletteItem): string {
  return [
    item.title,
    item.help,
    item.type,
    ...(item.tags ?? []),
    item.dataType,
    ...(item.forms ?? []),
  ]
    .map((part) => normalizeText(part))
    .join(' ')
}

function scoreWidget(item: WidgetPaletteItem, query: string): number {
  if (!query) return 0
  const title = normalizeText(item.title)
  const full = widgetSearchIndex(item)
  if (!full.includes(query)) return -1

  let score = 1
  if (title.startsWith(query)) score += 6
  if (title.includes(query)) score += 3
  if (normalizeText(item.type).startsWith(query)) score += 2
  for (const token of query.split(QUERY_SPLIT_RE)) {
    if (!token) continue
    if (title.includes(token)) score += 1
    if ((item.tags ?? []).some((tag) => normalizeText(tag).includes(token))) score += 1
  }
  return score
}

function sortItems(
  items: WidgetPaletteItem[],
  query: string,
  sort: PaletteFilterState['sort'],
): WidgetPaletteItem[] {
  const sorted = [...items]

  if (sort === 'title') {
    sorted.sort((a, b) => a.title.localeCompare(b.title, 'fr'))
    return sorted
  }

  if (sort === 'quick') {
    sorted.sort((a, b) => {
      const aDirect = a.formPicker === false || (a.forms?.length ?? 0) <= 1
      const bDirect = b.formPicker === false || (b.forms?.length ?? 0) <= 1
      if (aDirect !== bDirect) return aDirect ? -1 : 1
      return a.title.localeCompare(b.title, 'fr')
    })
    return sorted
  }

  sorted.sort((a, b) => {
    const delta = scoreWidget(b, query) - scoreWidget(a, query)
    if (delta !== 0) return delta
    return a.title.localeCompare(b.title, 'fr')
  })
  return sorted
}

export function filterPaletteGroups(
  groups: WidgetPaletteGroup[],
  state: PaletteFilterState,
): WidgetPaletteGroup[] {
  const query = normalizeText(state.query)

  return groups
    .filter((group) => state.category === 'all' || group.title === state.category)
    .map((group) => {
      const filtered = (group.items ?? []).filter((item) => {
        if (state.dataType !== 'all' && (item.dataType ?? 'mixed') !== state.dataType) return false
        if (!query) return true
        return scoreWidget(item, query) >= 0
      })

      return {
        ...group,
        items: sortItems(filtered, query, state.sort),
      }
    })
    .filter((group) => group.items.length > 0)
}

export function flattenPalette(groups: WidgetPaletteGroup[]): PaletteFlatItem[] {
  const output: PaletteFlatItem[] = []
  for (const group of groups) {
    for (const item of group.items ?? []) {
      output.push({
        groupTitle: group.title,
        groupColor: group.color,
        item,
      })
    }
  }
  return output
}

export function resolveGridColumns(width: number): number {
  if (width < 640) return 1
  if (width < 1024) return 2
  return 3
}

export function moveGridIndexByKey(params: {
  key: string
  index: number
  total: number
  columns: number
}): number {
  const { key, index, total, columns } = params
  if (total <= 0) return -1
  const last = total - 1

  switch (key) {
    case 'ArrowRight':
      return Math.min(index + 1, last)
    case 'ArrowLeft':
      return Math.max(index - 1, 0)
    case 'ArrowDown':
      return Math.min(index + columns, last)
    case 'ArrowUp':
      return Math.max(index - columns, 0)
    case 'Home':
      return 0
    case 'End':
      return last
    default:
      return index
  }
}

export function coercePreviewSpec(preview: WidgetPreviewSpec | undefined): WidgetPreviewSpec {
  if (!preview?.kind) {
    return { kind: 'fallback' }
  }

  const values = preview.values?.filter((value) => Number.isFinite(value))
  const secondaryValues = preview.secondaryValues?.filter((value) => Number.isFinite(value))

  return {
    ...preview,
    values: values?.length ? values : undefined,
    secondaryValues: secondaryValues?.length ? secondaryValues : undefined,
    legend: preview.legend?.filter((entry) => String(entry ?? '').trim().length > 0).slice(0, 5),
  }
}
