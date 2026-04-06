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
    <div class="flex h-full min-h-0 flex-col gap-3">
      <section class="flex flex-wrap items-center justify-between gap-2">
        <div class="inline-flex items-center gap-2 rounded-full border border-slate-300/20 bg-slate-900/45 p-1">
          <button
            v-for="mode in viewModes"
            :key="mode.key"
            type="button"
            class="rounded-full px-3 py-1 text-[11px] font-semibold transition"
            :class="
              selectedView === mode.key
                ? 'bg-sky-500/30 text-sky-100 ring-1 ring-sky-300/40'
                : 'text-slate-300/75 hover:bg-slate-700/40 hover:text-slate-100'
            "
            :aria-pressed="selectedView === mode.key"
            @click="switchView(mode.key)"
          >
            {{ mode.label }}
          </button>
        </div>

        <div class="flex flex-wrap items-center gap-2 text-[11px] text-slate-300/80">
          <span>Moyenne / {{ bucketLabel }}</span>
          <strong class="text-slate-100">{{ avgText }}</strong>
          <span>|</span>
          <span>Variation</span>
          <strong :class="changeToneClass">{{ changeText }}</strong>
        </div>
      </section>

      <section class="grid gap-3 xl:grid-cols-[minmax(0,1.35fr)_minmax(0,1fr)]">
        <article
          class="rounded-2xl border border-sky-300/20 bg-gradient-to-br from-slate-900/85 via-slate-900/70 to-slate-950/90 p-4 shadow-[0_14px_28px_rgba(2,6,23,0.26)]"
        >
          <div class="text-[10px] uppercase tracking-[0.2em] text-slate-300/70">Total periode</div>
          <div class="mt-2 font-extrabold leading-none tracking-[-0.04em] text-slate-50" :style="heroValueStyle">
            {{ totalText }}
          </div>

          <div class="mt-3 flex flex-wrap items-center gap-2">
            <span
              v-if="kpi.deltaPct != null"
              class="inline-flex items-center gap-1 rounded-full border px-2 py-1 text-[11px] font-bold"
              :class="deltaBadgeClass"
            >
              <span>{{ deltaArrow }}</span>
              <span>{{ deltaText }}</span>
            </span>
            <span class="text-[11px] text-slate-300/80">vs periode precedente</span>
          </div>

          <div class="mt-3 grid grid-cols-3 gap-2">
            <div class="rounded-lg border border-slate-400/20 bg-slate-900/40 px-3 py-2">
              <div class="text-[10px] uppercase tracking-[0.12em] text-slate-400">Moyenne</div>
              <div class="mt-1 text-sm font-semibold text-slate-100">{{ avgText }}</div>
            </div>
            <div class="rounded-lg border border-slate-400/20 bg-slate-900/40 px-3 py-2">
              <div class="text-[10px] uppercase tracking-[0.12em] text-slate-400">Amplitude</div>
              <div class="mt-1 text-sm font-semibold text-slate-100">{{ spreadText }}</div>
            </div>
            <div class="rounded-lg border border-slate-400/20 bg-slate-900/40 px-3 py-2">
              <div class="text-[10px] uppercase tracking-[0.12em] text-slate-400">Points</div>
              <div class="mt-1 text-sm font-semibold text-slate-100">{{ pointsCount }}</div>
            </div>
          </div>
        </article>

        <div class="grid grid-cols-2 gap-2">
          <article
            v-for="card in statCards"
            :key="card.key"
            class="rounded-xl border px-3 py-2.5"
            :class="card.className"
          >
            <div class="text-[10px] uppercase tracking-[0.14em] text-slate-400">{{ card.label }}</div>
            <div class="mt-1 text-[24px] font-extrabold leading-none tracking-[-0.03em] text-slate-100">
              {{ card.value }}
            </div>
            <div v-if="card.meta" class="mt-1 text-[11px] text-slate-300/75">{{ card.meta }}</div>
          </article>
        </div>
      </section>

      <section class="flex flex-wrap items-center justify-between gap-2 text-[11px]">
        <div class="flex flex-wrap items-center gap-2">
          <div class="inline-flex items-center gap-2 rounded-full border border-slate-300/20 bg-slate-900/50 px-3 py-1.5">
            <span class="uppercase tracking-[0.14em] text-slate-400">Periode</span>
            <span class="font-semibold text-slate-200">{{ fromLabel }} -> {{ toLabel }}</span>
          </div>
          <span class="rounded-full border border-slate-300/20 bg-slate-900/45 px-3 py-1 text-slate-300/90">
            {{ pointsText }}
          </span>
          <span class="rounded-full border border-slate-300/20 bg-slate-900/45 px-3 py-1 text-slate-300/90">
            {{ trendLabel }}
          </span>
        </div>

        <span class="rounded-full border border-slate-300/20 bg-slate-900/45 px-3 py-1 text-slate-300/90">
          Vue {{ selectedView === 'line' ? 'Graphe' : 'Chiffres' }}
        </span>
      </section>

      <section
        v-if="selectedView === 'line'"
        class="relative min-h-0 flex-1 overflow-hidden rounded-xl border border-slate-300/15 bg-gradient-to-b from-slate-950/50 to-slate-950/30"
        :style="{ minHeight: chartMinHeight }"
      >
        <VChart class="h-full w-full" :option="option" autoresize />
        <div
          v-if="pointsCount <= 1"
          class="pointer-events-none absolute inset-0 grid place-items-center bg-slate-950/35 text-center"
        >
          <div>
            <div class="text-sm font-semibold text-slate-100">Pas assez de points</div>
            <div class="mt-1 text-xs text-slate-300/85">Elargis la periode pour afficher une tendance.</div>
          </div>
        </div>
      </section>

      <section
        v-else
        class="grid gap-3 rounded-xl border border-slate-300/15 bg-gradient-to-b from-slate-950/40 to-slate-950/20 p-3 md:grid-cols-[minmax(0,1fr)_minmax(0,1.2fr)]"
      >
        <article class="rounded-xl border border-slate-400/20 bg-slate-900/45 p-3">
          <div class="text-[10px] uppercase tracking-[0.14em] text-slate-400">Lecture rapide</div>
          <div class="mt-2 text-xl font-bold text-slate-100">{{ trendLabel }}</div>
          <p class="mt-2 text-sm text-slate-300/85">
            Variation {{ changeText }} sur la periode, avec une moyenne de {{ avgText }} par {{ bucketLabel }}.
          </p>
          <p class="mt-2 text-[12px] text-slate-300/70">
            Pic {{ bestText }} | Creux {{ lowText }}
          </p>
        </article>

        <article class="rounded-xl border border-slate-400/20 bg-slate-900/45 p-3">
          <div class="mb-2 text-[10px] uppercase tracking-[0.14em] text-slate-400">Derniers points</div>
          <div v-if="recentPoints.length" class="space-y-1.5">
            <div
              v-for="point in recentPoints"
              :key="point.key"
              class="flex items-center justify-between rounded-lg border border-slate-400/15 bg-slate-950/35 px-2.5 py-1.5"
            >
              <span class="text-[11px] text-slate-300/80">{{ point.dateLabel }}</span>
              <strong class="text-[12px] text-slate-100">{{ point.valueText }}</strong>
            </div>
          </div>
          <div v-else class="text-xs text-slate-300/70">Aucun point disponible sur cette periode.</div>
        </article>
      </section>
    </div>
  </WidgetCard>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeKpi, normalizeSeries, parseYmdLocal } from '@/services/statsAdapters'
import { formatDateFR, formatEUR, signFmt } from '@/utils/formatters'
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
})

const emit = defineEmits(['view-change'])

const accent = '#3B82F6'
const loading = ref(false)
const error = ref('')
const kpi = ref({ value: 0, deltaPct: null })
const series = ref([])
let req = 0

const clamp = (value, min, max) => Math.max(min, Math.min(max, value))

const viewModes = [
  { key: 'number', label: 'Chiffres' },
  { key: 'line', label: 'Graphe' },
]

const selectedView = computed(() => (props.view === 'line' ? 'line' : 'number'))

function switchView(nextView) {
  if (nextView !== 'number' && nextView !== 'line') return
  if (nextView === selectedView.value) return
  emit('view-change', nextView)
}

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
const avgValue = computed(() => (pointsCount.value > 0 ? totalValue.value / pointsCount.value : 0))
const bestPoint = computed(() => {
  if (!series.value.length) return null
  return series.value.reduce((best, point) =>
    Number(point.value ?? 0) > Number(best.value ?? 0) ? point : best,
  )
})
const lowPoint = computed(() => {
  if (!series.value.length) return null
  return series.value.reduce((low, point) =>
    Number(point.value ?? 0) < Number(low.value ?? 0) ? point : low,
  )
})

const bestValue = computed(() => Number(bestPoint.value?.value ?? 0))
const lowValue = computed(() => Number(lowPoint.value?.value ?? 0))
const lastValue = computed(() =>
  series.value.length ? Number(series.value[series.value.length - 1].value ?? 0) : 0,
)
const firstValue = computed(() => (series.value.length ? Number(series.value[0].value ?? 0) : 0))

const changePct = computed(() => {
  const first = firstValue.value
  if (!Number.isFinite(first) || first === 0) return null
  return ((lastValue.value - first) / Math.abs(first)) * 100
})

const spreadValue = computed(() => Math.max(0, bestValue.value - lowValue.value))

const totalText = computed(() => formatEUR(totalValue.value, { compact: true }))
const avgText = computed(() => formatEUR(avgValue.value, { compact: true }))
const bestText = computed(() => formatEUR(bestValue.value, { compact: true }))
const lowText = computed(() => formatEUR(lowValue.value, { compact: true }))
const lastText = computed(() => formatEUR(lastValue.value, { compact: true }))
const spreadText = computed(() => formatEUR(spreadValue.value, { compact: true }))
const changeText = computed(() => (changePct.value == null ? '--' : signFmt(changePct.value)))
const deltaText = computed(() => (kpi.value.deltaPct == null ? '--' : signFmt(kpi.value.deltaPct)))
const pointsText = computed(() => `${pointsCount.value} ${pointsCount.value > 1 ? 'points' : 'point'}`)

const fromLabel = computed(() =>
  formatDateFR(props.from, { day: '2-digit', month: 'short', year: 'numeric' }),
)
const toLabel = computed(() =>
  formatDateFR(props.to, { day: '2-digit', month: 'short', year: 'numeric' }),
)
const bucketLabel = computed(() => (effectiveBucket.value === 'month' ? 'mois' : 'jour'))

const trendLabel = computed(() => {
  if (changePct.value == null) return 'Tendance indisponible'
  if (changePct.value >= 2) return 'Hausse nette'
  if (changePct.value <= -2) return 'Baisse nette'
  return 'Tendance stable'
})

const deltaArrow = computed(() => {
  if (kpi.value.deltaPct == null) return '='
  if (kpi.value.deltaPct > 0.01) return '+'
  if (kpi.value.deltaPct < -0.01) return '-'
  return '='
})

const deltaBadgeClass = computed(() => {
  if (kpi.value.deltaPct == null) return 'border-slate-400/35 bg-slate-700/25 text-slate-100'
  if (kpi.value.deltaPct >= 0) return 'border-emerald-400/35 bg-emerald-500/15 text-emerald-200'
  return 'border-rose-400/35 bg-rose-500/15 text-rose-200'
})

const changeToneClass = computed(() => {
  if (changePct.value == null) return 'text-slate-100'
  return changePct.value >= 0 ? 'text-emerald-200' : 'text-rose-200'
})

const recentPoints = computed(() =>
  [...series.value]
    .slice(-6)
    .reverse()
    .map((point, index) => ({
      key: `${point.date ?? 'na'}-${index}`,
      dateLabel: formatDateAxis(point.date, effectiveBucket.value),
      valueText: formatEUR(Number(point.value ?? 0), { compact: true }),
    })),
)

const statCards = computed(() => [
  {
    key: 'last',
    label: 'Dernier CA',
    value: lastText.value,
    meta: pointsCount.value > 0 ? `Point #${pointsCount.value}` : '',
    className: 'border-slate-400/20 bg-slate-900/45',
  },
  {
    key: 'change',
    label: 'Variation',
    value: changeText.value,
    meta: pointsCount.value > 1 ? 'vs premier point' : '',
    className:
      changePct.value == null
        ? 'border-slate-400/20 bg-slate-900/45'
        : changePct.value >= 0
          ? 'border-emerald-400/30 bg-emerald-500/10'
          : 'border-rose-400/30 bg-rose-500/10',
  },
  {
    key: 'high',
    label: 'Pic',
    value: bestText.value,
    meta: bestPoint.value ? formatDateAxis(bestPoint.value.date, effectiveBucket.value) : '',
    className: 'border-sky-400/28 bg-sky-500/10',
  },
  {
    key: 'low',
    label: 'Creux',
    value: lowText.value,
    meta: lowPoint.value ? formatDateAxis(lowPoint.value.date, effectiveBucket.value) : '',
    className: 'border-slate-400/20 bg-slate-900/45',
  },
])

const heroValueStyle = computed(() => {
  const size = clamp(Math.round(Math.min(props.widgetWidth * 0.052, props.widgetHeight * 0.17)), 34, 56)
  return { fontSize: `${size}px` }
})

const chartMinHeight = computed(() => {
  const h = clamp(Math.round(props.widgetHeight * 0.52), 220, 430)
  return `${h}px`
})

const option = computed(() => {
  const x = series.value.map((p) => p.date)
  const y = series.value.map((p) => p.value)
  const step = effectiveBucket.value === 'month' ? 1 : Math.max(1, Math.ceil(x.length / 6))
  const labelInterval = Math.max(0, step - 1)
  const axisFont = clamp(Math.round(Math.min(props.widgetWidth * 0.014, props.widgetHeight * 0.034)), 11, 14)

  return {
    backgroundColor: 'transparent',
    animationDuration: 320,
    animationDurationUpdate: 260,
    grid: { left: 50, right: 18, top: 16, bottom: 54, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'line', lineStyle: { color: '#94A3B8', type: 'dashed' } },
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
      axisLine: { lineStyle: { color: 'rgba(100,116,139,0.48)' } },
      axisLabel: {
        color: 'rgba(203,213,225,0.9)',
        fontSize: axisFont,
        margin: 12,
        interval: labelInterval,
        formatter: (value) => formatDateAxis(value, effectiveBucket.value),
      },
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisLabel: {
        color: 'rgba(148,163,184,0.92)',
        fontSize: axisFont,
        formatter: (value) => formatCompactNumber(value),
      },
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.18)', type: 'dashed' } },
    },
    series: [
      {
        type: 'line',
        data: y,
        smooth: x.length > 2 ? 0.3 : false,
        showSymbol: x.length <= 2,
        connectNulls: true,
        symbolSize: 7,
        lineStyle: { width: 3, color: accent },
        itemStyle: { color: accent },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: `${accent}66` },
              { offset: 1, color: `${accent}00` },
            ],
          },
        },
        markLine:
          pointsCount.value > 1
            ? {
                symbol: 'none',
                lineStyle: { color: 'rgba(148,163,184,0.42)', type: 'dashed' },
                label: {
                  show: true,
                  color: 'rgba(203,213,225,0.9)',
                  formatter: ({ value }) => `Moy ${formatEUR(Number(value ?? 0), { compact: true })}`,
                },
                data: [{ yAxis: avgValue.value }],
              }
            : undefined,
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
    const months = ['jan', 'fev', 'mar', 'avr', 'mai', 'jun', 'jul', 'aou', 'sep', 'oct', 'nov', 'dec']
    const mm = months[m - 1] || ''
    return bucket === 'month' ? `${mm} ${String(y).slice(-2)}` : `${String(d).padStart(2, '0')} ${mm}`
  }
  if (/^\d{4}-\d{2}$/.test(v)) {
    const parts = v.split('-').map(Number)
    const y = parts[0] || 0
    const m = parts[1] || 1
    const months = ['jan', 'fev', 'mar', 'avr', 'mai', 'jun', 'jul', 'aou', 'sep', 'oct', 'nov', 'dec']
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
