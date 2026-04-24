export type StatsTemplateSource = 'system' | 'custom'

export type StatsTemplateRangeKind = 'month' | 'ytd' | 'year' | 'custom'
export type StatsTemplateDensity = 'dense' | 'balanced' | 'minimal'
export type StatsTemplateSort = 'relevance' | 'name' | 'updated' | 'density'
export type TemplateThemeVariant = 'light' | 'dark'

export type StatsTemplateRange = {
  kind: StatsTemplateRangeKind
  from?: string
  to?: string
}

export type StatsTemplateScene = {
  id: string
  boardBackground: string
  boardBackgroundLight?: string
  boardBackgroundDark?: string
  overlayPattern?: string
  overlayGradient?: string
  themeTokenId?: string
  scaffold?: StatsTemplateScaffold
}

export type StatsTemplateScaffoldTone = 'hero' | 'kpi' | 'analysis' | 'actions'

export type StatsTemplateScaffoldZone = {
  id: string
  title: string
  subtitle?: string
  x: number
  y: number
  w: number
  h: number
  tone?: StatsTemplateScaffoldTone
}

export type StatsTemplateScaffold = {
  layout: 'executive' | 'stock' | 'sales' | 'board'
  zones: StatsTemplateScaffoldZone[]
}

export type StatsTemplateLayoutWidget = {
  id?: string
  type: string
  title?: string
  x?: number
  y?: number
  w?: number
  h?: number
  props?: Record<string, unknown>
  z?: number
}

export type TemplateThemeTypography = {
  titleFont: string
  bodyFont: string
  monoFont: string
}

export type TemplateThemeShadows = {
  board: string
  card: string
  cardInset: string
}

export type TemplateThemeSurfaces = {
  board: string
  card: string
  cardAlt: string
  glass: string
  line: string
}

export type TemplateThemePalette = {
  accent: string
  accentSoft: string
  accentStrong: string
  text: string
  textMuted: string
  success: string
  warning: string
  danger: string
}

export type TemplateThemeTokens = {
  id: string
  label: string
  density: StatsTemplateDensity
  variant: TemplateThemeVariant
  palette: TemplateThemePalette
  surfaces: TemplateThemeSurfaces
  typography: TemplateThemeTypography
  shadows: TemplateThemeShadows
  radius: string
  boardBackground: string
  boardPattern: string
}

export type StatsTemplateRecord = {
  id: string
  source: StatsTemplateSource
  name: string
  description: string
  category: string
  tags: string[]
  accent: string
  density?: StatsTemplateDensity
  sortRank?: number
  recommendedRange: StatsTemplateRange
  themeTokenId?: string
  scene?: StatsTemplateScene
  layout: StatsTemplateLayoutWidget[]
  createdAt?: string
  updatedAt?: string
}

export type TemplateFilterState = {
  query: string
  category: string
  source: 'all' | StatsTemplateSource
  density?: 'all' | StatsTemplateDensity
  sort?: StatsTemplateSort
  tags?: string[]
}

export type TemplatePreviewEntry = {
  type: string
  title: string
  preview: Record<string, unknown>
}

export type EditableTemplateFields = {
  name: string
  description: string
  category: string
  tags: string[]
  accent: string
  recommendedRange: StatsTemplateRange
}
