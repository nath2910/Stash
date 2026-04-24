<template>
  <WidgetCard
    title="Benefice net"
    subtitle="Suivi de rentabilite"
    :hide-header="true"
    :accent="accent"
    :surface="isLineView ? 'trend' : 'kpi'"
    :loading="loading"
    :error="error"
    :widget-width="props.widgetWidth"
    :widget-height="props.widgetHeight"
    :widget-base-width="props.widgetBaseWidth"
    :widget-base-height="props.widgetBaseHeight"
  >
    <div ref="contentEl" class="npw-root">
      <template v-if="dataState === 'ready'">
        <NetProfitTrendWidget
          v-if="isLineView"
          :metrics="metrics"
          :period-text="periodText"
          :bucket="activeBucket"
          :layout="layout"
          :show-comparison="props.showComparison"
          :show-area="props.showArea"
          :smooth-line="props.smoothLine"
        />
        <NetProfitKpiWidget
          v-else
          :metrics="metrics"
          :period-text="periodText"
          :bucket="activeBucket"
          :layout="layout"
          :show-comparison="props.showComparison"
          :show-sales-kpi="props.showSalesKpi"
          :show-avg-profit-per-sale="props.showAvgProfitPerSale"
          :show-net-margin="props.showNetMargin"
          :show-best-period="props.showBestPeriod"
          :secondary-limit="props.secondaryLimit"
        />
      </template>

      <div v-else-if="dataState === 'empty'" class="npw-state">
        <p class="npw-state__title">Aucune donnee exploitable</p>
        <p class="npw-state__copy">Aucune vente nette sur la periode selectionnee.</p>
      </div>

      <div v-else class="npw-state">
        <p class="npw-state__title">Periode invalide</p>
        <p class="npw-state__copy">Selectionne une plage de dates valide.</p>
      </div>
    </div>
  </WidgetCard>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeKpi, normalizeSeries, normalizeSummary, prevPeriod } from '@/services/statsAdapters'
import { formatDateFR } from '@/utils/formatters'
import WidgetCard from './_parts/WidgetCard.vue'
import NetProfitKpiWidget from './NetProfitKpiWidget.vue'
import NetProfitTrendWidget from './NetProfitTrendWidget.vue'

type Bucket = 'day' | 'week' | 'month' | string

type DataState = 'ready' | 'empty' | 'no-data'

type LayoutMode = 'compact' | 'medium' | 'large'

interface SeriesPoint {
  date: string
  value: number
}

interface NetProfitMetrics {
  value: number
  previousValue: number
  deltaValue: number
  deltaPct: number | null
  series: SeriesPoint[]
  bestPoint: SeriesPoint | null
  lastPoint: SeriesPoint | null
  salesCount: number | null
  avgProfitPerSale: number | null
  marginPct: number | null
}

interface WidgetProps {
  from?: string
  to?: string
  view?: 'number' | 'line' | string
  bucket?: Bucket
  categories?: string[]
  types?: string[]
  showComparison?: boolean
  showArea?: boolean
  smoothLine?: boolean
  showSalesKpi?: boolean
  showAvgProfitPerSale?: boolean
  showNetMargin?: boolean
  showBestPeriod?: boolean
  secondaryLimit?: number
  widgetWidth?: number
  widgetHeight?: number
  widgetBaseWidth?: number
  widgetBaseHeight?: number
  widgetRenderWidth?: number
  widgetRenderHeight?: number
}

interface LayoutInfo {
  width: number
  height: number
  mode: LayoutMode
  tiny: boolean
}

const props = withDefaults(defineProps<WidgetProps>(), {
  from: '',
  to: '',
  view: 'line',
  bucket: 'week',
  categories: () => [],
  types: () => [],
  showComparison: true,
  showArea: true,
  smoothLine: true,
  showSalesKpi: true,
  showAvgProfitPerSale: true,
  showNetMargin: true,
  showBestPeriod: true,
  secondaryLimit: 4,
  widgetWidth: 760,
  widgetHeight: 500,
  widgetBaseWidth: 0,
  widgetBaseHeight: 0,
  widgetRenderWidth: 0,
  widgetRenderHeight: 0,
})

const EPS = 0.0001
const loading = ref(false)
const error = ref('')

const currentValue = ref(0)
const previousValue = ref(0)
const serverDeltaPct = ref<number | null>(null)
const rawSeries = ref<SeriesPoint[]>([])
const summaryRaw = ref<ReturnType<typeof normalizeSummary> | null>(null)

const contentEl = ref<HTMLElement | null>(null)
const observedWidth = ref(0)
const observedHeight = ref(0)
let resizeObserver: ResizeObserver | null = null
let req = 0

const clamp = (value: number, min: number, max: number) => Math.max(min, Math.min(max, value))

function sanitizeBucket(value: Bucket): 'day' | 'week' | 'month' {
  const raw = String(value ?? '').trim()
  if (raw === 'day' || raw === 'week' || raw === 'month') return raw
  return 'week'
}

function sanitizeView(value: string) {
  return value === 'number' ? 'number' : 'line'
}

function connectResizeObserver() {
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }

  if (typeof ResizeObserver === 'undefined' || !contentEl.value) return

  resizeObserver = new ResizeObserver((entries) => {
    const entry = entries[0]
    if (!entry) return
    const nextWidth = Math.max(0, Math.round(entry.contentRect.width))
    const nextHeight = Math.max(0, Math.round(entry.contentRect.height))
    observedWidth.value = nextWidth
    observedHeight.value = nextHeight
  })

  resizeObserver.observe(contentEl.value)
}

onMounted(async () => {
  await nextTick()
  connectResizeObserver()
  await load()
})

onBeforeUnmount(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
})

watch(contentEl, () => {
  connectResizeObserver()
})

const hasValidRange = computed(
  () =>
    /^\d{4}-\d{2}-\d{2}$/.test(String(props.from ?? '')) &&
    /^\d{4}-\d{2}-\d{2}$/.test(String(props.to ?? '')),
)

const activeBucket = computed(() => sanitizeBucket(props.bucket))
const isLineView = computed(() => sanitizeView(String(props.view ?? 'line')) === 'line')

async function load() {
  const id = ++req

  if (!hasValidRange.value) {
    loading.value = false
    error.value = ''
    currentValue.value = 0
    previousValue.value = 0
    serverDeltaPct.value = null
    rawSeries.value = []
    summaryRaw.value = null
    return
  }

  loading.value = true
  error.value = ''

  try {
    const { from: prevFrom, to: prevTo } = prevPeriod(props.from, props.to)

    const [kpiNowRes, kpiPrevRes, seriesRes, summaryRes] = await Promise.all([
      StatsServices.kpi('netProfit', props.from, props.to, props.categories, props.types),
      StatsServices.kpi('netProfit', prevFrom, prevTo, props.categories, props.types),
      StatsServices.series(
        'netProfit',
        props.from,
        props.to,
        activeBucket.value,
        props.categories,
        props.types,
      ),
      StatsServices.summary(props.from, props.to, props.categories, props.types),
    ])

    if (id !== req) return

    const nowKpi = normalizeKpi(kpiNowRes?.data)
    const prevKpi = normalizeKpi(kpiPrevRes?.data)

    currentValue.value = Number(nowKpi.value ?? 0)
    previousValue.value = Number(prevKpi.value ?? 0)
    serverDeltaPct.value = nowKpi.deltaPct == null ? null : Number(nowKpi.deltaPct)
    rawSeries.value = normalizeSeries(seriesRes?.data)
    summaryRaw.value =
      summaryRes?.data && typeof summaryRes.data === 'object'
        ? normalizeSummary(summaryRes.data)
        : null
  } catch (e: unknown) {
    if (id !== req) return
    const err = e as { response?: { data?: { message?: string } }; message?: string }
    error.value = err?.response?.data?.message ?? err?.message ?? 'Impossible de charger les donnees.'
  } finally {
    if (id === req) loading.value = false
  }
}

const filtersKey = computed(() => `${props.categories.join('|')}::${props.types.join('|')}`)
watch(() => [props.from, props.to, activeBucket.value, filtersKey.value], load)

const renderWidth = computed(() => {
  const fromObserver = Number(observedWidth.value)
  if (Number.isFinite(fromObserver) && fromObserver > 0) return fromObserver
  const fromRender = Number(props.widgetRenderWidth ?? 0)
  if (Number.isFinite(fromRender) && fromRender > 0) return fromRender
  return Math.max(Number(props.widgetWidth ?? 700), 1)
})

const renderHeight = computed(() => {
  const fromObserver = Number(observedHeight.value)
  if (Number.isFinite(fromObserver) && fromObserver > 0) return fromObserver
  const fromRender = Number(props.widgetRenderHeight ?? 0)
  if (Number.isFinite(fromRender) && fromRender > 0) return fromRender
  return Math.max(Number(props.widgetHeight ?? 320), 1)
})

const layout = computed<LayoutInfo>(() => {
  const width = clamp(Math.round(renderWidth.value), 1, 5000)
  const height = clamp(Math.round(renderHeight.value), 1, 5000)

  let mode: LayoutMode = 'medium'
  if (width >= 760 && height >= 300) mode = 'large'
  else if (width < 520 || height < 230) mode = 'compact'

  return {
    width,
    height,
    mode,
    tiny: width < 430 || height < 190,
  }
})

const deltaValue = computed(() => currentValue.value - previousValue.value)
const deltaPct = computed<number | null>(() => {
  const prevAbs = Math.abs(previousValue.value)
  if (prevAbs < EPS) {
    if (Math.abs(currentValue.value) < EPS) return 0
    return serverDeltaPct.value
  }
  return (deltaValue.value / prevAbs) * 100
})

const salesCount = computed<number | null>(() => {
  const raw = Number(summaryRaw.value?.itemsVendues)
  if (!Number.isFinite(raw)) return null
  return raw
})

const avgProfitPerSale = computed<number | null>(() => {
  if (salesCount.value == null || salesCount.value <= 0) return null
  return currentValue.value / salesCount.value
})

const marginPct = computed<number | null>(() => {
  const raw = Number(summaryRaw.value?.profitMargin)
  if (!Number.isFinite(raw)) return null
  return Math.abs(raw) <= 1 ? raw * 100 : raw
})

const trendSeries = computed<SeriesPoint[]>(() => {
  const clean = rawSeries.value.filter((point) => Number.isFinite(Number(point.value)))
  if (clean.length >= 2) return clean

  const fromDate = String(props.from ?? '').trim()
  const toDate = String(props.to ?? '').trim() || fromDate

  if (clean.length === 1) {
    const only = clean[0]
    return [
      { date: fromDate || only.date, value: previousValue.value },
      { date: toDate || only.date, value: Number(only.value ?? currentValue.value) },
    ]
  }

  if (fromDate && toDate && (Math.abs(previousValue.value) > EPS || Math.abs(currentValue.value) > EPS)) {
    return [
      { date: fromDate, value: previousValue.value },
      { date: toDate, value: currentValue.value },
    ]
  }

  return clean
})

const bestPoint = computed<SeriesPoint | null>(() => {
  if (!trendSeries.value.length) return null
  return trendSeries.value.reduce((best, point) => (point.value > best.value ? point : best), trendSeries.value[0])
})

const lastPoint = computed<SeriesPoint | null>(() => {
  if (!trendSeries.value.length) return null
  return trendSeries.value[trendSeries.value.length - 1]
})

const metrics = computed<NetProfitMetrics>(() => ({
  value: currentValue.value,
  previousValue: previousValue.value,
  deltaValue: deltaValue.value,
  deltaPct: deltaPct.value,
  series: trendSeries.value,
  bestPoint: bestPoint.value,
  lastPoint: lastPoint.value,
  salesCount: salesCount.value,
  avgProfitPerSale: avgProfitPerSale.value,
  marginPct: marginPct.value,
}))

const hasSignal = computed(() => {
  if (Math.abs(currentValue.value) > EPS || Math.abs(previousValue.value) > EPS) return true
  if (trendSeries.value.length > 0) return true
  if (salesCount.value != null && salesCount.value > 0) return true
  return false
})

const dataState = computed<DataState>(() => {
  if (!hasValidRange.value) return 'no-data'
  if (!hasSignal.value) return 'empty'
  return 'ready'
})

const periodText = computed(() => {
  const shortYear = layout.value.mode === 'compact'
  const fromLabel = formatDateFR(props.from, {
    day: '2-digit',
    month: 'short',
    year: shortYear ? '2-digit' : 'numeric',
  })
  const toLabel = formatDateFR(props.to, {
    day: '2-digit',
    month: 'short',
    year: shortYear ? '2-digit' : 'numeric',
  })
  return `${fromLabel} au ${toLabel}`
})

const accent = computed(() => {
  if (metrics.value.value > EPS) return '#00d26a'
  if (metrics.value.value < -EPS) return '#e11d48'
  return '#2563eb'
})
</script>

<style scoped>
.npw-root {
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.npw-state {
  height: 100%;
  min-height: 0;
  display: grid;
  place-content: center;
  gap: 4px;
  text-align: center;
  padding: 10px;
}

.npw-state__title {
  margin: 0;
  font-size: 14px;
  font-weight: 650;
  color: rgba(241, 245, 249, 0.96);
}

.npw-state__copy {
  margin: 0;
  font-size: 12px;
  color: rgba(148, 163, 184, 0.88);
}
</style>
