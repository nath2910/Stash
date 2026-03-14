<template>
  <WidgetCard
    title="ROI moyen"
    subtitle="Rentabilite globale sur la periode"
    :accent="accent"
    :loading="loading"
    :error="error"
  >
    <div class="roi-layout">
      <div class="roi-top">
        <div class="roi-kpi min-w-0">
          <div class="text-3xl font-bold text-white leading-none tracking-tight">{{ roiText }}</div>
          <div class="mt-2 flex flex-wrap items-center gap-2">
            <span
              v-if="deltaPts != null"
              class="text-[11px] px-2 py-0.5 rounded-full border"
              :class="deltaBadgeClass"
            >
              {{ deltaPtsText }}
            </span>
            <span v-if="deltaPts != null" class="text-[11px] text-white/40">
              vs periode precedente
            </span>
            <div class="objective-chip">
              <span class="objective-label">Objectif</span>
              <span class="objective-value"><span class="ge">&ge;</span> 40%</span>
            </div>
          </div>
        </div>

        <div class="period-chip roi-period">
          <span class="period-label">Periode</span>
          <span class="period-value">{{ fromLabel }} -> {{ toLabel }}</span>
        </div>
      </div>

      <div class="roi-categories" :class="{ 'is-empty': !topCategories.length }">
        <div class="text-[10px] uppercase tracking-[0.2em] text-white/40 mb-2 shrink-0">
          Meilleures categories (benefice)
        </div>
        <ul class="roi-list space-y-1">
          <li
            v-for="item in topCategories"
            :key="item.label"
            class="flex items-center justify-between gap-3 text-[12px]"
          >
            <span class="truncate text-white/80">{{ item.label }}</span>
            <span class="text-white/70 tabular-nums">{{ formatEUR(item.value, { compact: true }) }}</span>
          </li>
          <li v-if="!topCategories.length" class="text-[11px] text-white/40">
            Pas assez de data sur la periode.
          </li>
        </ul>
      </div>
    </div>
  </WidgetCard>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeKpi, normalizeRank, prevPeriod } from '@/services/statsAdapters'
import { formatDateFR, formatEUR, formatPct, signFmt } from '@/utils/formatters'
import WidgetCard from './_parts/WidgetCard.vue'

const props = defineProps({
  from: String,
  to: String,
  categories: { type: Array, default: () => [] },
  types: { type: Array, default: () => [] },
})
const accent = '#8B5CF6'
const loading = ref(false)
const error = ref('')
const kpi = ref({ value: 0, deltaPct: null })
const prevKpi = ref({ value: null })
const topCategories = ref([])
let req = 0

async function load() {
  const id = ++req
  loading.value = true
  error.value = ''
  try {
    const { from: prevFrom, to: prevTo } = prevPeriod(props.from, props.to)
    const [k, kPrev, c] = await Promise.all([
      StatsServices.kpi('roi', props.from, props.to, props.categories, props.types),
      StatsServices.kpi('roi', prevFrom, prevTo, props.categories, props.types),
      StatsServices.rank('topCategoriesProfit', props.from, props.to, 4, props.categories, props.types),
    ])
    if (id !== req) return
    kpi.value = normalizeKpi(k.data)
    prevKpi.value = normalizeKpi(kPrev.data)
    topCategories.value = normalizeRank(c.data)
  } catch (e) {
    if (id !== req) return
    error.value = e?.response?.data?.message ?? e?.message ?? 'Impossible de charger'
  } finally {
    if (id === req) loading.value = false
  }
}

onMounted(load)
watch(() => [props.from, props.to, props.categories, props.types], load)

const roiText = computed(() => formatPct(kpi.value.value, { digits: 1 }))
const deltaPts = computed(() => {
  const curr = Number(kpi.value.value ?? 0)
  const prev = Number(prevKpi.value.value ?? 0)
  if (!Number.isFinite(curr) || !Number.isFinite(prev)) return null
  return curr - prev
})
const deltaPtsText = computed(() => {
  if (deltaPts.value == null) return ''
  const sign = deltaPts.value >= 0 ? '+' : ''
  return `${sign}${deltaPts.value.toFixed(1)} pts`
})
const roiTarget = 40
const deltaBadgeClass = computed(() => {
  if (kpi.value.value == null) return ''
  const ok = Number(kpi.value.value) >= roiTarget
  return ok
    ? 'border-emerald-400/30 bg-emerald-500/10 text-emerald-300'
    : 'border-red-400/30 bg-red-500/10 text-red-300'
})
const fromLabel = computed(() =>
  formatDateFR(props.from, { day: '2-digit', month: 'short', year: 'numeric' }),
)
const toLabel = computed(() =>
  formatDateFR(props.to, { day: '2-digit', month: 'short', year: 'numeric' }),
)
</script>

<style scoped>
.roi-layout {
  display: grid;
  grid-template-rows: auto auto 1fr;
  gap: 14px;
  min-height: 100%;
}
.roi-top {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}
.roi-kpi {
  flex: 1 1 320px;
}
.roi-categories {
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  padding-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1 1 auto;
  min-height: 0;
}
.roi-categories.is-empty {
  flex: 0 0 auto;
}
.roi-list {
  flex: 1 1 auto;
  min-height: 0;
  max-height: 100%;
  overflow: auto;
  padding-right: 4px;
}
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
.objective-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
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
.objective-value .ge {
  display: inline-block;
  padding: 0 6px;
  margin-right: 6px;
  font-size: 10px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  border-radius: 999px;
  background: rgba(34, 197, 94, 0.16);
  color: rgba(134, 239, 172, 0.95);
  border: 1px solid rgba(34, 197, 94, 0.35);
  box-shadow: 0 0 10px rgba(34, 197, 94, 0.18);
}
</style>
