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
          <VChart class="gr-chart" :option="option" autoresize />
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
import { computed, onMounted, ref, watch } from 'vue'
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
let req = 0

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
  const f = parseYmdLocal(props.from)
  const t = parseYmdLocal(props.to)
  if (!Number.isFinite(f?.getTime?.()) || !Number.isFinite(t?.getTime?.())) return props.bucket
  const months = diffMonths(f, t)
  return months < 6 ? 'day' : 'month'
})

async function load() {
  const id = ++req
  loading.value = true
  error.value = ''
  try {
    const [k, s] = await Promise.all([
      StatsServices.kpi('grossRevenue', props.from, props.to, props.categories, props.types),
      StatsServices.series(
        'grossRevenue',
        props.from,
        props.to,
        effectiveBucket.value,
        props.categories,
        props.types,
      ),
    ])
    if (id !== req) return
    kpi.value = normalizeKpi(k.data)
    series.value = normalizeSeries(s.data)
  } catch (e) {
    if (id !== req) return
    error.value = e?.response?.data?.message ?? e?.message ?? 'Impossible de charger'
  } finally {
    if (id === req) loading.value = false
  }
}

onMounted(load)
watch(() => [props.from, props.to, props.bucket, effectiveBucket.value, props.categories, props.types], load)

const totalValue = computed(() => {
  const k = Number(kpi.value.value ?? 0)
  if (Number.isFinite(k) && k > 0) return k
  return series.value.reduce((acc, p) => acc + Number(p.value ?? 0), 0)
})

const pointsCount = computed(() => series.value.length || 0)
const totalText = computed(() => formatEUR(totalValue.value, { compact: true }))

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
  bottom: clamp(Math.round(layoutHeight.value * 0.09), 30, 54),
  containLabel: true,
}))

const option = computed(() => {
  const x = series.value.map((p) => p.date)
  const y = series.value.map((p) => p.value)
  const step = effectiveBucket.value === 'month' ? 1 : Math.max(1, Math.ceil(x.length / 6))
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
        type: 'line',
        snap: true,
        lineStyle: { color: 'rgba(91,92,226,0.28)', width: 1, type: 'solid' },
      },
      formatter: (params) => {
        const p = Array.isArray(params) ? params[0] : params
        const label = formatDateAxis(p?.axisValue, effectiveBucket.value)
        const val = formatEUR(Number(p?.data ?? 0))
        return (
          `<div style="font-size:11px;color:#64748b;font-weight:700;text-transform:uppercase;letter-spacing:.06em;">${label}</div>` +
          `<div style="margin-top:4px;color:#4338ca;font-size:14px;font-weight:800;">${val}</div>`
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
      },
    ],
  }
})

function formatDateAxis(value, bucket) {
  const v = String(value ?? '')
  if (/^\d{4}-\d{2}-\d{2}$/.test(v)) {
    const parts = v.split('-').map(Number)
    const d = parts[2] || 1
    const m = parts[1] || 1
    const y = parts[0] || 0
    const mm = MONTHS[m - 1] || ''
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


