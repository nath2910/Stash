<template>
  <KpiCard
    title="Delai moyen de vente"
    subtitle="Temps pour vendre"
    :accent="accent"
    surface="kpi"
    :loading="loading"
    :error="error"
    :widget-width="props.widgetWidth"
    :widget-height="props.widgetHeight"
    :widget-base-width="props.widgetBaseWidth"
    :widget-base-height="props.widgetBaseHeight"
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
  widgetWidth: { type: Number, default: 520 },
  widgetHeight: { type: Number, default: 240 },
  widgetBaseWidth: { type: Number, default: 520 },
  widgetBaseHeight: { type: Number, default: 240 },
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
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.objective-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 10px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(226, 232, 240, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(15, 23, 42, 0.6);
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.02),
    0 6px 14px rgba(0, 0, 0, 0.25);
}
.objective-label {
  opacity: 0.7;
}
.objective-value {
  font-size: 11px;
  text-transform: none;
  letter-spacing: 0.02em;
  color: rgba(226, 232, 240, 0.92);
}
.objective-value .le {
  display: inline-block;
  padding: 0 6px;
  margin-right: 6px;
  font-size: 10px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.16);
  color: rgba(191, 219, 254, 0.95);
  border: 1px solid rgba(59, 130, 246, 0.35);
  box-shadow: 0 0 10px rgba(59, 130, 246, 0.18);
}
.objective-note {
  font-size: 10px;
  letter-spacing: 0.04em;
  text-transform: none;
  color: rgba(226, 232, 240, 0.55);
}
.period-chip {
  margin-left: auto;
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
