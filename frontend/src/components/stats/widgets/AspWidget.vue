<template>
  <KpiCard
    title="Prix de vente moyen"
    subtitle="ASP"
    :accent="accent"
    :loading="loading"
    :error="error"
    :valueText="valueText"
    :deltaPct="kpi.deltaPct"
    :deltaText="deltaText"
    hint="montée en gamme ?"
    :spark="spark"
  />
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeKpi, normalizeSeries } from '@/services/statsAdapters'
import { formatEUR, signFmt } from '@/utils/formatters'
import KpiCard from './_parts/KpiCard.vue'

const props = defineProps({
  from: String,
  to: String,
  bucket: { type: String, default: 'week' },
  categories: { type: Array, default: () => [] },
})
const accent = '#3B82F6'

const loading = ref(false)
const error = ref('')
const kpi = ref({ value: 0, deltaPct: null })
const series = ref([])
let req = 0

async function load() {
  const id = ++req
  loading.value = true
  error.value = ''
  try {
    const [k, s] = await Promise.all([
      StatsServices.kpi('asp', props.from, props.to, props.categories),
      StatsServices.series('asp', props.from, props.to, props.bucket, props.categories),
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
watch(() => [props.from, props.to, props.bucket, props.categories], load)

const valueText = computed(() => formatEUR(kpi.value.value, { compact: true }))
const deltaText = computed(() => (kpi.value.deltaPct == null ? '' : signFmt(kpi.value.deltaPct)))
const spark = computed(() => series.value.slice(-18).map((p) => p.value))
</script>
