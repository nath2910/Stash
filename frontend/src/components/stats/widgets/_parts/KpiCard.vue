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
    <div class="kpi-card h-full flex flex-col pb-1" :class="containerClass" :style="kpiStyle">
      <div class="kpi-card__head flex min-w-0 items-end justify-between gap-3">
        <div class="min-w-0">
          <div class="kpi-card__value" :class="{ truncate: isShort }">{{ valueText }}</div>
          <div class="kpi-card__meta-row mt-2 flex items-center gap-2 flex-wrap" :class="{ 'is-short': isShort }">
            <span
              v-if="deltaPct != null"
              class="kpi-card__badge"
              :class="deltaClass"
            >
              {{ deltaText }}
            </span>
            <span v-if="hint" class="kpi-card__hint truncate">{{ hint }}</span>
          </div>
        </div>

        <div v-if="showSpark" class="kpi-card__spark">
          <Sparkline :data="spark" :color="accent" />
        </div>
      </div>

      <div v-if="showSlot" class="kpi-card__slot">
        <slot />
      </div>
    </div>
  </WidgetCard>
</template>

<script setup>
import { computed } from 'vue'
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
})

const clamp = (value, min, max) => Math.max(min, Math.min(max, value))

const isShort = computed(() => Number(props.widgetHeight ?? 0) < 205)
const isVeryShort = computed(() => Number(props.widgetHeight ?? 0) < 186)

const deltaClass = computed(() => {
  if (props.deltaClass) return props.deltaClass
  if (props.deltaPct == null) return ''
  const up = props.deltaPct >= 0
  return up
    ? 'border-emerald-400/30 bg-emerald-500/10 text-emerald-300'
    : 'border-red-400/30 bg-red-500/10 text-red-300'
})

const containerClass = computed(() =>
  [
    props.compact ? 'justify-start gap-2.5' : 'justify-between',
    props.widgetWidth < 460 ? 'kpi-card--narrow' : '',
    isShort.value ? 'kpi-card--short' : '',
  ].filter(Boolean),
)
const showSpark = computed(
  () =>
    Boolean(props.spark?.length) &&
    Number(props.widgetWidth ?? 0) >= 380 &&
    Number(props.widgetHeight ?? 0) >= 205,
)
const showSlot = computed(() => !isVeryShort.value)

const kpiStyle = computed(() => {
  const valueBase = Math.min(props.widgetWidth * 0.05, props.widgetHeight * 0.24)
  const valueSize = clamp(Math.round(valueBase), 22, 44)
  const metaSize = clamp(Math.round(valueSize * 0.31), 10, 14)
  const sparkWidth = clamp(Math.round(props.widgetWidth * 0.2), 88, 170)

  return {
    '--kpi-value-size': `${valueSize}px`,
    '--kpi-meta-size': `${metaSize}px`,
    '--kpi-spark-width': `${sparkWidth}px`,
  }
})
</script>

<style scoped>
.kpi-card {
  min-width: 0;
  min-height: 0;
}

.kpi-card__head {
  flex: 0 0 auto;
}

.kpi-card__value {
  font-size: var(--kpi-value-size);
  line-height: 1.06;
  font-weight: 620;
  letter-spacing: -0.025em;
  color: rgba(248, 250, 252, 0.96);
}

.kpi-card__meta-row.is-short {
  margin-top: 0.36rem;
}

.kpi-card__badge {
  font-size: var(--kpi-meta-size);
  padding: 0.24rem 0.52rem;
  border-radius: 999px;
  border: none;
}

.kpi-card__hint {
  font-size: var(--kpi-meta-size);
  color: rgba(148, 163, 184, 0.84);
}

.kpi-card__spark {
  width: var(--kpi-spark-width);
  flex: 0 0 auto;
}

.kpi-card__slot {
  min-width: 0;
  min-height: 0;
  flex: 1 1 auto;
  overflow: auto;
  padding-right: 2px;
}

.kpi-card--narrow .kpi-card__value {
  letter-spacing: -0.02em;
}

.kpi-card--narrow .kpi-card__badge {
  padding-inline: 0.48rem;
}

.kpi-card--short {
  gap: 2px !important;
}
</style>
