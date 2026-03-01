<template>
  <KpiCard
    title="Benefice net"
    subtitle="Ventes - tous les frais"
    :accent="accent"
    :loading="loading"
    :error="error"
    :valueText="valueText"
    :deltaPct="kpi.deltaPct"
    :deltaText="deltaText"
    hint="sur la periode"
    :spark="spark"
  >
    <div class="mt-2 pb-0.5">
      <div class="period-chip">
        <span class="period-label">Periode</span>
        <span class="period-value">{{ fromLabel }} -> {{ toLabel }}</span>
      </div>
    </div>
  </KpiCard>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeKpi, normalizeSeries } from '@/services/statsAdapters'
import { formatDateFR, formatEUR, signFmt } from '@/utils/formatters'
import KpiCard from './_parts/KpiCard.vue'

const props = defineProps({
  from: String,
  to: String,
  bucket: { type: String, default: 'week' },
  categories: { type: Array, default: () => [] },
})
const accent = '#22C55E'

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
      StatsServices.kpi('netProfit', props.from, props.to, props.categories),
      StatsServices.series('netProfit', props.from, props.to, props.bucket, props.categories),
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

const fromLabel = computed(() => formatDateFR(props.from, { day: '2-digit', month: 'short', year: 'numeric' }))
const toLabel = computed(() => formatDateFR(props.to, { day: '2-digit', month: 'short', year: 'numeric' }))
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
