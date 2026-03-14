nan<template>
  <teleport to="body" :disabled="!isFullscréen">
    <div
      ref="root"
      class="widget panzoom-exclude"
      :data-id="widget.id"
      :data-edit="editMode ? 'true' : 'false'"
      :class="{
        'drag-armed': dragArmed,
        'widget--tight': widget.props?.tight,
        'is-fullscréen': isFullscréen,
      }"
      :style="isFullscréen ? fullscréenStyle : style"
      @pointerdown.capture="onPointerDown"
    >
      <div class="widget__header drag-handle">
        <div class="widget__title">
          <span class="dot" :style="dotStyle" />
          <span class="title">{{ widget.title }}</span>
          <span v-if="editMode" class="drag-grip" aria-hidden="true" />
        </div>

        <div class="widget__actions">
          <button
            type="button"
            class="iconbtn"
            :title="isFullscréen ? 'Quitter le plein écran' : 'Plein écran'"
            @click.stop="toggleFullscréen"
          >
            <Minimize2 v-if="isFullscréen" class="w-4 h-4" />
            <Maximize2 v-else class="w-4 h-4" />
          </button>
          <template v-if="editMode">
            <button type="button" class="iconbtn" title="Réglages" @click="$emit('settings')">
              <Settings class="w-4 h-4" />
            </button>
            <button type="button" class="iconbtn" title="Supprimer" @click="$emit('remove')">
              <Trash2 class="w-4 h-4" />
            </button>
          </template>
        </div>
      </div>

      <div
        ref="bodyEl"
        class="widget__body"
        :class="{ 'widget__body--auto': widget.props?.autoHeight === true }"
      >
        <component :is="comp" :from="from" :to="to" v-bind="mergedProps" />
      </div>

      <div v-if="editMode && !isFullscréen" class="widget__resize-overlay" aria-hidden="true">
        <span class="resize-rail resize-rail--h" />
        <span class="resize-rail resize-rail--v" />
        <div
          v-for="h in handles"
          :key="h.dir"
          class="resize-handle"
          :class="`resize-handle--${h.dir}`"
          :title="h.title"
          @pointerdown.stop.prevent="onResizeHandleDown(h.dir, $event)"
        >
          <span class="handle-dot" />
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Settings, Trash2, Maximize2, Minimize2 } from 'lucide-vue-next'
import { getCategoryColor, getWidgetDef } from '../widgetRegistry'

type Widget = {
  id: string
  type: string
  title: string
  x: number
  y: number
  w: number
  h: number
  props?: Record<string, unknown>
  z?: number
}

type ResizeDir = 'n' | 's' | 'e' | 'w' | 'ne' | 'nw' | 'se' | 'sw'

const props = defineProps<{
  widget: Widget
  editMode: boolean
  dragArmed: boolean
  comp: any
  from: string
  to: string
  style: Record<string, string | number>
}>()

const emit = defineEmits<{
  (e: 'settings'): void
  (e: 'remove'): void
  (e: 'dragStart', ev: PointerEvent): void
  (e: 'autoResize', height: number): void
  (e: 'fullscreen-change', active: boolean): void
  (e: 'resizeStart', payload: { dir: ResizeDir; event: PointerEvent }): void
}>()

const mergedProps = computed(() => {
  const p = { ...(props.widget?.props ?? {}) } as Record<string, unknown>
  delete p.from
  delete p.to
  return p
})

const dotStyle = computed(() => {
  const def = getWidgetDef(props.widget?.type)
  const tone = getCategoryColor(def?.category)
  return {
    background: tone.color,
    boxShadow: `0 0 0 4px ${tone.glow}`,
  }
})

function onPointerDown(e: PointerEvent) {
  if (!props.editMode) return
  const target = e.target as HTMLElement | null
  if (target?.closest('button, a, input, select, textarea, .iconbtn')) return
  if (target?.closest('.resize-handle')) return
  if (e.pointerType === 'touch' && !target?.closest('.drag-handle')) return
  emit('dragStart', e)
}

const handles: Array<{ dir: ResizeDir; title: string }> = [
  { dir: 'n', title: 'Redimensionner vers le haut' },
  { dir: 's', title: 'Redimensionner vers le bas' },
  { dir: 'e', title: 'Redimensionner vers la droite' },
  { dir: 'w', title: 'Redimensionner vers la gauche' },
  { dir: 'ne', title: 'Redimensionner (diagonale)' },
  { dir: 'nw', title: 'Redimensionner (diagonale)' },
  { dir: 'se', title: 'Redimensionner (diagonale)' },
  { dir: 'sw', title: 'Redimensionner (diagonale)' },
]

function onResizeHandleDown(dir: ResizeDir, event: PointerEvent) {
  if (!props.editMode) return
  event.preventDefault()
  event.stopPropagation()
  emit('resizeStart', { dir, event })
}

const root = ref<HTMLElement | null>(null)
const bodyEl = ref<HTMLElement | null>(null)
const isFullscréen = ref(false)

const fullscréenStyle = {
  position: 'fixed',
  inset: '12px',
  width: 'calc(100vw - 24px)',
  height: 'calc(100vh - 24px)',
  zIndex: 40, // keep below global header (z-50) so navigation stays usable
}

function enterFullscréen() {
  if (isFullscréen.value) return
  isFullscréen.value = true
  document.body.classList.add('widget-fullscréen-open')
  emit('fullscreen-change', true)
}

function exitFullscréen() {
  if (!isFullscréen.value) return
  isFullscréen.value = false
  document.body.classList.remove('widget-fullscréen-open')
  emit('fullscreen-change', false)
}

function toggleFullscréen() {
  isFullscréen.value ? exitFullscréen() : enterFullscréen()
}

defineExpose({ root })

let resizeRaf: number | null = null
let mo: MutationObserver | null = null
let ro: ResizeObserver | null = null

function scheduleAutoResize() {
  if (props.widget?.props?.autoHeight !== true) return
  if (resizeRaf) return
  resizeRaf = requestAnimationFrame(() => {
    resizeRaf = null
    const el = bodyEl.value
    if (!el) return
    const desired = Math.ceil(el.scrollHeight + 44)
    if (!Number.isFinite(desired) || desired <= 0) return
    if (Math.abs(desired - props.widget.h) >= 4) {
      emit('autoResize', desired)
    }
  })
}

onMounted(() => {
  nextTick(() => scheduleAutoResize())
  if (!bodyEl.value) return
  mo = new MutationObserver(() => scheduleAutoResize())
  mo.observe(bodyEl.value, { childList: true, subtree: true, characterData: true })
  if (globalThis.ResizeObserver) {
    ro = new ResizeObserver(() => scheduleAutoResize())
    ro.observe(bodyEl.value)
  }
  window.addEventListener('keydown', onKeydown)
})

watch(
  () => [props.widget?.props?.autoHeight, props.widget?.props?.content],
  () => scheduleAutoResize(),
  { deep: false },
)

onBeforeUnmount(() => {
  if (resizeRaf) cancelAnimationFrame(resizeRaf)
  if (mo) mo.disconnect()
  if (ro) ro.disconnect()
  window.removeEventListener('keydown', onKeydown)
  exitFullscréen()
})

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') exitFullscréen()
}
</script>

<style scoped>
.widget {
  position: absolute;
  border-radius: 22px;
  overflow: hidden;
  background: rgba(17, 24, 39, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.45);
  will-change: transform, width, height;
  contain: layout paint style;
  touch-action: none;
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease;
}
.widget.is-fullscréen {
  position: fixed;
  inset: 12px;
  width: calc(100vw - 24px) !important;
  height: calc(100vh - 24px) !important;
  z-index: 40; /* header is z-50 -> nav remains clickable even in fullscréen */
  cursor: default;
}
.widget.is-dragging {
  cursor: grabbing;
  transition: none;
}
.widget:hover {
  border-color: rgba(255, 255, 255, 0.14);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.55);
}
.widget.drag-armed {
  border-color: rgba(139, 92, 246, 0.6);
  box-shadow: 0 18px 50px rgba(139, 92, 246, 0.15);
  cursor: grab;
}

.widget__header {
  height: 44px;
  display: flex;
  align-items: center;
  padding: 0 10px 0 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(10, 15, 30, 0.88);
}

.widget__title {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  user-select: none;
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
}
.title {
  color: rgba(255, 255, 255, 0.92);
  font-weight: 650;
  font-size: 0.9rem;
}
.drag-grip {
  width: 14px;
  height: 14px;
  border-radius: 6px;
  opacity: 0.7;
  background:
    radial-gradient(circle, rgba(255, 255, 255, 0.45) 1px, transparent 1.5px) 0 0 / 6px 6px;
}

.widget__actions {
  margin-left: auto;
  display: inline-flex;
  gap: 8px;
}
.iconbtn {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.92);
  display: grid;
  place-items: center;
  transition:
    background 160ms ease,
    border-color 160ms ease;
}
.iconbtn:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.16);
}

.widget[data-edit='true'] .drag-handle {
  cursor: grab;
}
.widget.is-dragging .drag-handle {
  cursor: grabbing;
}

.widget__body {
  height: calc(100% - 44px);
  padding: 12px;
  display: flex;
  flex-direction: column;
}
.widget__body--auto {
  height: auto;
}
.widget--tight .widget__body {
  padding: 6px;
}
.widget__body > :deep(*) {
  width: 100%;
}
.widget__body:not(.widget__body--auto) > :deep(*) {
  height: 100%;
}



.widget__resize-overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.resize-rail {
  position: absolute;
  inset: 12px;
  border-radius: 18px;
  border: 1px dashed rgba(255, 255, 255, 0.1);
  opacity: 0;
  transition: opacity 140ms ease;
}
.resize-rail--h {
  inset-inline: 24px;
}
.resize-rail--v {
  inset-block: 24px;
}

.resize-handle {
  --handle-size: 20px;
  --handle-hit: 32px;
  position: absolute;
  width: var(--handle-size);
  height: var(--handle-size);
  border-radius: 12px;
  background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0.08));
  border: 1px solid rgba(255, 255, 255, 0.24);
  box-shadow:
    0 12px 24px rgba(0, 0, 0, 0.35),
    0 0 0 8px rgba(148, 163, 184, 0.09);
  display: grid;
  place-items: center;
  opacity: 0;
  transform: scale(0.9);
  transition:
    opacity 140ms ease,
    transform 140ms ease,
    box-shadow 140ms ease;
  pointer-events: auto;
  backdrop-filter: blur(5px);
  touch-action: none;
}
.resize-handle::before {
  content: '';
  position: absolute;
  inset: calc((var(--handle-size) - var(--handle-hit)) / 2);
}

.resize-handle:hover {
  box-shadow:
    0 16px 34px rgba(0, 0, 0, 0.42),
    0 0 0 10px rgba(148, 163, 184, 0.2);
  transform: scale(1.06);
}

.handle-dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: rgba(17, 24, 39, 0.92);
}

.resize-handle--n {
  top: -12px;
  left: 50%;
  transform: translate(-50%, -50%) scale(0.9);
  cursor: ns-resize;
}
.resize-handle--s {
  bottom: -12px;
  left: 50%;
  transform: translate(-50%, 50%) scale(0.9);
  cursor: ns-resize;
}
.resize-handle--e {
  right: -12px;
  top: 50%;
  transform: translate(50%, -50%) scale(0.9);
  cursor: ew-resize;
}
.resize-handle--w {
  left: -12px;
  top: 50%;
  transform: translate(-50%, -50%) scale(0.9);
  cursor: ew-resize;
}
.resize-handle--ne {
  right: -12px;
  top: -12px;
  transform: translate(50%, -50%) scale(0.9);
  cursor: nesw-resize;
}
.resize-handle--nw {
  left: -12px;
  top: -12px;
  transform: translate(-50%, -50%) scale(0.9);
  cursor: nwse-resize;
}
.resize-handle--se {
  right: -12px;
  bottom: -12px;
  transform: translate(50%, 50%) scale(0.9);
  cursor: nwse-resize;
}
.resize-handle--sw {
  left: -12px;
  bottom: -12px;
  transform: translate(-50%, 50%) scale(0.9);
  cursor: nesw-resize;
}

.widget[data-edit="true"]:hover .resize-handle,
.widget.is-resizing .resize-handle,
.widget.drag-armed .resize-handle {
  opacity: 1;
  transform: scale(1);
}
.widget[data-edit="true"]:hover .resize-rail,
.widget.is-resizing .resize-rail,
.widget.drag-armed .resize-rail {
  opacity: 1;
}
.widget.is-dragging .resize-handle {
  opacity: 0;
}

@media (pointer: coarse) {
  .resize-handle {
    --handle-size: 24px;
    --handle-hit: 38px;
  }
}
:global(body.widget-fullscréen-open) {
  overflow: hidden;
}
</style>
