<template>
  <button
    ref="buttonRef"
    type="button"
    class="widget-card"
    :class="{
      'is-active': active,
      'is-selected': selected,
      'is-adding': adding,
      'is-disabled': disabled,
    }"
    :style="cardStyle"
    :data-widget-type="widget.type"
    :tabindex="tabIndex"
    :aria-disabled="disabled"
    :aria-selected="selected"
    role="option"
    @click="onSelect"
    @focus="emit('focus', widget.type)"
  >
    <div class="widget-card__head">
      <div class="widget-card__title-wrap">
        <div class="widget-card__title">{{ widget.title }}</div>
        <div class="widget-card__help">{{ widget.help ?? 'Ajout rapide sur le canvas' }}</div>
      </div>
      <component :is="widget.icon" v-if="widget.icon" class="widget-card__icon" :size="16" aria-hidden="true" />
    </div>

    <div ref="previewAnchorRef" class="widget-card__preview-wrap">
      <WidgetPreview v-if="showPreview" :model="widget.preview" :accent="accent" />
      <div v-else class="widget-card__preview-skeleton"></div>
    </div>

    <div class="widget-card__foot">
      <div class="widget-card__tags" v-if="visibleTags.length">
        <span v-for="tag in visibleTags" :key="tag" class="widget-tag">{{ tag }}</span>
      </div>
      <span class="widget-card__cta" :aria-live="adding ? 'polite' : 'off'">
        {{ adding ? 'Ajout...' : ctaLabel }}
      </span>
    </div>
  </button>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import WidgetPreview from './WidgetPreview.vue'
import type { WidgetPaletteItem } from './types'

const props = defineProps<{
  widget: WidgetPaletteItem
  accent: string
  selected: boolean
  active: boolean
  adding: boolean
  disabled: boolean
  tabIndex: number
}>()

const emit = defineEmits<{
  (event: 'select', widget: WidgetPaletteItem): void
  (event: 'focus', type: string): void
}>()

const buttonRef = ref<HTMLButtonElement | null>(null)
const previewAnchorRef = ref<HTMLElement | null>(null)
const showPreview = ref(false)
let observer: IntersectionObserver | null = null

const cardStyle = computed(() => ({
  '--card-accent': props.accent,
}))

const visibleTags = computed(() => (props.widget.tags ?? []).slice(0, 3))

const ctaLabel = computed(() => {
  const formsCount = props.widget.forms?.length ?? 0
  if (props.widget.formPicker === false || formsCount <= 1) return 'Ajouter'
  return 'Choisir la vue'
})

function onSelect() {
  if (props.disabled) return
  emit('select', props.widget)
}

function initObserver() {
  if (typeof window === 'undefined' || !('IntersectionObserver' in window)) {
    showPreview.value = true
    return
  }
  observer = new window.IntersectionObserver(
    (entries) => {
      const entry = entries[0]
      if (!entry?.isIntersecting) return
      showPreview.value = true
      observer?.disconnect()
      observer = null
    },
    { rootMargin: '120px 0px' },
  )
  if (previewAnchorRef.value) observer.observe(previewAnchorRef.value)
}

onMounted(initObserver)

onBeforeUnmount(() => {
  observer?.disconnect()
  observer = null
})

defineExpose({
  focus: () => buttonRef.value?.focus(),
})
</script>

<style scoped>
.widget-card {
  display: grid;
  gap: 10px;
  min-height: 182px;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid rgba(100, 116, 139, 0.44);
  background:
    linear-gradient(180deg, rgba(214, 224, 237, 0.74), rgba(203, 213, 225, 0.72)),
    radial-gradient(circle at 100% 0, color-mix(in srgb, var(--card-accent) 10%, transparent), transparent 58%);
  text-align: left;
  transition: border-color 140ms ease, box-shadow 150ms ease, transform 140ms ease;
  content-visibility: auto;
  contain-intrinsic-size: 200px;
}

.widget-card:hover {
  border-color: color-mix(in srgb, var(--card-accent) 52%, rgba(100, 116, 139, 0.48));
  transform: translateY(-1px);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.14);
}

.widget-card:focus-visible {
  outline: 2px solid color-mix(in srgb, var(--card-accent) 46%, #3b82f6);
  outline-offset: 2px;
}

.widget-card.is-active {
  border-color: color-mix(in srgb, var(--card-accent) 46%, rgba(59, 130, 246, 0.52));
}

.widget-card.is-selected {
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--card-accent) 28%, #bfdbfe);
}

.widget-card.is-disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.widget-card.is-adding .widget-card__cta {
  color: #0369a1;
}

.widget-card__head {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  align-items: start;
}

.widget-card__title-wrap {
  min-width: 0;
}

.widget-card__title {
  font-size: 15px;
  line-height: 1.2;
  font-weight: 700;
  color: #0f172a;
}

.widget-card__help {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.4;
  color: #526277;
}

.widget-card__icon {
  color: color-mix(in srgb, var(--card-accent) 74%, #334155);
}

.widget-card__preview-wrap {
  min-height: 92px;
}

.widget-card__preview-skeleton {
  border-radius: 11px;
  min-height: 88px;
  border: 1px solid rgba(100, 116, 139, 0.18);
  background: linear-gradient(90deg, rgba(203, 213, 225, 0.56), rgba(226, 232, 240, 0.8), rgba(203, 213, 225, 0.56));
  background-size: 200% 100%;
  animation: palette-skeleton 1.3s linear infinite;
}

.widget-card__foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-top: auto;
  border-top: 1px solid rgba(148, 163, 184, 0.4);
  padding-top: 8px;
}

.widget-card__tags {
  display: flex;
  gap: 6px;
  min-width: 0;
  overflow: hidden;
}

.widget-tag {
  flex: 0 0 auto;
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  color: #334155;
  background: rgba(148, 163, 184, 0.36);
}

.widget-card__cta {
  margin-left: auto;
  font-size: 11px;
  font-weight: 700;
  color: #1d4ed8;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

@keyframes palette-skeleton {
  0% {
    background-position: 0 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>
