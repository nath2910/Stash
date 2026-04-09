<template>
  <teleport to="body" :disabled="!isFullscreen">
    <div
      ref="root"
      class="widget panzoom-exclude"
      :data-id="widget.id"
      :data-edit="editMode ? 'true' : 'false'"
      :data-text-active="textActive ? 'true' : 'false'"
      :data-group-selected="groupSelected ? 'true' : 'false'"
      :data-state="widgetState"
      role="group"
      :aria-selected="selected ? 'true' : 'false'"
      :aria-label="`Widget ${widget.title}`"
      :class="{
        'drag-armed': dragArmed,
        'is-selected': selected,
        'group-selected': groupSelected,
        'widget--tight': widget.props?.tight,
        'widget--text': widget.type === 'textTitle' || widget.type === 'textBlock',
        'widget--roi': widget.type === 'roi',
        'widget--net-profit': widget.type === 'netProfit',
        'widget--headerless': !showHeader,
        'is-text-active': textActive,
        'is-editing': isInlineTextEditing,
        'is-fullscreen': isFullscreen,
      }"
      :style="isFullscreen ? fullscreenStyle : style"
      :tabindex="rootTabIndex"
      @pointerdown.capture="onPointerDown"
      @keydown="onRootKeydown"
    >
      <div class="widget__surface" :style="surfaceLayoutStyle">
      <div v-if="showHeader" class="widget__header drag-handle" :style="headerStyle">
        <div class="widget__title">
          <span class="dot" :style="dotStyle" />
          <span class="title">{{ widget.title }}</span>
          <span v-if="editMode" class="drag-grip" aria-hidden="true" />
        </div>

        <div
          class="widget__actions"
          :class="{ 'widget__actions--compact': compactActions }"
          @pointerdown.stop
        >
          <button
            type="button"
            class="iconbtn"
            :title="isFullscreen ? 'Quitter le plein ecran' : 'Plein ecran'"
            :aria-label="isFullscreen ? 'Quitter le plein ecran' : 'Plein ecran'"
            @click.stop="toggleFullscreen"
          >
            <Minimize2 v-if="isFullscreen" class="w-4 h-4" />
            <Maximize2 v-else class="w-4 h-4" />
          </button>
          <template v-if="editMode && !compactActions">
            <button
              type="button"
              class="iconbtn"
              title="Dupliquer"
              aria-label="Dupliquer"
              @click.stop="emitDuplicateWidget"
            >
              <Copy class="w-4 h-4" />
            </button>
            <button
              type="button"
              class="iconbtn"
              title="Reglages"
              aria-label="Reglages"
              @click.stop="emitOpenSettings"
            >
              <Settings class="w-4 h-4" />
            </button>
            <button
              type="button"
              class="iconbtn"
              title="Supprimer"
              aria-label="Supprimer"
              @click.stop="emitRemoveWidget"
            >
              <Trash2 class="w-4 h-4" />
            </button>
          </template>
          <template v-else-if="editMode">
            <button
              type="button"
              class="iconbtn iconbtn--menu"
              :aria-expanded="isActionMenuOpen ? 'true' : 'false'"
              aria-label="Plus d actions"
              title="Plus d actions"
              @click.stop="toggleActionMenu"
            >
              <MoreHorizontal class="w-4 h-4" />
            </button>
            <div
              v-if="isActionMenuOpen"
              class="widget-action-menu panzoom-exclude"
              role="menu"
              aria-label="Actions du widget"
              @pointerdown.stop
            >
              <button
                type="button"
                class="widget-action-menu__item"
                role="menuitem"
                aria-label="Dupliquer"
                @click.stop="emitDuplicateWidget"
              >
                <Copy class="w-4 h-4" />
                <span>Dupliquer</span>
              </button>
              <button
                type="button"
                class="widget-action-menu__item"
                role="menuitem"
                aria-label="Reglages"
                @click.stop="emitOpenSettings"
              >
                <Settings class="w-4 h-4" />
                <span>Reglages</span>
              </button>
              <button
                type="button"
                class="widget-action-menu__item is-danger"
                role="menuitem"
                aria-label="Supprimer"
                @click.stop="emitRemoveWidget"
              >
                <Trash2 class="w-4 h-4" />
                <span>Supprimer</span>
              </button>
            </div>
          </template>
        </div>
      </div>

      <div
        v-if="showFloatingActions"
        class="widget__floating-actions panzoom-exclude"
        @pointerdown.stop
      >
        <button
          type="button"
          class="iconbtn iconbtn--glass"
          :title="isFullscreen ? 'Quitter le plein ecran' : 'Plein ecran'"
          :aria-label="isFullscreen ? 'Quitter le plein ecran' : 'Plein ecran'"
          @click.stop="toggleFullscreen"
        >
          <Minimize2 v-if="isFullscreen" class="w-4 h-4" />
          <Maximize2 v-else class="w-4 h-4" />
        </button>
        <template v-if="editMode && !compactActions">
          <button
            type="button"
            class="iconbtn iconbtn--glass"
            title="Dupliquer"
            aria-label="Dupliquer"
            @click.stop="emitDuplicateWidget"
          >
            <Copy class="w-4 h-4" />
          </button>
          <button
            type="button"
            class="iconbtn iconbtn--glass"
            title="Reglages"
            aria-label="Reglages"
            @click.stop="emitOpenSettings"
          >
            <Settings class="w-4 h-4" />
          </button>
          <button
            type="button"
            class="iconbtn iconbtn--glass"
            title="Supprimer"
            aria-label="Supprimer"
            @click.stop="emitRemoveWidget"
          >
            <Trash2 class="w-4 h-4" />
          </button>
        </template>
        <template v-else-if="editMode">
          <button
            type="button"
            class="iconbtn iconbtn--glass iconbtn--menu"
            :aria-expanded="isActionMenuOpen ? 'true' : 'false'"
            aria-label="Plus d actions"
            title="Plus d actions"
            @click.stop="toggleActionMenu"
          >
            <MoreHorizontal class="w-4 h-4" />
          </button>
          <div
            v-if="isActionMenuOpen"
            class="widget-action-menu widget-action-menu--floating panzoom-exclude"
            role="menu"
            aria-label="Actions du widget"
            @pointerdown.stop
          >
            <button
              type="button"
              class="widget-action-menu__item"
              role="menuitem"
              aria-label="Dupliquer"
              @click.stop="emitDuplicateWidget"
            >
              <Copy class="w-4 h-4" />
              <span>Dupliquer</span>
            </button>
            <button
              type="button"
              class="widget-action-menu__item"
              role="menuitem"
              aria-label="Reglages"
              @click.stop="emitOpenSettings"
            >
              <Settings class="w-4 h-4" />
              <span>Reglages</span>
            </button>
            <button
              type="button"
              class="widget-action-menu__item is-danger"
              role="menuitem"
              aria-label="Supprimer"
              @click.stop="emitRemoveWidget"
            >
              <Trash2 class="w-4 h-4" />
              <span>Supprimer</span>
            </button>
          </div>
        </template>
      </div>

      <div
        v-if="editMode && isTextWidget && !isFullscreen"
        class="text-toolbar panzoom-exclude"
        role="toolbar"
        aria-label="Outils de texte"
        @pointerdown.stop
      >
        <select
          class="text-toolbar__select"
          :value="String(widget.props?.fontFamily ?? 'open-sans')"
          aria-label="Police du texte"
          @change="emitTextProp('fontFamily', ($event.target as HTMLSelectElement).value)"
        >
          <option v-for="font in textFonts" :key="font.value" :value="font.value">
            {{ font.label }}
          </option>
        </select>
        <input
          class="text-toolbar__size"
          type="number"
          :value="Number(widget.props?.fontSize ?? defaultFontSize)"
          :min="isTitleWidget ? 16 : 12"
          :max="620"
          aria-label="Taille du texte"
          @input="emitTextProp('fontSize', Number(($event.target as HTMLInputElement).value || defaultFontSize))"
        />
        <button
          type="button"
          class="text-toolbar__btn"
          :class="{ 'is-active': String(widget.props?.weight ?? defaultWeight) === 'bold' || String(widget.props?.weight ?? defaultWeight) === 'heavy' }"
          :aria-pressed="String(widget.props?.weight ?? defaultWeight) === 'bold' || String(widget.props?.weight ?? defaultWeight) === 'heavy'"
          aria-label="Activer gras"
          @click="toggleBold"
        >
          B
        </button>
        <div class="text-toolbar__align">
          <button
            type="button"
            class="text-toolbar__btn text-toolbar__align-toggle"
            :class="{ 'is-active': isAlignMenuOpen }"
            :aria-expanded="isAlignMenuOpen"
            aria-label="Alignement du texte"
            @click.stop="toggleAlignMenu"
          >
            <component :is="currentTextAlignOption.icon" class="w-4 h-4" />
            <ChevronDown class="text-toolbar__align-caret w-3.5 h-3.5" :class="{ 'is-open': isAlignMenuOpen }" />
          </button>
          <div
            v-if="isAlignMenuOpen"
            class="text-toolbar__align-menu"
            role="menu"
            aria-label="Options d'alignement"
            @pointerdown.stop
          >
            <button
              v-for="align in textAlignOptions"
              :key="align.value"
              type="button"
              class="text-toolbar__align-option"
              role="menuitemradio"
              :aria-checked="currentTextAlign === align.value"
              :class="{ 'is-active': currentTextAlign === align.value }"
              :title="align.title"
              :aria-label="align.title"
              @click.stop="applyTextAlign(align.value)"
            >
              <component :is="align.icon" class="w-4 h-4" />
            </button>
          </div>
        </div>
        <input
          class="text-toolbar__color"
          type="color"
          :value="String(widget.props?.color ?? defaultColor)"
          aria-label="Couleur du texte"
          @input="emitTextProp('color', ($event.target as HTMLInputElement).value)"
        />
        <span class="text-toolbar__sep" aria-hidden="true"></span>
        <div class="text-toolbar__actions">
          <button
            type="button"
            class="text-toolbar__iconbtn"
            :title="isFullscreen ? 'Quitter le plein ecran' : 'Plein ecran'"
            :aria-label="isFullscreen ? 'Quitter le plein ecran' : 'Plein ecran'"
            @click.stop="toggleFullscreen"
          >
            <Minimize2 v-if="isFullscreen" class="w-4 h-4" />
            <Maximize2 v-else class="w-4 h-4" />
          </button>
          <button
            type="button"
            class="text-toolbar__iconbtn"
            title="Dupliquer"
            aria-label="Dupliquer"
            @click.stop="$emit('duplicate')"
          >
            <Copy class="w-4 h-4" />
          </button>
          <button
            type="button"
            class="text-toolbar__iconbtn text-toolbar__delete"
            title="Suppr"
            aria-label="Supprimer ce widget"
            @click.stop="emitRemoveWidget"
          >
            <Trash2 class="w-4 h-4" />
            <span>Suppr</span>
          </button>
        </div>
      </div>

      <div
        ref="bodyEl"
        class="widget__body"
        :class="{ 'widget__body--auto': shouldAutoHeight, 'widget__body--no-header': !showHeader }"
        :style="bodyStyle"
      >
        <div class="widget__content-scale" :style="contentLayoutStyle">
          <div
            ref="contentInnerEl"
            class="widget__content-inner"
            @dblclick.stop="onWidgetBodyDoubleClick"
          >
            <component
              v-if="!isInlineTextEditing"
              :is="comp"
              :from="from"
              :to="to"
              v-bind="mergedProps"
              @view-change="onWidgetViewChange"
            />
            <div
              v-else
              ref="inlineEditorEl"
              class="text-inline-editor"
              :style="inlineEditorStyle"
              contenteditable="true"
              role="textbox"
              aria-multiline="true"
              aria-label="Edition de texte"
              @input="onInlineEditorInput"
              @keydown="onInlineEditorKeydown"
              @blur="onInlineEditorBlur"
              @paste="onInlineEditorPaste"
              @pointerdown.stop
            />
          </div>
        </div>
      </div>
      </div>

      <div v-if="editMode && !isFullscreen" class="widget__resize-overlay">
        <template v-if="widget.type !== 'textTitle' && widget.type !== 'textBlock'">
          <span class="resize-rail resize-rail--h" />
          <span class="resize-rail resize-rail--v" />
        </template>
        <button
          v-for="edge in edgeHandles"
          :key="`edge-${edge.dir}`"
          type="button"
          class="resize-edge"
          :class="`resize-edge--${edge.dir}`"
          :title="edge.title"
          :aria-label="edge.title"
          @pointerdown.stop.prevent="onResizeHandleDown(edge.dir, $event)"
        >
          <span class="resize-edge__line" aria-hidden="true"></span>
        </button>
        <button
          v-for="h in visibleHandles"
          :key="h.dir"
          type="button"
          class="resize-handle"
          :class="[`resize-handle--${h.dir}`, { 'resize-handle--corner': h.dir.length === 2 }]"
          :title="h.title"
          :aria-label="h.title"
          @pointerdown.stop.prevent="onResizeHandleDown(h.dir, $event)"
        >
          <span v-if="h.dir.length === 2" class="handle-corner-dot" aria-hidden="true"></span>
          <span v-else class="handle-dot" />
        </button>
        <button
          v-if="isTextWidget"
          type="button"
          class="text-scale-handle"
          aria-label="Agrandir ou reduire le texte"
          title="Agrandir ou reduire le texte"
          @pointerdown.stop.prevent="$emit('textScaleStart', $event)"
        />
        <div class="resize-meta" aria-hidden="true">
          <div class="resize-metrics">{{ Math.round(widget.w) }} x {{ Math.round(widget.h) }}</div>
          <div class="resize-hints">{{ resizeHintText }}</div>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, type Component, watch } from 'vue'
import {
  AlignCenter,
  AlignLeft,
  AlignRight,
  ChevronDown,
  Copy,
  Maximize2,
  Minimize2,
  MoreHorizontal,
  Settings,
  Trash2,
} from 'lucide-vue-next'
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
  canvasScale: number
  editMode: boolean
  dragArmed: boolean
  selected: boolean
  groupSelected: boolean
  textActive: boolean
  comp: Component
  from: string
  to: string
  style: Record<string, string | number>
}>()

const emit = defineEmits<{
  (e: 'duplicate'): void
  (e: 'settings'): void
  (e: 'remove'): void
  (e: 'propsPatch', patch: Record<string, unknown>): void
  (e: 'dragStart', ev: PointerEvent): void
  (e: 'activate'): void
  (e: 'textPropsChange', patch: Record<string, unknown>): void
  (e: 'textScaleStart', ev: PointerEvent): void
  (e: 'autoResize', height: number): void
  (e: 'fullscreen-change', active: boolean): void
  (e: 'resizeStart', payload: { dir: ResizeDir; event: PointerEvent }): void
}>()

const isTextWidget = computed(
  () => props.widget?.type === 'textTitle' || props.widget?.type === 'textBlock',
)
const isRoiWidget = computed(() => props.widget?.type === 'roi')
const showHeader = computed(() => !isTextWidget.value)
const showFloatingActions = computed(() => false)
const isTitleWidget = computed(() => props.widget?.type === 'textTitle')
const textFonts = [
  { label: 'Open Sans', value: 'open-sans' },
  { label: 'PT Sans', value: 'pt-sans' },
  { label: 'PT Serif', value: 'pt-serif' },
  { label: 'Roboto', value: 'roboto' },
  { label: 'Roboto Slab', value: 'roboto-slab' },
  { label: 'IBM Plex Sans', value: 'ibm-plex-sans' },
  { label: 'Georgia', value: 'georgia' },
]
const textAlignOptions: Array<{
  value: 'left' | 'center' | 'right'
  title: string
  icon: Component
}> = [
  { value: 'left', title: 'Aligner a gauche', icon: AlignLeft },
  { value: 'center', title: 'Aligner au centre', icon: AlignCenter },
  { value: 'right', title: 'Aligner a droite', icon: AlignRight },
]
const defaultFontSize = computed(() => (isTitleWidget.value ? 52 : 17))
const defaultWeight = computed(() => (isTitleWidget.value ? 'bold' : 'regular'))
const defaultColor = computed(() => (isTitleWidget.value ? '#ffffff' : '#e2e8f0'))
const liveRenderWidth = ref(0)
const liveRenderHeight = ref(0)
const TEXT_FONT_FAMILIES: Record<string, string> = {
  'open-sans': '"Open Sans", Arial, sans-serif',
  'pt-sans': '"PT Sans", Arial, sans-serif',
  'pt-serif': '"PT Serif", Georgia, serif',
  roboto: 'Roboto, Arial, sans-serif',
  'roboto-slab': '"Roboto Slab", Georgia, serif',
  'ibm-plex-sans': '"IBM Plex Sans", Arial, sans-serif',
  'ibm-plex-mono': '"IBM Plex Mono", monospace',
  georgia: 'Georgia, serif',
  arial: 'Arial, sans-serif',
  'permanent-marker': '"Permanent Marker", "Comic Sans MS", cursive',
  caveat: 'Caveat, "Comic Sans MS", cursive',
}
const inlineEditorEl = ref<HTMLElement | null>(null)
const isInlineTextEditing = ref(false)
const isAlignMenuOpen = ref(false)
const isActionMenuOpen = ref(false)

const currentTextAlign = computed<'left' | 'center' | 'right'>(() => {
  const align = String(props.widget?.props?.align ?? 'left')
  if (align === 'center' || align === 'right') return align
  return 'left'
})
const currentTextAlignOption = computed(
  () => textAlignOptions.find((align) => align.value === currentTextAlign.value) ?? textAlignOptions[0],
)

function resolveTextFontSize() {
  const fallback = Number(props.widget?.props?.fontSize ?? defaultFontSize.value) || defaultFontSize.value
  const min = isTitleWidget.value ? 16 : 12
  const max = 620
  return Math.max(min, Math.min(max, Math.round(fallback)))
}

function resolveTextLineHeight() {
  const sizeKey = String(props.widget?.props?.size ?? (isTitleWidget.value ? 'lg' : 'md'))
  if (isTitleWidget.value) {
    const map: Record<string, number> = { sm: 0.98, md: 0.96, lg: 0.94, xl: 0.9 }
    return map[sizeKey] ?? 0.94
  }
  const map: Record<string, number> = { sm: 1.6, md: 1.58, lg: 1.5, xl: 1.42 }
  return map[sizeKey] ?? 1.58
}

function resolveTextWeight() {
  const weight = String(props.widget?.props?.weight ?? defaultWeight.value)
  if (isTitleWidget.value) {
    if (weight === 'heavy') return 800
    if (weight === 'bold') return 700
    if (weight === 'semibold') return 600
    if (weight === 'medium') return 500
    if (weight === 'regular') return 400
    return 700
  }
  if (weight === 'bold') return 700
  if (weight === 'semibold') return 600
  if (weight === 'medium') return 500
  return 400
}

const inlineEditorStyle = computed(() => {
  if (!isTextWidget.value) return {}
  return {
    width: '100%',
    maxWidth: '100%',
    minHeight: '100%',
    outline: 'none',
    border: 'none',
    background: 'transparent',
    color: String(props.widget?.props?.color ?? defaultColor.value),
    textAlign: String(props.widget?.props?.align ?? 'left'),
    fontSize: `${resolveTextFontSize()}px`,
    lineHeight: String(resolveTextLineHeight()),
    fontFamily:
      TEXT_FONT_FAMILIES[String(props.widget?.props?.fontFamily ?? 'open-sans')] ??
      TEXT_FONT_FAMILIES['open-sans'],
    fontWeight: String(resolveTextWeight()),
    fontStyle: Boolean(props.widget?.props?.italic) ? 'italic' : 'normal',
    textDecoration: Boolean(props.widget?.props?.underline) ? 'underline' : 'none',
    whiteSpace: 'pre-wrap',
    overflowWrap: 'break-word',
    wordBreak: 'normal',
    hyphens: 'none',
    overflow: 'hidden',
    cursor: 'text',
    caretColor: String(props.widget?.props?.color ?? defaultColor.value),
  } as Record<string, string>
})
const textUiScale = computed(() => {
  if (!isTextWidget.value) return 1
  const raw = Number(props.widget?.props?.fontSize ?? defaultFontSize.value) || defaultFontSize.value
  const base = isTitleWidget.value ? 52 : 17
  return Math.max(1, Math.min(raw / Math.max(base, 1), 2.4))
})
const liveWidgetWidth = computed(() => {
  const live = Number(liveRenderWidth.value)
  if (Number.isFinite(live) && live > 0) return live
  const fromProps = Number(props.widget?.w ?? 0)
  return Number.isFinite(fromProps) && fromProps > 0 ? fromProps : 0
})
const actionCount = computed(() => (props.editMode ? 4 : 1))
const actionUiScale = computed(() => {
  if (!isTextWidget.value) return 1
  const width = liveWidgetWidth.value
  if (!Number.isFinite(width) || width <= 0) return 1
  const reserveForTitle = isTextWidget.value ? 92 : 132
  const available = Math.max(width - reserveForTitle, 42)
  const buttonBase = isTextWidget.value ? 22 : 34
  const gapBase = isTextWidget.value ? 6 : 8
  const count = Math.max(actionCount.value, 1)
  const needed = count * buttonBase + Math.max(0, count - 1) * gapBase
  if (!needed) return 1
  return Math.max(0.72, Math.min(1, available / needed))
})
const fullHeaderActionsWidth = computed(() => {
  const buttonSize = 34
  const gap = 8
  const count = props.editMode ? 4 : 1
  return count * buttonSize + Math.max(0, count - 1) * gap
})
const headerStyle = computed(() => {
  if (!isTextWidget.value) {
    return {
      '--action-ui-scale': String(actionUiScale.value),
    } as Record<string, string>
  }
  return {
    '--text-ui-scale': String(textUiScale.value),
    '--action-ui-scale': String(actionUiScale.value),
  } as Record<string, string>
})
const rootTabIndex = computed(() => (props.editMode ? 0 : -1))
const compactActions = computed(() => {
  if (!showHeader.value) return false
  if (!props.editMode) return false
  const width = liveWidgetWidth.value
  if (!Number.isFinite(width) || width <= 0) return false
  // Reserve a minimal space so title stays readable and actions remain clickable.
  const minTitleSpace = 84
  const headerPadding = 26
  return width < fullHeaderActionsWidth.value + minTitleSpace + headerPadding
})
const widgetState = computed(() => {
  if (!props.editMode) return 'idle'
  if (isInlineTextEditing.value) return 'editing'
  if (props.dragArmed) return 'dragging'
  if (props.groupSelected) return 'selected-group'
  if (props.selected) return 'selected'
  return 'hoverable'
})
const shouldAutoHeight = computed(
  () => isTextWidget.value && props.widget?.props?.autoHeight !== false,
)
const headerOffset = computed(() => (showHeader.value ? 44 : 0))
const widgetDef = computed(() => getWidgetDef(props.widget?.type))

const widgetRenderWidth = computed(() => {
  const live = Number(liveRenderWidth.value)
  if (Number.isFinite(live) && live > 0) return live
  return Math.max(Number(props.widget?.w ?? 0), 1)
})

const widgetRenderHeight = computed(() => {
  const live = Number(liveRenderHeight.value)
  if (Number.isFinite(live) && live > 0) return live
  return Math.max(Number((props.widget?.h ?? 0) - headerOffset.value), 1)
})

const contentSizing = computed(() => {
  const currentWidth = Math.max(widgetRenderWidth.value, 1)
  const currentHeight = Math.max(widgetRenderHeight.value, 1)
  const baseWidth = Math.max(Number(widgetDef.value?.defaultSize?.w ?? props.widget?.w ?? 0), 1)
  const baseHeight = Math.max(
    Number((widgetDef.value?.defaultSize?.h ?? props.widget?.h ?? 0) - headerOffset.value),
    1,
  )

  return {
    renderWidth: currentWidth,
    renderHeight: currentHeight,
    baseWidth,
    baseHeight,
  }
})

const surfaceScale = computed(() => {
  if (isTextWidget.value) {
    return {
      scaleX: 1,
      scaleY: 1,
    }
  }
  const sizing = contentSizing.value
  const renderTotalHeight = sizing.renderHeight + headerOffset.value
  const baseTotalHeight = Math.max(sizing.baseHeight + headerOffset.value, 1)
  return {
    scaleX: Math.max(0.2, Math.min(6, sizing.renderWidth / Math.max(sizing.baseWidth, 1))),
    scaleY: Math.max(0.2, Math.min(6, renderTotalHeight / baseTotalHeight)),
  }
})

const surfaceLayoutStyle = computed(() => {
  if (isTextWidget.value) return {}
  const sizing = contentSizing.value
  const scale = surfaceScale.value
  return {
    width: `${Math.round(sizing.baseWidth)}px`,
    height: `${Math.round(sizing.baseHeight + headerOffset.value)}px`,
    transform: `scale(${scale.scaleX}, ${scale.scaleY})`,
    transformOrigin: 'top left',
  }
})

const mergedProps = computed(() => {
  const p = { ...(props.widget?.props ?? {}) } as Record<string, unknown>
  const sizing = contentSizing.value
  const logicalWidth = isTextWidget.value ? sizing.renderWidth : sizing.baseWidth
  const logicalHeight = isTextWidget.value ? sizing.renderHeight : sizing.baseHeight
  delete p.from
  delete p.to
  p.widgetWidth = logicalWidth
  p.widgetHeight = logicalHeight
  p.widgetBaseWidth = sizing.baseWidth
  p.widgetBaseHeight = sizing.baseHeight
  p.widgetRenderWidth = sizing.renderWidth
  p.widgetRenderHeight = sizing.renderHeight
  p.canvasEditMode = props.editMode
  return p
})

const bodyStyle = computed(() => {
  const rawPadding = String(props.widget?.props?.padding ?? '').trim()
  const paddingMap: Record<string, string> = {
    none: '0px',
    sm: '8px',
    md: '12px',
    lg: '18px',
    xl: '24px',
  }
  if (!isTextWidget.value) {
    if (rawPadding && rawPadding in paddingMap) {
      return { padding: paddingMap[rawPadding] }
    }
    return { padding: '0px' }
  }
  return {
    padding: paddingMap[rawPadding] ?? (props.widget?.props?.tight ? '8px' : '12px'),
  }
})

const contentInnerEl = ref<HTMLElement | null>(null)

const contentLayoutStyle = computed(() => {
  if (!isTextWidget.value) {
    // Keep a stable widget structure at every canvas/page zoom level.
    return {}
  }
  return {
    width: '100%',
    height: shouldAutoHeight.value ? 'auto' : '100%',
    transform: 'none',
    transformOrigin: 'top left',
  }
})

const dotStyle = computed(() => {
  const tone = getCategoryColor(widgetDef.value?.category)
  return {
    background: tone.color,
    boxShadow: `0 0 0 2px ${tone.glow}`,
  }
})

function onPointerDown(e: PointerEvent) {
  if (!props.editMode) return
  if (e.button !== 0) return
  const target = e.target as HTMLElement | null
  if (target?.closest('[contenteditable="true"], .text-inline-editor')) return
  if (isInlineTextEditing.value && isTextWidget.value) return
  if (target?.closest('.widget-action-menu')) return
  if (target?.closest('button, a, input, select, textarea, .iconbtn')) return
  if (target?.closest('.text-toolbar')) return
  if (target?.closest('.resize-handle, .resize-edge, .text-scale-handle')) return
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

const visibleHandles = computed(() => {
  return handles.filter((handle) => ['ne', 'nw', 'se', 'sw'].includes(handle.dir))
})

const edgeHandles = computed(() => {
  // Text widgets keep Canva-like side rails (left/right) for width resize.
  if (isTextWidget.value) {
    return handles.filter((handle) => handle.dir === 'e' || handle.dir === 'w')
  }
  // Other widgets: corner handles only.
  return []
})

const resizeHintText = computed(() =>
  isTextWidget.value
    ? 'Coins: taille du texte'
    : isRoiWidget.value
      ? 'Coins: zoom uniforme | Alt: centre'
      : 'Coins: zoom uniforme | Alt: centre',
)

function onResizeHandleDown(dir: ResizeDir, event: PointerEvent) {
  if (!props.editMode) return
  event.preventDefault()
  event.stopPropagation()
  emit('resizeStart', { dir, event })
}

function emitTextProp(key: string, value: unknown) {
  emit('textPropsChange', { [key]: value })
}

function onWidgetViewChange(nextView: unknown) {
  if (typeof nextView !== 'string' || !nextView.trim()) return
  emit('propsPatch', { view: nextView })
}

function closeActionMenu() {
  isActionMenuOpen.value = false
}

function toggleActionMenu() {
  if (!props.editMode || isTextWidget.value) return
  isActionMenuOpen.value = !isActionMenuOpen.value
}

function emitDuplicateWidget() {
  closeActionMenu()
  emit('duplicate')
}

function emitOpenSettings() {
  closeActionMenu()
  emit('settings')
}

function toggleBold() {
  const current = String(props.widget?.props?.weight ?? defaultWeight.value)
  const offValue = 'regular'
  emitTextProp('weight', current === 'bold' || current === 'heavy' ? offValue : 'bold')
}

function toggleAlignMenu() {
  if (!props.editMode || !isTextWidget.value) return
  emit('activate')
  isAlignMenuOpen.value = !isAlignMenuOpen.value
}

function applyTextAlign(value: 'left' | 'center' | 'right') {
  emitTextProp('align', value)
  isAlignMenuOpen.value = false
}

function placeCaretAtEnd(el: HTMLElement) {
  const selection = window.getSelection()
  if (!selection) return
  const range = document.createRange()
  range.selectNodeContents(el)
  range.collapse(false)
  selection.removeAllRanges()
  selection.addRange(range)
}

function readInlineEditorContent(el: HTMLElement) {
  let content = (el.innerText ?? '').replace(/\r\n?/g, '\n')
  if (!content.includes('\n')) {
    const html = el.innerHTML ?? ''
    if (/(<br\s*\/?>|<\/div>\s*<div|<\/p>\s*<p|<\/li>\s*<li)/i.test(html)) {
      const normalizedHtml = html
        .replace(/<br\s*\/?>/gi, '\n')
        .replace(/<\/(div|p|li)>\s*<(div|p|li)[^>]*>/gi, '\n')
        .replace(/<\/?(div|p|li)[^>]*>/gi, '')
      const temp = document.createElement('div')
      temp.innerHTML = normalizedHtml
      content = (temp.textContent ?? '').replace(/\r\n?/g, '\n')
    }
  }
  content = content.replace(/\u200b/g, '')
  if (content.endsWith('\n')) {
    content = content.slice(0, -1)
  }
  return content
}

function syncInlineEditorContent() {
  if (!isInlineTextEditing.value) return
  const el = inlineEditorEl.value
  if (!el) return
  const content = readInlineEditorContent(el)
  if (String(props.widget?.props?.content ?? '') === content) return
  emitTextProp('content', content)
}

function startInlineTextEdit() {
  if (!props.editMode || !isTextWidget.value) return
  emit('activate')
  isInlineTextEditing.value = true
  nextTick(() => {
    const el = inlineEditorEl.value
    if (!el) return
    const content = String(props.widget?.props?.content ?? '')
    if ((el.innerText ?? '').replace(/\r\n/g, '\n') !== content) {
      el.innerText = content
    }
    el.focus()
    placeCaretAtEnd(el)
  })
}

function stopInlineTextEdit() {
  if (!isInlineTextEditing.value) return
  syncInlineEditorContent()
  isInlineTextEditing.value = false
}

function emitRemoveWidget() {
  isAlignMenuOpen.value = false
  closeActionMenu()
  stopInlineTextEdit()
  emit('remove')
}

function onWidgetBodyDoubleClick() {
  if (isTextWidget.value) {
    startInlineTextEdit()
    return
  }
  if (!props.editMode || isFullscreen.value) return
  emitOpenSettings()
}

function onInlineEditorInput(event: Event) {
  const target = event.target as HTMLElement | null
  if (!target) return
  const content = readInlineEditorContent(target)
  emitTextProp('content', content)
}

function onInlineEditorBlur() {
  syncInlineEditorContent()
  stopInlineTextEdit()
}

function onInlineEditorPaste(event: ClipboardEvent) {
  event.preventDefault()
  const text = event.clipboardData?.getData('text/plain') ?? ''
  document.execCommand('insertText', false, text)
}

function onInlineEditorKeydown(event: KeyboardEvent) {
  event.stopPropagation()
  if (event.key === 'Escape') {
    event.preventDefault()
    stopInlineTextEdit()
    root.value?.focus()
  }
}

function onWindowPointerDown(event: PointerEvent) {
  const target = event.target as HTMLElement | null
  if (isInlineTextEditing.value) {
    const insideEditor = Boolean(target?.closest('[contenteditable="true"], .text-inline-editor'))
    if (!insideEditor) {
      stopInlineTextEdit()
    }
  }
  if (isAlignMenuOpen.value && !target?.closest('.text-toolbar__align')) {
    isAlignMenuOpen.value = false
  }
  if (isActionMenuOpen.value && !target?.closest('.widget__actions--compact')) {
    isActionMenuOpen.value = false
  }
}

const root = ref<HTMLElement | null>(null)
const bodyEl = ref<HTMLElement | null>(null)
const isFullscreen = ref(false)

const fullscreenStyle = {
  position: 'fixed',
  inset: '12px',
  width: 'calc(100vw - 24px)',
  height: 'calc(100vh - 24px)',
  zIndex: 40, // keep below global header (z-50) so navigation stays usable
}

function enterFullscreen() {
  if (isFullscreen.value) return
  isFullscreen.value = true
  document.body.classList.add('widget-fullscreen-open')
  emit('fullscreen-change', true)
}

function exitFullscreen() {
  if (!isFullscreen.value) return
  isFullscreen.value = false
  document.body.classList.remove('widget-fullscreen-open')
  emit('fullscreen-change', false)
}

function toggleFullscreen() {
  if (isFullscreen.value) {
    exitFullscreen()
    return
  }
  enterFullscreen()
}

defineExpose({ root })

let resizeRaf: number | null = null
let widgetSizeRaf: number | null = null
let widgetSizeRo: ResizeObserver | null = null
let mo: MutationObserver | null = null
let ro: ResizeObserver | null = null
let postInteractionRaf: number | null = null

function isInteractionLocked() {
  const el = root.value
  if (!el) return false
  return el.classList.contains('is-resizing') || el.classList.contains('is-dragging')
}

function schedulePostInteractionRefresh() {
  if (postInteractionRaf) return
  postInteractionRaf = requestAnimationFrame(() => {
    postInteractionRaf = null
    if (isInteractionLocked()) {
      schedulePostInteractionRefresh()
      return
    }
    scheduleWidgetSizeMeasure()
    scheduleAutoResize()
  })
}

function clearWidgetSizeObserver() {
  if (widgetSizeRaf) {
    cancelAnimationFrame(widgetSizeRaf)
    widgetSizeRaf = null
  }
  if (postInteractionRaf) {
    cancelAnimationFrame(postInteractionRaf)
    postInteractionRaf = null
  }
  if (widgetSizeRo) {
    widgetSizeRo.disconnect()
    widgetSizeRo = null
  }
}

function applyLiveRenderSize(width: number, height: number) {
  const epsilon = isTextWidget.value ? 4 : 2
  const nextWidth = Math.max(1, Math.round(width))
  const nextHeight = Math.max(1, Math.round(height - headerOffset.value))

  if (Math.abs(nextWidth - liveRenderWidth.value) >= epsilon) {
    liveRenderWidth.value = nextWidth
  }
  if (Math.abs(nextHeight - liveRenderHeight.value) >= epsilon) {
    liveRenderHeight.value = nextHeight
  }
}

function measureWidgetSize() {
  if (isInteractionLocked()) {
    schedulePostInteractionRefresh()
    return
  }
  const el = root.value
  if (!el) return
  const width = Number(el.offsetWidth)
  const height = Number(el.offsetHeight)
  if (!Number.isFinite(width) || !Number.isFinite(height) || width <= 0 || height <= 0) return
  applyLiveRenderSize(width, height)
}

function scheduleWidgetSizeMeasure() {
  if (widgetSizeRaf) return
  widgetSizeRaf = requestAnimationFrame(() => {
    widgetSizeRaf = null
    measureWidgetSize()
  })
}

function ensureWidgetSizeObserver() {
  clearWidgetSizeObserver()
  if (!root.value || !globalThis.ResizeObserver) {
    scheduleWidgetSizeMeasure()
    return
  }
  widgetSizeRo = new ResizeObserver(() => {
    if (isInteractionLocked()) {
      schedulePostInteractionRefresh()
      return
    }
    scheduleWidgetSizeMeasure()
  })
  widgetSizeRo.observe(root.value)
  scheduleWidgetSizeMeasure()
}

function clearAutoResizeObservers() {
  if (mo) {
    mo.disconnect()
    mo = null
  }
  if (ro) {
    ro.disconnect()
    ro = null
  }
}

function scheduleAutoResize() {
  if (!shouldAutoHeight.value) return
  if (isInteractionLocked()) {
    schedulePostInteractionRefresh()
    return
  }
  if (resizeRaf) return
  resizeRaf = requestAnimationFrame(() => {
    resizeRaf = null
    const body = bodyEl.value
    const inner = contentInnerEl.value
    if (!body || !inner) return
    const bodyStyle = window.getComputedStyle(body)
    const padTop = parseFloat(bodyStyle.paddingTop || '0') || 0
    const padBottom = parseFloat(bodyStyle.paddingBottom || '0') || 0
    const desired = Math.ceil(inner.scrollHeight + padTop + padBottom + headerOffset.value)
    if (!Number.isFinite(desired) || desired <= 0) return
    if (Math.abs(desired - props.widget.h) >= 4) {
      emit('autoResize', desired)
    }
  })
}

function ensureAutoResizeObservers() {
  clearAutoResizeObservers()
  if (!shouldAutoHeight.value) return
  if (!bodyEl.value || !contentInnerEl.value) return
  mo = new MutationObserver(() => scheduleAutoResize())
  mo.observe(contentInnerEl.value, { childList: true, subtree: true, characterData: true })
  if (globalThis.ResizeObserver) {
    ro = new ResizeObserver(() => scheduleAutoResize())
    ro.observe(contentInnerEl.value)
  }
}

onMounted(() => {
  nextTick(() => {
    ensureWidgetSizeObserver()
    ensureAutoResizeObservers()
    scheduleWidgetSizeMeasure()
    scheduleAutoResize()
  })
  window.addEventListener('keydown', onKeydown)
  window.addEventListener('pointerdown', onWindowPointerDown, true)
})

watch(
  () => [props.widget?.props?.autoHeight, isTextWidget.value],
  () => {
    ensureAutoResizeObservers()
    scheduleAutoResize()
  },
)

watch(
  () => [props.widget?.w, props.widget?.h, props.widget?.id, isFullscreen.value, headerOffset.value],
  () => {
    scheduleWidgetSizeMeasure()
  },
)

watch(
  () => [props.widget?.props?.content, props.widget?.w, props.widget?.h],
  () => {
    if (!shouldAutoHeight.value) return
    scheduleAutoResize()
  },
  { deep: false },
)

watch(
  () => [props.editMode, props.textActive, isTextWidget.value, props.widget?.id],
  ([editMode, textActive, textWidget]) => {
    if (!editMode || !textActive || !textWidget) {
      stopInlineTextEdit()
      isAlignMenuOpen.value = false
    }
  },
)

watch(
  () => [props.editMode, props.selected, props.widget?.id],
  ([editMode, selected]) => {
    if (!editMode || !selected) {
      closeActionMenu()
    }
  },
)

watch(compactActions, (compact) => {
  if (!compact) closeActionMenu()
})

onBeforeUnmount(() => {
  if (resizeRaf) cancelAnimationFrame(resizeRaf)
  if (postInteractionRaf) cancelAnimationFrame(postInteractionRaf)
  clearWidgetSizeObserver()
  clearAutoResizeObservers()
  window.removeEventListener('keydown', onKeydown)
  window.removeEventListener('pointerdown', onWindowPointerDown, true)
  exitFullscreen()
})

function onKeydown(e: KeyboardEvent) {
  if (e.key !== 'Escape') return
  if (isActionMenuOpen.value) {
    closeActionMenu()
    return
  }
  if (isAlignMenuOpen.value) {
    isAlignMenuOpen.value = false
    return
  }
  if (isInlineTextEditing.value) {
    stopInlineTextEdit()
    return
  }
  exitFullscreen()
}

function onRootKeydown(event: KeyboardEvent) {
  if (!props.editMode) return
  if (event.key === 'Enter' || event.key === 'F2') {
    const target = event.target as HTMLElement | null
    if (target?.closest('input, textarea, select, [contenteditable="true"]')) return
    event.preventDefault()
    event.stopPropagation()
    if (isTextWidget.value) {
      startInlineTextEdit()
      return
    }
    emitOpenSettings()
  }
}
</script>

<style scoped>
.widget {
  position: absolute;
  border-radius: 18px;
  overflow: visible;
  background: rgba(10, 15, 26, 0.88);
  border: 1px solid rgba(148, 163, 184, 0.2);
  box-shadow: 0 4px 14px rgba(2, 6, 23, 0.18);
  contain: layout paint;
  touch-action: none;
  outline: none;
  transform-origin: top left;
}
.widget[data-edit='true'] {
  contain: layout;
}
.widget:focus-visible {
  box-shadow:
    0 0 0 1px rgba(56, 189, 248, 0.92),
    0 0 0 3px rgba(56, 189, 248, 0.18),
    0 8px 18px rgba(2, 6, 23, 0.24);
}
.widget__surface {
  width: 100%;
  height: 100%;
  border-radius: inherit;
  overflow: hidden;
  background: inherit;
}
.widget--text {
  background: transparent;
  border-color: transparent;
  box-shadow: none;
  contain: layout;
  --text-accent: rgba(59, 130, 246, 1);
  --text-accent-soft: rgba(59, 130, 246, 0.18);
  --text-panel-bg: linear-gradient(180deg, rgba(15, 23, 42, 0.96), rgba(7, 12, 24, 0.94));
  --text-panel-border: rgba(148, 163, 184, 0.34);
  --text-control-bg: rgba(15, 23, 42, 0.76);
  --text-control-border: rgba(148, 163, 184, 0.28);
}
.widget--text .widget__surface {
  overflow: visible;
  background: transparent;
}
.widget--text .widget__body {
  background: transparent;
  overflow: hidden;
  height: 100%;
}
.widget--text::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 14px;
  border: 1px solid rgba(96, 165, 250, 0.92);
  pointer-events: none;
  opacity: 0;
  box-shadow:
    inset 0 0 0 1px rgba(191, 219, 254, 0.22),
    0 0 0 2px rgba(59, 130, 246, 0.12);
  transition:
    opacity 120ms ease,
    border-color 120ms ease,
    box-shadow 120ms ease;
}
.widget--text[data-text-active='true']::before,
.widget--text.is-selected::before,
.widget--text.is-resizing::before {
  opacity: 1;
  border-color: rgba(59, 130, 246, 1);
  box-shadow:
    inset 0 0 0 1px rgba(191, 219, 254, 0.28),
    0 0 0 2px rgba(59, 130, 246, 0.16),
    0 14px 30px rgba(15, 23, 42, 0.26);
}
.text-toolbar {
  position: absolute;
  top: -78px;
  left: 50%;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 7px 9px;
  border-radius: 15px;
  border: 1px solid var(--text-panel-border);
  background: var(--text-panel-bg);
  backdrop-filter: blur(8px);
  box-shadow:
    0 14px 30px rgba(2, 6, 23, 0.44),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  z-index: 80;
  max-width: min(1080px, calc(100vw - 20px));
  min-height: 40px;
  overflow-x: auto;
  overflow-y: visible;
  white-space: nowrap;
  opacity: 0;
  pointer-events: none;
  transform: translate(-50%, 4px) scale(0.98);
  transition:
    opacity 130ms ease,
    transform 130ms ease;
}
.text-toolbar::-webkit-scrollbar {
  height: 6px;
}
.text-toolbar::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.42);
}
.widget--text[data-text-active='true'] .text-toolbar,
.widget--text.is-resizing .text-toolbar {
  opacity: 1;
  pointer-events: auto;
  transform: translate(-50%, 0) scale(1);
}
.text-toolbar__select,
.text-toolbar__size {
  height: 32px;
  border-radius: 10px;
  border: 1px solid var(--text-control-border);
  background: var(--text-control-bg);
  color: rgba(241, 245, 249, 0.96);
  padding: 0 10px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
}
.text-toolbar__select {
  min-width: 132px;
}
.text-toolbar__size {
  width: 68px;
  font-weight: 600;
  text-align: center;
}
.text-toolbar__btn {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  border: 1px solid var(--text-control-border);
  background: var(--text-control-bg);
  color: rgba(241, 245, 249, 0.96);
  font-weight: 700;
  cursor: pointer;
  transition:
    border-color 140ms ease,
    background 140ms ease,
    transform 140ms ease;
}
.text-toolbar__btn:hover {
  border-color: rgba(147, 197, 253, 0.66);
  background: rgba(30, 41, 59, 0.94);
  transform: translateY(-1px);
}
.text-toolbar__btn.is-active {
  border-color: rgba(96, 165, 250, 1);
  background: rgba(59, 130, 246, 0.28);
  box-shadow:
    inset 0 1px 0 rgba(219, 234, 254, 0.2),
    0 0 0 1px rgba(37, 99, 235, 0.3);
}
.text-toolbar__align {
  position: relative;
  display: inline-flex;
  align-items: center;
}
.text-toolbar__align-toggle {
  width: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}
.text-toolbar__align-caret {
  opacity: 0.78;
  transition: transform 120ms ease;
}
.text-toolbar__align-caret.is-open {
  transform: rotate(180deg);
}
.text-toolbar__align-menu {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px;
  border-radius: 12px;
  border: 1px solid var(--text-panel-border);
  background: linear-gradient(180deg, rgba(9, 16, 31, 0.98), rgba(8, 12, 22, 0.95));
  box-shadow:
    0 16px 24px rgba(2, 6, 23, 0.52),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
  z-index: 36;
}
.text-toolbar__align-option {
  width: 34px;
  height: 32px;
  border-radius: 9px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: rgba(15, 23, 42, 0.75);
  color: rgba(241, 245, 249, 0.95);
  display: grid;
  place-items: center;
  cursor: pointer;
  transition:
    border-color 130ms ease,
    background 130ms ease,
    transform 130ms ease;
}
.text-toolbar__align-option:hover {
  border-color: rgba(147, 197, 253, 0.66);
  background: rgba(30, 41, 59, 0.94);
  transform: translateY(-1px);
}
.text-toolbar__align-option.is-active {
  border-color: rgba(96, 165, 250, 0.96);
  background: rgba(59, 130, 246, 0.28);
  box-shadow:
    inset 0 1px 0 rgba(219, 234, 254, 0.2),
    0 0 0 1px rgba(37, 99, 235, 0.28);
}
.text-toolbar__color {
  width: 32px;
  height: 32px;
  padding: 2px;
  border: 1px solid var(--text-control-border);
  border-radius: 10px;
  background: var(--text-control-bg);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
  cursor: pointer;
}
.text-toolbar__color::-webkit-color-swatch-wrapper {
  padding: 0;
}
.text-toolbar__color::-webkit-color-swatch {
  border: none;
  border-radius: 7px;
}
.text-toolbar__color::-moz-color-swatch {
  border: none;
  border-radius: 7px;
}
.text-toolbar__iconbtn {
  min-width: 32px;
  height: 32px;
  border-radius: 10px;
  border: 1px solid var(--text-control-border);
  background: var(--text-control-bg);
  color: rgba(241, 245, 249, 0.95);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition:
    border-color 140ms ease,
    background 140ms ease,
    transform 140ms ease;
}
.text-toolbar__iconbtn:hover {
  border-color: rgba(147, 197, 253, 0.66);
  background: rgba(30, 41, 59, 0.94);
  transform: translateY(-1px);
}
.text-toolbar__sep {
  width: 1px;
  height: 22px;
  border-radius: 999px;
  background: linear-gradient(
    180deg,
    rgba(148, 163, 184, 0.12),
    rgba(148, 163, 184, 0.42),
    rgba(148, 163, 184, 0.12)
  );
  margin: 0 2px;
  flex: 0 0 auto;
}
.text-toolbar__actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding-left: 1px;
}
.text-toolbar__delete {
  gap: 5px;
  padding: 0 9px;
}
.text-toolbar__delete span {
  font-size: 0.73rem;
  font-weight: 650;
  letter-spacing: 0.01em;
}
.widget__resize-overlay {
  inset: -14px;
  z-index: 30;
}
.resize-edge--e,
.resize-edge--w {
  top: 18px;
  bottom: 18px;
  width: 36px;
  border-radius: 999px;
  background: linear-gradient(
    180deg,
    rgba(59, 130, 246, 0),
    rgba(59, 130, 246, 0.08) 50%,
    rgba(59, 130, 246, 0)
  );
}
.resize-edge--e {
  right: -14px;
}
.resize-edge--w {
  left: -14px;
}
.resize-edge--e .resize-edge__line,
.resize-edge--w .resize-edge__line {
  top: 16px;
  bottom: 16px;
  width: 4px;
  left: 16px;
  border-radius: 999px;
  background: linear-gradient(
    180deg,
    rgba(248, 250, 252, 1),
    rgba(226, 232, 240, 1)
  );
  box-shadow:
    0 0 0 1px rgba(59, 130, 246, 0.22),
    0 0 8px rgba(96, 165, 250, 0.22),
    inset 0 0 0 1px rgba(255, 255, 255, 0.5);
}
.widget--text[data-edit='true'] .resize-edge {
  opacity: 0;
}
.widget--text .text-scale-handle {
  display: none;
  position: absolute;
  right: -16px;
  bottom: -16px;
  width: 30px;
  height: 30px;
  padding: 0;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.96);
  background: rgba(255, 255, 255, 0.98);
  box-shadow:
    0 10px 20px rgba(2, 6, 23, 0.32),
    0 0 0 5px rgba(99, 102, 241, 0.12);
  pointer-events: auto;
  appearance: none;
  cursor: nwse-resize;
  transition:
    transform 140ms ease,
    box-shadow 140ms ease;
}
.widget--text .text-scale-handle:hover {
  transform: scale(1.06);
}
.widget--text .iconbtn {
  width: calc(22px * var(--action-ui-scale, 1));
  height: calc(22px * var(--action-ui-scale, 1));
  border-radius: calc(8px * var(--action-ui-scale, 1));
}
.widget--text .widget__actions {
  gap: calc(6px * var(--action-ui-scale, 1));
}
.widget--text .iconbtn :deep(svg) {
  width: calc(14px * var(--action-ui-scale, 1));
  height: calc(14px * var(--action-ui-scale, 1));
}
.widget--text .title {
  font-size: 0.72rem;
}
.widget--text .drag-grip {
  width: 10px;
  height: 10px;
}
.widget--text:hover,
.widget--text.drag-armed,
.widget--text.is-resizing,
.widget--text.is-dragging {
  box-shadow: none;
  border-color: transparent;
}
.widget--text::after,
.widget--text:hover::after,
.widget--text.drag-armed::after,
.widget--text.is-resizing::after,
.widget--text.is-dragging::after {
  box-shadow: none;
  opacity: 0;
}
.widget--text .resize-meta {
  top: -34px;
}
.widget--roi .resize-edge__line {
  background: rgba(16, 185, 129, 0.5);
}
.widget--roi .resize-edge:hover .resize-edge__line,
.widget--roi .resize-edge:focus-visible .resize-edge__line,
.widget--roi.is-resizing .resize-edge__line {
  background: rgba(5, 150, 105, 0.9);
}
.widget--roi .resize-handle--corner {
  --handle-size: 30px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(236, 253, 245, 1), rgba(209, 250, 229, 1));
  border: 1.5px solid rgba(16, 185, 129, 0.94);
  box-shadow:
    0 3px 10px rgba(2, 6, 23, 0.28),
    0 0 0 3px rgba(16, 185, 129, 0.22);
}
.widget--roi .handle-corner-dot {
  width: 5px;
  height: 5px;
  background: rgba(5, 150, 105, 0.96);
  box-shadow: 0 0 0 1px rgba(236, 253, 245, 0.95);
}
.widget::after {
  content: none;
}
.widget.is-fullscreen {
  position: fixed;
  top: max(10px, env(safe-area-inset-top, 0px) + 6px);
  right: max(10px, env(safe-area-inset-right, 0px) + 6px);
  bottom: max(10px, env(safe-area-inset-bottom, 0px) + 6px);
  left: max(10px, env(safe-area-inset-left, 0px) + 6px);
  width: auto !important;
  height: auto !important;
  z-index: 40; /* header is z-50 -> nav remains clickable even in fullscreen */
  cursor: default;
}
.widget.is-dragging {
  cursor: grabbing;
  transition: none;
  box-shadow: 0 5px 16px rgba(2, 6, 23, 0.24);
  will-change: transform;
}
.widget.is-resizing {
  box-shadow: 0 5px 16px rgba(2, 6, 23, 0.24);
  will-change: transform;
}
.widget.is-resizing .widget__surface {
  outline: 1px solid rgba(129, 140, 248, 0.82);
  outline-offset: -1px;
}
.widget.is-resizing .widget__content-inner {
  pointer-events: none;
}
.widget:hover {
  box-shadow: 0 6px 18px rgba(2, 6, 23, 0.24);
}
.widget.is-selected {
  box-shadow:
    0 0 0 1px rgba(56, 189, 248, 0.92),
    0 0 0 3px rgba(56, 189, 248, 0.18),
    0 6px 18px rgba(2, 6, 23, 0.24);
}
.widget.drag-armed {
  box-shadow: 0 6px 18px rgba(2, 6, 23, 0.24);
  cursor: grab;
}

.widget__header {
  height: 44px;
  display: flex;
  align-items: center;
  padding: 0 10px 0 12px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
  background: rgba(8, 13, 24, 0.78);
  position: relative;
  z-index: 18;
}

.widget__title {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex: 1 1 auto;
  min-width: 0;
  user-select: none;
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
}
.title {
  color: rgba(241, 245, 249, 0.98);
  font-weight: 650;
  font-size: 0.88rem;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.drag-grip {
  width: 14px;
  height: 14px;
  border-radius: 6px;
  opacity: 0.52;
  background:
    radial-gradient(circle, rgba(226, 232, 240, 0.62) 1px, transparent 1.5px) 0 0 / 6px 6px;
}

.widget__actions {
  margin-left: auto;
  display: inline-flex;
  flex: 0 0 auto;
  gap: calc(8px * var(--action-ui-scale, 1));
  position: relative;
  z-index: 26;
  opacity: 0;
  transform: translateY(-2px);
  pointer-events: none;
  transition:
    opacity 140ms ease,
    transform 140ms ease;
}

.widget[data-edit='true']:hover .widget__actions,
.widget[data-edit='false']:hover .widget__actions,
.widget.is-selected .widget__actions,
.widget.is-resizing .widget__actions,
.widget:focus-within .widget__actions,
.widget.is-fullscreen .widget__actions {
  opacity: 1;
  transform: translateY(0);
  pointer-events: auto;
}
.widget__actions--compact {
  gap: 6px;
}
.widget__floating-actions {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 32;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0;
  border-radius: 0;
  background: transparent;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
  box-shadow: none;
  opacity: 0;
  transform: translateY(-2px);
  pointer-events: none;
  transition:
    opacity 140ms ease,
    transform 140ms ease;
}
.iconbtn {
  width: calc(34px * var(--action-ui-scale, 1));
  height: calc(34px * var(--action-ui-scale, 1));
  border-radius: calc(11px * var(--action-ui-scale, 1));
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: rgba(15, 23, 42, 0.52);
  color: rgba(226, 232, 240, 0.92);
  display: grid;
  place-items: center;
  transition:
    border-color 160ms ease,
    background 160ms ease,
    opacity 160ms ease;
}
.iconbtn:hover {
  border-color: rgba(148, 163, 184, 0.42);
  background: rgba(30, 41, 59, 0.68);
}
.iconbtn :deep(svg) {
  width: calc(16px * var(--action-ui-scale, 1));
  height: calc(16px * var(--action-ui-scale, 1));
}
.iconbtn--menu {
  position: relative;
}
.widget-action-menu--floating {
  top: calc(100% + 6px);
}
.widget-action-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 162px;
  padding: 6px;
  border-radius: 11px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: rgba(9, 15, 28, 0.96);
  box-shadow: 0 14px 24px rgba(2, 6, 23, 0.4);
  display: grid;
  gap: 4px;
  z-index: 62;
}
.widget-action-menu__item {
  border: 1px solid rgba(148, 163, 184, 0);
  background: rgba(15, 23, 42, 0);
  color: rgba(241, 245, 249, 0.96);
  border-radius: 9px;
  min-height: 32px;
  padding: 0 9px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 0.76rem;
  font-weight: 600;
}
.widget-action-menu__item:hover {
  border-color: rgba(148, 163, 184, 0.34);
  background: rgba(51, 65, 85, 0.36);
}
.widget-action-menu__item.is-danger {
  color: rgba(254, 205, 211, 0.96);
}
.widget-action-menu__item.is-danger:hover {
  border-color: rgba(248, 113, 113, 0.34);
  background: rgba(127, 29, 29, 0.28);
}
.widget-action-menu__item:focus-visible {
  outline: 2px solid rgba(56, 189, 248, 0.9);
  outline-offset: 2px;
}
.iconbtn:focus-visible,
.text-toolbar__btn:focus-visible,
.text-toolbar__iconbtn:focus-visible,
.text-toolbar__select:focus-visible,
.text-toolbar__size:focus-visible,
.text-toolbar__color:focus-visible,
.resize-edge:focus-visible,
.resize-handle:focus-visible,
.text-scale-handle:focus-visible {
  outline: 2px solid rgba(56, 189, 248, 0.9);
  outline-offset: 2px;
}

.widget[data-edit='true'] .drag-handle {
  cursor: grab;
}
.widget.is-dragging .drag-handle {
  cursor: grabbing;
}

.widget__body {
  height: calc(100% - 44px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.widget__body--no-header {
  height: 100%;
}
.widget__body--auto {
  height: auto;
  overflow: visible;
}
.widget__body > :deep(*) {
  width: 100%;
}
.widget__body:not(.widget__body--auto) > :deep(*) {
  height: 100%;
}
.widget[data-edit='true']:not(.widget--text):not(.is-fullscreen) .widget__content-inner :deep(.echarts),
.widget[data-edit='true']:not(.widget--text):not(.is-fullscreen) .widget__content-inner :deep(canvas),
.widget[data-edit='true']:not(.widget--text):not(.is-fullscreen) .widget__content-inner :deep(svg) {
  pointer-events: none;
}

.widget__content-scale {
  flex: 1 1 auto;
  min-width: 0;
  min-height: 0;
  transform-origin: center center;
}

.widget__content-inner {
  width: 100%;
  height: 100%;
}

.widget__content-inner :deep([class*='border']) {
  border-color: rgba(148, 163, 184, 0.12) !important;
}

.widget__body--auto .widget__content-scale {
  flex: 0 0 auto;
  height: auto;
}

.widget__body--auto .widget__content-inner {
  height: auto;
  min-height: 0;
}

.text-inline-editor {
  display: block;
  max-width: 100%;
  white-space: pre-wrap;
  overflow-wrap: break-word;
  word-break: normal;
  overflow: hidden;
  user-select: text;
}

.text-inline-editor:focus {
  outline: none;
}

.text-inline-editor:focus-visible {
  outline: 2px solid rgba(129, 140, 248, 0.92);
  outline-offset: 2px;
  border-radius: 10px;
}



.widget__resize-overlay {
  position: absolute;
  inset: -14px;
  pointer-events: none;
  z-index: 30;
}

.resize-rail {
  position: absolute;
  display: block;
  pointer-events: none;
  opacity: 0;
  transition:
    opacity 140ms ease,
    background 140ms ease;
  border-radius: 999px;
  background: rgba(129, 140, 248, 0.26);
}
.resize-rail--h {
  inset-inline: 28px;
  top: 50%;
  height: 1px;
  transform: translateY(-50%);
}
.resize-rail--v {
  inset-block: 28px;
  left: 50%;
  width: 1px;
  transform: translateX(-50%);
}

.resize-edge {
  position: absolute;
  pointer-events: none;
  background: transparent;
  border: none;
  padding: 0;
  margin: 0;
  touch-action: none;
  opacity: 0;
  transition: opacity 120ms ease;
}

.resize-edge__line {
  position: absolute;
  background: rgba(129, 140, 248, 0.48);
  border-radius: 999px;
  transition: background 120ms ease;
}

.resize-edge:hover .resize-edge__line,
.resize-edge:focus-visible .resize-edge__line,
.widget.is-resizing .resize-edge__line {
  background: rgba(99, 102, 241, 0.82);
}

.resize-edge--n,
.resize-edge--s {
  left: 28px;
  right: 28px;
  height: 24px;
  cursor: ns-resize;
}

.resize-edge--n {
  top: 0;
}

.resize-edge--s {
  bottom: 0;
}

.resize-edge--n .resize-edge__line,
.resize-edge--s .resize-edge__line {
  left: 16px;
  right: 16px;
  top: 10px;
  height: 3px;
}

.resize-edge--e,
.resize-edge--w {
  top: 18px;
  bottom: 18px;
  width: 36px;
  border-radius: 999px;
  background: linear-gradient(
    180deg,
    rgba(59, 130, 246, 0),
    rgba(59, 130, 246, 0.08) 50%,
    rgba(59, 130, 246, 0)
  );
  cursor: ew-resize;
}

.resize-edge--e {
  right: -14px;
}

.resize-edge--w {
  left: -14px;
}

.resize-edge--e .resize-edge__line,
.resize-edge--w .resize-edge__line {
  top: 16px;
  bottom: 16px;
  width: 4px;
  left: 16px;
  border-radius: 999px;
  background: linear-gradient(
    180deg,
    rgba(248, 250, 252, 1),
    rgba(226, 232, 240, 1)
  );
  box-shadow:
    0 0 0 1px rgba(59, 130, 246, 0.22),
    0 0 8px rgba(96, 165, 250, 0.22),
    inset 0 0 0 1px rgba(255, 255, 255, 0.5);
}

.resize-handle {
  --handle-size: 20px;
  --handle-scale: 0.84;
  position: absolute;
  width: var(--handle-size);
  height: var(--handle-size);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 1), rgba(241, 245, 249, 1));
  border: 1.5px solid rgba(59, 130, 246, 0.96);
  box-shadow:
    0 2px 7px rgba(2, 6, 23, 0.22),
    0 0 0 2px rgba(147, 197, 253, 0.32);
  display: grid;
  place-items: center;
  opacity: 0;
  transform: scale(var(--handle-scale));
  transition:
    opacity 140ms ease,
    transform 140ms ease,
    box-shadow 120ms ease;
  pointer-events: none;
  touch-action: none;
  padding: 0;
  appearance: none;
  cursor: pointer;
}

.resize-handle:hover {
  --handle-scale: 1.06;
  box-shadow:
    0 2px 9px rgba(2, 6, 23, 0.24),
    0 0 0 2px rgba(125, 211, 252, 0.42);
}

.resize-handle:focus-visible {
  --handle-scale: 1.06;
}

.handle-dot {
  width: 4px;
  height: 4px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.96);
  box-shadow: 0 0 0 0.5px rgba(255, 255, 255, 0.95);
}

.handle-corner-dot {
  width: 4px;
  height: 4px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.96);
  box-shadow: 0 0 0 0.5px rgba(255, 255, 255, 0.95);
}

.resize-handle--n {
  top: 0;
  left: 50%;
  transform: translate(-50%, -50%) scale(var(--handle-scale));
  cursor: ns-resize;
}
.resize-handle--s {
  bottom: 0;
  left: 50%;
  transform: translate(-50%, 50%) scale(var(--handle-scale));
  cursor: ns-resize;
}
.resize-handle--e {
  right: 0;
  top: 50%;
  transform: translate(50%, -50%) scale(var(--handle-scale));
  cursor: ew-resize;
}
.resize-handle--w {
  left: 0;
  top: 50%;
  transform: translate(-50%, -50%) scale(var(--handle-scale));
  cursor: ew-resize;
}
.resize-handle--ne {
  right: 0;
  top: 0;
  transform: translate(50%, -50%) scale(var(--handle-scale));
  cursor: nesw-resize;
}
.resize-handle--nw {
  left: 0;
  top: 0;
  transform: translate(-50%, -50%) scale(var(--handle-scale));
  cursor: nwse-resize;
}
.resize-handle--se {
  right: 0;
  bottom: 0;
  transform: translate(50%, 50%) scale(var(--handle-scale));
  cursor: nwse-resize;
}
.resize-handle--sw {
  left: 0;
  bottom: 0;
  transform: translate(-50%, 50%) scale(var(--handle-scale));
  cursor: nesw-resize;
}

.resize-handle--n,
.resize-handle--s {
  width: 52px;
  height: 16px;
  border-radius: 999px;
}

.resize-handle--e,
.resize-handle--w {
  width: 16px;
  height: 52px;
  border-radius: 999px;
}

.resize-handle--ne,
.resize-handle--nw,
.resize-handle--se,
.resize-handle--sw {
  --handle-size: 20px;
}

.resize-handle--corner {
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 1), rgba(241, 245, 249, 1));
  border: 1.5px solid rgba(59, 130, 246, 0.96);
  box-shadow:
    0 2px 7px rgba(2, 6, 23, 0.22),
    0 0 0 2px rgba(147, 197, 253, 0.32);
}

.resize-handle--corner .handle-dot {
  width: 7px;
  height: 7px;
}

.widget--text[data-edit="true"] .resize-handle--corner {
  --handle-scale: 0.84;
  opacity: 0;
  pointer-events: none;
}

.widget--text .resize-handle--corner {
  z-index: 24;
}

.widget--text .resize-handle--ne {
  top: 0;
  right: 0;
  transform: translate(50%, -50%) scale(var(--handle-scale));
}

.widget--text .resize-handle--nw {
  top: 0;
  left: 0;
  transform: translate(-50%, -50%) scale(var(--handle-scale));
}

.widget--text .resize-handle--se {
  right: 0;
  bottom: 0;
  transform: translate(50%, 50%) scale(var(--handle-scale));
}

.widget--text .resize-handle--sw {
  left: 0;
  bottom: 0;
  transform: translate(-50%, 50%) scale(var(--handle-scale));
}

.widget--text[data-edit="true"] .resize-handle--corner {
  opacity: 0;
  --handle-scale: 0.84;
  pointer-events: none;
}

.widget.is-selected .resize-handle,
.widget.is-resizing .resize-handle {
  --handle-scale: 1;
  opacity: 1;
  pointer-events: auto;
}

.widget.is-selected .resize-edge,
.widget.is-resizing .resize-edge {
  opacity: 1;
  pointer-events: auto;
}

.widget[data-edit="true"]:hover .resize-rail,
.widget.is-resizing .resize-rail,
.widget.drag-armed .resize-rail,
.widget:focus-within .resize-rail {
  opacity: 0.92;
}
.widget.is-dragging .resize-handle {
  opacity: 0.25;
}

.widget.is-dragging .resize-edge {
  opacity: 0.18;
}

.widget--text[data-edit="true"] .resize-handle,
.widget--text[data-edit="true"] .resize-edge {
  opacity: 0;
  --handle-scale: 0.84;
  pointer-events: none;
}

.widget--text[data-text-active="true"] .resize-handle,
.widget--text[data-text-active="true"] .resize-edge,
.widget--text.is-resizing .resize-handle,
.widget--text.is-resizing .resize-edge {
  --handle-scale: 1;
  opacity: 1;
  pointer-events: auto;
}

.resize-meta {
  position: absolute;
  top: -38px;
  left: 50%;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  opacity: 0;
  pointer-events: none;
  transform: translateX(-50%) translateY(3px);
  transition:
    opacity 140ms ease,
    transform 140ms ease;
}

.resize-metrics {
  padding: 1px 7px;
  border-radius: 999px;
  border: 1px solid rgba(129, 140, 248, 0.5);
  background: rgba(15, 23, 42, 0.9);
  color: rgba(241, 245, 249, 0.95);
  font-size: 10px;
  letter-spacing: 0.02em;
  font-variant-numeric: tabular-nums;
}

.resize-hints {
  padding: 1px 7px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.26);
  background: rgba(15, 23, 42, 0.72);
  color: rgba(226, 232, 240, 0.82);
  font-size: 9px;
  letter-spacing: 0.01em;
  white-space: nowrap;
}

.widget[data-edit="true"]:hover .resize-meta,
.widget.is-resizing .resize-meta,
.widget.drag-armed .resize-meta,
.widget:focus-within .resize-meta {
  opacity: 1;
  transform: translateX(-50%) translateY(0);
}

@media (pointer: coarse) {
  .widget__header {
    min-height: 46px;
    padding: 0 10px 0 11px;
  }

  .widget[data-edit='true'] .widget__actions,
  .widget[data-edit='false'] .widget__actions,
  .widget.is-fullscreen .widget__actions {
    opacity: 1;
    transform: translateY(0);
    pointer-events: auto;
  }

  .widget__actions {
    gap: 6px;
  }

  .iconbtn {
    width: 36px;
    height: 36px;
    border-radius: 12px;
  }

  .widget-action-menu {
    min-width: 176px;
  }

  .widget__resize-overlay {
    inset: -18px;
  }

  .resize-edge--n,
  .resize-edge--s {
    height: 26px;
    left: 24px;
    right: 24px;
  }

  .resize-edge--e,
  .resize-edge--w {
    width: 26px;
    top: 24px;
    bottom: 24px;
  }

  .resize-handle {
    --handle-size: 22px;
  }

  .resize-handle--n,
  .resize-handle--s {
    width: 54px;
    height: 18px;
  }

  .resize-handle--e,
  .resize-handle--w {
    width: 18px;
    height: 54px;
  }

  .resize-handle--ne,
  .resize-handle--nw,
  .resize-handle--se,
  .resize-handle--sw {
    --handle-size: 38px;
  }

  .resize-hints {
    display: none;
  }
}
:global(body.widget-fullscreen-open) {
  overflow: hidden;
}
</style>
