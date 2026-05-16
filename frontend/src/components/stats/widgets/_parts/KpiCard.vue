<template>
  <WidgetCard
    :title="title"
    :subtitle="subtitle"
    :accent="accent"
    :surface="surface"
    :loading="loading"
    :error="error"
    :widget-width="widgetWidth"
    :widget-height="widgetHeight"
    :widget-base-width="widgetBaseWidth"
    :widget-base-height="widgetBaseHeight"
  >
    <div
      class="kpi-card"
      :class="{ 'kpi-card--short': isShort, 'kpi-card--tiny': isTiny, 'has-details': hasDetails }"
      :style="kpiStyle"
    >
      <span class="kpi-card__accent" aria-hidden="true"></span>
      <div class="kpi-card__value" :class="{ truncate: isShort }">{{ valueText }}</div>
      <div v-if="showMeta" class="kpi-card__meta">
        <span v-if="deltaText" class="kpi-card__delta" :class="deltaToneClass">
          {{ deltaText }}
        </span>
        <span v-if="hint" class="kpi-card__hint">{{ hint }}</span>
      </div>
      <Sparkline v-if="showSpark" class="kpi-card__spark" :data="spark" :color="accent" />
      <div v-if="hasDetails" class="kpi-card__details">
        <slot />
      </div>
    </div>
  </WidgetCard>
</template>

<script setup>
import { computed, useSlots } from 'vue'
import WidgetCard from './WidgetCard.vue'
import Sparkline from './Sparkline.vue'

const props = defineProps({
  title: String,
  subtitle: String,
  accent: { type: String, default: '#38BDF8' },
  surface: { type: String, default: 'kpi' },
  loading: Boolean,
  error: String,
  compact: { type: Boolean, default: false },
  widgetWidth: { type: Number, default: 520 },
  widgetHeight: { type: Number, default: 240 },
  widgetBaseWidth: { type: Number, default: 0 },
  widgetBaseHeight: { type: Number, default: 0 },
  valueText: { type: String, default: '--' },
  deltaPct: { type: Number, default: null },
  deltaText: { type: String, default: '' },
  deltaClass: { type: String, default: '' },
  hint: { type: String, default: '' },
  spark: { type: Array, default: () => [] },
  showDetails: { type: Boolean, default: true },
})

const slots = useSlots()
const clamp = (value, min, max) => Math.max(min, Math.min(max, value))

const isShort = computed(() => Number(props.widgetHeight ?? 0) < 205)
const isTiny = computed(() => Number(props.widgetHeight ?? 0) < 155 || Number(props.widgetWidth ?? 0) < 300)
const valueLength = computed(() => String(props.valueText ?? '').replace(/\s+/g, '').length)
const valueScale = computed(() => {
  const len = valueLength.value
  if (len >= 16) return 0.56
  if (len >= 13) return 0.66
  if (len >= 10) return 0.82
  return 1
})
const sparkValues = computed(() => (Array.isArray(props.spark) ? props.spark : []))
const showSpark = computed(
  () => props.showDetails !== false && sparkValues.value.length > 1 && !isTiny.value && Number(props.widgetHeight ?? 0) >= 210,
)
const showMeta = computed(() => Boolean(props.deltaText || props.hint) && !isTiny.value)
const hasDetails = computed(
  () => props.showDetails !== false && Boolean(slots.default) && !isTiny.value && Number(props.widgetHeight ?? 0) >= 230,
)
const deltaToneClass = computed(() => {
  if (props.deltaClass) return props.deltaClass
  const delta = Number(props.deltaPct ?? 0)
  if (!Number.isFinite(delta) || delta === 0) return 'is-neutral'
  return delta > 0 ? 'is-positive' : 'is-negative'
})

const kpiStyle = computed(() => {
  const width = Number(props.widgetWidth ?? 0) || 520
  const height = Number(props.widgetHeight ?? 0) || 240
  const valueBase = Math.min(width * 0.24, height * 0.48)
  const valueSize = clamp(Math.round(valueBase * valueScale.value), 30, 64)
  const padding = clamp(Math.round(Math.min(width * 0.024, height * 0.1)), 8, 16)
  const topPadding = clamp(Math.round(height * 0.026), 3, 8)
  const rowGap = clamp(Math.round(height * 0.035), 5, 9)
  const metaSize = clamp(Math.round(Math.min(width * 0.034, height * 0.07)), 11, 13)

  return {
    '--kpi-value-size': `${valueSize}px`,
    '--kpi-padding': `${padding}px`,
    '--kpi-top-padding': `${topPadding}px`,
    '--kpi-gap': `${rowGap}px`,
    '--kpi-meta-size': `${metaSize}px`,
    '--kpi-accent': props.accent,
  }
})
</script>

<style scoped>
.kpi-card {
  position: relative;
  height: 100%;
  min-width: 0;
  min-height: 0;
  display: grid;
  grid-template-rows: auto auto auto auto minmax(0, auto);
  align-content: start;
  justify-items: center;
  gap: var(--kpi-gap);
  padding: var(--kpi-top-padding) var(--kpi-padding) var(--kpi-padding);
  color: var(--template-text, #111827);
  overflow: hidden;
}

.kpi-card__accent {
  position: relative;
  display: block;
  width: clamp(52px, 20%, 84px);
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--kpi-accent), color-mix(in srgb, var(--kpi-accent) 62%, #2563eb));
  box-shadow: 0 8px 18px color-mix(in srgb, var(--kpi-accent) 16%, transparent);
  pointer-events: none;
}

.kpi-card__value {
  max-width: 100%;
  font-size: var(--kpi-value-size);
  line-height: 0.96;
  font-family: var(--template-title-font, var(--font-display, "Poppins", sans-serif));
  font-weight: 820;
  letter-spacing: 0;
  color: var(--template-text, #111827);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-variant-numeric: tabular-nums;
}

.kpi-card__meta {
  max-width: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--template-text-muted, #64748b);
  font-size: var(--kpi-meta-size);
  line-height: 1.2;
  overflow: hidden;
  text-align: center;
}

.kpi-card__delta,
.kpi-card__hint {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kpi-card__delta {
  flex: 0 1 auto;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(248, 250, 252, 0.76);
  padding: 3px 7px;
  font-weight: 760;
}

.kpi-card__delta.is-positive {
  border-color: rgba(16, 185, 129, 0.24);
  background: rgba(209, 250, 229, 0.62);
  color: #047857;
}

.kpi-card__delta.is-negative {
  border-color: rgba(244, 63, 94, 0.22);
  background: rgba(255, 228, 230, 0.72);
  color: #be123c;
}

.kpi-card__delta.is-neutral {
  color: #475569;
}

.kpi-card__hint {
  flex: 1 1 auto;
  color: var(--template-text-muted, #64748b);
  font-weight: 650;
}

.kpi-card__spark {
  width: min(100%, 180px);
  height: 38px;
  opacity: 0.9;
}

.kpi-card__details {
  width: 100%;
  min-width: 0;
  min-height: 0;
  overflow: auto;
}

.kpi-card--short {
  gap: max(5px, calc(var(--kpi-gap) - 1px));
  padding-top: max(3px, calc(var(--kpi-top-padding) - 1px));
}

.kpi-card--tiny .kpi-card__accent {
  width: min(48px, 24%);
}

.kpi-card--tiny .kpi-card__value {
  font-weight: 780;
}

.kpi-card--tiny .kpi-card__meta,
.kpi-card--tiny .kpi-card__spark,
.kpi-card--tiny .kpi-card__details {
  display: none;
}
</style>
