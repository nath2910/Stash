<template>
  <WidgetCard
    title="ROI moyen"
    subtitle="Performance ROI et contribution profit"
    :accent="accent"
    surface="kpi"
    :loading="loading"
    :error="error"
    :widget-width="props.widgetWidth"
    :widget-height="props.widgetHeight"
    :widget-base-width="props.widgetBaseWidth"
    :widget-base-height="props.widgetBaseHeight"
  >
    <template #actions>
      <div class="roi-header-period" :title="periodText" aria-label="Periode active">
        <CalendarDays class="roi-header-period__icon" aria-hidden="true" />
        <span class="roi-header-period__text">{{ periodText }}</span>
      </div>
    </template>

    <div class="roi-root" :class="layoutClass" :style="roiVars">
      <section class="roi-summary-grid" aria-label="Synthese ROI">
        <article class="roi-tile roi-tile--hero">
          <div class="roi-tile-head">
            <p class="roi-overline">ROI moyen</p>
            <span class="roi-state-chip" :class="`is-${trendState}`">
              <component :is="trendIcon" class="roi-state-icon" aria-hidden="true" />
              <span>{{ trendLabel }}</span>
            </span>
          </div>
          <p class="roi-main-value" aria-live="polite">{{ roiText }}</p>
          <div class="roi-delta-row" :class="`is-${trendState}`">
            <span class="roi-delta-value">{{ deltaPtsText }}</span>
            <span class="roi-delta-note">{{ deltaDescription }}</span>
          </div>
        </article>

        <article class="roi-tile roi-tile--goal" :class="`is-${targetState}`">
          <div class="roi-goal-head">
            <div class="roi-goal-meta">
              <p class="roi-overline">Objectif ROI</p>
              <p class="roi-goal-target">{{ goalTargetText }}</p>
            </div>
            <p class="roi-goal-delta">{{ targetDeltaText }}</p>
          </div>
          <div
            class="roi-progress"
            role="progressbar"
            :aria-valuemin="0"
            :aria-valuemax="100"
            :aria-valuenow="goalProgressAria"
            :aria-valuetext="goalProgressLabel"
          >
            <span class="roi-progress-fill" :style="goalFillStyle"></span>
          </div>
          <p class="roi-goal-hint">{{ targetHint }}</p>
        </article>
      </section>

      <section class="roi-breakdown-panel" aria-label="Top categories profit">
        <header class="roi-breakdown-head">
          <div>
            <p class="roi-overline">Top categories profit</p>
            <p class="roi-breakdown-sub">Impact sur le profit total</p>
          </div>
        </header>

        <ul
          v-if="hasTopCategories"
          class="roi-list"
          aria-label="Classement des categories les plus rentables"
        >
          <li
            v-for="(item, index) in topCategoriesSafe"
            :key="`${item.label}-${index}`"
            class="roi-list-item"
            :class="`is-${categoryState(item)}`"
          >
            <div class="roi-item-head">
              <span class="roi-item-rank" aria-hidden="true">{{ String(index + 1).padStart(2, '0') }}</span>
              <span class="roi-item-label" :title="item.label">{{ item.label }}</span>
              <span class="roi-item-value">{{ formatEUR(item.value, { compact: true }) }}</span>
            </div>

            <div class="roi-item-bar" aria-hidden="true">
              <span class="roi-item-fill" :style="{ width: categoryWidth(item) }"></span>
            </div>

            <p class="roi-item-share">{{ categoryShare(item) }} du top categories</p>
          </li>
        </ul>

        <p v-else class="roi-empty">Pas assez de data sur la periode.</p>
      </section>
    </div>
  </WidgetCard>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ArrowDownRight, ArrowUpRight, CalendarDays, Minus } from 'lucide-vue-next'
import StatsServices from '@/services/StatsServices'
import { normalizeKpi, normalizeRank, prevPeriod } from '@/services/statsAdapters'
import { formatDateFR, formatEUR, formatPct } from '@/utils/formatters'
import WidgetCard from './_parts/WidgetCard.vue'

const props = defineProps({
  from: String,
  to: String,
  categories: { type: Array, default: () => [] },
  types: { type: Array, default: () => [] },
  widgetWidth: { type: Number, default: 520 },
  widgetHeight: { type: Number, default: 240 },
  widgetBaseWidth: { type: Number, default: 0 },
  widgetBaseHeight: { type: Number, default: 0 },
})

const accent = '#22C55E'
const loading = ref(false)
const error = ref('')
const kpi = ref({ value: 0, deltaPct: null })
const prevKpi = ref({ value: null })
const topCategories = ref([])
const roiTarget = 40
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
      StatsServices.rank('topCategoriesProfit', props.from, props.to, 2, props.categories, props.types),
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

const clamp = (value, min, max) => Math.max(min, Math.min(max, value))
const asFinite = (value, fallback = 0) => {
  const number = Number(value)
  return Number.isFinite(number) ? number : fallback
}

const roiNumeric = computed(() => asFinite(kpi.value?.value, 0))
const prevRoiNumeric = computed(() => {
  const number = Number(prevKpi.value?.value)
  return Number.isFinite(number) ? number : null
})

const roiText = computed(() => formatPct(roiNumeric.value, { digits: 1 }))

const deltaPts = computed(() => {
  if (prevRoiNumeric.value == null) return null
  return roiNumeric.value - prevRoiNumeric.value
})

const trendState = computed(() => {
  if (deltaPts.value == null) return 'neutral'
  if (deltaPts.value > 0.05) return 'up'
  if (deltaPts.value < -0.05) return 'down'
  return 'neutral'
})

const trendLabel = computed(() => {
  if (trendState.value === 'up') return 'En hausse'
  if (trendState.value === 'down') return 'En baisse'
  return 'Stable'
})

const trendIcon = computed(() => {
  if (trendState.value === 'up') return ArrowUpRight
  if (trendState.value === 'down') return ArrowDownRight
  return Minus
})

const deltaPtsText = computed(() => {
  if (deltaPts.value == null) return '--'
  const sign = deltaPts.value >= 0 ? '+' : ''
  return `${sign}${deltaPts.value.toFixed(1)} pts`
})

const deltaDescription = computed(() => {
  if (deltaPts.value == null) return 'Comparaison indisponible'
  return 'vs periode precedente'
})

const fromLabel = computed(() =>
  formatDateFR(props.from, { day: '2-digit', month: 'short', year: 'numeric' }),
)
const toLabel = computed(() =>
  formatDateFR(props.to, { day: '2-digit', month: 'short', year: 'numeric' }),
)

const periodText = computed(() => `${fromLabel.value} - ${toLabel.value}`)

const targetDelta = computed(() => roiNumeric.value - roiTarget)
const targetState = computed(() => (targetDelta.value >= 0 ? 'ok' : 'risk'))

const goalTargetText = computed(() => `Cible: ${formatPct(roiTarget, { digits: 0 })}`)
const targetDeltaText = computed(() => {
  const sign = targetDelta.value >= 0 ? '+' : ''
  return `${sign}${targetDelta.value.toFixed(1)} pts`
})

const goalProgressRaw = computed(() => {
  if (roiTarget <= 0) return 0
  return (roiNumeric.value / roiTarget) * 100
})

const goalProgressAria = computed(() => clamp(Math.round(goalProgressRaw.value), 0, 100))
const goalProgressLabel = computed(() => `${Math.round(goalProgressRaw.value)}% de l'objectif`)
const goalFillStyle = computed(() => ({ width: `${clamp(goalProgressRaw.value, 0, 100)}%` }))

const targetHint = computed(() => {
  if (targetDelta.value >= 0) return 'Objectif atteint et depasse'
  return `${Math.abs(targetDelta.value).toFixed(1)} pts restants pour atteindre la cible`
})

const topCategoriesSafe = computed(() =>
  topCategories.value
    .slice(0, 2)
    .map((item) => ({
      label: String(item?.label ?? '-'),
      value: asFinite(item?.value, 0),
    })),
)

const hasTopCategories = computed(() => topCategoriesSafe.value.length > 0)

const topCategoriesTotalAbs = computed(() =>
  topCategoriesSafe.value.reduce((sum, item) => sum + Math.abs(item.value), 0),
)

const topCategoryMaxAbs = computed(() => {
  const max = topCategoriesSafe.value.reduce((acc, item) => Math.max(acc, Math.abs(item.value)), 0)
  return max > 0 ? max : 1
})

function categoryWidth(item) {
  const width = (Math.abs(item.value) / topCategoryMaxAbs.value) * 100
  return `${clamp(width, 8, 100)}%`
}

function categoryShare(item) {
  if (topCategoriesTotalAbs.value <= 0) return '--'
  const pct = (Math.abs(item.value) / topCategoriesTotalAbs.value) * 100
  return formatPct(pct, { digits: 0 })
}

function categoryState(item) {
  if (item.value > 0) return 'up'
  if (item.value < 0) return 'down'
  return 'neutral'
}

const safeWidth = computed(() => Math.max(Number(props.widgetWidth ?? 0), 1))
const safeHeight = computed(() => Math.max(Number(props.widgetHeight ?? 0), 1))
const baseWidth = computed(() => Math.max(Number(props.widgetBaseWidth ?? 0) || 520, 1))
const baseHeight = computed(() => Math.max(Number(props.widgetBaseHeight ?? 0) || 260, 1))
const widthScale = computed(() => safeWidth.value / baseWidth.value)
const heightScale = computed(() => safeHeight.value / baseHeight.value)

const visualScale = computed(() => {
  const fitScale = Math.min(widthScale.value, heightScale.value)
  const areaScale = Math.sqrt(widthScale.value * heightScale.value)
  return clamp(fitScale * 0.88 + areaScale * 0.12, 0.82, 1.38)
})

const isWide = computed(() => safeWidth.value >= 760 && safeHeight.value >= 230)
const isCompact = computed(() => safeWidth.value < 520 || safeHeight.value < 240)
const isDense = computed(() => safeWidth.value < 450 || safeHeight.value < 220)

const layoutClass = computed(() => ({
  'is-wide': isWide.value,
  'is-compact': isCompact.value,
  'is-dense': isDense.value,
}))

const roiVars = computed(() => {
  const scale = visualScale.value

  return {
    '--roi-gap': `${clamp(Math.round(12 * scale), 8, 18)}px`,
    '--roi-panel-pad': `${clamp(Math.round(11 * scale), 9, 18)}px`,
    '--roi-panel-radius': `${clamp(Math.round(12 * scale), 10, 18)}px`,
    '--roi-kpi-size': `${clamp(Math.round(36 * scale), 24, 62)}px`,
    '--roi-title-size': `${clamp(Math.round(11 * scale), 10, 14)}px`,
    '--roi-body-size': `${clamp(Math.round(13 * scale), 11, 16)}px`,
    '--roi-meta-size': `${clamp(Math.round(12 * scale), 10, 14)}px`,
    '--roi-small-size': `${clamp(Math.round(10 * scale), 9, 12)}px`,
    '--roi-bar-height': `${clamp(Math.round(8 * scale), 6, 14)}px`,
  }
})
</script>

<style scoped>
.roi-root {
  --roi-bg: rgba(15, 23, 42, 0.54);
  --roi-border: rgba(148, 163, 184, 0.24);
  --roi-text-main: rgba(248, 250, 252, 0.98);
  --roi-text-soft: rgba(203, 213, 225, 0.88);
  --roi-text-muted: rgba(148, 163, 184, 0.9);
  --roi-positive: rgba(74, 222, 128, 1);
  --roi-positive-soft: rgba(22, 163, 74, 0.22);
  --roi-negative: rgba(251, 113, 133, 0.98);
  --roi-negative-soft: rgba(190, 24, 93, 0.2);
  --roi-neutral: rgba(148, 163, 184, 0.98);
  --roi-neutral-soft: rgba(100, 116, 139, 0.24);

  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: var(--roi-gap);
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.roi-header-period {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: min(52vw, 420px);
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.26);
  background: rgba(15, 23, 42, 0.56);
  color: rgba(226, 232, 240, 0.9);
}

.roi-header-period__icon {
  width: 12px;
  height: 12px;
  color: rgba(148, 163, 184, 0.9);
  flex: 0 0 auto;
}

.roi-header-period__text {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.01em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.roi-summary-grid {
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--roi-gap);
}

.roi-tile,
.roi-breakdown-panel {
  min-width: 0;
  min-height: 0;
  border-radius: var(--roi-panel-radius);
  border: 1px solid var(--roi-border);
  background: linear-gradient(160deg, rgba(15, 23, 42, 0.68), var(--roi-bg));
  padding: var(--roi-panel-pad);
  animation: roi-fade 180ms ease-out both;
}

.roi-tile {
  display: grid;
  align-content: start;
  gap: clamp(8px, calc(var(--roi-panel-pad) * 0.58), 14px);
}

.roi-tile-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.roi-overline {
  margin: 0;
  font-size: var(--roi-small-size);
  letter-spacing: 0.11em;
  text-transform: uppercase;
  color: var(--roi-text-muted);
}

.roi-state-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border-radius: 999px;
  border: 1px solid transparent;
  padding: 3px 8px;
  font-size: var(--roi-small-size);
  font-weight: 600;
  line-height: 1.1;
  color: var(--roi-text-main);
  font-variant-numeric: tabular-nums;
  transition: background-color 160ms ease, border-color 160ms ease, color 160ms ease;
}

.roi-state-chip.is-up {
  border-color: color-mix(in srgb, var(--roi-positive) 46%, transparent);
  background: var(--roi-positive-soft);
  color: rgba(187, 247, 208, 0.98);
}

.roi-state-chip.is-down {
  border-color: color-mix(in srgb, var(--roi-negative) 46%, transparent);
  background: var(--roi-negative-soft);
  color: rgba(254, 205, 211, 0.98);
}

.roi-state-chip.is-neutral {
  border-color: color-mix(in srgb, var(--roi-neutral) 40%, transparent);
  background: var(--roi-neutral-soft);
  color: rgba(226, 232, 240, 0.96);
}

.roi-state-icon {
  width: clamp(12px, calc(var(--roi-small-size) * 1.05), 16px);
  height: clamp(12px, calc(var(--roi-small-size) * 1.05), 16px);
  flex: 0 0 auto;
}

.roi-main-value {
  margin: 0;
  font-size: var(--roi-kpi-size);
  line-height: 1;
  letter-spacing: -0.04em;
  font-weight: 760;
  color: var(--roi-text-main);
  font-variant-numeric: tabular-nums;
}

.roi-delta-row {
  display: inline-flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 6px 10px;
  min-height: 0;
}

.roi-delta-value {
  font-size: var(--roi-body-size);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.roi-delta-row.is-up .roi-delta-value {
  color: rgba(134, 239, 172, 0.98);
}

.roi-delta-row.is-down .roi-delta-value {
  color: rgba(253, 164, 175, 0.98);
}

.roi-delta-row.is-neutral .roi-delta-value {
  color: rgba(203, 213, 225, 0.96);
}

.roi-delta-note {
  font-size: var(--roi-meta-size);
  color: var(--roi-text-soft);
}

.roi-tile--goal.is-ok {
  border-color: color-mix(in srgb, var(--roi-positive) 38%, var(--roi-border));
  background: linear-gradient(160deg, rgba(20, 83, 45, 0.24), rgba(15, 23, 42, 0.56));
}

.roi-tile--goal.is-risk {
  border-color: color-mix(in srgb, var(--roi-negative) 34%, var(--roi-border));
  background: linear-gradient(160deg, rgba(136, 19, 55, 0.2), rgba(15, 23, 42, 0.56));
}

.roi-goal-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.roi-goal-meta {
  min-width: 0;
}

.roi-goal-target {
  margin: 3px 0 0;
  font-size: var(--roi-meta-size);
  color: var(--roi-text-main);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.roi-goal-delta {
  margin: 0;
  font-size: var(--roi-meta-size);
  color: var(--roi-text-main);
  font-weight: 630;
  font-variant-numeric: tabular-nums;
}

.roi-progress {
  width: 100%;
  height: clamp(8px, calc(var(--roi-bar-height) + 1px), 16px);
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  background: rgba(30, 41, 59, 0.82);
  overflow: hidden;
}

.roi-progress-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(34, 197, 94, 0.95), rgba(74, 222, 128, 0.95));
  transition: width 220ms ease;
}

.roi-goal-hint {
  margin: 0;
  font-size: var(--roi-small-size);
  color: var(--roi-text-soft);
}

.roi-breakdown-panel {
  display: grid;
  grid-template-rows: auto 1fr;
  gap: clamp(8px, calc(var(--roi-panel-pad) * 0.58), 12px);
}

.roi-breakdown-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.roi-breakdown-sub {
  margin: 3px 0 0;
  font-size: var(--roi-small-size);
  color: var(--roi-text-soft);
}

.roi-list {
  list-style: none;
  margin: 0;
  padding: 0 2px 0 0;
  display: grid;
  align-content: start;
  gap: clamp(6px, calc(var(--roi-panel-pad) * 0.44), 10px);
  min-height: 0;
  overflow: auto;
}

.roi-list-item {
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: calc(var(--roi-panel-radius) - 4px);
  padding: clamp(6px, calc(var(--roi-panel-pad) * 0.36), 9px);
  display: grid;
  gap: clamp(4px, calc(var(--roi-panel-pad) * 0.3), 7px);
  background: rgba(15, 23, 42, 0.38);
  animation: roi-fade 180ms ease-out both;
}

.roi-list-item.is-up {
  border-color: rgba(74, 222, 128, 0.3);
}

.roi-list-item.is-down {
  border-color: rgba(251, 113, 133, 0.28);
}

.roi-list-item.is-neutral {
  border-color: rgba(148, 163, 184, 0.24);
}

.roi-item-head {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: baseline;
  gap: 8px;
}

.roi-item-rank {
  font-size: var(--roi-small-size);
  color: var(--roi-text-muted);
  font-variant-numeric: tabular-nums;
}

.roi-item-label {
  font-size: var(--roi-meta-size);
  color: var(--roi-text-main);
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.roi-item-value {
  font-size: var(--roi-meta-size);
  color: var(--roi-text-soft);
  font-variant-numeric: tabular-nums;
}

.roi-item-bar {
  width: 100%;
  height: var(--roi-bar-height);
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: rgba(30, 41, 59, 0.82);
  overflow: hidden;
}

.roi-item-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(56, 189, 248, 0.94), rgba(96, 165, 250, 0.96));
  transition: width 220ms ease;
}

.roi-list-item.is-down .roi-item-fill {
  background: linear-gradient(90deg, rgba(251, 113, 133, 0.92), rgba(244, 63, 94, 0.95));
}

.roi-list-item.is-neutral .roi-item-fill {
  background: linear-gradient(90deg, rgba(148, 163, 184, 0.92), rgba(100, 116, 139, 0.95));
}

.roi-item-share {
  margin: 0;
  font-size: var(--roi-small-size);
  color: var(--roi-text-muted);
  font-variant-numeric: tabular-nums;
}

.roi-empty {
  margin: 0;
  align-self: center;
  font-size: var(--roi-meta-size);
  color: var(--roi-text-muted);
}

.roi-root.is-compact .roi-state-chip {
  padding-inline: 7px;
}

.roi-root.is-wide .roi-summary-grid {
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr);
}

.roi-root.is-compact .roi-summary-grid {
  grid-template-columns: 1fr;
}

.roi-root.is-compact .roi-tile--meta {
  grid-column: auto;
}

.roi-root.is-dense .roi-tile,
.roi-root.is-dense .roi-breakdown-panel {
  padding: max(9px, calc(var(--roi-panel-pad) - 2px));
}

.roi-root.is-dense .roi-list {
  gap: clamp(6px, calc(var(--roi-panel-pad) * 0.35), 10px);
}

.roi-root.is-dense .roi-header-period {
  max-width: min(56vw, 300px);
}

.roi-root.is-dense .roi-header-period__text {
  font-size: 10px;
}

@keyframes roi-fade {
  from {
    opacity: 0;
    transform: translateY(3px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .roi-tile,
  .roi-breakdown-panel,
  .roi-list-item {
    animation: none;
  }

  .roi-progress-fill,
  .roi-item-fill,
  .roi-state-chip {
    transition: none;
  }
}
</style>
