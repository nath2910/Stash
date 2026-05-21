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
      <button
        type="button"
        class="momentum-info"
        :title="momentumHelp"
        :aria-label="momentumHelp"
      >
        <Info class="h-3.5 w-3.5" />
      </button>
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
import { Info } from 'lucide-vue-next'
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
const clamp = (value, min, max) => Math.max(min, Math.min(max, value))
const layoutWidth = computed(() => Math.max(Number(props.widgetWidth ?? 0), 1))
const layoutHeight = computed(() => Math.max(Number(props.widgetHeight ?? 0), 1))
const denseMode = computed(() => layoutWidth.value < 520 || layoutHeight.value < 270)
const trendText = computed(() => {
  const value = Number(kpi.value.value || 0)
  const formatted = formatMetricValue(value)
  const delta = kpi.value.deltaPct == null ? '' : ` (${signFmt(kpi.value.deltaPct)})`
  return `${formatted}${delta}`
})
const momentumHelp =
  "Le momentum compare la moyenne recente de la mesure avec sa moyenne precedente. Score positif: acceleration, score negatif: ralentissement."
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
      radius: denseMode.value ? '88%' : '94%',
      center: ['50%', denseMode.value ? '55%' : '54%'],
      pointer: {
        length: denseMode.value ? '48%' : '55%',
        width: denseMode.value ? 4 : 5,
      },
      progress: {
        show: true,
        width: denseMode.value ? 10 : 14,
        itemStyle: {
          color: score.value >= 0 ? '#22c55e' : '#f43f5e',
        },
      },
      axisLine: {
        lineStyle: {
          width: denseMode.value ? 10 : 14,
          color: [[1, 'rgba(148, 163, 184, 0.24)']],
        },
      },
      axisLabel: {
        show: !denseMode.value,
        color: '#64748b',
        fontSize: clamp(Math.round(Math.min(layoutWidth.value * 0.012, layoutHeight.value * 0.03)), 9, 11),
      },
      axisTick: { show: false },
      splitLine: { show: false },
      detail: {
        show: true,
        formatter: () => scoreText.value,
        color: '#111827',
        fontSize: clamp(Math.round(Math.min(layoutWidth.value * 0.042, layoutHeight.value * 0.075)), 14, 22),
        fontWeight: 800,
        offsetCenter: [0, denseMode.value ? '34%' : '38%'],
      },
      data: [{ value: scoreNormalized.value }],
    },
  ],
}))

const sparkOption = computed(() => ({
  backgroundColor: 'transparent',
  grid: { left: 0, right: 0, top: 0, bottom: 0 },
  tooltip: {
    trigger: 'axis',
    triggerOn: 'mousemove|click',
    confine: true,
    transitionDuration: 0,
    backgroundColor: 'rgba(255,255,255,0.98)',
    borderColor: 'rgba(148,163,184,0.28)',
    borderWidth: 1,
    padding: [7, 9],
    textStyle: { color: '#0f172a', fontSize: 11, fontWeight: 650 },
    extraCssText: 'border-radius:9px;box-shadow:0 10px 22px rgba(15,23,42,0.13);',
    axisPointer: {
      type: 'line',
      snap: true,
      lineStyle: { color: 'rgba(45,212,191,0.55)', width: 1 },
    },
    formatter: formatSparkTooltip,
  },
  xAxis: {
    type: 'category',
    show: false,
    data: values.value.map((_, index) => index),
    axisPointer: { show: true, snap: true },
  },
  yAxis: { type: 'value', show: false },
  series: [
    {
      type: 'line',
      data: values.value,
      smooth: true,
      showSymbol: false,
      lineStyle: { width: 2, color: accent },
      emphasis: {
        focus: 'self',
        scale: true,
        itemStyle: {
          color: '#ffffff',
          borderColor: accent,
          borderWidth: 2,
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
            { offset: 0, color: 'rgba(45, 212, 191, 0.34)' },
            { offset: 1, color: 'rgba(45, 212, 191, 0)' },
          ],
        },
      },
    },
  ],
}))

function formatMetricValue(value) {
  if (props.metric === 'roi' || props.metric === 'avgMargin' || props.metric === 'sellThrough') {
    return formatPct(value, { digits: 1 })
  }
  return formatEUR(value, { compact: true, digits: 1 })
}

function formatSignedMetricDelta(value) {
  const n = Number(value ?? 0)
  const sign = n > 0 ? '+' : n < 0 ? '-' : ''
  const abs = Math.abs(n)
  if (props.metric === 'roi' || props.metric === 'avgMargin' || props.metric === 'sellThrough') {
    return `${sign}${formatPct(abs, { digits: 1 })}`
  }
  return `${sign}${formatEUR(abs, { compact: true, digits: 1 })}`
}

function formatSparkTooltip(params) {
  const entry = Array.isArray(params) ? params[0] : params
  const index = Number(entry?.dataIndex ?? -1)
  const value = Number(entry?.data ?? 0)
  const previous = Number(values.value[index - 1] ?? NaN)
  const detail =
    Number.isFinite(previous) && index > 0
      ? `<div style="margin-top:3px;color:#64748b;">Variation: <strong style="color:${
          value - previous >= 0 ? '#047857' : '#be123c'
        };">${escapeHtml(formatSignedMetricDelta(value - previous))}</strong></div>`
      : ''
  return `<div style="color:#64748b;font-size:10px;text-transform:uppercase;letter-spacing:.06em;">Point ${index + 1}</div><div style="margin-top:3px;font-weight:800;color:#111827;">${escapeHtml(formatMetricValue(value))}</div>${detail}`
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
</script>

<style scoped>
.momentum-grid {
  position: relative;
  height: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 0;
}

.momentum-info {
  position: absolute;
  top: 4px;
  right: 4px;
  z-index: 2;
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  background: rgba(248, 250, 252, 0.82);
  color: #64748b;
}

.momentum-info:hover {
  border-color: rgba(45, 212, 191, 0.56);
  color: #0f766e;
}

.momentum-gauge,
.momentum-spark {
  width: 100%;
  height: 100%;
  min-height: 0;
}

.momentum-side {
  display: none;
}

.momentum-score {
  font-size: 30px;
  line-height: 1;
  font-weight: 700;
  color: #111827;
}

.momentum-score.is-good {
  color: #047857;
}

.momentum-score.is-mid {
  color: #b45309;
}

.momentum-score.is-low {
  color: #be123c;
}

.momentum-meta {
  font-size: 12px;
  color: #64748b;
}

.momentum-spark {
  height: 88px;
}
</style>
