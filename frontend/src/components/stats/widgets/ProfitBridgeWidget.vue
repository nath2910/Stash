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

const option = computed(() => ({
  backgroundColor: 'transparent',
  grid: { left: 24, right: 14, top: 24, bottom: 34, containLabel: true },
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
    confine: true,
    transitionDuration: 0,
  },
  xAxis: {
    type: 'category',
    data: bridgeBars.value.map((entry) => entry.label),
    axisLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.35)' } },
    axisTick: { show: false },
    axisLabel: { color: 'rgba(226, 232, 240, 0.82)', fontSize: 11 },
  },
  yAxis: {
    type: 'value',
    axisLabel: {
      color: 'rgba(148, 163, 184, 0.9)',
      formatter: (value) => formatEUR(value, { compact: true, digits: 1 }),
    },
    splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.14)' } },
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
      barWidth: 44,
      label: {
        show: true,
        position: 'top',
        color: 'rgba(226, 232, 240, 0.9)',
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
}))
</script>

<style scoped>
.bridge-wrap {
  height: 100%;
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
}

.bridge-chart {
  min-height: 0;
}

.bridge-foot {
  margin-top: 6px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 11px;
  color: rgba(226, 232, 240, 0.78);
}

.bridge-foot span {
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 999px;
  padding: 5px 9px;
  background: rgba(15, 23, 42, 0.56);
}

.bridge-foot span.is-up {
  color: #86efac;
}

.bridge-foot span.is-down {
  color: #fda4af;
}
</style>

