<template>
  <section class="npt-root" :class="layoutClass" :style="layoutVars">
    <header class="npt-head">
      <div class="npt-head__main">
        <p class="npt-overline">Benefice net</p>
        <p class="npt-value">{{ valueText }}</p>
      </div>
      <div class="npt-head__side">
        <div v-if="showComparison" class="npt-delta" :class="`is-${deltaTone}`">
          <span class="npt-delta__value">{{ deltaValueText }}</span>
          <span class="npt-delta__pct">{{ deltaPctText }}</span>
        </div>
        <p class="npt-period" :title="periodText">{{ periodText }}</p>
      </div>
    </header>

    <section ref="chartShellEl" class="npt-chart-shell" :style="chartShellStyle">
      <VChart ref="chartRef" class="npt-chart" :option="option" autoresize />
      <div v-if="props.metrics.series.length <= 1" class="npt-chart-overlay">
        <p class="npt-overlay__title">Pas assez de points</p>
        <p class="npt-overlay__copy">Elargis la periode pour afficher une tendance fiable.</p>
      </div>
    </section>

    <footer v-if="showFooter" class="npt-footer">
      <article class="npt-card">
        <p class="npt-card__label">Points</p>
        <p class="npt-card__value">{{ pointCountText }}</p>
      </article>
      <article class="npt-card">
        <p class="npt-card__label">Pic</p>
        <p class="npt-card__value" :class="`is-${peakTone}`">{{ peakValueText }}</p>
        <p class="npt-card__meta">{{ peakDateText }}</p>
      </article>
      <article class="npt-card">
        <p class="npt-card__label">Dernier point</p>
        <p class="npt-card__value" :class="`is-${lastTone}`">{{ lastPointText }}</p>
        <p class="npt-card__meta">{{ cadenceLabel }}</p>
      </article>
    </footer>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { formatEUR } from '@/utils/formatters'

type Bucket = 'day' | 'week' | 'month'
type Tone = 'positive' | 'negative' | 'neutral'
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

interface LayoutInfo {
  width: number
  height: number
  mode: LayoutMode
  tiny: boolean
}

interface ChartApi {
  resize: () => void
  getEchartsInstance?: () => {
    on: (event: string, handler: (payload: unknown) => void) => void
    off: (event: string, handler: (payload: unknown) => void) => void
  } | null
}

const props = withDefaults(
  defineProps<{
    metrics: NetProfitMetrics
    periodText: string
    bucket: Bucket
    layout: LayoutInfo
    showComparison?: boolean
    showArea?: boolean
    smoothLine?: boolean
  }>(),
  {
    showComparison: true,
    showArea: true,
    smoothLine: true,
  },
)

const EPS = 0.0001
const chartRef = ref<ChartApi | null>(null)
const chartShellEl = ref<HTMLElement | null>(null)
let chartResizeObserver: ResizeObserver | null = null
const hoveredIndex = ref<number | null>(null)
let axisPointerHandler: ((payload: unknown) => void) | null = null
let globalOutHandler: ((payload: unknown) => void) | null = null

const clamp = (value: number, min: number, max: number) => Math.max(min, Math.min(max, value))

function toneFromValue(value: number): Tone {
  if (value > EPS) return 'positive'
  if (value < -EPS) return 'negative'
  return 'neutral'
}

function signedCurrency(value: number) {
  const abs = formatEUR(Math.abs(value), { compact: true, digits: 1 })
  if (value > 0) return `+${abs}`
  if (value < 0) return `-${abs}`
  return abs
}

function signedPercent(value: number | null) {
  if (value == null || !Number.isFinite(value)) return 'n/d'
  const sign = value > 0 ? '+' : ''
  return `${sign}${value.toFixed(1)}%`
}

function formatAxisDate(date: string) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(String(date ?? ''))) return String(date ?? '--')
  const raw = new Date(`${date}T00:00:00`)
  if (Number.isNaN(raw.getTime())) return date
  if (props.bucket === 'month') {
    return raw.toLocaleDateString('fr-FR', { month: 'short', year: '2-digit' })
  }
  return raw.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' })
}

function compactCurrency(value: number) {
  if (!Number.isFinite(Number(value))) return '--'
  return formatEUR(Number(value), { compact: true, digits: 1 })
}

function percentFromDelta(current: number, previous: number) {
  const base = Math.abs(previous)
  if (base < EPS) return null
  return ((current - previous) / base) * 100
}

function requestChartResize() {
  nextTick(() => {
    chartRef.value?.resize?.()
  })
}

function clearChartFocus() {
  hoveredIndex.value = null
}

function resolveHoverIndex(payload: unknown): number | null {
  const source = payload as {
    axesInfo?: Array<{ value?: unknown; dataIndex?: unknown }>
  }
  const axis = Array.isArray(source?.axesInfo) ? source.axesInfo[0] : null
  if (!axis) return null

  const points = props.metrics.series
  if (!points.length) return null

  const rawDataIndex = Number(axis.dataIndex)
  if (Number.isFinite(rawDataIndex)) {
    const rounded = Math.round(rawDataIndex)
    if (rounded >= 0 && rounded < points.length) return rounded
  }

  const rawValue = axis.value
  const rawIndex = Number(rawValue)
  if (Number.isFinite(rawIndex)) {
    const rounded = Math.round(rawIndex)
    if (rounded >= 0 && rounded < points.length) return rounded
  }

  const asString = String(rawValue ?? '')
  if (!asString) return null
  const byDate = points.findIndex((point) => point.date === asString)
  return byDate >= 0 ? byDate : null
}

function disconnectChartEvents() {
  const instance = chartRef.value?.getEchartsInstance?.()
  if (!instance) {
    axisPointerHandler = null
    globalOutHandler = null
    return
  }

  if (axisPointerHandler) {
    instance.off('updateAxisPointer', axisPointerHandler)
    axisPointerHandler = null
  }
  if (globalOutHandler) {
    instance.off('globalout', globalOutHandler)
    globalOutHandler = null
  }
}

function connectChartEvents() {
  disconnectChartEvents()
  const instance = chartRef.value?.getEchartsInstance?.()
  if (!instance) return

  axisPointerHandler = (payload: unknown) => {
    hoveredIndex.value = resolveHoverIndex(payload)
  }
  globalOutHandler = () => {
    clearChartFocus()
  }

  instance.on('updateAxisPointer', axisPointerHandler)
  instance.on('globalout', globalOutHandler)
}

function connectChartObserver() {
  if (chartResizeObserver) {
    chartResizeObserver.disconnect()
    chartResizeObserver = null
  }

  if (typeof ResizeObserver === 'undefined' || !chartShellEl.value) return

  chartResizeObserver = new ResizeObserver(() => {
    requestChartResize()
  })

  chartResizeObserver.observe(chartShellEl.value)
}

onMounted(() => {
  connectChartObserver()
  requestChartResize()
  nextTick(() => {
    connectChartEvents()
  })
})

onBeforeUnmount(() => {
  if (chartResizeObserver) {
    chartResizeObserver.disconnect()
    chartResizeObserver = null
  }
  disconnectChartEvents()
})

watch(chartShellEl, () => {
  connectChartObserver()
})

watch(chartRef, () => {
  nextTick(() => {
    connectChartEvents()
  })
})

watch(
  () => [
    props.layout.width,
    props.layout.height,
    props.layout.mode,
    props.layout.tiny,
    props.metrics.series.length,
    props.showArea,
    props.smoothLine,
  ],
  () => {
    requestChartResize()
    if (!props.metrics.series.length) clearChartFocus()
  },
)

watch(
  () => props.metrics.series,
  () => {
    clearChartFocus()
  },
)

const valueTone = computed(() => toneFromValue(props.metrics.value))
const deltaTone = computed(() => toneFromValue(props.metrics.deltaValue))
const showComparison = computed(() => props.showComparison !== false)
const showFooter = computed(
  () => props.layout.mode === 'large' && !props.layout.tiny && props.layout.height >= 470,
)

const palette = computed(() => {
  if (valueTone.value === 'positive') {
    return {
      line: '#00d26a',
      lineSoft: '#34ff9c',
      areaTop: 'rgba(0, 210, 106, 0.24)',
      areaBottom: 'rgba(0, 210, 106, 0.03)',
    }
  }
  if (valueTone.value === 'negative') {
    return {
      line: '#e11d48',
      lineSoft: '#f43f5e',
      areaTop: 'rgba(225, 29, 72, 0.2)',
      areaBottom: 'rgba(225, 29, 72, 0.02)',
    }
  }
  return {
    line: '#2563eb',
    lineSoft: '#3b82f6',
    areaTop: 'rgba(37, 99, 235, 0.18)',
    areaBottom: 'rgba(37, 99, 235, 0.02)',
  }
})

const valueText = computed(() => {
  const compact = props.layout.mode !== 'large' || Math.abs(props.metrics.value) >= 1_000_000
  return formatEUR(props.metrics.value, { compact, digits: compact ? 1 : 0 })
})

const deltaValueText = computed(() => signedCurrency(props.metrics.deltaValue))
const deltaPctText = computed(() => signedPercent(props.metrics.deltaPct))

const pointCountText = computed(() => String(props.metrics.series.length))
const peakValueText = computed(() =>
  props.metrics.bestPoint ? formatEUR(props.metrics.bestPoint.value, { compact: true, digits: 1 }) : '--',
)
const peakDateText = computed(() =>
  props.metrics.bestPoint ? formatAxisDate(props.metrics.bestPoint.date) : 'Aucun pic',
)
const peakTone = computed(() => toneFromValue(Number(props.metrics.bestPoint?.value ?? 0)))

const lastPointText = computed(() =>
  props.metrics.lastPoint ? formatEUR(props.metrics.lastPoint.value, { compact: true, digits: 1 }) : '--',
)
const lastTone = computed(() => toneFromValue(Number(props.metrics.lastPoint?.value ?? 0)))

const cadenceLabel = computed(() => {
  if (props.bucket === 'month') return 'cadence mensuelle'
  if (props.bucket === 'week') return 'cadence hebdomadaire'
  return 'cadence journaliere'
})

const hoverPoint = computed(() => {
  if (hoveredIndex.value == null) return null
  const index = Math.max(0, Math.min(props.metrics.series.length - 1, hoveredIndex.value))
  const point = props.metrics.series[index]
  if (!point) return null
  return {
    index,
    date: point.date,
    value: Number(point.value ?? 0),
  }
})

const chartHeight = computed(() => {
  const headerBudget = props.layout.tiny ? 70 : props.layout.mode === 'compact' ? 82 : 94
  const footerBudget = showFooter.value ? 78 : 0
  const available = Math.round(props.layout.height - headerBudget - footerBudget - 8)
  const floor = props.layout.tiny ? 88 : props.layout.mode === 'compact' ? 118 : 168
  if (available < floor) return Math.max(props.layout.tiny ? 72 : 96, available)
  return Math.min(available, 560)
})

const chartShellStyle = computed(() => ({
  minHeight: `${chartHeight.value}px`,
}))

const chartGrid = computed(() => {
  const axisFont = props.layout.tiny ? 10 : 11
  const bottom = props.layout.tiny ? 22 : props.layout.mode === 'compact' ? 26 : 30
  return {
    left: props.layout.tiny ? 40 : props.layout.mode === 'compact' ? 48 : 56,
    right: props.layout.tiny ? 10 : 14,
    top: props.layout.tiny ? 8 : 10,
    bottom,
    containLabel: true,
    axisFont,
  }
})

const option = computed(() => {
  const x = props.metrics.series.map((point) => point.date)
  const y = props.metrics.series.map((point) => Number(point.value ?? 0))
  const focus = hoverPoint.value
  const maxLabels = Math.max(2, Math.floor(props.layout.width / (props.layout.tiny ? 170 : 125)))
  const interval = x.length > maxLabels ? Math.ceil(x.length / maxLabels) - 1 : 0
  const symbolSize = props.layout.tiny ? 4 : 5

  return {
    backgroundColor: 'transparent',
    animationDuration: 220,
    animationDurationUpdate: 160,
    grid: {
      left: chartGrid.value.left,
      right: chartGrid.value.right,
      top: chartGrid.value.top,
      bottom: chartGrid.value.bottom,
      containLabel: chartGrid.value.containLabel,
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15, 23, 42, 0.96)',
      borderColor: 'rgba(148, 163, 184, 0.3)',
      borderWidth: 1,
      textStyle: {
        color: '#e2e8f0',
        fontSize: 12,
      },
      axisPointer: {
        type: 'line',
        snap: true,
        lineStyle: {
          color: 'rgba(100, 116, 139, 0.72)',
          type: 'dashed',
        },
      },
      formatter: (params: unknown) => {
        const entry = Array.isArray(params) ? params[0] : params
        const row = entry as { axisValue?: string; data?: number; dataIndex?: number }
        const label = formatAxisDate(String(row?.axisValue ?? ''))
        const value = formatEUR(Number(row?.data ?? 0))
        const idx = Number(row?.dataIndex ?? -1)
        let detail = ''
        if (Number.isFinite(idx) && idx > 0 && idx < y.length) {
          const prev = Number(y[idx - 1] ?? 0)
          const curr = Number(y[idx] ?? 0)
          const delta = curr - prev
          const pct = percentFromDelta(curr, prev)
          detail = `<div style="margin-top:4px;font-size:11px;color:#cbd5e1;">Variation: ${signedCurrency(delta)}${
            pct == null || !Number.isFinite(pct) ? '' : ` (${signedPercent(pct)})`
          }</div>`
        }
        return `<div style="font-size:12px;color:#e2e8f0;">${label}</div><div style="margin-top:4px;color:#f8fafc;font-weight:700;">${value}</div>${detail}`
      },
    },
    xAxis: {
      type: 'category',
      data: x,
      boundaryGap: false,
      axisTick: { show: false },
      axisLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.32)' } },
      axisLabel: {
        color: 'rgba(148, 163, 184, 0.88)',
        fontSize: chartGrid.value.axisFont,
        interval,
        hideOverlap: true,
        formatter: (value: string) => formatAxisDate(value),
      },
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: 'rgba(148, 163, 184, 0.84)',
        fontSize: chartGrid.value.axisFont,
        formatter: (value: number) => compactCurrency(value),
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(148, 163, 184, 0.12)',
          type: 'dashed',
        },
      },
    },
    series: [
      {
        type: 'line',
        data: y,
        smooth: props.smoothLine !== false && x.length > 2 ? 0.22 : false,
        showSymbol: false,
        connectNulls: true,
        sampling: 'lttb',
        lineStyle: {
          width: 2.6,
          color: palette.value.line,
        },
        itemStyle: {
          color: palette.value.line,
        },
        emphasis: {
          symbol: 'circle',
          symbolSize: symbolSize + 2,
          itemStyle: {
            color: '#f8fafc',
            borderColor: palette.value.lineSoft,
            borderWidth: 2,
          },
          lineStyle: {
            width: 3,
          },
        },
        areaStyle:
          props.showArea !== false
            ? {
                color: {
                  type: 'linear',
                  x: 0,
                  y: 0,
                  x2: 0,
                  y2: 1,
                  colorStops: [
                    { offset: 0, color: palette.value.areaTop },
                    { offset: 1, color: palette.value.areaBottom },
                  ],
                },
              }
            : undefined,
      },
      {
        type: 'scatter',
        data: focus ? [[focus.date, focus.value]] : [],
        symbol: 'circle',
        symbolSize: props.layout.tiny ? 7 : 8,
        silent: true,
        z: 8,
        itemStyle: {
          color: '#f8fafc',
          borderColor: palette.value.line,
          borderWidth: 2,
          shadowBlur: 8,
          shadowColor: 'rgba(15, 23, 42, 0.5)',
        },
        emphasis: {
          disabled: true,
        },
        tooltip: {
          show: false,
        },
      },
    ],
  }
})

const layoutClass = computed(() => ({
  'is-compact': props.layout.mode === 'compact',
  'is-tiny': props.layout.tiny,
}))

const layoutVars = computed(() => {
  const valueSize = clamp(Math.round(Math.min(props.layout.width * 0.06, props.layout.height * 0.18)), 23, 44)
  const len = valueText.value.replace(/\s+/g, '').length
  const lenScale = len >= 12 ? 0.8 : len >= 10 ? 0.9 : 1

  return {
    '--npt-gap': `${clamp(Math.round(Math.min(props.layout.width * 0.015, props.layout.height * 0.06)), 6, 12)}px`,
    '--npt-value-size': `${clamp(Math.round(valueSize * lenScale), 22, 44)}px`,
    '--npt-label-size': `${clamp(Math.round(Math.min(props.layout.width * 0.014, props.layout.height * 0.05)), 10, 12)}px`,
    '--npt-sub-size': `${clamp(Math.round(Math.min(props.layout.width * 0.017, props.layout.height * 0.058)), 11, 14)}px`,
    '--npt-card-size': `${clamp(Math.round(Math.min(props.layout.width * 0.022, props.layout.height * 0.07)), 13, 18)}px`,
  }
})
</script>

<style scoped>
.npt-root {
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: var(--npt-gap);
  overflow: hidden;
}

.npt-head {
  min-width: 0;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--npt-gap);
}

.npt-head__main {
  min-width: 0;
}

.npt-overline {
  margin: 0;
  font-size: var(--npt-label-size);
  text-transform: uppercase;
  letter-spacing: 0.1em;
  font-weight: 700;
  color: rgba(148, 163, 184, 0.9);
}

.npt-value {
  margin: 5px 0 0;
  font-size: var(--npt-value-size);
  line-height: 0.94;
  letter-spacing: -0.04em;
  font-weight: 760;
  color: rgba(248, 250, 252, 0.98);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.npt-head__side {
  min-width: 0;
  display: grid;
  justify-items: end;
  gap: 2px;
}

.npt-delta {
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
}

.npt-delta__value {
  font-size: var(--npt-sub-size);
  font-weight: 730;
  color: rgba(226, 232, 240, 0.95);
}

.npt-delta__pct {
  font-size: var(--npt-label-size);
  color: rgba(148, 163, 184, 0.9);
}

.npt-delta.is-positive .npt-delta__value,
.npt-delta.is-positive .npt-delta__pct {
  color: rgba(52, 255, 156, 0.98);
}

.npt-delta.is-negative .npt-delta__value,
.npt-delta.is-negative .npt-delta__pct {
  color: rgba(253, 164, 175, 0.97);
}

.npt-period {
  margin: 0;
  font-size: var(--npt-label-size);
  color: rgba(148, 163, 184, 0.84);
  max-width: min(52vw, 340px);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.npt-chart-shell {
  position: relative;
  min-height: 0;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.5), rgba(15, 23, 42, 0.34));
  overflow: hidden;
}

.npt-chart {
  width: 100%;
  height: 100%;
  min-height: 0;
}

.npt-chart-overlay {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  text-align: center;
  background: rgba(2, 6, 23, 0.3);
  padding: 10px;
}

.npt-overlay__title {
  margin: 0;
  font-size: 13px;
  font-weight: 650;
  color: rgba(241, 245, 249, 0.96);
}

.npt-overlay__copy {
  margin: 4px 0 0;
  font-size: 12px;
  color: rgba(148, 163, 184, 0.88);
}

.npt-footer {
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--npt-gap);
}

.npt-card {
  min-width: 0;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(15, 23, 42, 0.4);
  padding: 7px 8px;
  display: grid;
  gap: 2px;
}

.npt-card__label {
  margin: 0;
  font-size: var(--npt-label-size);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: rgba(148, 163, 184, 0.86);
}

.npt-card__value {
  margin: 0;
  font-size: var(--npt-card-size);
  line-height: 1;
  letter-spacing: -0.02em;
  font-weight: 700;
  color: rgba(241, 245, 249, 0.97);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.npt-card__value.is-positive {
  color: rgba(52, 255, 156, 0.97);
}

.npt-card__value.is-negative {
  color: rgba(253, 164, 175, 0.97);
}

.npt-card__meta {
  margin: 0;
  font-size: var(--npt-label-size);
  color: rgba(148, 163, 184, 0.82);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.npt-root.is-compact .npt-head {
  align-items: flex-start;
}

.npt-root.is-compact .npt-period {
  max-width: min(46vw, 240px);
}

.npt-root.is-tiny .npt-head {
  flex-direction: column;
  align-items: flex-start;
}

.npt-root.is-tiny .npt-head__side {
  justify-items: start;
}

.npt-root.is-tiny .npt-period {
  max-width: none;
}
</style>
