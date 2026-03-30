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
      <div class="flex items-end justify-between gap-3">
        <div class="min-w-0">
          <div class="kpi-card__value truncate">{{ valueText }}</div>
          <div class="mt-2 flex items-center gap-2 flex-wrap">
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

      <slot />
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
  accent: { type: String, default: '#8B5CF6' },
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
    props.compact ? 'justify-start gap-3' : 'justify-between',
    props.widgetWidth < 460 ? 'kpi-card--narrow' : '',
  ].filter(Boolean),
)
const showSpark = computed(() => Boolean(props.spark?.length) && props.widgetWidth >= 360)

const kpiStyle = computed(() => {
  const valueBase = Math.min(props.widgetWidth * 0.06, props.widgetHeight * 0.34)
  const valueSize = clamp(Math.round(valueBase), 28, 56)
  const metaSize = clamp(Math.round(valueSize * 0.28), 11, 15)
  const sparkWidth = clamp(Math.round(props.widgetWidth * 0.22), 92, 180)

  return {
    '--kpi-value-size': `${valueSize}px`,
    '--kpi-meta-size': `${metaSize}px`,
    '--kpi-spark-width': `${sparkWidth}px`,
  }
})
</script>

<style scoped>
.kpi-card__value {
  font-size: var(--kpi-value-size);
  line-height: 0.96;
  font-weight: 700;
  letter-spacing: -0.04em;
  color: rgba(255, 255, 255, 0.98);
}

.kpi-card__badge {
  font-size: var(--kpi-meta-size);
  padding: 0.28rem 0.58rem;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.kpi-card__hint {
  font-size: var(--kpi-meta-size);
  color: rgba(255, 255, 255, 0.45);
}

.kpi-card__spark {
  width: var(--kpi-spark-width);
  flex: 0 0 auto;
}

.kpi-card--narrow .kpi-card__value {
  letter-spacing: -0.02em;
}

.kpi-card--narrow .kpi-card__badge {
  padding-inline: 0.48rem;
}
</style>
