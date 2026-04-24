<template>
  <div
    class="widget-card"
    :class="[surfaceClass, { 'is-narrow': isNarrow, 'is-tiny': isTiny, 'is-auto': autoHeight }]"
    :style="cardStyle"
  >
    <div class="widget-card__zoom">
      <div v-if="!hideHeader" class="widget-card__head">
        <div class="widget-card__titlewrap min-w-0">
          <p v-if="subtitle" class="widget-card__subtitle truncate">{{ subtitle }}</p>
          <p class="widget-card__title truncate">{{ title }}</p>
        </div>

        <div class="widget-card__head-actions">
          <slot name="actions" />
          <span class="widget-card__dot" :style="{ backgroundColor: accent }" />
        </div>
      </div>

      <div class="widget-card__content" :class="{ 'widget-card__content--headless': hideHeader }">
        <div v-if="loading" class="h-full flex items-center justify-center">
          <div class="animate-pulse widget-card__status">Chargement...</div>
        </div>

        <div v-else-if="error" class="h-full flex items-center justify-center">
          <div class="widget-card__status widget-card__status--error">Erreur : {{ error }}</div>
        </div>

        <div v-else class="widget-card__slot">
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
  accent: { type: String, default: '#38BDF8' },
  surface: { type: String, default: 'generic' },
  hideHeader: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' },
  autoHeight: { type: Boolean, default: false },
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
  position: relative;
  height: 100%;
  overflow: hidden;
  border-radius: var(--widget-card-radius);
  border: 1px solid color-mix(in srgb, var(--template-surface-line, rgba(148, 163, 184, 0.22)) 90%, transparent);
  background: var(--template-surface-card, linear-gradient(180deg, rgba(10, 15, 28, 0.95), rgba(7, 12, 22, 0.97)));
  box-shadow:
    var(--template-shadow-card, 0 8px 16px rgba(2, 6, 23, 0.16)),
    var(--template-shadow-card-inset, inset 0 1px 0 rgba(255, 255, 255, 0.04));
}
.widget-card.is-auto {
  height: auto;
}

.widget-card--kpi,
.widget-card--trend,
.widget-card--distribution,
.widget-card--ranking,
.widget-card--utility,
.widget-card--generic {
  background: linear-gradient(180deg, rgba(10, 15, 28, 0.95), rgba(7, 12, 22, 0.97));
}

.widget-card__zoom {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  padding: var(--widget-card-padding);
}
.widget-card.is-auto .widget-card__zoom {
  height: auto;
}

.widget-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
  padding-bottom: 9px;
  border-bottom: 1px solid color-mix(in srgb, var(--template-surface-line, rgba(148, 163, 184, 0.18)) 90%, transparent);
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
  min-width: 0;
  min-height: 0;
}
.widget-card.is-auto .widget-card__content {
  flex: 0 0 auto;
}

.widget-card__content--headless {
  padding-top: 0;
}

.widget-card__slot {
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: auto;
  padding-right: 2px;
}
.widget-card.is-auto .widget-card__slot {
  height: auto;
  overflow: visible;
}

.widget-card__subtitle {
  font-size: var(--widget-card-subtitle-size);
  color: var(--template-text-muted, rgba(203, 213, 225, 0.78));
  letter-spacing: 0.05em;
  text-transform: uppercase;
  font-weight: 700;
}

.widget-card__title {
  font-size: var(--widget-card-title-size);
  line-height: 1.14;
  font-weight: 720;
  color: var(--template-text, rgba(248, 250, 252, 0.98));
  font-family: var(--template-title-font, "Inter", sans-serif);
}

.widget-card__dot {
  width: calc(var(--widget-card-dot-size) + 6px);
  height: 4px;
  border-radius: 999px;
  opacity: 0.9;
  box-shadow: none;
}

.widget-card__status {
  font-size: var(--widget-card-status-size);
  color: var(--template-text-muted, rgba(203, 213, 225, 0.78));
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
