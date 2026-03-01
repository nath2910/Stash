<template>
  <WidgetCard
    title="Top marques"
    subtitle="Volume des ventes"
    :accent="accent"
    :loading="loading"
    :error="error"
  >
    <VChart class="chart" :option="option" autoresize />
  </WidgetCard>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeBrands } from '@/services/statsAdapters'
import { formatNumber } from '@/utils/formatters'
import WidgetCard from './_parts/WidgetCard.vue'

const props = defineProps({
  from: String,
  to: String,
  top: { type: Number, default: 8 },
  view: { type: String, default: 'bars' },
  categories: { type: Array, default: () => [] },
})
const accent = '#8B5CF6'

const loading = ref(false)
const error = ref('')
const brands = ref([])
let req = 0

async function load() {
  const id = ++req
  loading.value = true
  error.value = ''
  try {
    const { data } = await StatsServices.brands(props.from, props.to, props.categories)
    if (id !== req) return
    brands.value = normalizeBrands(data)
  } catch (e) {
    if (id !== req) return
    error.value = e?.response?.data?.message ?? e?.message ?? 'Impossible de charger'
  } finally {
    if (id === req) loading.value = false
  }
}

onMounted(load)
watch(() => [props.from, props.to, props.top, props.categories], load)

const PALETTE = [
  '#22C55E',
  '#3B82F6',
  '#F59E0B',
  '#A855F7',
  '#EF4444',
  '#14B8A6',
  '#F97316',
  '#84CC16',
  '#06B6D4',
  '#E11D48',
  '#8B5CF6',
  '#10B981',
]
function pickColorByIndex(index) {
  return PALETTE[index % PALETTE.length]
}

const option = computed(() => {
  const items = brands.value.slice(0, props.top)
  const labels = items.map((i) => i.label)
  const values = items.map((i) => i.nb)

  if (props.view === 'treemap') {
    return {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item',
        confine: true,
        transitionDuration: 0,
        formatter: ({ name, value }) => `${name}<br/>${formatNumber(value)}`,
      },
      series: [
        {
          type: 'treemap',
          roam: false,
          nodeClick: false,
          breadcrumb: { show: false },
          label: { show: true, formatter: '{b}\n{c}' },
          upperLabel: { show: false },
          itemStyle: { borderColor: 'rgba(2,6,23,0.6)', borderWidth: 2, gapWidth: 2 },
          data: items.map((r, i) => ({
            name: r.label,
            value: r.nb,
            itemStyle: { color: pickColorByIndex(i) },
          })),
        },
      ],
    }
  }

  if (props.view === 'heatmap') {
    const maxVal = values.reduce((m, v) => Math.max(m, Number(v || 0)), 0)
    return {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item',
        confine: true,
        transitionDuration: 0,
        formatter: (p) => {
          const v = Array.isArray(p?.value) ? p.value[2] : p?.value
          return `${p?.name ?? ''}<br/>${formatNumber(v)}`
        },
      },
      grid: { left: 60, right: 16, top: 16, bottom: 30, containLabel: true },
      xAxis: {
        type: 'category',
        data: labels,
        axisLabel: { color: '#E5E7EB', fontSize: 10, interval: 0 },
        axisLine: { lineStyle: { color: '#334155' } },
        axisTick: { show: false },
      },
      yAxis: {
        type: 'category',
        data: ['Ventes'],
        axisLabel: { color: '#9CA3AF', fontSize: 10 },
        axisLine: { lineStyle: { color: '#334155' } },
        axisTick: { show: false },
      },
      visualMap: {
        min: 0,
        max: maxVal || 1,
        show: false,
        inRange: { color: ['#1E293B', '#8B5CF6', '#EC4899'] },
      },
      series: [
        {
          type: 'heatmap',
          data: labels.map((_, i) => [i, 0, values[i] ?? 0]),
          itemStyle: { borderColor: 'rgba(255,255,255,0.08)', borderWidth: 1 },
          label: {
            show: true,
            color: '#E2E8F0',
            fontSize: 10,
            formatter: (p) => formatNumber(p.value?.[2] ?? 0),
          },
          emphasis: { disabled: true },
        },
      ],
    }
  }

  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis' },
    grid: { left: 110, right: 14, top: 10, bottom: 24 },
    xAxis: {
      type: 'value',
      axisLabel: { color: '#9CA3AF' },
      splitLine: { lineStyle: { color: '#1F2937' } },
    },
    yAxis: {
      type: 'category',
      data: labels,
      axisLabel: { color: '#E5E7EB' },
      axisLine: { lineStyle: { color: '#374151' } },
    },
    series: [
      { type: 'bar', data: values, barWidth: 18, itemStyle: { borderRadius: [10, 10, 10, 10] } },
    ],
    color: ['#8B5CF6'],
  }
})
</script>

<style scoped>
.chart {
  width: 100%;
  height: 100%;
}
</style>
