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
      :class="{ 'is-compact': compactMode, 'is-tiny': tinyMode }"
      :style="numberLayoutStyle"
    >
      <template v-if="selectedView === 'number'">
        <section class="gr-number-main">
          <div class="gr-top-cards">
            <article
              v-for="card in topCards"
              :key="card.key"
              class="gr-card gr-card--kpi"
              :class="{ 'gr-card--total': card.key === 'total' }"
            >
              <p class="gr-kpi__label">{{ card.label }}</p>
              <p class="gr-kpi__value" :class="card.toneClass">{{ card.value }}</p>
              <p v-if="card.meta" class="gr-kpi__meta">{{ card.meta }}</p>
            </article>
          </div>

          <div class="gr-mini-grid">
            <article v-for="card in miniCards" :key="card.key" class="gr-mini">
              <p class="gr-mini__label">{{ card.label }}</p>
              <p class="gr-mini__value">{{ card.value }}</p>
            </article>
          </div>
        </section>
      </template>

      <template v-else>
        <section class="gr-graph-main">
          <div class="gr-top-cards">
            <article
              v-for="card in topCards"
              :key="card.key"
              class="gr-card gr-card--kpi"
              :class="{ 'gr-card--total': card.key === 'total' }"
            >
              <p class="gr-kpi__label">{{ card.label }}</p>
              <p class="gr-kpi__value" :class="card.toneClass">{{ card.value }}</p>
              <p v-if="card.meta" class="gr-kpi__meta">{{ card.meta }}</p>
            </article>
          </div>

          <div class="gr-mini-strip">
            <article v-for="card in miniCards" :key="card.key" class="gr-mini-chip">
              <p class="gr-mini-chip__label">{{ card.label }}</p>
              <p class="gr-mini-chip__value">{{ card.value }}</p>
            </article>
          </div>

          <div class="gr-chart-wrap">
            <VChart class="gr-chart" :option="option" autoresize />
            <div v-if="pointsCount <= 1" class="gr-chart-empty">
              <div>
                <div class="gr-chart-empty__title">Pas assez de points</div>
                <div class="gr-chart-empty__desc">
                  Elargis la periode pour afficher une tendance.
                </div>
              </div>
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
import { formatEUR, signFmt } from '@/utils/formatters'
import WidgetCard from './_parts/WidgetCard.vue'

const props = defineProps({
  from: String,
  to: String,
  bucket: { type: String, default: 'day' },
  view: { type: String, default: 'number' },
  categories: { type: Array, default: () => [] },
  types: { type: Array, default: () => [] },
  widgetWidth: { type: Number, default: 820 },
  widgetHeight: { type: Number, default: 520 },
  widgetBaseWidth: { type: Number, default: 0 },
  widgetBaseHeight: { type: Number, default: 0 },
  widgetRenderWidth: { type: Number, default: 0 },
  widgetRenderHeight: { type: Number, default: 0 },
})

const accent = '#3B82F6'
const loading = ref(false)
const error = ref('')
const kpi = ref({ value: 0, deltaPct: null })
const series = ref([])
let req = 0

const clamp = (value, min, max) => Math.max(min, Math.min(max, value))

const selectedView = computed(() => (props.view === 'line' ? 'line' : 'number'))
const layoutWidth = computed(() => {
  const raw = Number(props.widgetWidth ?? 0)
  return Number.isFinite(raw) && raw > 0 ? raw : 820
})
const layoutHeight = computed(() => {
  const raw = Number(props.widgetHeight ?? 0)
  return Number.isFinite(raw) && raw > 0 ? raw : 520
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
watch(
  () => [props.from, props.to, props.bucket, effectiveBucket.value, props.categories, props.types],
  load,
)

const totalValue = computed(() => {
  const k = Number(kpi.value.value ?? 0)
  if (Number.isFinite(k) && k > 0) return k
  return series.value.reduce((acc, p) => acc + Number(p.value ?? 0), 0)
})

const pointsCount = computed(() => series.value.length || 0)
const avgValue = computed(() => (pointsCount.value > 0 ? totalValue.value / pointsCount.value : 0))
const lastValue = computed(() =>
  series.value.length ? Number(series.value[series.value.length - 1].value ?? 0) : 0,
)
const firstValue = computed(() => (series.value.length ? Number(series.value[0].value ?? 0) : 0))
const bestValue = computed(() =>
  series.value.length
    ? series.value.reduce((best, point) => Math.max(best, Number(point.value ?? 0)), 0)
    : 0,
)
const lowValue = computed(() =>
  series.value.length
    ? series.value.reduce(
        (low, point) => Math.min(low, Number(point.value ?? 0)),
        Number.POSITIVE_INFINITY,
      )
    : 0,
)

const changePct = computed(() => {
  const first = firstValue.value
  if (!Number.isFinite(first) || first === 0) return null
  return ((lastValue.value - first) / Math.abs(first)) * 100
})

const spreadValue = computed(() => Math.max(0, bestValue.value - lowValue.value))

const totalText = computed(() => formatEUR(totalValue.value, { compact: true }))
const avgText = computed(() => formatEUR(avgValue.value, { compact: true }))
const lastText = computed(() => formatEUR(lastValue.value, { compact: true }))
const spreadText = computed(() => formatEUR(spreadValue.value, { compact: true }))
const changeText = computed(() => (changePct.value == null ? '--' : signFmt(changePct.value)))

const changeToneClass = computed(() => {
  if (changePct.value == null) return 'is-neutral'
  return changePct.value >= 0 ? 'is-positive' : 'is-negative'
})

const topCards = computed(() => [
  {
    key: 'total',
    label: 'Total periode',
    value: totalText.value,
    meta: '',
    toneClass: 'is-neutral',
  },
  {
    key: 'last',
    label: 'Dernier CA',
    value: lastText.value,
    meta: pointsCount.value > 0 ? `Point #${pointsCount.value}` : '',
    toneClass: 'is-neutral',
  },
  {
    key: 'variation',
    label: 'Variation',
    value: changeText.value,
    meta: '',
    toneClass: changeToneClass.value,
  },
])

const miniCards = computed(() => [
  {
    key: 'average',
    label: 'Moyenne',
    value: avgText.value,
  },
  {
    key: 'spread',
    label: 'Amplitude',
    value: spreadText.value,
  },
  {
    key: 'points',
    label: 'Points',
    value: String(pointsCount.value),
  },
])

const numberLayoutStyle = computed(() => {
  const maxTopLen = topCards.value.reduce(
    (max, card) => Math.max(max, String(card.value ?? '').replace(/\s+/g, '').length),
    0,
  )
  const topScale = maxTopLen >= 12 ? 0.72 : maxTopLen >= 10 ? 0.82 : maxTopLen >= 8 ? 0.92 : 1
  const kpiValueSize = clamp(
    Math.round(Math.min(layoutWidth.value * 0.05, layoutHeight.value * 0.1) * topScale),
    20,
    34,
  )
  const miniValueSize = clamp(Math.round(Math.min(layoutWidth.value * 0.032, layoutHeight.value * 0.058)), 14, 22)

  return {
    '--gr-kpi-value-size': `${kpiValueSize}px`,
    '--gr-mini-value-size': `${miniValueSize}px`,
  }
})

const chartGrid = computed(() => ({
  left: clamp(Math.round(layoutWidth.value * 0.048), 34, 68),
  right: clamp(Math.round(layoutWidth.value * 0.02), 10, 26),
  top: clamp(Math.round(layoutHeight.value * 0.026), 8, 24),
  bottom: clamp(Math.round(layoutHeight.value * 0.12), 44, 84),
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
    backgroundColor: 'transparent',
    animationDuration: 260,
    animationDurationUpdate: 200,
    grid: chartGrid.value,
    tooltip: {
      trigger: 'axis',
      triggerOn: 'mousemove|click',
      axisPointer: {
        type: 'line',
        snap: true,
        lineStyle: { color: 'rgba(148,163,184,0.85)', type: 'dashed' },
      },
      formatter: (params) => {
        const p = Array.isArray(params) ? params[0] : params
        const label = formatDateAxis(p?.axisValue, effectiveBucket.value)
        const val = formatEUR(Number(p?.data ?? 0))
        return (
          `<div style="font-size:12px;color:#E2E8F0;">${label}</div>` +
          `<div style="margin-top:4px;color:#93C5FD;font-weight:700;">${val}</div>`
        )
      },
    },
    xAxis: {
      type: 'category',
      data: x,
      boundaryGap: false,
      axisTick: { show: false },
      axisLine: { lineStyle: { color: 'rgba(100,116,139,0.45)' } },
      axisPointer: {
        show: true,
        snap: true,
        label: { show: false },
      },
      axisLabel: {
        color: 'rgba(203,213,225,0.82)',
        fontSize: axisFont,
        margin: 7,
        interval: labelInterval,
        formatter: (value) => formatDateAxis(value, effectiveBucket.value),
      },
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisLabel: {
        color: 'rgba(148,163,184,0.88)',
        fontSize: axisFont,
        formatter: (value) => formatCompactNumber(value),
      },
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.14)', type: 'dashed' } },
    },
    series: [
      {
        type: 'line',
        data: y,
        smooth: x.length > 2 ? 0.25 : false,
        showSymbol: true,
        showAllSymbol: false,
        connectNulls: true,
        symbol: 'circle',
        symbolSize: baseSymbol,
        hoverAnimation: true,
        sampling: 'lttb',
        z: 3,
        lineStyle: { width: 2.4, color: accent },
        itemStyle: {
          color: accent,
          opacity: x.length <= 2 ? 1 : 0,
        },
        emphasis: {
          focus: 'self',
          scale: true,
          symbolSize: baseSymbol + 4,
          lineStyle: { width: 3 },
          itemStyle: {
            color: '#dbeafe',
            borderColor: accent,
            borderWidth: 2,
            opacity: 1,
            shadowBlur: 12,
            shadowColor: 'rgba(59,130,246,0.48)',
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
              { offset: 0, color: `${accent}55` },
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
    const months = [
      'jan',
      'fev',
      'mar',
      'avr',
      'mai',
      'jun',
      'jul',
      'aou',
      'sep',
      'oct',
      'nov',
      'dec',
    ]
    const mm = months[m - 1] || ''
    return bucket === 'month'
      ? `${mm} ${String(y).slice(-2)}`
      : `${String(d).padStart(2, '0')} ${mm}`
  }
  if (/^\d{4}-\d{2}$/.test(v)) {
    const parts = v.split('-').map(Number)
    const y = parts[0] || 0
    const m = parts[1] || 1
    const months = [
      'jan',
      'fev',
      'mar',
      'avr',
      'mai',
      'jun',
      'jul',
      'aou',
      'sep',
      'oct',
      'nov',
      'dec',
    ]
    const mm = months[m - 1] || ''
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
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.gr-number-main,
.gr-graph-main {
  height: 100%;
  min-height: 0;
  display: grid;
  gap: 8px;
}

.gr-number-main {
  grid-template-rows: auto minmax(0, 1fr);
}

.gr-graph-main {
  grid-template-rows: auto auto minmax(0, 1fr);
}

.gr-top-cards {
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  align-items: start;
}

.gr-card {
  min-width: 0;
  min-height: 0;
  display: grid;
  align-content: start;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(15, 23, 42, 0.5);
  padding: 8px 10px;
  overflow: hidden;
}

.gr-card--total {
  background: linear-gradient(135deg, rgba(14, 116, 144, 0.2), rgba(15, 23, 42, 0.52));
}

.gr-kpi__label {
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.13em;
  font-size: 10px;
  color: rgba(148, 163, 184, 0.86);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.gr-kpi__value {
  margin: 5px 0 0;
  font-size: var(--gr-kpi-value-size);
  line-height: 0.96;
  letter-spacing: -0.03em;
  font-weight: 800;
  color: rgba(241, 245, 249, 0.98);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.gr-kpi__meta {
  margin: 4px 0 0;
  font-size: 12px;
  color: rgba(203, 213, 225, 0.72);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.gr-mini-grid {
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.gr-mini {
  border-radius: 11px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(15, 23, 42, 0.44);
  padding: 8px 10px;
  min-width: 0;
  min-height: 0;
}

.gr-mini__label {
  margin: 0;
  font-size: 10px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: rgba(148, 163, 184, 0.82);
}

.gr-mini__value {
  margin: 4px 0 0;
  font-size: var(--gr-mini-value-size);
  line-height: 1;
  letter-spacing: -0.02em;
  font-weight: 800;
  color: rgba(241, 245, 249, 0.96);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.gr-chart-wrap {
  min-height: 0;
  height: 100%;
  position: relative;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.5), rgba(2, 6, 23, 0.36));
  overflow: hidden;
}

.gr-mini-strip {
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.gr-mini-chip {
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: rgba(15, 23, 42, 0.42);
  padding: 6px 9px;
  min-width: 0;
}

.gr-mini-chip__label {
  margin: 0;
  font-size: 9px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: rgba(148, 163, 184, 0.84);
}

.gr-mini-chip__value {
  margin: 3px 0 0;
  font-size: 14px;
  line-height: 1;
  letter-spacing: -0.02em;
  font-weight: 740;
  color: rgba(226, 232, 240, 0.96);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.gr-chart {
  width: 100%;
  height: 100%;
  min-height: 0;
}

.gr-chart-empty {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  text-align: center;
  background: rgba(2, 6, 23, 0.3);
}

.gr-chart-empty__title {
  font-size: 14px;
  font-weight: 700;
  color: rgba(241, 245, 249, 0.98);
}

.gr-chart-empty__desc {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(203, 213, 225, 0.84);
}

.is-positive {
  color: #86efac;
}

.is-negative {
  color: #fda4af;
}

.is-neutral {
  color: rgba(226, 232, 240, 0.96);
}

.is-compact .gr-top-cards,
.is-compact .gr-mini-grid,
.is-compact .gr-mini-strip {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.is-compact .gr-card {
  padding: 7px 8px;
}

.is-compact .gr-top-cards > :last-child,
.is-compact .gr-mini-grid > :last-child,
.is-compact .gr-mini-strip > :last-child {
  grid-column: 1 / -1;
}

.is-tiny .gr-top-cards,
.is-tiny .gr-mini-grid,
.is-tiny .gr-mini-strip {
  grid-template-columns: 1fr;
  gap: 6px;
}

.is-tiny .gr-kpi__label,
.is-tiny .gr-mini__label {
  font-size: 9px;
}

.is-tiny .gr-kpi__meta {
  font-size: 11px;
}

.is-tiny .gr-mini-chip__value {
  font-size: 13px;
}
</style>
