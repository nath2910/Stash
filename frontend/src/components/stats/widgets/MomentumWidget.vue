<template>
  <WidgetCard
    title="Momentum"
    subtitle="Vitesse d acceleration"
    :accent="accent"
    surface="trend"
    :loading="loading"
    :error="error"
    :widget-width="props.widgetWidth"
    :widget-height="props.widgetHeight"
    :widget-base-width="props.widgetBaseWidth"
    :widget-base-height="props.widgetBaseHeight"
  >
    <div class="momentum-grid">
      <VChart class="momentum-gauge" :option="gaugeOption" autoresize />
      <div class="momentum-side">
        <div class="momentum-score" :class="toneClass">{{ scoreText }}</div>
        <div class="momentum-meta">{{ trendText }}</div>
        <VChart class="momentum-spark" :option="sparkOption" autoresize />
      </div>
    </div>
  </WidgetCard>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeKpi, normalizeSeries } from '@/services/statsAdapters'
import { formatEUR, formatPct, signFmt } from '@/utils/formatters'
import WidgetCard from './_parts/WidgetCard.vue'

const props = defineProps({
  from: String,
  to: String,
  metric: { type: String, default: 'netProfit' },
  bucket: { type: String, default: 'week' },
  categories: { type: Array, default: () => [] },
  types: { type: Array, default: () => [] },
  widgetWidth: { type: Number, default: 720 },
  widgetHeight: { type: Number, default: 340 },
  widgetBaseWidth: { type: Number, default: 720 },
  widgetBaseHeight: { type: Number, default: 340 },
})

const accent = '#2dd4bf'
const loading = ref(false)
const error = ref('')
const kpi = ref({ value: 0, deltaPct: null })
const series = ref([])
let requestSeq = 0

async function load() {
  const seq = ++requestSeq
  loading.value = true
  error.value = ''
  try {
    const [k, s] = await Promise.all([
      StatsServices.kpi(props.metric, props.from, props.to, props.categories, props.types),
      StatsServices.series(props.metric, props.from, props.to, props.bucket, props.categories, props.types),
    ])
    if (seq !== requestSeq) return
    kpi.value = normalizeKpi(k.data)
    series.value = normalizeSeries(s.data)
  } catch (err) {
    if (seq !== requestSeq) return
    error.value = err?.response?.data?.message ?? err?.message ?? 'Impossible de charger'
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

onMounted(load)
watch(() => [props.from, props.to, props.metric, props.bucket, props.categories, props.types], load)

const values = computed(() => series.value.map((entry) => Number(entry.value || 0)).slice(-12))
const velocity = computed(() => {
  if (values.value.length < 4) return 0
  const mid = Math.floor(values.value.length / 2)
  const firstHalf = values.value.slice(0, mid)
  const secondHalf = values.value.slice(mid)
  const avg = (list) => list.reduce((sum, value) => sum + value, 0) / Math.max(list.length, 1)
  const base = avg(firstHalf)
  const current = avg(secondHalf)
  if (!base) return current ? 100 : 0
  return ((current - base) / Math.abs(base)) * 100
})
const score = computed(() => Math.max(-100, Math.min(100, velocity.value)))
const scoreNormalized = computed(() => Math.round((score.value + 100) / 2))
const scoreText = computed(() => `${Math.round(score.value)} pts`)
const trendText = computed(() => {
  const value = Number(kpi.value.value || 0)
  const formatted =
    props.metric === 'roi' || props.metric === 'avgMargin' || props.metric === 'sellThrough'
      ? formatPct(value, { digits: 1 })
      : formatEUR(value, { compact: true, digits: 1 })
  const delta = kpi.value.deltaPct == null ? '' : ` (${signFmt(kpi.value.deltaPct)})`
  return `${formatted}${delta}`
})
const toneClass = computed(() => {
  if (score.value > 18) return 'is-good'
  if (score.value < -18) return 'is-low'
  return 'is-mid'
})

const gaugeOption = computed(() => ({
  backgroundColor: 'transparent',
  series: [
    {
      type: 'gauge',
      startAngle: 200,
      endAngle: -20,
      min: 0,
      max: 100,
      splitNumber: 5,
      pointer: {
        length: '55%',
        width: 5,
      },
      progress: {
        show: true,
        width: 14,
        itemStyle: {
          color: score.value >= 0 ? '#22c55e' : '#f43f5e',
        },
      },
      axisLine: {
        lineStyle: {
          width: 14,
          color: [[1, 'rgba(148, 163, 184, 0.24)']],
        },
      },
      axisLabel: { color: 'rgba(203, 213, 225, 0.7)' },
      axisTick: { show: false },
      splitLine: { show: false },
      detail: { show: false },
      data: [{ value: scoreNormalized.value }],
    },
  ],
}))

const sparkOption = computed(() => ({
  backgroundColor: 'transparent',
  grid: { left: 0, right: 0, top: 0, bottom: 0 },
  xAxis: { type: 'category', show: false, data: values.value.map((_, index) => index) },
  yAxis: { type: 'value', show: false },
  series: [
    {
      type: 'line',
      data: values.value,
      smooth: true,
      showSymbol: false,
      lineStyle: { width: 2, color: accent },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(45, 212, 191, 0.34)' },
            { offset: 1, color: 'rgba(45, 212, 191, 0)' },
          ],
        },
      },
    },
  ],
}))
</script>

<style scoped>
.momentum-grid {
  height: 100%;
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 10px;
}

.momentum-gauge,
.momentum-spark {
  min-height: 0;
}

.momentum-side {
  display: grid;
  gap: 8px;
  align-content: start;
}

.momentum-score {
  font-size: 30px;
  line-height: 1;
  font-weight: 700;
  color: rgba(248, 250, 252, 0.96);
}

.momentum-score.is-good {
  color: #86efac;
}

.momentum-score.is-mid {
  color: #fde047;
}

.momentum-score.is-low {
  color: #fda4af;
}

.momentum-meta {
  font-size: 12px;
  color: rgba(203, 213, 225, 0.82);
}

.momentum-spark {
  height: 88px;
}
</style>

