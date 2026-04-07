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
    <div class="gr-root" :class="{ 'is-compact': compactMode, 'is-tiny': tinyMode }" :style="layoutVars">
      <template v-if="selectedView === 'number'">
        <section class="gr-number-main">
          <article class="gr-hero">
            <p class="gr-hero__kicker">CA cumule</p>
            <p class="gr-hero__value">{{ totalText }}</p>
            <div class="gr-hero__meta">
              <span class="gr-pill" :class="changeToneClass">{{ changeText }}</span>
              <span class="gr-hero__range">{{ rangeText }}</span>
            </div>
          </article>

          <div class="gr-number-grid">
            <article v-for="card in numberCards" :key="card.key" class="gr-stat">
              <p class="gr-stat__label">{{ card.label }}</p>
              <p class="gr-stat__value" :class="card.toneClass">{{ card.value }}</p>
              <p v-if="card.meta" class="gr-stat__meta">{{ card.meta }}</p>
            </article>
          </div>

          <section class="gr-insights">
            <header class="gr-insights__head">
              <p class="gr-insights__title">Radar rapide</p>
              <p class="gr-insights__subtitle">{{ insightSummary }}</p>
            </header>

            <div class="gr-insights__grid">
              <article v-for="item in insightCards" :key="item.key" class="gr-insight-chip">
                <p class="gr-insight-chip__label">{{ item.label }}</p>
                <p class="gr-insight-chip__value" :class="item.toneClass">{{ item.value }}</p>
                <p class="gr-insight-chip__meta">{{ item.meta }}</p>
              </article>
            </div>

            <div class="gr-insights__meter">
              <p class="gr-insights__meter-label">Regularite des releves</p>
              <div class="gr-insights__meter-track">
                <span class="gr-insights__meter-fill" :style="{ width: `${stabilityPct}%` }" />
              </div>
              <p class="gr-insights__meter-value">{{ stabilityText }}</p>
            </div>
          </section>
        </section>
      </template>

      <template v-else>
        <section class="gr-line-main">
          <div class="gr-line-strip">
            <article v-for="chip in lineChips" :key="chip.key" class="gr-chip">
              <p class="gr-chip__label">{{ chip.label }}</p>
              <p class="gr-chip__value" :class="chip.toneClass">{{ chip.value }}</p>
            </article>
          </div>

          <div class="gr-chart-wrap">
            <VChart class="gr-chart" :option="option" autoresize />
            <div v-if="pointsCount <= 1" class="gr-chart-empty">
              <div>
                <div class="gr-chart-empty__title">Pas assez de points</div>
                <div class="gr-chart-empty__desc">Elargis la periode pour afficher une tendance.</div>
              </div>
            </div>
          </div>

          <div class="gr-line-foot">
            <article v-for="card in lineFootCards" :key="card.key" class="gr-foot-card">
              <p class="gr-foot-card__label">{{ card.label }}</p>
              <p class="gr-foot-card__value" :class="card.toneClass">{{ card.value }}</p>
              <p v-if="card.meta" class="gr-foot-card__meta">{{ card.meta }}</p>
            </article>
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
  widgetWidth: { type: Number, default: 760 },
  widgetHeight: { type: Number, default: 470 },
  widgetBaseWidth: { type: Number, default: 0 },
  widgetBaseHeight: { type: Number, default: 0 },
  widgetRenderWidth: { type: Number, default: 0 },
  widgetRenderHeight: { type: Number, default: 0 },
})

const accent = '#3B82F6'
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

const cadenceLabel = computed(() => (effectiveBucket.value === 'month' ? 'mensuelle' : 'journaliere'))

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

const bestPoint = computed(() => {
  if (!series.value.length) return null
  return series.value.reduce((best, point) => (Number(point.value ?? 0) > Number(best.value ?? 0) ? point : best))
})

const lowPoint = computed(() => {
  if (!series.value.length) return null
  return series.value.reduce((low, point) => (Number(point.value ?? 0) < Number(low.value ?? 0) ? point : low))
})

const changePct = computed(() => {
  const first = firstValue.value
  if (!Number.isFinite(first) || first === 0) return null
  return ((lastValue.value - first) / Math.abs(first)) * 100
})

const spreadValue = computed(() => Math.max(0, bestValue.value - lowValue.value))
const seriesValues = computed(() =>
  series.value
    .map((point) => Number(point.value ?? 0))
    .filter((value) => Number.isFinite(value)),
)

const momentumPct = computed(() => {
  const values = seriesValues.value
  if (values.length < 4) return null
  const windowSize = Math.max(2, Math.floor(values.length / 3))
  const recentSlice = values.slice(-windowSize)
  const previousSlice = values.slice(-(windowSize * 2), -windowSize)
  if (!recentSlice.length || !previousSlice.length) return null
  const recentAvg = recentSlice.reduce((sum, value) => sum + value, 0) / recentSlice.length
  const previousAvg = previousSlice.reduce((sum, value) => sum + value, 0) / previousSlice.length
  if (!Number.isFinite(previousAvg) || previousAvg === 0) return null
  return ((recentAvg - previousAvg) / Math.abs(previousAvg)) * 100
})

const peakSharePct = computed(() => {
  if (!Number.isFinite(totalValue.value) || totalValue.value <= 0) return null
  return (bestValue.value / totalValue.value) * 100
})

const stabilityScore = computed(() => {
  const values = seriesValues.value
  if (values.length < 2) return null
  const mean = avgValue.value
  if (!Number.isFinite(mean) || mean <= 0) return null
  const variance = values.reduce((sum, value) => sum + (value - mean) ** 2, 0) / values.length
  const stdDev = Math.sqrt(variance)
  const coeff = stdDev / Math.max(Math.abs(mean), 1)
  return clamp(Math.round(100 - coeff * 120), 0, 100)
})

const totalText = computed(() => formatEUR(totalValue.value, { compact: true }))
const avgText = computed(() => formatEUR(avgValue.value, { compact: true }))
const lastText = computed(() => formatEUR(lastValue.value, { compact: true }))
const firstText = computed(() => formatEUR(firstValue.value, { compact: true }))
const bestText = computed(() => formatEUR(bestValue.value, { compact: true }))
const lowText = computed(() => formatEUR(lowValue.value, { compact: true }))
const spreadText = computed(() => formatEUR(spreadValue.value, { compact: true }))
const changeText = computed(() => (changePct.value == null ? '--' : signFmt(changePct.value)))
const momentumText = computed(() => (momentumPct.value == null ? '--' : signFmt(momentumPct.value)))
const peakShareText = computed(() => (peakSharePct.value == null ? '--' : `${peakSharePct.value.toFixed(1)}%`))
const stabilityText = computed(() => (stabilityScore.value == null ? '--' : `${stabilityScore.value}/100`))
const stabilityPct = computed(() => (stabilityScore.value == null ? 0 : stabilityScore.value))

const changeToneClass = computed(() => {
  if (changePct.value == null) return 'is-neutral'
  return changePct.value >= 0 ? 'is-positive' : 'is-negative'
})
const momentumToneClass = computed(() => {
  if (momentumPct.value == null) return 'is-neutral'
  return momentumPct.value >= 0 ? 'is-positive' : 'is-negative'
})
const peakShareToneClass = computed(() => {
  if (peakSharePct.value == null) return 'is-neutral'
  if (peakSharePct.value <= 18) return 'is-positive'
  if (peakSharePct.value <= 32) return 'is-neutral'
  return 'is-negative'
})
const stabilityToneClass = computed(() => {
  if (stabilityScore.value == null) return 'is-neutral'
  if (stabilityScore.value >= 70) return 'is-positive'
  if (stabilityScore.value >= 45) return 'is-neutral'
  return 'is-negative'
})

const trendText = computed(() => {
  if (changePct.value == null) return 'Pas assez d historique pour comparer.'
  const abs = Math.abs(changePct.value)
  if (abs < 0.15) return 'Trajectoire stable sur la periode.'
  return changePct.value > 0 ? 'Trajectoire en acceleration.' : 'Trajectoire en ralentissement.'
})

const rangeText = computed(() => {
  const from = formatShortDate(props.from, effectiveBucket.value)
  const to = formatShortDate(props.to, effectiveBucket.value)
  if (from && to) return `${from} -> ${to}`
  if (from) return `Depuis ${from}`
  if (to) return `Jusqu a ${to}`
  return 'Periode active'
})

const numberCards = computed(() => [
  {
    key: 'last',
    label: 'Dernier point',
    value: lastText.value,
    meta: pointsCount.value > 0 ? formatDateAxis(series.value[series.value.length - 1].date, effectiveBucket.value) : 'Aucun point',
    toneClass: 'is-neutral',
  },
  {
    key: 'average',
    label: 'Moyenne',
    value: avgText.value,
    meta: `${pointsCount.value} point${pointsCount.value > 1 ? 's' : ''}`,
    toneClass: 'is-neutral',
  },
  {
    key: 'best',
    label: 'Pic',
    value: bestText.value,
    meta: bestPoint.value ? formatDateAxis(bestPoint.value.date, effectiveBucket.value) : 'Aucun pic',
    toneClass: 'is-positive',
  },
  {
    key: 'low',
    label: 'Point bas',
    value: lowText.value,
    meta: lowPoint.value ? formatDateAxis(lowPoint.value.date, effectiveBucket.value) : 'Aucun point',
    toneClass: 'is-negative',
  },
])

const insightSummary = computed(() => {
  const points = pointsCount.value
  if (points <= 1) return 'Donnees limitees sur la plage active.'
  return `${trendText.value} ${points} point${points > 1 ? 's' : ''} analyses.`
})

const insightCards = computed(() => [
  {
    key: 'momentum',
    label: 'Momentum',
    value: momentumText.value,
    meta: 'Moyenne recente vs precedente',
    toneClass: momentumToneClass.value,
  },
  {
    key: 'concentration',
    label: 'Poids du pic',
    value: peakShareText.value,
    meta: 'Part du meilleur point dans le total',
    toneClass: peakShareToneClass.value,
  },
  {
    key: 'spread',
    label: 'Amplitude',
    value: spreadText.value,
    meta: `Vue ${cadenceLabel.value} sur la plage`,
    toneClass: stabilityToneClass.value,
  },
])

const lineChips = computed(() => [
  {
    key: 'total',
    label: 'Total',
    value: totalText.value,
    toneClass: 'is-neutral',
  },
  {
    key: 'variation',
    label: 'Variation',
    value: changeText.value,
    toneClass: changeToneClass.value,
  },
  {
    key: 'points',
    label: 'Points',
    value: String(pointsCount.value),
    toneClass: 'is-neutral',
  },
])

const lineFootCards = computed(() => [
  {
    key: 'first',
    label: 'Depart',
    value: firstText.value,
    meta: series.value.length ? formatDateAxis(series.value[0].date, effectiveBucket.value) : '',
    toneClass: 'is-neutral',
  },
  {
    key: 'last',
    label: 'Arrivee',
    value: lastText.value,
    meta: series.value.length ? formatDateAxis(series.value[series.value.length - 1].date, effectiveBucket.value) : '',
    toneClass: 'is-neutral',
  },
  {
    key: 'average',
    label: 'Moyenne',
    value: avgText.value,
    meta: rangeText.value,
    toneClass: 'is-neutral',
  },
  {
    key: 'spread',
    label: 'Amplitude',
    value: spreadText.value,
    meta: 'Ecart pic / point bas',
    toneClass: 'is-neutral',
  },
])

const layoutVars = computed(() => {
  const heroLen = totalText.value.replace(/\s+/g, '').length
  const heroScale = heroLen >= 12 ? 0.72 : heroLen >= 10 ? 0.84 : 1
  const heroSize = clamp(
    Math.round(Math.min(layoutWidth.value * 0.062, layoutHeight.value * 0.165) * heroScale),
    28,
    56,
  )
  const statSize = clamp(Math.round(Math.min(layoutWidth.value * 0.028, layoutHeight.value * 0.08)), 16, 28)
  const chipSize = clamp(Math.round(Math.min(layoutWidth.value * 0.024, layoutHeight.value * 0.06)), 14, 20)

  return {
    '--gr-hero-size': `${heroSize}px`,
    '--gr-stat-size': `${statSize}px`,
    '--gr-chip-size': `${chipSize}px`,
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

function formatShortDate(value, bucket) {
  const v = String(value ?? '')
  if (!/^\d{4}-\d{2}-\d{2}$/.test(v)) return ''
  const [year, month, day] = v.split('-').map(Number)
  const mm = MONTHS[(month || 1) - 1] || ''
  if (bucket === 'month') return `${mm} ${String(year || '').slice(-2)}`
  return `${String(day || 1).padStart(2, '0')} ${mm}`
}

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
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.gr-number-main,
.gr-line-main {
  height: 100%;
  min-height: 0;
  display: grid;
  gap: 6px;
}

.gr-number-main {
  grid-template-rows: auto auto auto;
  align-content: start;
}

.gr-line-main {
  grid-template-rows: auto minmax(0, 1fr) auto;
}

.gr-hero {
  border-radius: 13px;
  border: 1px solid rgba(125, 211, 252, 0.22);
  background:
    radial-gradient(circle at 0 0, rgba(59, 130, 246, 0.22), transparent 52%),
    linear-gradient(150deg, rgba(15, 23, 42, 0.7), rgba(2, 6, 23, 0.6));
  padding: 10px 12px;
  min-width: 0;
}

.gr-hero__kicker {
  margin: 0;
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: rgba(191, 219, 254, 0.84);
}

.gr-hero__value {
  margin: 8px 0 0;
  font-size: var(--gr-hero-size);
  line-height: 0.94;
  font-weight: 820;
  letter-spacing: -0.04em;
  color: rgba(248, 250, 252, 0.98);
  text-shadow: 0 12px 20px rgba(15, 23, 42, 0.42);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.gr-hero__meta {
  margin-top: 7px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.gr-pill {
  display: inline-flex;
  align-items: center;
  height: 22px;
  border-radius: 999px;
  padding: 0 8px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  font-size: 11px;
  font-weight: 760;
  background: rgba(15, 23, 42, 0.52);
}

.gr-hero__range {
  font-size: 11px;
  color: rgba(191, 219, 254, 0.76);
}

.gr-number-grid {
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
}

.gr-stat,
.gr-chip,
.gr-foot-card,
.gr-insight-chip {
  min-width: 0;
  border-radius: 11px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(15, 23, 42, 0.48);
  padding: 7px 8px;
}

.gr-stat__label,
.gr-chip__label,
.gr-foot-card__label,
.gr-insight-chip__label {
  margin: 0;
  font-size: 10px;
  letter-spacing: 0.11em;
  text-transform: uppercase;
  color: rgba(148, 163, 184, 0.86);
}

.gr-stat__value,
.gr-foot-card__value,
.gr-insight-chip__value {
  margin: 5px 0 0;
  font-size: var(--gr-stat-size);
  line-height: 0.98;
  letter-spacing: -0.03em;
  font-weight: 760;
  color: rgba(241, 245, 249, 0.97);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.gr-stat__meta,
.gr-foot-card__meta {
  margin: 4px 0 0;
  font-size: 11px;
  color: rgba(203, 213, 225, 0.74);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.gr-insights {
  display: grid;
  gap: 6px;
  min-height: 0;
  border-radius: 13px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.42), rgba(2, 6, 23, 0.34));
  padding: 8px;
}

.gr-insights__head {
  display: grid;
  gap: 2px;
}

.gr-insights__title {
  margin: 0;
  font-size: 10px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: rgba(148, 163, 184, 0.9);
}

.gr-insights__subtitle {
  margin: 0;
  font-size: 12px;
  color: rgba(226, 232, 240, 0.92);
  line-height: 1.3;
}

.gr-insights__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}

.gr-insight-chip {
  display: grid;
  align-content: start;
  gap: 3px;
}

.gr-insight-chip__meta {
  margin: 0;
  font-size: 10px;
  color: rgba(203, 213, 225, 0.74);
  line-height: 1.25;
}

.gr-insights__meter {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 6px;
}

.gr-insights__meter-label,
.gr-insights__meter-value {
  margin: 0;
  font-size: 10px;
  color: rgba(191, 219, 254, 0.8);
}

.gr-insights__meter-track {
  position: relative;
  height: 6px;
  border-radius: 999px;
  overflow: hidden;
  background: rgba(30, 41, 59, 0.85);
  border: 1px solid rgba(148, 163, 184, 0.22);
}

.gr-insights__meter-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #fb7185, #fbbf24 42%, #4ade80);
}

.gr-line-strip {
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}

.gr-chip {
  padding: 6px 8px;
}

.gr-chip__value {
  margin: 4px 0 0;
  font-size: var(--gr-chip-size);
  line-height: 1;
  letter-spacing: -0.02em;
  font-weight: 760;
  color: rgba(241, 245, 249, 0.97);
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

.gr-chart {
  width: 100%;
  height: 100%;
  min-height: 0;
}

.gr-line-foot {
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
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

.is-compact .gr-number-grid,
.is-compact .gr-line-foot {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.is-compact .gr-insights__grid {
  grid-template-columns: 1fr;
}

.is-compact .gr-line-strip {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.is-compact .gr-line-strip > :last-child {
  grid-column: 1 / -1;
}

.is-tiny .gr-number-grid,
.is-tiny .gr-line-strip,
.is-tiny .gr-line-foot {
  grid-template-columns: 1fr;
  gap: 6px;
}

.is-tiny .gr-chip,
.is-tiny .gr-stat,
.is-tiny .gr-insight-chip,
.is-tiny .gr-foot-card {
  padding: 7px 8px;
}

.is-tiny .gr-insights {
  gap: 5px;
}

.is-tiny .gr-insights__meter {
  display: none;
}

.is-tiny .gr-hero {
  padding: 10px 11px;
}

.is-tiny .gr-hero__range,
.is-tiny .gr-stat__meta,
.is-tiny .gr-foot-card__meta {
  font-size: 10px;
}
</style>


