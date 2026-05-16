export type WidgetPreviewKind =
  | 'kpi'
  | 'sparkline'
  | 'bars'
  | 'pie'
  | 'text'
  | 'treemap'
  | 'heatmap'
  | 'fallback'

export type WidgetDataType = 'all' | 'finance' | 'inventory' | 'performance' | 'text' | 'mixed'

export type PaletteSort = 'relevance' | 'title' | 'quick'

export interface WidgetPreviewSpec {
  kind: WidgetPreviewKind
  values?: number[]
  secondaryValues?: number[]
  ratio?: number
  label?: string
  valueText?: string
  deltaText?: string
  unit?: string
  legend?: string[]
}

export interface WidgetVariantOption {
  key: string
  label: string
  hint?: string
  view?: string
  props?: Record<string, unknown>
  widgetTitle?: string
  badge?: string
}

export interface WidgetDisplayGroup {
  key: string
  label: string
  hint?: string
  variants: WidgetVariantOption[]
}

export interface WidgetPaletteItem {
  type: string
  title: string
  help?: string
  icon?: unknown
  forms?: string[]
  formPicker?: boolean
  selection?: WidgetDisplayGroup[]
  tags?: string[]
  dataType?: WidgetDataType
  preview?: WidgetPreviewSpec
  disabled?: boolean
}

export interface WidgetPaletteGroup {
  title: string
  color: string
  glow?: string
  description?: string
  items: WidgetPaletteItem[]
}

export interface PaletteFilterState {
  query: string
  category: string
  dataType: WidgetDataType
  sort: PaletteSort
}

export interface PaletteFlatItem {
  groupTitle: string
  groupColor: string
  item: WidgetPaletteItem
}
