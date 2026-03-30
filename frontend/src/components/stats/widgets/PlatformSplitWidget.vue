<template>
  <ChartCard
    title="Répartition par plateforme"
    subtitle="D’où vient l’argent"
    :accent="accent"
    surface="distribution"
    :loading="loading"
    :error="error"
    :option="option"
  />
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeBreakdown } from '@/services/statsAdapters'
import { formatEUR } from '@/utils/formatters'
import ChartCard from './_parts/ChartCard.vue'

const props = defineProps({ from: String, to: String })
const accent = '#22C55E'

const loading = ref(false)
const error = ref('')
const items = ref([])
let req = 0

async function load() {
  const id = ++req
  loading.value = true
  error.value = ''
  try {
    const { data } = await StatsServices.breakdown('platformSplit', props.from, props.to)
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
  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      confine: true,
      transitionDuration: 0,
      formatter: ({ name, value, percent }) => `${name}<br/>${formatEUR(value)} • ${percent}%`,
    },
    legend: { bottom: 0, textStyle: { color: '#9CA3AF', fontSize: 11 } },
    series: [
      {
        type: 'pie',
        radius: ['40%', '72%'],
        center: ['50%', '48%'],
        itemStyle: { borderColor: '#0B1220', borderWidth: 2, borderRadius: 8 },
        label: { show: false },
        animationDurationUpdate: 0,
        emphasis: { scale: false },
        data,
      },
    ],
  }
})
</script>
