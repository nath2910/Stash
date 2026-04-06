<template>
  <ChartCard
    title="Dépenses opérationnelles"
    subtitle="Hors achat de stock"
    :accent="accent"
    surface="distribution"
    :loading="loading"
    :error="error"
    :widget-width="props.widgetWidth"
    :widget-height="props.widgetHeight"
    :widget-base-width="props.widgetBaseWidth"
    :widget-base-height="props.widgetBaseHeight"
    :option="option"
  />
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeBreakdown } from '@/services/statsAdapters'
import { formatEUR } from '@/utils/formatters'
import ChartCard from './_parts/ChartCard.vue'

const props = defineProps({
  from: String,
  to: String,
  widgetWidth: { type: Number, default: 720 },
  widgetHeight: { type: Number, default: 360 },
  widgetBaseWidth: { type: Number, default: 720 },
  widgetBaseHeight: { type: Number, default: 360 },
})
const accent = '#EF4444'

const loading = ref(false)
const error = ref('')
const items = ref([])
let req = 0

async function load() {
  const id = ++req
  loading.value = true
  error.value = ''
  try {
    const { data } = await StatsServices.breakdown('opex', props.from, props.to)
    if (id !== req) return
    items.value = normalizeBreakdown(data)
  } catch (e) {
    if (id !== req) return
    error.value = e?.response?.data?.message ?? e?.message ?? 'Impossible de charger'
  } finally {
    if (id === req) loading.value = false
  }
}

onMounted(load)
watch(() => [props.from, props.to], load)

const option = computed(() => {
  const data = items.value.map((i) => ({ name: i.label, value: i.value }))
  const total = data.reduce((a, b) => a + Number(b.value || 0), 0)
  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      confine: true,
      transitionDuration: 0,
      formatter: ({ name, value, percent }) => `${name}<br/>${formatEUR(value)} • ${percent}%`,
    },
    series: [
      {
        type: 'pie',
        radius: ['55%', '78%'],
        center: ['50%', '52%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#0B1220', borderWidth: 2 },
        label: { color: '#E5E7EB', fontSize: 11 },
        labelLine: { lineStyle: { color: '#475569' } },
        animationDurationUpdate: 0,
        emphasis: { scale: false },
        data,
      },
    ],
    graphic: [
      {
        type: 'text',
        left: 'center',
        top: '43%',
        style: { text: 'Total', fill: '#9CA3AF', fontSize: 11 },
      },
      {
        type: 'text',
        left: 'center',
        top: '50%',
        style: {
          text: formatEUR(total, { compact: true }),
          fill: '#E5E7EB',
          fontSize: 16,
          fontWeight: 700,
        },
      },
    ],
  }
})
</script>
