<template>
  <div
    class="widget-card h-full"
    :class="[surfaceClass, { 'is-narrow': isNarrow, 'is-tiny': isTiny }]"
    :style="cardStyle"
  >
    <div class="widget-card__zoom">
      <div class="widget-card__head">
        <div class="widget-card__titlewrap min-w-0">
          <p v-if="subtitle" class="widget-card__subtitle truncate">{{ subtitle }}</p>
          <p class="widget-card__title truncate">{{ title }}</p>
        </div>

        <div class="widget-card__head-actions">
          <slot name="actions" />
          <span class="widget-card__dot" :style="{ backgroundColor: accent }" />
        </div>
      </div>

      <div class="widget-card__content">
        <div v-if="loading" class="h-full flex items-center justify-center">
          <div class="animate-pulse widget-card__status">Chargement...</div>
        </div>

        <div v-else-if="error" class="h-full flex items-center justify-center">
          <div class="widget-card__status widget-card__status--error">Erreur : {{ error }}</div>
        </div>

        <div v-else class="h-full">
          <slot />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  accent: { type: String, default: '#8B5CF6' },
  surface: { type: String, default: 'generic' },
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' },
  widgetWidth: { type: Number, default: 520 },
  widgetHeight: { type: Number, default: 240 },
  widgetBaseWidth: { type: Number, default: 0 },
  widgetBaseHeight: { type: Number, default: 0 },
})

const clamp = (value, min, max) => Math.max(min, Math.min(max, value))

const SURFACE_CLASSES = new Set(['kpi', 'trend', 'distribution', 'ranking', 'utility', 'generic'])
const surfaceClass = computed(() => {
  const key = String(props.surface ?? 'generic').trim().toLowerCase()
  return SURFACE_CLASSES.has(key) ? `widget-card--${key}` : 'widget-card--generic'
})

const isNarrow = computed(() => Number(props.widgetWidth ?? 0) > 0 && Number(props.widgetWidth ?? 0) < 520)
const isTiny = computed(() => Number(props.widgetWidth ?? 0) > 0 && Number(props.widgetWidth ?? 0) < 390)

const cardStyle = computed(() => {
  const headerBase = Math.min(props.widgetWidth * 0.018, props.widgetHeight * 0.16)
  const titleSize = clamp(Math.round(headerBase), 14, 30)
  const subtitleSize = clamp(Math.round(titleSize * 0.78), 10, 20)
  const padding = clamp(
    Math.round(Math.min(props.widgetWidth * 0.024, props.widgetHeight * 0.1)),
    12,
    34,
  )
  const dot = clamp(Math.round(titleSize * 0.62), 8, 16)
  const status = clamp(Math.round(titleSize * 0.82), 11, 20)
  const radius = clamp(Math.round(Math.min(props.widgetWidth * 0.032, props.widgetHeight * 0.18)), 14, 24)

  return {
    '--widget-card-title-size': `${titleSize}px`,
    '--widget-card-subtitle-size': `${subtitleSize}px`,
    '--widget-card-padding': `${padding}px`,
    '--widget-card-dot-size': `${dot}px`,
    '--widget-card-status-size': `${status}px`,
    '--widget-card-radius': `${radius}px`,
    '--widget-card-accent': props.accent,
  }
})
</script>

<style scoped>
.widget-card {
  overflow: hidden;
  border-radius: var(--widget-card-radius);
  border: 1px solid rgba(148, 163, 184, 0.08);
  background: linear-gradient(180deg, rgba(8, 12, 22, 0.95), rgba(6, 10, 18, 0.97));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.04),
    0 8px 16px rgba(2, 6, 23, 0.18);
}

.widget-card--kpi {
  background:
    linear-gradient(180deg, rgba(8, 12, 21, 0.96), rgba(6, 10, 17, 0.98)),
    linear-gradient(
      90deg,
      color-mix(in srgb, var(--widget-card-accent) 12%, transparent),
      transparent 52%
    );
}

.widget-card--trend {
  background:
    linear-gradient(180deg, rgba(7, 11, 20, 0.96), rgba(5, 9, 16, 0.98)),
    linear-gradient(
      90deg,
      color-mix(in srgb, var(--widget-card-accent) 10%, transparent),
      transparent 58%
    );
}

.widget-card--distribution {
  background:
    linear-gradient(180deg, rgba(9, 13, 21, 0.96), rgba(7, 10, 17, 0.98)),
    linear-gradient(
      90deg,
      color-mix(in srgb, var(--widget-card-accent) 12%, transparent),
      transparent 60%
    );
}

.widget-card--ranking {
  background:
    linear-gradient(180deg, rgba(8, 12, 21, 0.96), rgba(6, 10, 17, 0.98)),
    linear-gradient(
      90deg,
      color-mix(in srgb, var(--widget-card-accent) 11%, transparent),
      transparent 56%
    );
}

.widget-card--utility,
.widget-card--generic {
  background:
    linear-gradient(180deg, rgba(8, 12, 21, 0.96), rgba(6, 10, 17, 0.98)),
    linear-gradient(
      90deg,
      color-mix(in srgb, var(--widget-card-accent) 8%, transparent),
      transparent 64%
    );
}

.widget-card__zoom {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  padding: var(--widget-card-padding);
}

.widget-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
  padding-bottom: 0;
  border-bottom: none;
}

.widget-card__titlewrap {
  display: grid;
  gap: 2px;
}

.widget-card__head-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.widget-card__content {
  flex: 1 1 auto;
  min-height: 0;
}

.widget-card__subtitle {
  font-size: var(--widget-card-subtitle-size);
  color: rgba(226, 232, 240, 0.56);
  letter-spacing: 0.01em;
}

.widget-card__title {
  font-size: var(--widget-card-title-size);
  line-height: 1.12;
  font-weight: 600;
  color: rgba(248, 250, 252, 0.96);
}

.widget-card__dot {
  width: var(--widget-card-dot-size);
  height: var(--widget-card-dot-size);
  border-radius: 999px;
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--widget-card-accent) 14%, transparent);
}

.widget-card__status {
  font-size: var(--widget-card-status-size);
  color: rgba(226, 232, 240, 0.62);
}

.widget-card__status--error {
  color: rgba(254, 202, 202, 0.96);
}

.widget-card.is-narrow .widget-card__zoom {
  padding: max(10px, calc(var(--widget-card-padding) - 4px));
}

.widget-card.is-narrow .widget-card__head {
  margin-bottom: 10px;
  padding-bottom: 0;
}

.widget-card.is-tiny .widget-card__subtitle {
  display: none;
}

.widget-card.is-tiny .widget-card__head {
  gap: 6px;
}

.widget-card.is-tiny .widget-card__head-actions {
  gap: 6px;
}
</style>
