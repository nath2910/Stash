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
      <div class="widget-card__views" v-if="visibleDisplays.length">
        <span v-for="display in visibleDisplays" :key="display" class="widget-view">{{ display }}</span>
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

const visibleDisplays = computed(() => {
  const selection = props.widget.selection ?? []
  if (selection.length) return selection.map((group) => group.label).slice(0, 3)
  return (props.widget.forms ?? []).slice(0, 3)
})

const ctaLabel = computed(() => {
  const variantsCount = (props.widget.selection ?? []).reduce(
    (sum, group) => sum + (group.variants?.length ?? 0),
    0,
  )
  const formsCount = props.widget.forms?.length ?? 0
  if (props.widget.formPicker === false || Math.max(variantsCount, formsCount) <= 1) return 'Ajouter'
  return 'Configurer'
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
  min-height: 176px;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid rgba(100, 116, 139, 0.18);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.96)),
    linear-gradient(135deg, color-mix(in srgb, var(--card-accent) 7%, transparent), transparent 62%);
  text-align: left;
  transition: border-color 140ms ease, box-shadow 150ms ease, transform 140ms ease;
  content-visibility: auto;
  contain-intrinsic-size: 200px;
}

.widget-card:hover {
  border-color: color-mix(in srgb, var(--card-accent) 40%, rgba(100, 116, 139, 0.4));
  transform: translateY(-1px);
  box-shadow:
    0 12px 24px rgba(15, 23, 42, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.widget-card:focus-visible {
  outline: 2px solid color-mix(in srgb, var(--card-accent) 46%, #3b82f6);
  outline-offset: 2px;
}

.widget-card.is-active {
  border-color: color-mix(in srgb, var(--card-accent) 46%, rgba(59, 130, 246, 0.52));
}

.widget-card.is-selected {
  border-color: color-mix(in srgb, var(--card-accent) 46%, rgba(100, 116, 139, 0.3));
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--card-accent) 14%, transparent);
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
  font-size: 14px;
  line-height: 1.2;
  font-weight: 700;
  color: #0f172a;
}

.widget-card__help {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.4;
  color: #64748b;
}

.widget-card__icon {
  color: color-mix(in srgb, var(--card-accent) 74%, #334155);
}

.widget-card__preview-wrap {
  min-height: 92px;
}

.widget-card__preview-skeleton {
  border-radius: 8px;
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
  border-top: 1px solid rgba(100, 116, 139, 0.16);
  padding-top: 8px;
}

.widget-card__views {
  display: flex;
  gap: 6px;
  min-width: 0;
  overflow: hidden;
}

.widget-view {
  flex: 0 0 auto;
  padding: 3px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
  color: #334155;
  background: rgba(248, 250, 252, 0.8);
  border: 1px solid rgba(100, 116, 139, 0.16);
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
