import { computed, ref, watch, type Ref } from 'vue'
import { parseYmdLocal } from '@/services/statsAdapters'
import { formatEUR, formatNumber, signFmt } from '@/utils/formatters'
import {
  TEMPLATE_CHART_H,
  TEMPLATE_CHART_PAD_Y,
  TEMPLATE_CHART_W,
  TEMPLATE_EMPTY_SUMMARY,
  TEMPLATE_MONTHS,
  TEMPLATE_PIE_COLORS,
  buildTemplateChartAreaPath,
  buildTemplateChartCoords,
  buildTemplateChartLinePath,
  buildTemplateChartSourcePoints,
  computeTemplateDataState,
  fetchTemplateDashboardData,
  formatPercent,
  formatTemplateDate,
  formatYmd,
  normalizePercent,
  normalizeTemplateMonths,
  templateMonthRange,
  type TemplateBrandPoint,
  type TemplateChartHoverState,
  type TemplateDataState,
  type TemplatePieSlice,
  type TemplateSeriesPoint,
  type TemplateSummary,
  type TemplateTopSalePoint,
} from './templateModeEngine'

type TemplateUser = {
  firstName?: string | null
  lastName?: string | null
  email?: string | null
}

type UseTemplateDashboardOptions = {
  templateActive: Ref<boolean>
  localFrom: Ref<string>
  localTo: Ref<string>
  minDate: Ref<string>
  maxDate: Ref<string>
  user: Ref<TemplateUser | null | undefined>
  emitRangeUpdate: (from: string, to: string) => void
}

export function useTemplateDashboard(options: UseTemplateDashboardOptions) {
  const { templateActive, localFrom, localTo, minDate, maxDate, user, emitRangeUpdate } = options

  const templateLoading = ref(false)
  const templateError = ref('')
  const templateKpiValue = ref(0)
  const templateDeltaPct = ref<number | null>(null)
  const templateSeries = ref<TemplateSeriesPoint[]>([])
  const templateProfitSeries = ref<TemplateSeriesPoint[]>([])
  const templateSummary = ref<TemplateSummary>({ ...TEMPLATE_EMPTY_SUMMARY })
  const templateBrands = ref<TemplateBrandPoint[]>([])
  const templateTopSales = ref<TemplateTopSalePoint[]>([])
  let templateRequestId = 0

  const templateHasValidRange = computed(
    () =>
      /^\d{4}-\d{2}-\d{2}$/.test(String(localFrom.value ?? '')) &&
      /^\d{4}-\d{2}-\d{2}$/.test(String(localTo.value ?? '')) &&
      String(localFrom.value) <= String(localTo.value),
  )

  const templateMainDataState = computed<TemplateDataState>(() => {
    return computeTemplateDataState(templateProfitSeries.value, templateLoading.value, templateError.value)
  })

  const templateMiniDataState = computed<TemplateDataState>(() => {
    return computeTemplateDataState(templateSeries.value, templateLoading.value, templateError.value)
  })

  const templateDataState = computed<TemplateDataState>(() => {
    if (templateLoading.value) return 'loading'
    if (templateError.value) return 'error'
    if (templateSeries.value.length < 1) return 'empty'
    return 'ready'
  })

  const templatePointCount = computed(() => templateSeries.value.length)

  const templateTotalNumber = computed(() => {
    if (Number.isFinite(templateKpiValue.value) && templateKpiValue.value > 0) {
      return templateKpiValue.value
    }
    return templateSeries.value.reduce((sum, point) => sum + Number(point.value || 0), 0)
  })

  const templateTotalText = computed(() => formatEUR(templateTotalNumber.value))

  const templateDeltaText = computed(() => {
    if (templateDeltaPct.value == null || !Number.isFinite(templateDeltaPct.value)) return 'n/d'
    return signFmt(templateDeltaPct.value)
  })

  const templateDeltaToneClass = computed(() => {
    if (templateDeltaPct.value == null || !Number.isFinite(templateDeltaPct.value)) return ''
    if (templateDeltaPct.value > 0) return 'is-positive'
    if (templateDeltaPct.value < 0) return 'is-negative'
    return ''
  })

  const templateMarginPct = computed(() => normalizePercent(templateSummary.value.profitMargin))
  const templateRoiPct = computed(() => normalizePercent(templateSummary.value.roi))
  const templateReturnRatePct = computed(() => normalizePercent(templateSummary.value.returnRate))

  const templateCadenceText = computed(() => {
    const fromDate = parseYmdLocal(localFrom.value)
    const toDate = parseYmdLocal(localTo.value)
    const diffDays = Math.round((toDate.getTime() - fromDate.getTime()) / (1000 * 60 * 60 * 24))
    return diffDays > 180 ? 'Cadence mensuelle' : 'Cadence journaliere'
  })

  const templateUserInitials = computed(() => {
    const first = String(user.value?.firstName ?? '').trim()
    const last = String(user.value?.lastName ?? '').trim()
    const email = String(user.value?.email ?? '').trim()
    const fromNames = `${first.charAt(0)}${last.charAt(0)}`.toUpperCase()
    if (fromNames.trim()) return fromNames
    if (email) return email.slice(0, 2).toUpperCase()
    return 'NT'
  })

  const templateSelectedYear = ref(new Date().getFullYear())
  const templateSelectedMonths = ref<number[]>([])
  let templateSkipMonthSync = 0

  function clampDate(v: string) {
    if (!v) return ''
    let out = v
    if (minDate.value && out < minDate.value) out = minDate.value
    if (maxDate.value && out > maxDate.value) out = maxDate.value
    return out
  }

  function isTemplateMonthAvailable(year: number, monthIndex: number) {
    const boundsFrom = parseYmdLocal(minDate.value)
    const boundsTo = parseYmdLocal(maxDate.value)
    const hasBounds = !Number.isNaN(boundsFrom.getTime()) && !Number.isNaN(boundsTo.getTime())
    if (!hasBounds) return true
    const { monthStart, monthEnd } = templateMonthRange(year, monthIndex)
    return monthStart <= boundsTo && monthEnd >= boundsFrom
  }

  function firstAvailableTemplateMonth(year: number) {
    for (let month = 0; month < 12; month += 1) {
      if (isTemplateMonthAvailable(year, month)) return month
    }
    return null
  }

  function applyTemplateMonths(year: number, months: number[]) {
    const selected = normalizeTemplateMonths(months).filter((month) => isTemplateMonthAvailable(year, month))
    if (!selected.length) return
    templateSelectedYear.value = year
    templateSelectedMonths.value = selected
    const fromDate = new Date(year, selected[0], 1)
    const toDate = new Date(year, selected[selected.length - 1] + 1, 0)
    const nextFrom = clampDate(formatYmd(fromDate))
    const nextTo = clampDate(formatYmd(toDate))
    if (!nextFrom || !nextTo) return
    const fromSafe = nextFrom <= nextTo ? nextFrom : nextTo
    const toSafe = nextFrom <= nextTo ? nextTo : nextFrom
    templateSkipMonthSync = 2
    localFrom.value = fromSafe
    localTo.value = toSafe
    emitRangeUpdate(fromSafe, toSafe)
  }

  function syncTemplateMonthsFromRange() {
    if (templateSkipMonthSync > 0) {
      templateSkipMonthSync -= 1
      return
    }
    const fromDate = parseYmdLocal(localFrom.value)
    const toDate = parseYmdLocal(localTo.value)
    const anchor = Number.isNaN(toDate.getTime())
      ? Number.isNaN(fromDate.getTime())
        ? new Date()
        : fromDate
      : toDate
    const year = anchor.getFullYear()
    templateSelectedYear.value = year
    if (Number.isNaN(fromDate.getTime()) || Number.isNaN(toDate.getTime())) {
      templateSelectedMonths.value = []
      return
    }
    const selected: number[] = []
    for (let month = 0; month < 12; month += 1) {
      const { monthStart, monthEnd } = templateMonthRange(year, month)
      if (monthStart <= toDate && monthEnd >= fromDate) selected.push(month)
    }
    templateSelectedMonths.value = normalizeTemplateMonths(selected)
  }

  watch(
    () => [localFrom.value, localTo.value],
    () => {
      syncTemplateMonthsFromRange()
    },
    { immediate: true },
  )

  const templateYearOptions = computed(() => {
    const nowYear = new Date().getFullYear()
    const boundsFrom = parseYmdLocal(minDate.value)
    const boundsTo = parseYmdLocal(maxDate.value)
    const hasBounds = !Number.isNaN(boundsFrom.getTime()) && !Number.isNaN(boundsTo.getTime())
    let minYear = hasBounds ? boundsFrom.getFullYear() : nowYear - 3
    let maxYear = hasBounds ? boundsTo.getFullYear() : nowYear + 1
    minYear = Math.min(minYear, templateSelectedYear.value)
    maxYear = Math.max(maxYear, templateSelectedYear.value)
    const years: number[] = []
    for (let year = maxYear; year >= minYear; year -= 1) {
      years.push(year)
    }
    return years
  })

  function onTemplateYearChange(event: Event) {
    const rawYear = Number((event.target as HTMLSelectElement | null)?.value ?? Number.NaN)
    if (!Number.isFinite(rawYear)) return
    const nextYear = Math.trunc(rawYear)
    templateSelectedYear.value = nextYear
    const nextMonths = templateSelectedMonths.value.filter((month) => isTemplateMonthAvailable(nextYear, month))
    if (nextMonths.length) {
      applyTemplateMonths(nextYear, nextMonths)
      return
    }
    const refDate = parseYmdLocal(localTo.value || localFrom.value)
    const preferredMonth = Number.isNaN(refDate.getTime()) ? new Date().getMonth() : refDate.getMonth()
    if (isTemplateMonthAvailable(nextYear, preferredMonth)) {
      applyTemplateMonths(nextYear, [preferredMonth])
      return
    }
    const firstAvailable = firstAvailableTemplateMonth(nextYear)
    if (firstAvailable != null) {
      applyTemplateMonths(nextYear, [firstAvailable])
    }
  }

  const templateMonthChips = computed(() => {
    const fromDate = parseYmdLocal(localFrom.value)
    const toDate = parseYmdLocal(localTo.value)
    const boundsFrom = parseYmdLocal(minDate.value)
    const boundsTo = parseYmdLocal(maxDate.value)
    const hasBounds = !Number.isNaN(boundsFrom.getTime()) && !Number.isNaN(boundsTo.getTime())
    const year = templateSelectedYear.value
    const selected = new Set(templateSelectedMonths.value)
    if (Number.isNaN(fromDate.getTime()) || Number.isNaN(toDate.getTime())) {
      return TEMPLATE_MONTHS.map((label, index) => {
        const monthStart = new Date(year, index, 1)
        const monthEnd = new Date(year, index + 1, 0)
        const available = hasBounds ? monthStart <= boundsTo && monthEnd >= boundsFrom : true
        return { label, index, active: selected.has(index), available }
      })
    }
    return TEMPLATE_MONTHS.map((label, index) => {
      const monthStart = new Date(year, index, 1)
      const monthEnd = new Date(year, index + 1, 0)
      const activeByRange = monthStart <= toDate && monthEnd >= fromDate
      const active = selected.size ? selected.has(index) : activeByRange
      const available = hasBounds ? monthStart <= boundsTo && monthEnd >= boundsFrom : true
      return { label, index, active, available }
    })
  })

  const templateBrandsRows = computed(() => templateBrands.value.slice(0, 5))
  const templateTopSalesRows = computed(() => templateTopSales.value.slice(0, 5))

  const templatePieSlices = computed<TemplatePieSlice[]>(() => {
    const rows = templateBrandsRows.value
    if (!rows.length) {
      return [
        {
          label: 'Aucune donnee',
          value: 1,
          color: '#94a3b8',
          percentText: '100%',
          ratio: 1,
        },
      ]
    }
    const total = rows.reduce((sum, row) => sum + Math.max(Number(row.nb || 0), 0), 0)
    if (total <= 0) {
      return [
        {
          label: 'Aucune donnee',
          value: 1,
          color: '#94a3b8',
          percentText: '100%',
          ratio: 1,
        },
      ]
    }
    return rows.map((row, index) => {
      const value = Math.max(Number(row.nb || 0), 0)
      const ratio = value / total
      const percentText = `${(ratio * 100).toFixed(1)}%`
      return {
        label: row.label,
        value,
        color: TEMPLATE_PIE_COLORS[index % TEMPLATE_PIE_COLORS.length],
        percentText,
        ratio,
      }
    })
  })

  const templatePieChartStyle = computed<Record<string, string>>(() => {
    let cursor = 0
    const segments = templatePieSlices.value.map((slice) => {
      const start = cursor
      const end = cursor + slice.ratio * 360
      cursor = end
      return `${slice.color} ${start}deg ${end}deg`
    })
    return {
      background: `conic-gradient(${segments.join(', ')})`,
    }
  })

  const templateSuccessRate = computed(() => {
    const clampRate = (value: number) => Math.max(0, Math.min(100, value))
    if (templateReturnRatePct.value != null) return clampRate(100 - templateReturnRatePct.value)
    const sales = Number(templateSummary.value.itemsVendues || 0)
    const stock = Number(templateSummary.value.itemsEnStock || 0)
    const volume = sales + stock
    if (volume > 0) return clampRate((sales / volume) * 100)
    if (templateMarginPct.value != null) return clampRate(templateMarginPct.value)
    return 0
  })

  const templateSuccessRateText = computed(() => `${templateSuccessRate.value.toFixed(1)}%`)
  const templateGaugeDasharray = computed(() => `${templateSuccessRate.value.toFixed(2)} 100`)

  const templateChartGridLines = computed(() => {
    const top = TEMPLATE_CHART_PAD_Y
    const middle = TEMPLATE_CHART_H / 2
    const bottom = TEMPLATE_CHART_H - TEMPLATE_CHART_PAD_Y
    return [top, middle, bottom]
  })

  const templateProfitStartLabel = computed(() =>
    templateProfitSeries.value.length
      ? `${formatTemplateDate(templateProfitSeries.value[0].date)} • ${formatEUR(templateProfitSeries.value[0].value, { compact: true })}`
      : '--',
  )

  const templateProfitEndLabel = computed(() => {
    if (!templateProfitSeries.value.length) return '--'
    const last = templateProfitSeries.value[templateProfitSeries.value.length - 1]
    return `${formatTemplateDate(last.date)} • ${formatEUR(last.value, { compact: true })}`
  })

  const templateEmptyTitle = computed(() => {
    if (templateDataState.value === 'loading') return 'Chargement des donnees'
    if (templateDataState.value === 'error') return 'Impossible de charger la tendance'
    return 'Pas assez de points'
  })

  const templateAmplitude = computed(() => {
    if (!templateSeries.value.length) return 0
    const values = templateSeries.value.map((point) => Number(point.value ?? 0))
    return Math.max(...values) - Math.min(...values)
  })

  const templateAveragePerPoint = computed(() => {
    if (!templatePointCount.value) return 0
    return templateTotalNumber.value / templatePointCount.value
  })

  const templateProfitPerSale = computed(() => {
    const sales = Number(templateSummary.value.itemsVendues || 0)
    if (!sales) return 0
    return Number(templateSummary.value.profit || 0) / sales
  })

  const templateKpiCards = computed(() => [
    { key: 'ca', label: "Chiffre d'affaires", value: templateTotalText.value, tone: '' },
    {
      key: 'profit',
      label: 'Profit net',
      value: formatEUR(templateSummary.value.profit, { compact: true }),
      tone:
        templateSummary.value.profit > 0
          ? 'is-positive'
          : templateSummary.value.profit < 0
            ? 'is-negative'
            : '',
    },
    { key: 'delta', label: 'Variation', value: templateDeltaText.value, tone: templateDeltaToneClass.value },
    {
      key: 'margin',
      label: 'Marge nette',
      value: formatPercent(templateMarginPct.value),
      tone:
        templateMarginPct.value == null
          ? ''
          : templateMarginPct.value > 0
            ? 'is-positive'
            : templateMarginPct.value < 0
              ? 'is-negative'
              : '',
    },
    {
      key: 'sales',
      label: 'Ventes',
      value: formatNumber(templateSummary.value.itemsVendues),
      tone: '',
    },
    {
      key: 'stock',
      label: 'Valeur stock',
      value: formatEUR(templateSummary.value.valeurStock, { compact: true }),
      tone: '',
    },
  ])

  const templateQuickFacts = computed(() => [
    {
      key: 'avg',
      label: 'Moyenne / point',
      value: formatEUR(templateAveragePerPoint.value, { compact: true }),
    },
    {
      key: 'amplitude',
      label: 'Amplitude',
      value: formatEUR(templateAmplitude.value, { compact: true }),
    },
    {
      key: 'roi',
      label: 'ROI',
      value: formatPercent(templateRoiPct.value),
    },
    {
      key: 'returns',
      label: 'Taux retour',
      value: formatPercent(templateReturnRatePct.value),
    },
    {
      key: 'profitSale',
      label: 'Profit / vente',
      value: formatEUR(templateProfitPerSale.value, { compact: true }),
    },
    {
      key: 'stockItems',
      label: 'Stock restant',
      value: formatNumber(templateSummary.value.itemsEnStock),
    },
  ])

  const templateMainHover = ref<TemplateChartHoverState>({
    visible: false,
    x: 0,
    y: 0,
    xPct: 0,
    yPct: 0,
    dateText: '',
    valueText: '',
  })
  const templateMiniHover = ref<TemplateChartHoverState>({
    visible: false,
    x: 0,
    y: 0,
    xPct: 0,
    yPct: 0,
    dateText: '',
    valueText: '',
  })

  const templateMainChartSourcePoints = computed(() => buildTemplateChartSourcePoints(templateProfitSeries.value))
  const templateMiniChartSourcePoints = computed(() => buildTemplateChartSourcePoints(templateSeries.value))

  const templateMainChartCoords = computed(() => buildTemplateChartCoords(templateMainChartSourcePoints.value))
  const templateMiniChartCoords = computed(() => buildTemplateChartCoords(templateMiniChartSourcePoints.value))

  const templateMainChartLinePath = computed(() => buildTemplateChartLinePath(templateMainChartCoords.value))
  const templateMiniChartLinePath = computed(() => buildTemplateChartLinePath(templateMiniChartCoords.value))

  const templateMainChartAreaPath = computed(() => buildTemplateChartAreaPath(templateMainChartCoords.value))
  const templateMiniChartAreaPath = computed(() => buildTemplateChartAreaPath(templateMiniChartCoords.value))

  const templateMainLastPointCoord = computed(() => {
    const coords = templateMainChartCoords.value
    if (!coords.length) return null
    return coords[coords.length - 1]
  })

  const templateMiniLastPointCoord = computed(() => {
    const coords = templateMiniChartCoords.value
    if (!coords.length) return null
    return coords[coords.length - 1]
  })

  function getClosestTemplateChartPoint(clientX: number, bounds: DOMRect, target: 'main' | 'mini') {
    const coords = target === 'main' ? templateMainChartCoords.value : templateMiniChartCoords.value
    const source = target === 'main' ? templateMainChartSourcePoints.value : templateMiniChartSourcePoints.value
    if (!coords.length || !source.length || !bounds.width) return null
    const ratio = Math.max(0, Math.min(1, (clientX - bounds.left) / bounds.width))
    const hoverX = ratio * TEMPLATE_CHART_W
    let bestIndex = 0
    let bestDist = Number.POSITIVE_INFINITY
    for (let i = 0; i < coords.length; i += 1) {
      const dist = Math.abs(coords[i].x - hoverX)
      if (dist < bestDist) {
        bestDist = dist
        bestIndex = i
      }
    }
    return {
      coord: coords[bestIndex],
      point: source[bestIndex],
    }
  }

  function onTemplateChartHover(event: MouseEvent, target: 'main' | 'mini') {
    if (target === 'main' && templateMainDataState.value !== 'ready') return
    if (target === 'mini' && templateMiniDataState.value !== 'ready') return
    const el = event.currentTarget as SVGElement | null
    if (!el) return
    const closest = getClosestTemplateChartPoint(event.clientX, el.getBoundingClientRect(), target)
    if (!closest) return
    const next: TemplateChartHoverState = {
      visible: true,
      x: closest.coord.x,
      y: closest.coord.y,
      xPct: (closest.coord.x / TEMPLATE_CHART_W) * 100,
      yPct: (closest.coord.y / TEMPLATE_CHART_H) * 100,
      dateText: formatTemplateDate(closest.point.date),
      valueText: formatEUR(closest.point.value, { compact: true }),
    }
    if (target === 'main') {
      templateMainHover.value = next
      return
    }
    templateMiniHover.value = next
  }

  function clearTemplateChartHover(target?: 'main' | 'mini') {
    if (!target || target === 'main') {
      templateMainHover.value = {
        visible: false,
        x: 0,
        y: 0,
        xPct: 0,
        yPct: 0,
        dateText: '',
        valueText: '',
      }
    }
    if (!target || target === 'mini') {
      templateMiniHover.value = {
        visible: false,
        x: 0,
        y: 0,
        xPct: 0,
        yPct: 0,
        dateText: '',
        valueText: '',
      }
    }
  }

  function setTemplateMonth(monthIndex: number, event?: MouseEvent) {
    const year = templateSelectedYear.value
    if (!isTemplateMonthAvailable(year, monthIndex)) return
    const isMultiSelect = Boolean(event?.ctrlKey || event?.metaKey)
    if (!isMultiSelect) {
      applyTemplateMonths(year, [monthIndex])
      return
    }

    const next = new Set(templateSelectedMonths.value)
    if (next.has(monthIndex)) next.delete(monthIndex)
    else next.add(monthIndex)

    if (!next.size) next.add(monthIndex)
    applyTemplateMonths(year, [...next])
  }

  async function loadTemplateDashboard() {
    if (!templateActive.value) return
    if (!templateHasValidRange.value) {
      templateLoading.value = false
      templateError.value = 'Plage de dates invalide.'
      templateSeries.value = []
      templateProfitSeries.value = []
      templateKpiValue.value = 0
      templateDeltaPct.value = null
      templateSummary.value = { ...TEMPLATE_EMPTY_SUMMARY }
      templateBrands.value = []
      templateTopSales.value = []
      return
    }

    const requestId = ++templateRequestId
    templateLoading.value = true
    templateError.value = ''

    try {
      const cadence = templateCadenceText.value === 'Cadence mensuelle' ? 'month' : 'day'
      const data = await fetchTemplateDashboardData(localFrom.value, localTo.value, cadence)
      if (requestId !== templateRequestId) return

      templateKpiValue.value = data.kpiValue
      templateDeltaPct.value = data.deltaPct
      templateSeries.value = data.series
      templateProfitSeries.value = data.profitSeries
      templateSummary.value = data.summary
      templateBrands.value = data.brands
      templateTopSales.value = data.topSales
    } catch (error: unknown) {
      const message = extractTemplateErrorMessage(error)
      if (requestId !== templateRequestId) return
      templateError.value = message
      templateSeries.value = []
      templateProfitSeries.value = []
      templateKpiValue.value = 0
      templateDeltaPct.value = null
      templateSummary.value = { ...TEMPLATE_EMPTY_SUMMARY }
      templateBrands.value = []
      templateTopSales.value = []
    } finally {
      if (requestId === templateRequestId) {
        templateLoading.value = false
      }
    }
  }

  function extractTemplateErrorMessage(error: unknown): string {
    if (typeof error === 'object' && error !== null && 'response' in error) {
      const response = (error as { response?: unknown }).response
      if (typeof response === 'object' && response !== null && 'data' in response) {
        const data = (response as { data?: unknown }).data
        if (typeof data === 'object' && data !== null && 'message' in data) {
          const message = (data as { message?: unknown }).message
          if (typeof message === 'string' && message.trim()) return message
        }
      }
    }
    if (error instanceof Error && error.message.trim()) return error.message
    return 'Erreur de chargement'
  }

  watch(
    () => [templateActive.value, localFrom.value, localTo.value],
    ([active]) => {
      clearTemplateChartHover()
      if (!active) return
      void loadTemplateDashboard()
    },
  )

  watch(
    () => templateActive.value,
    (active) => {
      window.dispatchEvent(
        new CustomEvent('snk:stats-template-mode', {
          detail: { active },
        }),
      )
    },
    { immediate: true },
  )

  return {
    templateLoading,
    templateError,
    templateKpiValue,
    templateDeltaPct,
    templateSeries,
    templateProfitSeries,
    templateSummary,
    templateBrands,
    templateTopSales,
    templateHasValidRange,
    templateMainDataState,
    templateMiniDataState,
    templateDataState,
    templatePointCount,
    templateTotalNumber,
    templateTotalText,
    templateDeltaText,
    templateDeltaToneClass,
    templateMarginPct,
    templateRoiPct,
    templateReturnRatePct,
    templateCadenceText,
    templateUserInitials,
    templateSelectedYear,
    templateSelectedMonths,
    templateYearOptions,
    templateMonthChips,
    templatePieSlices,
    templatePieChartStyle,
    templateSuccessRateText,
    templateGaugeDasharray,
    templateChartGridLines,
    templateProfitStartLabel,
    templateProfitEndLabel,
    templateEmptyTitle,
    templateKpiCards,
    templateQuickFacts,
    templateBrandsRows,
    templateTopSalesRows,
    templateMainHover,
    templateMiniHover,
    templateMainChartSourcePoints,
    templateMiniChartSourcePoints,
    templateMainChartCoords,
    templateMiniChartCoords,
    templateMainChartLinePath,
    templateMiniChartLinePath,
    templateMainChartAreaPath,
    templateMiniChartAreaPath,
    templateMainLastPointCoord,
    templateMiniLastPointCoord,
    onTemplateChartHover,
    clearTemplateChartHover,
    setTemplateMonth,
    onTemplateYearChange,
    loadTemplateDashboard,
  }
}
