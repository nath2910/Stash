<template>
  <KpiCard
    title="Marge moyenne"
    subtitle="Gain moyen par vente"
    :accent="accent"
    :loading="loading"
    :error="error"
    compact
    :valueText="valueText"
    :deltaPct="kpi.deltaPct"
    :deltaText="deltaText"
    hint="par article"
    :spark="spark"
  >
    <div class="mt-0.5 pb-0.5">
      <div class="period-chip">
        <span class="period-label">Periode</span>
        <span class="period-value">{{ periodText }}</span>
      </div>
    </div>

    <div v-if="topItems.length" class="mt-0.5 space-y-1">
      <div
        v-for="(x, idx) in topItems"
        :key="`${x.nomItem}-${idx}`"
        class="flex items-center justify-between gap-3 rounded-xl border border-slate-800 bg-slate-950/30 px-3 py-1"
      >
        <div class="min-w-0">
          <p class="text-xs text-white/90 truncate">{{ x.nomItem }}</p>
          <p class="text-[11px] text-white/45">#{{ idx + 1 }}</p>
        </div>
        <div class="text-sm font-semibold text-emerald-300">
          {{ formatEUR(x.benefice, { compact: true }) }}
        </div>
      </div>
    </div>
    <div v-else-if="!loading && !error" class="mt-3 text-[11px] text-white/40">
      Aucun article sur la periode.
    </div>
  </KpiCard>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeKpi, normalizeSeries, normalizeTopSales } from '@/services/statsAdapters'
import { formatDateFR, formatEUR, signFmt } from '@/utils/formatters'
import KpiCard from './_parts/KpiCard.vue'

const props = defineProps({
  from: String,
  to: String,
  bucket: { type: String, default: 'week' },
  categories: { type: Array, default: () => [] },
  types: { type: Array, default: () => [] },
})
const accent = '#F59E0B'

const loading = ref(false)
const error = ref('')
const kpi = ref({ value: 0, deltaPct: null })
const series = ref([])
const topItems = ref([])
let req = 0

async function load() {
  const id = ++req
  if (!props.from || !props.to) {
    kpi.value = { value: 0, deltaPct: null }
    series.value = []
    topItems.value = []
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    const [k, s, t] = await Promise.all([
      StatsServices.kpi('avgMargin', props.from, props.to, props.categories, props.types),
      StatsServices.series('avgMargin', props.from, props.to, props.bucket, props.categories, props.types),
      StatsServices.topSales(props.from, props.to, 3, props.categories, props.types),
    ])
    if (id !== req) return
    kpi.value = normalizeKpi(k.data)
    series.value = normalizeSeries(s.data)
    topItems.value = normalizeTopSales(t.data)
  } catch (e) {
    if (id !== req) return
    error.value = e?.response?.data?.message ?? e?.message ?? 'Impossible de charger'
  } finally {
    if (id === req) loading.value = false
  }
}

onMounted(load)
watch(() => [props.from, props.to, props.bucket, props.categories, props.types], load)

const valueText = computed(() => formatEUR(kpi.value.value, { compact: true }))
const deltaText = computed(() => (kpi.value.deltaPct == null ? '' : signFmt(kpi.value.deltaPct)))
const spark = computed(() => series.value.slice(-18).map((p) => p.value))

const periodText = computed(() => {
  if (!props.from || !props.to) return '--'
  const fromText = formatDateFR(props.from, { day: '2-digit', month: 'short', year: 'numeric' })
  const toText = formatDateFR(props.to, { day: '2-digit', month: 'short', year: 'numeric' })
  return `${fromText} - ${toText}`
})
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
