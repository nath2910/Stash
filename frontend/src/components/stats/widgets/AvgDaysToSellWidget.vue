<template>
  <KpiCard
    title="Delai moyen de vente"
    subtitle="Temps pour vendre"
    :accent="accent"
    surface="kpi"
    :loading="loading"
    :error="error"
    :valueText="valueText"
    :deltaPct="deltaDays"
    :deltaText="deltaLabel"
    
    :spark="spark"
  >
    <div class="aux-row">
      <div class="objective-chip">
        <span class="objective-label">Objectif</span>
        <span class="objective-value">
          <span class="le">&le;</span>
          30 j
        </span>
        <span class="objective-note">delai cible</span>
      </div>
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
import { normalizeKpi, normalizeSeries, prevPeriod } from '@/services/statsAdapters'
import { formatDateFR } from '@/utils/formatters'
import KpiCard from './_parts/KpiCard.vue'

const props = defineProps({
  from: String,
  to: String,
  bucket: { type: String, default: 'week' },
  categories: { type: Array, default: () => [] },
  types: { type: Array, default: () => [] },
})
const accent = '#60A5FA'

const loading = ref(false)
const error = ref('')
const kpi = ref({ value: 0, deltaPct: null }) // value = jours
const prevValue = ref(null)
const series = ref([])
let req = 0

async function load() {
  const id = ++req
  loading.value = true
  error.value = ''
  try {
    const { from: prevFrom, to: prevTo } = prevPeriod(props.from, props.to)
    const [k, s, kPrev] = await Promise.all([
      StatsServices.kpi('avgDaysToSell', props.from, props.to, props.categories, props.types),
      StatsServices.series('avgDaysToSell', props.from, props.to, props.bucket, props.categories, props.types),
      StatsServices.kpi('avgDaysToSell', prevFrom, prevTo, props.categories, props.types),
    ])
    if (id !== req) return
    kpi.value = normalizeKpi(k.data)
    series.value = normalizeSeries(s.data)
    prevValue.value = normalizeKpi(kPrev.data).value
  } catch (e) {
    if (id !== req) return
    error.value = e?.response?.data?.message ?? e?.message ?? 'Impossible de charger'
  } finally {
    if (id === req) loading.value = false
  }
}

onMounted(load)
watch(() => [props.from, props.to, props.bucket, props.categories, props.types], load)

const valueText = computed(() => `${Number(kpi.value.value ?? 0).toFixed(0)} j`)
const deltaDays = computed(() => {
  const curr = Number(kpi.value.value ?? 0)
  const prev = Number(prevValue.value ?? 0)
  if (!Number.isFinite(prev) || prev === 0) return null
  return curr - prev
})
const deltaLabel = computed(() => {
  if (deltaDays.value == null) return ''
  const sign = deltaDays.value >= 0 ? '+' : ''
  return `${sign}${deltaDays.value.toFixed(0)} j vs periode precedente`
})
const spark = computed(() => series.value.slice(-18).map((p) => p.value))
const fromLabel = computed(() =>
  formatDateFR(props.from, { day: '2-digit', month: 'short', year: 'numeric' }),
)
const toLabel = computed(() =>
  formatDateFR(props.to, { day: '2-digit', month: 'short', year: 'numeric' }),
)
</script>

<style scoped>
.aux-row {
  margin-top: 6px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}
.objective-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0;
  border-radius: 0;
  font-size: 11px;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  color: rgba(203, 213, 225, 0.7);
  border: none;
  background: transparent;
  box-shadow: none;
}
.objective-label {
  opacity: 0.72;
}
.objective-value {
  font-size: 12px;
  text-transform: none;
  letter-spacing: 0.02em;
  color: rgba(241, 245, 249, 0.9);
}
.objective-value .le {
  display: inline-block;
  margin-right: 4px;
  font-size: 12px;
  letter-spacing: 0;
  text-transform: none;
  color: rgba(191, 219, 254, 0.9);
}
.objective-note {
  font-size: 11px;
  letter-spacing: 0.01em;
  text-transform: none;
  color: rgba(148, 163, 184, 0.82);
}
.period-chip {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  color: rgba(203, 213, 225, 0.66);
  white-space: nowrap;
  padding: 0;
  border-radius: 0;
  background: transparent;
  border: none;
  box-shadow: none;
}
.period-label {
  opacity: 0.72;
}
.period-value {
  font-size: 12px;
  letter-spacing: 0.02em;
  text-transform: none;
  color: rgba(241, 245, 249, 0.9);
}

@media (max-width: 620px) {
  .period-chip {
    margin-left: 0;
  }
}
</style>
