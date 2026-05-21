<template>
  <WidgetCard
    title="Chiffre d'affaires"
    subtitle="Synthese et evolution sur la periode"
    :accent="accent"
    surface="trend"
    :loading="loading"
    :error="error"
    :widget-width="props.widgetWidth"
    :widget-height="props.widgetHeight"
    :widget-base-width="props.widgetBaseWidth"
    :widget-base-height="props.widgetBaseHeight"
  >
    <div
      class="gr-root"
      :class="{
        'is-compact': compactMode,
        'is-tiny': tinyMode,
        'is-number-view': selectedView === 'number',
        'is-line-view': selectedView === 'line',
      }"
      :style="layoutVars"
    >
      <template v-if="selectedView === 'number'">
        <section class="gr-value-only">
          <span class="gr-value-only__accent" aria-hidden="true"></span>
          <p class="gr-value-only__number">{{ totalText }}</p>
        </section>
      </template>

      <template v-else>
        <section class="gr-chart-only">
          <VChart ref="chartRef" class="gr-chart" :option="option" autoresize />
          <div v-if="pointsCount <= 1" class="gr-chart-empty">
            <div>
              <div class="gr-chart-empty__title">Pas assez de points</div>
              <div class="gr-chart-empty__desc">Elargis la periode pour afficher une tendance.</div>
            </div>
          </div>
        </section>
      </template>
    </div>
  </WidgetCard>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeKpi, normalizeSeries, parseYmdLocal } from '@/services/statsAdapters'
import { formatEUR } from '@/utils/formatters'
import WidgetCard from './_parts/WidgetCard.vue'
import { fitKpiValueSize } from './_parts/kpiTextFit'

const props = defineProps({
  from: String,
  to: String,
  bucket: { type: String, default: 'day' },
  view: { type: String, default: 'number' },
  categories: { type: Array, default: () => [] },
  types: { type: Array, default: () => [] },
  widgetWidth: { type: Number, default: 760 },
  widgetHeight: { type: Number, default: 470 },
  widgetBaseWidth: { type: Number, default: 0 },
  widgetBaseHeight: { type: Number, default: 0 },
  widgetRenderWidth: { type: Number, default: 0 },
  widgetRenderHeight: { type: Number, default: 0 },
})

const accent = '#5B5CE2'
const MONTHS = ['jan', 'fev', 'mar', 'avr', 'mai', 'jun', 'jul', 'aou', 'sep', 'oct', 'nov', 'dec']

const loading = ref(false)
const error = ref('')
const kpi = ref({ value: 0, deltaPct: null })
const series = ref([])
const chartRef = ref(null)
const hoveredIndex = ref(null)
let req = 0
let axisPointerHandler = null
let globalOutHandler = null
let hoverFrame = 0
let pendingHoverIndex = null

const clamp = (value, min, max) => Math.max(min, Math.min(max, value))

const selectedView = computed(() => (props.view === 'line' ? 'line' : 'number'))
const layoutWidth = computed(() => {
  const raw = Number(props.widgetWidth ?? 0)
  return Number.isFinite(raw) && raw > 0 ? raw : 760
})
const layoutHeight = computed(() => {
  const raw = Number(props.widgetHeight ?? 0)
  return Number.isFinite(raw) && raw > 0 ? raw : 470
})
const compactMode = computed(() => layoutWidth.value < 760 || layoutHeight.value < 420)
const tinyMode = computed(() => layoutWidth.value < 660 || layoutHeight.value < 360)

const effectiveBucket = computed(() => {
  const requested = sanitizeBucket(props.bucket)
  const f = parseYmdLocal(props.from)
  const t = parseYmdLocal(props.to)
  if (requested !== 'auto') return requested
  if (!Number.isFinite(f?.getTime?.()) || !Number.isFinite(t?.getTime?.())) return 'month'
  const days = diffDays(f, t)
  const months = diffMonths(f, t)
  if (months >= 48) return 'year'
  if (months >= 7) return 'month'
  if (days >= 45) return 'week'
  return 'day'
})

async function load() {
  const id = ++req
  loading.value = true
  error.value = ''
  try {
    const requestBucket = effectiveBucket.value === 'year' ? 'month' : effectiveBucket.value
    const [k, s] = await Promise.all([
      StatsServices.kpi('grossRevenue', props.from, props.to, props.categories, props.types),
      StatsServices.series(
        'grossRevenue',
        props.from,
        props.to,
        requestBucket,
        props.categories,
        props.types,
      ),
    ])
    if (id !== req) return
    kpi.value = normalizeKpi(k.data)
    const normalized = normalizeSeries(s.data)
    series.value = effectiveBucket.value === 'year' ? aggregateByYear(normalized) : normalized
  } catch (e) {
    if (id !== req) return
    error.value = e?.response?.data?.message ?? e?.message ?? 'Impossible de charger'
  } finally {
    if (id === req) loading.value = false
  }
}

onMounted(() => {
  load()
  nextTick(connectChartEvents)
})
onBeforeUnmount(() => {
  disconnectChartEvents()
  if (hoverFrame) cancelAnimationFrame(hoverFrame)
})
watch(() => [props.from, props.to, props.bucket, effectiveBucket.value, props.categories, props.types], load)
watch(chartRef, () => nextTick(connectChartEvents))
watch(
  () => series.value,
  () => {
    clearChartFocus()
    nextTick(connectChartEvents)
  },
)

const totalValue = computed(() => {
  const k = Number(kpi.value.value ?? 0)
  if (Number.isFinite(k) && k > 0) return k
  return series.value.reduce((acc, p) => acc + Number(p.value ?? 0), 0)
})

const pointsCount = computed(() => series.value.length || 0)
const totalText = computed(() => formatEUR(totalValue.value, { compact: true }))
const axisLabelRotation = computed(() => {
  if (pointsCount.value <= 6) return 0
  if (effectiveBucket.value === 'year') return 0
  if (effectiveBucket.value === 'month') return pointsCount.value > 14 ? 28 : 0
  if (effectiveBucket.value === 'week') return pointsCount.value > 10 ? 28 : 0
  return pointsCount.value > 8 ? 34 : 0
})

const layoutVars = computed(() => {
  const heroSize = fitKpiValueSize(totalText.value, layoutWidth.value, layoutHeight.value, {
    min: 22,
    max: 76,
    paddingX: Math.max(54, layoutWidth.value * 0.3),
    paddingY: Math.max(18, layoutHeight.value * 0.2),
    heightRatio: 0.48,
  })

  return {
    '--gr-hero-size': `${heroSize}px`,
  }
})

const chartGrid = computed(() => ({
  left: clamp(Math.round(layoutWidth.value * 0.04), 30, 56),
  right: clamp(Math.round(layoutWidth.value * 0.022), 14, 26),
  top: clamp(Math.round(layoutHeight.value * 0.05), 14, 24),
  bottom: clamp(Math.round(layoutHeight.value * 0.09) + (axisLabelRotation.value ? 16 : 0), 30, 70),
  containLabel: true,
}))

const hoverPoint = computed(() => {
  if (hoveredIndex.value == null) return null
  const index = clamp(Math.round(Number(hoveredIndex.value)), 0, Math.max(series.value.length - 1, 0))
  const point = series.value[index]
  if (!point) return null
  return {
    index,
    date: point.date,
    value: Number(point.value ?? 0),
  }
})

const importantPointMap = computed(() => {
  const points = series.value
    .map((point, index) => ({
      index,
      date: point?.date,
      value: Number(point?.value ?? 0),
    }))
    .filter((point) => Number.isFinite(point.value))
  const map = new Map()
  if (!points.length) return map

  const peak = points.reduce((best, point) => (point.value > best.value ? point : best), points[0])
  addImportantPoint(map, peak, 'Pic', 'Meilleur niveau de la periode', accent)

  let strongestMove = null
  for (let index = 1; index < points.length; index += 1) {
    const previous = points[index - 1]
    const current = points[index]
    if (!previous || !current) continue
    const delta = current.value - previous.value
    if (!strongestMove || Math.abs(delta) > Math.abs(strongestMove.delta)) {
      strongestMove = { ...current, delta }
    }
  }
  if (strongestMove && Math.abs(strongestMove.delta) > 0) {
    addImportantPoint(
      map,
      strongestMove,
      strongestMove.delta >= 0 ? 'Forte hausse' : 'Forte baisse',
      `Variation ${formatSignedCurrency(strongestMove.delta)}`,
      strongestMove.delta >= 0 ? '#059669' : '#dc2626',
    )
  }

  addImportantPoint(map, points[points.length - 1], 'Dernier', 'Dernier point affiche', '#2563eb')
  return map
})

const importantMarks = computed(() => {
  if (selectedView.value !== 'line' || layoutWidth.value < 560 || layoutHeight.value < 290) return []
  return Array.from(importantPointMap.value.entries())
    .slice(0, 3)
    .map(([index, meta]) => {
      const point = series.value[index]
      return {
        name: meta.label,
        value: meta.label,
        xAxis: point?.date,
        yAxis: Number(point?.value ?? 0),
        symbol: 'circle',
        symbolSize: meta.label === 'Dernier' ? 8 : 10,
        itemStyle: {
          color: '#ffffff',
          borderColor: meta.color,
          borderWidth: 2,
          shadowBlur: 10,
          shadowColor: `${meta.color}38`,
        },
        label: {
          show: true,
          formatter: meta.label,
          position: 'top',
          distance: 8,
          color: meta.color,
          fontSize: 10,
          fontWeight: 750,
        },
      }
    })
})

const option = computed(() => {
  const x = series.value.map((p) => p.date)
  const y = series.value.map((p) => p.value)
  const focus = hoverPoint.value
  const marks = importantMarks.value
  const maxLabels =
    effectiveBucket.value === 'year'
      ? 8
      : effectiveBucket.value === 'month'
        ? 7
        : tinyMode.value
          ? 4
          : 6
  const step = Math.max(1, Math.ceil(x.length / maxLabels))
  const labelInterval = Math.max(0, step - 1)
  const axisFont = clamp(
    Math.round(Math.min(layoutWidth.value * 0.012, layoutHeight.value * 0.028)),
    10,
    12,
  )
  const baseSymbol = clamp(Math.round(Math.min(layoutWidth.value * 0.009, layoutHeight.value * 0.02)), 5, 8)

  return {
    color: [accent],
    backgroundColor: 'transparent',
    animationDuration: 280,
    animationDurationUpdate: 220,
    animationEasing: 'cubicOut',
    grid: chartGrid.value,
    tooltip: {
      trigger: 'axis',
      triggerOn: 'mousemove|click',
      confine: true,
      backgroundColor: 'rgba(255,255,255,0.98)',
      borderColor: 'rgba(148,163,184,0.32)',
      borderWidth: 1,
      padding: [8, 10],
      textStyle: {
        color: '#0f172a',
        fontSize: 12,
        fontWeight: 600,
      },
      extraCssText: 'border-radius:10px;box-shadow:0 12px 28px rgba(15,23,42,0.14);',
      axisPointer: {
        type: 'cross',
        snap: true,
        lineStyle: { color: 'rgba(91,92,226,0.28)', width: 1, type: 'solid' },
        crossStyle: { color: 'rgba(91,92,226,0.22)', width: 1 },
        label: {
          show: !tinyMode.value,
          backgroundColor: 'rgba(15,23,42,0.92)',
          color: '#ffffff',
          fontSize: 10,
          formatter: (params) =>
            params?.axisDimension === 'y'
              ? formatCompactNumber(params.value)
              : formatDateAxis(params?.value, effectiveBucket.value),
        },
      },
      formatter: (params) => {
        const p = Array.isArray(params) ? params[0] : params
        const index = Number(p?.dataIndex ?? -1)
        const label = escapeHtml(formatDateAxis(p?.axisValue, effectiveBucket.value))
        const val = escapeHtml(formatEUR(Number(p?.data ?? 0)))
        const meta = importantPointMap.value.get(index)
        const badge = meta
          ? `<div style="display:inline-flex;margin-top:7px;padding:3px 7px;border-radius:999px;background:${meta.color}14;color:${meta.color};font-size:11px;font-weight:800;">${escapeHtml(meta.label)}</div>`
          : ''
        return (
          `<div style="font-size:11px;color:#64748b;font-weight:700;text-transform:uppercase;letter-spacing:.06em;">${label}</div>` +
          `<div style="margin-top:4px;color:#4338ca;font-size:14px;font-weight:800;">${val}</div>` +
          formatDeltaDetail(index) +
          badge
        )
      },
    },
    xAxis: {
      type: 'category',
      data: x,
      boundaryGap: false,
      axisTick: { show: false },
      axisLine: { lineStyle: { color: 'rgba(148,163,184,0.32)' } },
      axisPointer: {
        show: true,
        snap: true,
        label: { show: false },
      },
      axisLabel: {
        color: '#64748b',
        fontSize: axisFont,
        fontWeight: 600,
        margin: 8,
        interval: labelInterval,
        rotate: axisLabelRotation.value,
        hideOverlap: true,
        formatter: (value) => formatDateAxis(value, effectiveBucket.value),
      },
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: '#64748b',
        fontSize: axisFont,
        fontWeight: 600,
        margin: 10,
        formatter: (value) => formatCompactNumber(value),
      },
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.18)', type: 'dashed' } },
    },
    series: [
      {
        type: 'line',
        data: y,
        smooth: x.length > 2 ? 0.32 : false,
        showSymbol: x.length <= 2,
        showAllSymbol: false,
        connectNulls: true,
        symbol: 'circle',
        symbolSize: baseSymbol,
        hoverAnimation: true,
        sampling: 'lttb',
        z: 3,
        lineStyle: { width: 2.7, color: accent },
        itemStyle: {
          color: accent,
        },
        emphasis: {
          focus: 'self',
          scale: true,
          symbolSize: baseSymbol + 4,
          lineStyle: { width: 3.2 },
          itemStyle: {
            color: '#ffffff',
            borderColor: accent,
            borderWidth: 2,
            opacity: 1,
            shadowBlur: 10,
            shadowColor: 'rgba(91,92,226,0.28)',
          },
        },
        blur: {
          itemStyle: {
            opacity: 0,
          },
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: `${accent}2e` },
              { offset: 1, color: `${accent}00` },
            ],
          },
        },
        markPoint: marks.length
          ? {
              symbolKeepAspect: true,
              data: marks,
              animation: false,
              silent: true,
            }
          : undefined,
      },
      {
        type: 'scatter',
        data: focus ? [[focus.date, focus.value]] : [],
        symbol: 'circle',
        symbolSize: baseSymbol + 4,
        silent: true,
        z: 9,
        itemStyle: {
          color: '#ffffff',
          borderColor: accent,
          borderWidth: 2,
          shadowBlur: 12,
          shadowColor: 'rgba(91,92,226,0.28)',
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

function addImportantPoint(map, point, label, detail, color) {
  if (!point || map.has(point.index)) return
  map.set(point.index, { label, detail, color })
}

function scheduleHoverIndex(index) {
  pendingHoverIndex = index
  if (hoverFrame) return
  hoverFrame = requestAnimationFrame(() => {
    hoverFrame = 0
    hoveredIndex.value = pendingHoverIndex
  })
}

function clearChartFocus() {
  pendingHoverIndex = null
  if (hoverFrame) {
    cancelAnimationFrame(hoverFrame)
    hoverFrame = 0
  }
  hoveredIndex.value = null
}

function resolveHoverIndex(payload) {
  const axis = Array.isArray(payload?.axesInfo) ? payload.axesInfo[0] : null
  if (!axis || !series.value.length) return null

  const rawDataIndex = Number(axis.dataIndex)
  if (Number.isFinite(rawDataIndex)) {
    const rounded = Math.round(rawDataIndex)
    if (rounded >= 0 && rounded < series.value.length) return rounded
  }

  const rawIndex = Number(axis.value)
  if (Number.isFinite(rawIndex)) {
    const rounded = Math.round(rawIndex)
    if (rounded >= 0 && rounded < series.value.length) return rounded
  }

  const rawValue = String(axis.value ?? '')
  if (!rawValue) return null
  const byDate = series.value.findIndex((point) => point.date === rawValue)
  return byDate >= 0 ? byDate : null
}

function connectChartEvents() {
  disconnectChartEvents()
  const instance = chartRef.value?.getEchartsInstance?.()
  if (!instance) return

  axisPointerHandler = (payload) => {
    scheduleHoverIndex(resolveHoverIndex(payload))
  }
  globalOutHandler = () => {
    scheduleHoverIndex(null)
  }

  instance.on('updateAxisPointer', axisPointerHandler)
  instance.on('globalout', globalOutHandler)
}

function disconnectChartEvents() {
  const instance = chartRef.value?.getEchartsInstance?.()
  if (instance && axisPointerHandler) instance.off('updateAxisPointer', axisPointerHandler)
  if (instance && globalOutHandler) instance.off('globalout', globalOutHandler)
  axisPointerHandler = null
  globalOutHandler = null
}

function formatDeltaDetail(index) {
  if (!Number.isFinite(index) || index <= 0 || index >= series.value.length) return ''
  const current = Number(series.value[index]?.value ?? 0)
  const previous = Number(series.value[index - 1]?.value ?? 0)
  const delta = current - previous
  const pct = previous ? (delta / Math.abs(previous)) * 100 : null
  const color = delta >= 0 ? '#047857' : '#be123c'
  const pctText = pct == null || !Number.isFinite(pct) ? '' : ` (${formatSignedPercent(pct)})`
  return `<div style="margin-top:4px;color:#64748b;font-size:11px;">Variation: <span style="color:${color};font-weight:800;">${escapeHtml(formatSignedCurrency(delta))}</span>${escapeHtml(pctText)}</div>`
}

function formatSignedCurrency(value) {
  const n = Number(value ?? 0)
  const abs = formatEUR(Math.abs(n), { compact: true, digits: 1 })
  if (n > 0) return `+${abs}`
  if (n < 0) return `-${abs}`
  return abs
}

function formatSignedPercent(value) {
  const n = Number(value ?? 0)
  const sign = n > 0 ? '+' : ''
  return `${sign}${n.toFixed(1)}%`
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (char) => {
    const entities = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;',
    }
    return entities[char] || char
  })
}

function formatDateAxis(value, bucket) {
  const v = String(value ?? '')
  if (/^\d{4}-\d{2}-\d{2}$/.test(v)) {
    const parts = v.split('-').map(Number)
    const d = parts[2] || 1
    const m = parts[1] || 1
    const y = parts[0] || 0
    const mm = MONTHS[m - 1] || ''
    if (bucket === 'year') return String(y)
    return bucket === 'month' ? `${mm} ${String(y).slice(-2)}` : `${String(d).padStart(2, '0')} ${mm}`
  }
  if (/^\d{4}-\d{2}$/.test(v)) {
    const parts = v.split('-').map(Number)
    const y = parts[0] || 0
    const m = parts[1] || 1
    const mm = MONTHS[m - 1] || ''
    return `${mm} ${String(y).slice(-2)}`
  }
  return v
}

function sanitizeBucket(value) {
  const raw = String(value || 'auto').trim().toLowerCase()
  if (['auto', 'day', 'week', 'month', 'year'].includes(raw)) return raw
  return 'auto'
}

function formatCompactNumber(value) {
  const n = Number(value ?? 0)
  if (!Number.isFinite(n)) return '--'
  if (Math.abs(n) < 1000) return `${Math.round(n)}`
  return new Intl.NumberFormat('fr-FR', {
    notation: 'compact',
    maximumFractionDigits: 1,
  }).format(n)
}

function diffMonths(a, b) {
  const start = a < b ? a : b
  const end = a < b ? b : a
  return (end.getFullYear() - start.getFullYear()) * 12 + (end.getMonth() - start.getMonth())
}

function diffDays(a, b) {
  const start = a < b ? a : b
  const end = a < b ? b : a
  return Math.round((end.getTime() - start.getTime()) / 86_400_000)
}

function aggregateByYear(points) {
  const byYear = new Map()
  for (const point of points) {
    const year = String(point?.date ?? '').slice(0, 4)
    if (!/^\d{4}$/.test(year)) continue
    byYear.set(year, (byYear.get(year) || 0) + Number(point?.value ?? 0))
  }
  return Array.from(byYear.entries())
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([year, value]) => ({ date: `${year}-01-01`, value }))
}
</script>

<style scoped>
.gr-root {
  --gr-accent: #5b5ce2;
  --gr-accent-strong: #4338ca;
  --gr-text: #111827;
  --gr-muted: #64748b;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  color: var(--gr-text);
}

.gr-value-only,
.gr-chart-only {
  position: relative;
  height: 100%;
  min-height: 0;
  isolation: isolate;
  overflow: hidden;
}

.gr-value-only {
  display: grid;
  grid-template-rows: auto auto;
  align-content: center;
  justify-items: center;
  gap: clamp(7px, 4%, 12px);
  padding: clamp(6px, 4%, 14px) clamp(12px, 5%, 22px) clamp(10px, 5%, 20px);
}

.gr-value-only__accent {
  display: block;
  width: clamp(46px, 18%, 76px);
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--gr-accent), #2563eb);
  box-shadow: 0 8px 18px rgba(91, 92, 226, 0.18);
  pointer-events: none;
}

.gr-value-only__number {
  position: relative;
  z-index: 1;
  margin: 0;
  max-width: 100%;
  color: var(--gr-text);
  font-family: var(--template-title-font, var(--font-display, "Poppins", sans-serif));
  font-size: var(--gr-hero-size);
  line-height: 1;
  font-weight: 820;
  letter-spacing: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: clip;
}

.gr-chart-only {
  padding: 2px 0 0;
}

.gr-chart-only::before {
  content: '';
  position: absolute;
  top: 0;
  left: clamp(14px, 3%, 24px);
  width: clamp(42px, 10%, 72px);
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--gr-accent), rgba(37, 99, 235, 0.72));
  box-shadow: 0 8px 18px rgba(91, 92, 226, 0.16);
  pointer-events: none;
  z-index: 2;
}

.gr-chart {
  width: 100%;
  height: 100%;
  min-height: 0;
  position: relative;
  z-index: 1;
}

.gr-chart-empty {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  text-align: center;
  background: rgba(248, 250, 252, 0.82);
  backdrop-filter: blur(2px);
  z-index: 3;
}

.gr-chart-empty__title {
  color: var(--gr-text);
  font-size: 14px;
  font-weight: 760;
}

.gr-chart-empty__desc {
  margin-top: 4px;
  font-size: 12px;
  color: var(--gr-muted);
}
</style>


