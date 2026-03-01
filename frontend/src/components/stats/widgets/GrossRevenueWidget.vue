<template>
  <WidgetCard
    title="Chiffre d'affaires"
    subtitle="Synthese et evolution sur la periode"
    :accent="accent"
    :loading="loading"
    :error="error"
  >
    <div class="flex flex-col gap-4">
      <div class="flex items-start justify-between gap-6">
        <div class="min-w-0">
          <div class="text-[11px] uppercase tracking-[0.2em] text-white/45">Total periode</div>
          <div class="mt-1 text-3xl font-bold text-white leading-none tracking-tight">
            {{ totalText }}
          </div>
          <div class="mt-2 flex items-center gap-2">
            <span
              v-if="kpi.deltaPct != null"
              class="text-[11px] px-2 py-0.5 rounded-full border"
              :class="
                kpi.deltaPct >= 0
                  ? 'border-emerald-400/30 bg-emerald-500/10 text-emerald-300'
                  : 'border-red-400/30 bg-red-500/10 text-red-300'
              "
            >
              {{ deltaText }}
            </span>
            <span class="text-[11px] text-white/45">vs periode precedente</span>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-3 text-right min-w-[260px]">
          <div class="rounded-lg border border-white/5 bg-white/5 px-3 py-2">
            <div class="text-[10px] uppercase tracking-[0.2em] text-white/40">Dernier CA</div>
            <div class="mt-1 text-sm font-semibold text-white/90">{{ lastText }}</div>
          </div>
          <div class="rounded-lg border border-white/5 bg-white/5 px-3 py-2">
            <div class="text-[10px] uppercase tracking-[0.2em] text-white/40">Variation</div>
            <div class="mt-1 text-sm font-semibold" :class="changeClass">{{ changeText }}</div>
          </div>
          <div class="rounded-lg border border-white/5 bg-white/5 px-3 py-2">
            <div class="text-[10px] uppercase tracking-[0.2em] text-white/40">Plus haut</div>
            <div class="mt-1 text-sm font-semibold text-white/90">{{ bestText }}</div>
          </div>
          <div class="rounded-lg border border-white/5 bg-white/5 px-3 py-2">
            <div class="text-[10px] uppercase tracking-[0.2em] text-white/40">Plus bas</div>
            <div class="mt-1 text-sm font-semibold text-white/80">{{ lowText }}</div>
          </div>
        </div>
      </div>

      <div class="flex flex-wrap items-center justify-between gap-2 text-[11px] text-white/45 leading-snug">
        <div class="flex flex-wrap items-center gap-2">
          <div class="period-chip">
            <span class="period-label">Periode</span>
            <span class="period-value">{{ fromLabel }} -> {{ toLabel }}</span>
          </div>
          <span class="text-white/25">•</span>
          <span>{{ pointsCount }} points</span>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-white/35">Moyenne / {{ bucketLabel }}:</span>
          <span class="text-white/80">{{ avgText }}</span>
        </div>
      </div>

      <div class="h-[280px]">
        <VChart class="w-full h-full" :option="option" autoresize />
      </div>
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
  categories: { type: Array, default: () => [] },
})
const accent = '#3B82F6'

const loading = ref(false)
const error = ref('')
const kpi = ref({ value: 0, deltaPct: null })
const series = ref([])
let req = 0

const effectiveBucket = computed(() => {
  const f = parseYmdLocal(props.from)
  const t = parseYmdLocal(props.to)
  if (!Number.isFinite(f?.getTime?.()) || !Number.isFinite(t?.getTime?.())) return props.bucket
  const months = diffMonths(f, t)
  return months < 6 ? 'day' : 'month'
})

const bucketLabel = computed(() => (effectiveBucket.value === 'month' ? 'mois' : 'jour'))

async function load() {
  const id = ++req
  loading.value = true
  error.value = ''
  try {
    const [k, s] = await Promise.all([
      StatsServices.kpi('grossRevenue', props.from, props.to, props.categories),
      StatsServices.series('grossRevenue', props.from, props.to, effectiveBucket.value, props.categories),
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
watch(() => [props.from, props.to, props.bucket, effectiveBucket.value, props.categories], load)

const totalValue = computed(() => {
  const k = Number(kpi.value.value ?? 0)
  if (Number.isFinite(k) && k > 0) return k
  return series.value.reduce((acc, p) => acc + Number(p.value ?? 0), 0)
})

const pointsCount = computed(() => series.value.length || 0)

const avgValue = computed(() => {
  const n = pointsCount.value
  return n > 0 ? totalValue.value / n : 0
})

const bestValue = computed(() => {
  if (!series.value.length) return 0
  return series.value.reduce((m, p) => Math.max(m, Number(p.value ?? 0)), 0)
})

const lowValue = computed(() => {
  if (!series.value.length) return 0
  return series.value.reduce((m, p) => Math.min(m, Number(p.value ?? 0)), Number.MAX_VALUE)
})

const lastValue = computed(() => {
  if (!series.value.length) return 0
  return Number(series.value[series.value.length - 1].value ?? 0)
})

const firstValue = computed(() => {
  if (!series.value.length) return 0
  return Number(series.value[0].value ?? 0)
})

const changePct = computed(() => {
  const first = firstValue.value
  if (!Number.isFinite(first) || first === 0) return null
  return ((lastValue.value - first) / Math.abs(first)) * 100
})

const totalText = computed(() => formatEUR(totalValue.value, { compact: true }))
const deltaText = computed(() => (kpi.value.deltaPct == null ? '' : signFmt(kpi.value.deltaPct)))
const avgText = computed(() => formatEUR(avgValue.value, { compact: true }))
const bestText = computed(() => formatEUR(bestValue.value, { compact: true }))
const lowText = computed(() => formatEUR(lowValue.value, { compact: true }))
const lastText = computed(() => formatEUR(lastValue.value, { compact: true }))
const changeText = computed(() => (changePct.value == null ? '--' : signFmt(changePct.value)))
const changeClass = computed(() =>
  changePct.value == null
    ? 'text-white/60'
    : changePct.value >= 0
      ? 'text-emerald-300'
      : 'text-red-300',
)

const fromLabel = computed(() =>
  formatDateFR(props.from, { day: '2-digit', month: 'short', year: 'numeric' }),
)
const toLabel = computed(() =>
  formatDateFR(props.to, { day: '2-digit', month: 'short', year: 'numeric' }),
)

const option = computed(() => {
  const x = series.value.map((p) => p.date)
  const y = series.value.map((p) => p.value)
  const step = effectiveBucket.value === 'month' ? 1 : Math.max(1, Math.ceil(x.length / 6))
  const labelInterval = Math.max(0, step - 1)

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'line', lineStyle: { color: '#94A3B8', type: 'dashed' } },
      formatter: (params) => {
        const p = Array.isArray(params) ? params[0] : params
        const label = formatDateAxis(p?.axisValue, effectiveBucket.value)
        const val = formatEUR(p?.data ?? 0)
        return `<div style="font-size:12px; color:#E2E8F0;">${label}</div>` +
          `<div style="margin-top:4px; color:#93C5FD; font-weight:600;">${val}</div>`
      },
    },
    grid: { left: 40, right: 14, top: 10, bottom: 60, containLabel: true },
    xAxis: {
      type: 'category',
      data: x,
      boundaryGap: true,
      axisTick: { alignWithLabel: true, show: true },
      axisLabel: {
        color: '#CBD5F5',
        show: true,
        hideOverlap: false,
        showMinLabel: true,
        showMaxLabel: true,
        inside: false,
        margin: 14,
        fontSize: 10,
        interval: labelInterval,
        formatter: (value) => formatDateAxis(value, effectiveBucket.value),
      },
      axisLine: { lineStyle: { color: '#374151' } },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#9CA3AF' },
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.15)', type: 'dashed' } },
    },
    series: [
      {
        type: 'line',
        data: y,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 3 },
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
        markPoint: {
          symbolSize: 44,
          data: [
            { type: 'max', name: 'Plus haut' },
            { type: 'min', name: 'Plus bas' },
          ],
          label: { color: '#E2E8F0', fontSize: 10 },
        },
        markLine: {
          symbol: 'none',
          lineStyle: { color: 'rgba(148,163,184,0.45)', type: 'dashed' },
          data: [{ yAxis: lastValue.value }],
          label: { show: false },
        },
      },
    ],
    color: [accent],
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

function diffMonths(a, b) {
  const start = a < b ? a : b
  const end = a < b ? b : a
  return (end.getFullYear() - start.getFullYear()) * 12 + (end.getMonth() - start.getMonth())
}
</script>

<style scoped>
.period-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(226, 232, 240, 0.6);
  white-space: nowrap;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.65);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.03),
    0 6px 16px rgba(0, 0, 0, 0.25);
}
.period-label {
  opacity: 0.7;
}
.period-value {
  font-size: 11px;
  letter-spacing: 0.02em;
  text-transform: none;
  color: rgba(226, 232, 240, 0.9);
}
</style>
