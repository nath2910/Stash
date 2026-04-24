import type {
  TemplateBrandPoint,
  TemplateChartHoverState,
  TemplateDataState,
  TemplatePieSlice,
  TemplateSeriesPoint,
  TemplateSummary,
  TemplateTopSalePoint,
} from '../../templateModeEngine'

export type TemplateKind = 'yearly-overview' | 'monthly-focus'

export const DEFAULT_TEMPLATE_KIND: TemplateKind = 'yearly-overview'

export type TemplateMonthChip = {
  label: string
  index: number
  active: boolean
  available: boolean
}

export type TemplateKpiCard = {
  key: string
  label: string
  value: string
  tone: string
}

export type TemplateQuickFact = {
  key: string
  label: string
  value: string
}

export type TemplatePickerItem = {
  kind: TemplateKind
  badge: string
  title: string
  description: string
}

export type TemplateChartTarget = 'main' | 'mini'

export type TemplateSharedViewProps = {
  selectedYear: number
  yearOptions: number[]
  monthChips: TemplateMonthChip[]
  pieChartStyle: Record<string, string>
  pieSlices: TemplatePieSlice[]
  kpiCards: TemplateKpiCard[]
  quickFacts: TemplateQuickFact[]
  brandsRows: TemplateBrandPoint[]
  topSalesRows: TemplateTopSalePoint[]
  cadenceText: string
  totalText: string
  mainDataState: TemplateDataState
  miniDataState: TemplateDataState
  chartGridLines: number[]
  mainChartAreaPath: string
  mainChartLinePath: string
  miniChartAreaPath: string
  miniChartLinePath: string
  mainLastPointCoord: { x: number; y: number } | null
  miniLastPointCoord: { x: number; y: number } | null
  mainHover: TemplateChartHoverState
  miniHover: TemplateChartHoverState
  profitStartLabel: string
  profitEndLabel: string
  emptyTitle: string
  gaugeDasharray: string
  successRateText: string
  series: TemplateSeriesPoint[]
  profitSeries: TemplateSeriesPoint[]
  summary: TemplateSummary
  totalNumber: number
  deltaPct: number | null
  localFrom: string
  localTo: string
}

export function isTemplateKind(value: unknown): value is TemplateKind {
  return value === 'yearly-overview' || value === 'monthly-focus'
}
