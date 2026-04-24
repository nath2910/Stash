import StatsServices from '@/services/StatsServices'
import {
  normalizeBrands,
  normalizeKpi,
  normalizeSeries,
  normalizeSummary,
  normalizeTopSales,
} from '@/services/statsAdapters'

export type TemplateSeriesPoint = {
  date: string
  value: number
}

export type TemplateBrandPoint = {
  label: string
  nb: number
}

export type TemplateTopSalePoint = {
  nomItem: string
  benefice: number
}

export type TemplateSummary = {
  ca: number
  profit: number
  profitMargin: number
  itemsVendues: number
  itemsEnStock: number
  valeurStock: number
  roi: number | null
  totalAchat: number | null
  cashAvailable: number | null
  returnRate: number | null
}

export type TemplatePieSlice = {
  label: string
  value: number
  color: string
  percentText: string
  ratio: number
}

export type TemplateChartHoverState = {
  visible: boolean
  x: number
  y: number
  xPct: number
  yPct: number
  dateText: string
  valueText: string
}

export type TemplateDataState = 'loading' | 'error' | 'empty' | 'ready'

export type TemplateDashboardData = {
  kpiValue: number
  deltaPct: number | null
  series: TemplateSeriesPoint[]
  profitSeries: TemplateSeriesPoint[]
  summary: TemplateSummary
  brands: TemplateBrandPoint[]
  topSales: TemplateTopSalePoint[]
}

export const TEMPLATE_CHART_W = 640
export const TEMPLATE_CHART_H = 640
export const TEMPLATE_CHART_PAD_X = 58
export const TEMPLATE_CHART_PAD_Y = 54

export const TEMPLATE_MONTHS = [
  'janvier',
  'fevrier',
  'mars',
  'avril',
  'mai',
  'juin',
  'juillet',
  'aout',
  'septembre',
  'octobre',
  'novembre',
  'decembre',
]

export const TEMPLATE_PIE_COLORS = ['#2387e5', '#1f56bf', '#e97837', '#742db9', '#cc4aa7', '#6d87db']

export const TEMPLATE_EMPTY_SUMMARY: TemplateSummary = {
  ca: 0,
  profit: 0,
  profitMargin: 0,
  itemsVendues: 0,
  itemsEnStock: 0,
  valeurStock: 0,
  roi: null,
  totalAchat: null,
  cashAvailable: null,
  returnRate: null,
}

export function computeTemplateDataState(
  series: TemplateSeriesPoint[],
  loading: boolean,
  error: string,
): TemplateDataState {
  if (loading) return 'loading'
  if (error) return 'error'
  if (series.length < 1) return 'empty'
  return 'ready'
}

export function normalizeTemplateMonths(values: number[]) {
  return [...new Set(values.filter((value) => Number.isInteger(value) && value >= 0 && value <= 11))].sort(
    (a, b) => a - b,
  )
}

export function templateMonthRange(year: number, monthIndex: number) {
  const monthStart = new Date(year, monthIndex, 1)
  const monthEnd = new Date(year, monthIndex + 1, 0)
  return { monthStart, monthEnd }
}

export function formatTemplateDate(value: string) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(String(value ?? ''))) return '--'
  const date = new Date(`${value}T00:00:00`)
  if (Number.isNaN(date.getTime())) return '--'
  return date.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' })
}

export function normalizePercent(value: number | null | undefined) {
  if (value == null || !Number.isFinite(value)) return null
  if (Math.abs(value) <= 1) return value * 100
  return value
}

export function formatPercent(value: number | null, digits = 1) {
  if (value == null || !Number.isFinite(value)) return 'n/d'
  return `${value.toFixed(digits)}%`
}

export function formatYmd(date: Date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export function buildTemplateChartSourcePoints(points: TemplateSeriesPoint[]) {
  if (!points.length) return []
  if (points.length === 1) {
    return [
      points[0],
      {
        date: points[0].date,
        value: points[0].value,
      },
    ]
  }
  return points
}

export function buildTemplateChartCoords(sourcePoints: TemplateSeriesPoint[]) {
  if (!sourcePoints.length) return []
  const plotWidth = TEMPLATE_CHART_W - TEMPLATE_CHART_PAD_X * 2
  const plotHeight = TEMPLATE_CHART_H - TEMPLATE_CHART_PAD_Y * 2
  const values = sourcePoints.map((point) => Number(point.value ?? 0))
  const minVal = Math.min(...values)
  const maxVal = Math.max(...values)
  const range = Math.max(maxVal - minVal, 1)

  return sourcePoints.map((point, index) => {
    const x =
      TEMPLATE_CHART_PAD_X +
      (sourcePoints.length <= 1 ? plotWidth / 2 : (index / (sourcePoints.length - 1)) * plotWidth)
    const norm = (Number(point.value ?? 0) - minVal) / range
    const y = TEMPLATE_CHART_H - TEMPLATE_CHART_PAD_Y - norm * plotHeight
    return { x, y }
  })
}

export function buildTemplateChartLinePath(coords: Array<{ x: number; y: number }>) {
  if (!coords.length) return ''
  return coords.map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x} ${point.y}`).join(' ')
}

export function buildTemplateChartAreaPath(coords: Array<{ x: number; y: number }>) {
  if (!coords.length) return ''
  const baseline = TEMPLATE_CHART_H - TEMPLATE_CHART_PAD_Y
  const first = coords[0]
  const last = coords[coords.length - 1]
  const line = coords.map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x} ${point.y}`).join(' ')
  return `${line} L ${last.x} ${baseline} L ${first.x} ${baseline} Z`
}

export async function fetchTemplateDashboardData(
  from: string,
  to: string,
  cadence: 'day' | 'month',
): Promise<TemplateDashboardData> {
  const [kpiRes, seriesRes, profitSeriesRes, summaryRes, brandsRes, topSalesRes] = await Promise.all([
    StatsServices.kpi('grossRevenue', from, to),
    StatsServices.series('grossRevenue', from, to, cadence),
    StatsServices.series('netProfit', from, to, cadence),
    StatsServices.summary(from, to),
    StatsServices.brands(from, to),
    StatsServices.topSales(from, to, 5),
  ])

  const kpi = normalizeKpi(kpiRes?.data)
  return {
    kpiValue: Number(kpi.value || 0),
    deltaPct: kpi.deltaPct,
    series: normalizeSeries(seriesRes?.data),
    profitSeries: normalizeSeries(profitSeriesRes?.data),
    summary: normalizeSummary(summaryRes?.data) as TemplateSummary,
    brands: normalizeBrands(brandsRes?.data),
    topSales: normalizeTopSales(topSalesRes?.data),
  }
}
