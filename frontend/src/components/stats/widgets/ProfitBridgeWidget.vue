<template>
  <WidgetCard
    title="Profit bridge"
    subtitle="Evolution de la rentabilite"
    :accent="accent"
    surface="trend"
    :loading="loading"
    :error="error"
    :widget-width="props.widgetWidth"
    :widget-height="props.widgetHeight"
    :widget-base-width="props.widgetBaseWidth"
    :widget-base-height="props.widgetBaseHeight"
  >
    <div class="bridge-wrap">
      <VChart class="bridge-chart" :option="option" autoresize />
      <div class="bridge-foot">
        <span>Start {{ prevText }}</span>
        <span :class="deltaClass">{{ deltaText }}</span>
        <span>End {{ currText }}</span>
      </div>
    </div>
  </WidgetCard>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeSummary, prevPeriod } from '@/services/statsAdapters'
import { formatEUR, signFmt } from '@/utils/formatters'
import WidgetCard from './_parts/WidgetCard.vue'

const props = defineProps({
  from: String,
  to: String,
  categories: { type: Array, default: () => [] },
  types: { type: Array, default: () => [] },
  widgetWidth: { type: Number, default: 1080 },
  widgetHeight: { type: Number, default: 470 },
  widgetBaseWidth: { type: Number, default: 1080 },
  widgetBaseHeight: { type: Number, default: 470 },
})

const accent = '#22c55e'
const loading = ref(false)
const error = ref('')
const current = ref({ profit: 0, ca: 0, profitMargin: 0 })
const previous = ref({ profit: 0, ca: 0, profitMargin: 0 })
let requestSeq = 0

async function load() {
  if (!props.from || !props.to) return
  const seq = ++requestSeq
  loading.value = true
  error.value = ''
  try {
    const prev = prevPeriod(props.from, props.to)
    const [currRes, prevRes] = await Promise.all([
      StatsServices.summary({
        from: props.from,
        to: props.to,
        categories: props.categories,
        types: props.types,
      }),
      StatsServices.summary({
        from: prev.from,
        to: prev.to,
        categories: props.categories,
        types: props.types,
      }),
    ])
    if (seq !== requestSeq) return
    current.value = normalizeSummary(currRes.data)
    previous.value = normalizeSummary(prevRes.data)
  } catch (err) {
    if (seq !== requestSeq) return
    error.value = err?.response?.data?.message ?? err?.message ?? 'Impossible de charger'
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

onMounted(load)
watch(() => [props.from, props.to, props.categories, props.types], load)

const start = computed(() => Number(previous.value.profit || 0))
const end = computed(() => Number(current.value.profit || 0))
const delta = computed(() => end.value - start.value)
const bridgeBars = computed(() => {
  const up = delta.value >= 0
  return [
    { label: 'Start', base: 0, value: start.value, color: '#64748b' },
    {
      label: 'Delta',
      base: up ? start.value : end.value,
      value: Math.abs(delta.value),
      color: up ? '#22c55e' : '#f43f5e',
    },
    { label: 'End', base: 0, value: end.value, color: '#38bdf8' },
  ]
})

const prevText = computed(() => formatEUR(start.value, { compact: true, digits: 1 }))
const currText = computed(() => formatEUR(end.value, { compact: true, digits: 1 }))
const deltaText = computed(() => signFmt(delta.value, ' EUR'))
const deltaClass = computed(() => (delta.value >= 0 ? 'is-up' : 'is-down'))
const clamp = (value, min, max) => Math.max(min, Math.min(max, value))
const layoutWidth = computed(() => Math.max(Number(props.widgetWidth ?? 0), 1))
const layoutHeight = computed(() => Math.max(Number(props.widgetHeight ?? 0), 1))
const denseMode = computed(() => layoutWidth.value < 620 || layoutHeight.value < 300)

const option = computed(() => {
  const axisFont = clamp(
    Math.round(Math.min(layoutWidth.value * 0.012, layoutHeight.value * 0.032)),
    9,
    12,
  )
  const labelVisible = layoutWidth.value >= 460 && layoutHeight.value >= 220

  return {
    backgroundColor: 'transparent',
    grid: {
      left: clamp(Math.round(layoutWidth.value * 0.045), 34, 68),
      right: clamp(Math.round(layoutWidth.value * 0.025), 12, 28),
      top: labelVisible ? clamp(Math.round(layoutHeight.value * 0.08), 20, 36) : 12,
      bottom: clamp(Math.round(layoutHeight.value * 0.09), 28, 46),
      containLabel: true,
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      confine: true,
      transitionDuration: 0,
      backgroundColor: 'rgba(255,255,255,0.98)',
      borderColor: 'rgba(148,163,184,0.32)',
      textStyle: { color: '#0f172a', fontSize: 12, fontWeight: 600 },
      extraCssText: 'border-radius:10px;box-shadow:0 12px 28px rgba(15,23,42,0.14);',
    },
    xAxis: {
      type: 'category',
      data: bridgeBars.value.map((entry) => entry.label),
      axisLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.35)' } },
      axisTick: { show: false },
      axisLabel: { color: '#64748b', fontSize: axisFont, fontWeight: 650 },
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#64748b',
        fontSize: axisFont,
        fontWeight: 650,
        formatter: (value) => formatEUR(value, { compact: true, digits: 1 }),
      },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.18)' } },
    },
    series: [
      {
        type: 'bar',
        stack: 'bridge',
        silent: true,
        itemStyle: { color: 'transparent' },
        data: bridgeBars.value.map((entry) => entry.base),
      },
      {
        type: 'bar',
        stack: 'bridge',
        barWidth: clamp(Math.round(layoutWidth.value * 0.045), denseMode.value ? 22 : 30, 52),
        label: {
          show: labelVisible,
          position: 'top',
          color: '#334155',
          fontSize: axisFont,
          fontWeight: 750,
          formatter: (params) => formatEUR(params.value, { compact: true, digits: 1 }),
        },
        itemStyle: {
          borderRadius: [10, 10, 0, 0],
        },
        data: bridgeBars.value.map((entry) => ({
          value: entry.value,
          itemStyle: { color: entry.color },
        })),
      },
    ],
  }
})
</script>

<style scoped>
.bridge-wrap {
  height: 100%;
  display: grid;
  grid-template-rows: minmax(0, 1fr);
}

.bridge-chart {
  width: 100%;
  height: 100%;
  min-height: 0;
}

.bridge-foot {
  display: none;
}

.bridge-foot span {
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 999px;
  padding: 5px 9px;
  background: rgba(248, 250, 252, 0.82);
}

.bridge-foot span.is-up {
  color: #047857;
}

.bridge-foot span.is-down {
  color: #be123c;
}
</style>
