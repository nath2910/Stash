<template>
  <div
    class="widget-card"
    :class="[surfaceClass, { 'is-narrow': isNarrow, 'is-tiny': isTiny, 'is-auto': autoHeight, 'is-headless': hideHeader }]"
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
        <div v-if="loading" class="widget-card__state widget-card__state--loading" role="status">
          <span class="widget-card__skeleton widget-card__skeleton--lg" />
          <span class="widget-card__skeleton widget-card__skeleton--md" />
          <span class="widget-card__skeleton widget-card__skeleton--sm" />
          <span class="widget-card__state-label">Chargement</span>
        </div>

        <div v-else-if="error" class="widget-card__state widget-card__state--error" role="alert">
          <div class="widget-card__state-title">Donnees indisponibles</div>
          <div class="widget-card__status widget-card__status--error">{{ error }}</div>
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
  hideHeader: { type: Boolean, default: true },
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
  const titleSize = clamp(Math.round(headerBase), 12, 18)
  const subtitleSize = clamp(Math.round(titleSize * 0.72), 10, 15)
  const padding = clamp(
    Math.round(Math.min(props.widgetWidth * 0.018, props.widgetHeight * 0.055)),
    8,
    16,
  )
  const dot = clamp(Math.round(titleSize * 0.48), 6, 11)
  const status = clamp(Math.round(titleSize * 0.82), 11, 20)
  const radius = 8
  const contentGap = clamp(Math.round(Math.min(props.widgetWidth * 0.014, props.widgetHeight * 0.045)), 6, 12)

  return {
    '--widget-card-title-size': `${titleSize}px`,
    '--widget-card-subtitle-size': `${subtitleSize}px`,
    '--widget-card-padding': `${padding}px`,
    '--widget-card-dot-size': `${dot}px`,
    '--widget-card-status-size': `${status}px`,
    '--widget-card-radius': `${radius}px`,
    '--widget-card-content-gap': `${contentGap}px`,
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
  border: 0;
  background: transparent;
  box-shadow: none;
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
  background: transparent;
}

.widget-card__zoom {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  padding: var(--widget-card-padding);
  gap: var(--widget-card-content-gap);
}
.widget-card.is-auto .widget-card__zoom {
  height: auto;
}

.widget-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: 0;
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
  overflow: hidden;
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
  scrollbar-width: thin;
  scrollbar-color: rgba(100, 116, 139, 0.28) transparent;
}
.widget-card.is-auto .widget-card__slot {
  height: auto;
  overflow: visible;
}

.widget-card__subtitle {
  font-size: var(--widget-card-subtitle-size);
  color: var(--template-text-muted, rgba(203, 213, 225, 0.78));
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 720;
  opacity: 0.78;
}

.widget-card__title {
  font-size: var(--widget-card-title-size);
  line-height: 1.14;
  font-weight: 760;
  color: var(--template-text, rgba(248, 250, 252, 0.98));
  font-family: var(--template-title-font, var(--font-display, "Poppins", sans-serif));
}

.widget-card__dot {
  width: calc(var(--widget-card-dot-size) + 10px);
  height: 2px;
  border-radius: 999px;
  opacity: 0.74;
  box-shadow: none;
}

.widget-card__state {
  height: 100%;
  min-height: 88px;
  display: grid;
  place-content: center;
  gap: 8px;
  padding: 12px;
  text-align: center;
}

.widget-card__state--loading {
  justify-items: center;
}

.widget-card__skeleton {
  display: block;
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(
    90deg,
    rgba(203, 213, 225, 0.42),
    rgba(241, 245, 249, 0.9),
    rgba(203, 213, 225, 0.42)
  );
  background-size: 220% 100%;
  animation: widget-card-skeleton 1.2s linear infinite;
}

.widget-card__skeleton--lg {
  width: min(190px, 72%);
}

.widget-card__skeleton--md {
  width: min(150px, 58%);
}

.widget-card__skeleton--sm {
  width: min(96px, 42%);
}

.widget-card__state-label,
.widget-card__state-title {
  font-size: var(--widget-card-status-size);
  color: var(--template-text-muted, #64748b);
  font-weight: 650;
}

.widget-card__state-title {
  color: var(--template-text, #111827);
}

.widget-card__status {
  font-size: var(--widget-card-status-size);
  color: var(--template-text-muted, rgba(203, 213, 225, 0.78));
}

.widget-card__status--error {
  color: #be123c;
}

@keyframes widget-card-skeleton {
  0% {
    background-position: 0 0;
  }
  100% {
    background-position: -220% 0;
  }
}

.widget-card.is-narrow .widget-card__zoom {
  padding: max(8px, calc(var(--widget-card-padding) - 3px));
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
