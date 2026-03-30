<template>
  <ChartCard
    title="Taux retours / litiges"
    subtitle="À surveiller"
    :accent="accent"
    surface="trend"
    :loading="loading"
    :error="error"
    :option="option"
  />
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeSeries } from '@/services/statsAdapters'
import { formatPct } from '@/utils/formatters'
import ChartCard from './_parts/ChartCard.vue'

const props = defineProps({
  from: String,
  to: String,
  bucket: { type: String, default: 'week' },
  warn: { type: Number, default: 4 }, // 4%
})
const accent = '#F97316'

const loading = ref(false)
const error = ref('')
const series = ref([])
let req = 0

async function load() {
  const id = ++req
  loading.value = true
  error.value = ''
  try {
    const { data } = await StatsServices.series('returnRate', props.from, props.to, props.bucket)
    if (id !== req) return
    series.value = normalizeSeries(data) // value = %
  } catch (e) {
    if (id !== req) return
    error.value = e?.response?.data?.message ?? e?.message ?? 'Impossible de charger'
  } finally {
    if (id === req) loading.value = false
  }
}

onMounted(load)
watch(() => [props.from, props.to, props.bucket, props.warn], load)

const option = computed(() => {
  const x = series.value.map((p) => p.date)
  const y = series.value.map((p) => p.value)
  const warn = Number(props.warn ?? 0)
  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      valueFormatter: (v) => formatPct(v, { digits: 1 }),
    },
    grid: { left: 46, right: 14, top: 10, bottom: 26 },
    xAxis: {
      type: 'category',
      data: x,
      axisLabel: { color: '#9CA3AF' },
      axisLine: { lineStyle: { color: '#374151' } },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#9CA3AF', formatter: (v) => `${v}%` },
      splitLine: { lineStyle: { color: '#1F2937' } },
    },
    series: [
      {
        type: 'line',
        data: y,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 3 },
        markLine: {
          symbol: 'none',
          label: { color: '#FCA5A5', formatter: `Seuil ${warn}%` },
          lineStyle: { color: '#EF4444', type: 'dashed' },
          data: [{ yAxis: warn }],
        },
      },
    ],
    color: [accent],
  }
})
</script>
