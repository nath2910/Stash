<template>
  <KpiCard
    title="Marge moyenne"
    subtitle="Gain moyen par vente"
    :accent="accent"
    surface="kpi"
    :loading="loading"
    :error="error"
    :widget-width="props.widgetWidth"
    :widget-height="props.widgetHeight"
    :widget-base-width="props.widgetBaseWidth"
    :widget-base-height="props.widgetBaseHeight"
    compact
    :valueText="valueText"
    :deltaPct="kpi.deltaPct"
    :deltaText="deltaText"
    hint="par article"
    :spark="spark"
  >
    <div v-if="showPeriodChip" class="mt-0.5 pb-0.5">
      <div class="period-chip">
        <span class="period-label">Periode</span>
        <span class="period-value">{{ periodText }}</span>
      </div>
    </div>

    <div v-if="visibleTopItems.length" class="margin-list" :class="{ 'has-period': showPeriodChip }">
      <div
        v-for="(x, idx) in visibleTopItems"
        :key="`${x.nomItem}-${idx}`"
        class="margin-row"
      >
        <div class="margin-row__copy">
          <p>{{ x.nomItem }}</p>
          <span>#{{ idx + 1 }}</span>
        </div>
        <div class="margin-row__value">
          {{ formatEUR(x.benefice, { compact: true }) }}
        </div>
      </div>
    </div>
    <div v-else-if="!loading && !error && !topItems.length" class="margin-empty">
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
  widgetWidth: { type: Number, default: 520 },
  widgetHeight: { type: Number, default: 200 },
  widgetBaseWidth: { type: Number, default: 520 },
  widgetBaseHeight: { type: Number, default: 200 },
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
      StatsServices.series(
        'avgMargin',
        props.from,
        props.to,
        props.bucket,
        props.categories,
        props.types,
      ),
      StatsServices.topSales(props.from, props.to, topLimit.value, props.categories, props.types),
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

const valueText = computed(() => formatEUR(kpi.value.value, { compact: true }))
const deltaText = computed(() => (kpi.value.deltaPct == null ? '' : signFmt(kpi.value.deltaPct)))
const spark = computed(() => series.value.slice(-18).map((p) => p.value))

const topLimit = computed(() => {
  const h = Number(props.widgetHeight ?? 0)
  if (h < 320) return 3
  if (h < 430) return 5
  if (h < 560) return 7
  return 10
})

onMounted(load)
watch(() => [props.from, props.to, props.bucket, props.categories, props.types, topLimit.value], load)

const showPeriodChip = computed(() => Number(props.widgetHeight ?? 0) >= 205)
const visibleRowCount = computed(() => {
  const h = Number(props.widgetHeight ?? 0)
  if (h < 220) return 0
  if (h < 280) return 1
  if (h < 340) return 3
  if (h < 430) return 5
  return topItems.value.length
})
const visibleTopItems = computed(() => topItems.value.slice(0, visibleRowCount.value))

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
  color: #64748b;
  white-space: nowrap;
  padding: 5px 9px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--widget-panel-border, rgba(100, 116, 139, 0.16));
}
.period-label {
  opacity: 0.7;
}
.period-value {
  font-size: 11px;
  letter-spacing: 0.02em;
  text-transform: none;
  color: #334155;
}

.margin-list {
  margin-top: 4px;
  display: grid;
  gap: 7px;
}

.margin-list.has-period {
  margin-top: 10px;
}

.margin-row {
  min-width: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  border-radius: 8px;
  border: 1px solid var(--widget-row-border, rgba(100, 116, 139, 0.16));
  background: var(--widget-row-bg, rgba(248, 250, 252, 0.86));
  padding: 7px 9px;
}

.margin-row__copy {
  min-width: 0;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 8px;
}

.margin-row__copy p {
  margin: 0;
  color: #111827;
  font-size: 12px;
  font-weight: 720;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  grid-column: 2;
}

.margin-row__copy span {
  grid-column: 1;
  grid-row: 1;
  width: 28px;
  height: 22px;
  border-radius: 6px;
  border: 1px solid rgba(100, 116, 139, 0.16);
  background: rgba(255, 255, 255, 0.74);
  color: #64748b;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 780;
}

.margin-row__value {
  flex: 0 0 auto;
  color: #047857;
  font-size: 12px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.margin-empty {
  margin-top: 10px;
  color: #64748b;
  font-size: 11px;
}
</style>
