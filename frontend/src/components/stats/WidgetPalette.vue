<template>
  <TransitionRoot appear :show="open" as="template">
    <Dialog as="div" class="relative z-50" @close="emit('close')">
      <TransitionChild
        as="template"
        enter="duration-150 ease-out"
        enter-from="opacity-0"
        enter-to="opacity-100"
        leave="duration-120 ease-in"
        leave-from="opacity-100"
        leave-to="opacity-0"
      >
        <div class="fixed inset-0 bg-slate-950/68 backdrop-blur-[2px]" />
      </TransitionChild>

      <div class="fixed inset-0 overflow-hidden">
        <div class="palette-stage">
          <TransitionChild
            as="template"
            enter="duration-180 ease-[cubic-bezier(0.22,1,0.36,1)]"
            enter-from="opacity-0 -translate-x-3"
            enter-to="opacity-100 translate-x-0"
            leave="duration-120 ease-in"
            leave-from="opacity-100"
            leave-to="opacity-0 -translate-x-2"
          >
            <DialogPanel class="palette-cluster" @keydown="onDialogKeydown">
              <aside class="palette-float-side" aria-label="Categories widgets">
                <div class="palette-float-side__label">Categories</div>
                <button
                  v-for="cat in categoryChips"
                  :key="cat.value"
                  type="button"
                  class="palette-float-side__item"
                  :class="{ 'is-active': activeCategory === cat.value }"
                  @click="onCategorySelect(cat.value)"
                >
                  <span
                    v-if="cat.color"
                    class="palette-float-side__dot"
                    :style="{ backgroundColor: cat.color }"
                    aria-hidden="true"
                  ></span>
                  <span class="palette-float-side__text">{{ cat.label }}</span>
                  <span class="palette-float-side__count">{{ cat.count }}</span>
                </button>
              </aside>

              <section class="palette-shell">
                <header class="palette-head">
                  <div>
                    <DialogTitle class="palette-title">Bibliotheque widgets</DialogTitle>
                    <p class="palette-subtitle">Choisis une metrique, son affichage, puis sa variante.</p>
                  </div>
                  <button class="palette-close" type="button" aria-label="Fermer la palette" @click="emit('close')">
                    <X :size="16" aria-hidden="true" />
                  </button>
                </header>

                <div ref="scrollEl" class="palette-scroll">
                  <section class="palette-tools">
                    <WidgetPaletteSearchBar
                      ref="searchBarRef"
                      v-model="query"
                      :result-count="visibleWidgetsCount"
                      :total-count="totalWidgets"
                      @keydown="onSearchKeydown"
                    />
                  </section>

                  <section ref="gridEl" class="palette-body" @keydown="onGridKeydown">
                    <div v-if="loading" class="palette-loading" role="status" aria-live="polite">
                      <div v-for="index in 6" :key="`skeleton-${index}`" class="palette-loading__item"></div>
                    </div>

                    <div v-else-if="error" class="palette-error" role="alert">
                      <div class="palette-error__title">Impossible de charger la palette</div>
                      <p class="palette-error__copy">{{ error }}</p>
                    </div>

                    <template v-else>
                      <section
                        v-for="group in visibleGroups"
                        :key="group.title"
                        class="palette-group"
                        :data-group="group.title"
                      >
                        <header class="palette-group__head">
                          <h3 class="palette-group__title">
                            <span class="palette-group__dot" :style="{ backgroundColor: group.color }"></span>
                            {{ group.title }}
                          </h3>
                          <span class="palette-group__count">{{ group.items.length }}</span>
                        </header>
                        <div class="palette-grid" role="listbox" :aria-label="`Widgets ${group.title}`">
                          <WidgetPaletteCard
                            v-for="item in group.items"
                            :key="item.type"
                            :widget="item"
                            :accent="group.color"
                            :selected="selectedWidgetType === item.type"
                            :active="activeWidgetType === item.type"
                            :adding="optimisticWidgetType === item.type"
                            :disabled="Boolean(item.disabled)"
                            :tab-index="activeWidgetType === item.type ? 0 : -1"
                            @focus="activeWidgetType = $event"
                            @select="onWidgetClick"
                          />
                        </div>
                      </section>

                      <div v-if="!visibleGroups.length" class="palette-empty" role="status" aria-live="polite">
                        <div class="palette-empty__title">Aucun widget</div>
                        <p class="palette-empty__copy">
                          Ajuste la recherche ou les filtres pour retrouver la bibliotheque.
                        </p>
                      </div>
                    </template>
                  </section>
                </div>

                <footer class="palette-foot">
                  <span>Entree ajoute le widget actif, Echap ferme la palette.</span>
                </footer>
              </section>
            </DialogPanel>
          </TransitionChild>
        </div>
      </div>
    </Dialog>
  </TransitionRoot>

  <teleport to="body">
    <Transition
      enter-active-class="duration-150 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="duration-120 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div v-if="formOpen" class="form-overlay" role="dialog" aria-modal="true" aria-label="Choisir une vue">
        <div class="form-backdrop" @click="closeFormPicker"></div>
        <div class="form-shell" @click.stop>
          <header class="form-head">
            <div>
              <div class="form-kicker">Composer le widget</div>
              <h3 class="form-title">{{ pendingWidget?.title }}</h3>
              <p class="form-subtitle">{{ chooserStepText }}</p>
            </div>
            <button class="palette-close" type="button" aria-label="Fermer" @click="closeFormPicker">
              <X :size="16" aria-hidden="true" />
            </button>
          </header>

          <div v-if="formList.length > 1" class="form-display-row" role="tablist" aria-label="Types d'affichage">
            <button
              v-for="display in formList"
              :key="display.key"
              type="button"
              class="form-display"
              :class="{ 'is-active': activeDisplay?.key === display.key }"
              :style="{ '--display-color': displayColor(display.key) }"
              role="tab"
              :aria-selected="activeDisplay?.key === display.key"
              @click="activeDisplayKey = display.key"
            >
              <component :is="displayIcon(display.key)" :size="16" aria-hidden="true" />
              <span>{{ display.label }}</span>
            </button>
          </div>

          <div class="form-variant-head">
            <span>{{ activeDisplay?.label ?? 'Affichage' }}</span>
            <small>{{ activeDisplay?.hint }}</small>
          </div>

          <div class="form-grid">
            <button
              v-for="variant in activeVariants"
              :key="variant.key"
              type="button"
              class="form-card"
              @click="selectVariant(variant)"
            >
              <component
                :is="displayIcon(activeDisplay?.key ?? 'number')"
                :size="18"
                class="form-card__icon"
                :style="{ color: displayColor(activeDisplay?.key ?? 'number') }"
              />
              <div class="form-card__body">
                <div class="form-card__title">{{ variant.label }}</div>
                <div class="form-card__hint">{{ variant.hint ?? activeDisplay?.hint }}</div>
              </div>
              <span v-if="variant.badge" class="form-card__badge">{{ variant.badge }}</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { Dialog, DialogPanel, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue'
import { BarChart3, Hash, LayoutList, LineChart, PieChart, Target, X } from 'lucide-vue-next'
import WidgetPaletteSearchBar from './palette/WidgetPaletteSearchBar.vue'
import WidgetPaletteCard from './palette/WidgetPaletteCard.vue'
import { filterPaletteGroups, flattenPalette, moveGridIndexByKey, resolveGridColumns } from './palette/paletteUtils'
import type { WidgetDisplayGroup, WidgetPaletteGroup, WidgetPaletteItem, WidgetVariantOption } from './palette/types'

const props = withDefaults(
  defineProps<{
    open: boolean
    groups: WidgetPaletteGroup[]
    loading?: boolean
    error?: string
  }>(),
  {
    loading: false,
    error: '',
  },
)

const emit = defineEmits<{
  (event: 'close'): void
  (
    event: 'add',
    payload: string | { type: string; view?: string; props?: Record<string, unknown>; title?: string },
  ): void
}>()

const FORM_OPTIONS = {
  number: {
    key: 'number',
    label: 'KPI',
    hint: 'Vue compacte',
    icon: Hash,
    color: '#2563eb',
  },
  bars: {
    key: 'bars',
    label: 'Barres',
    hint: 'Comparer des categories',
    icon: BarChart3,
    color: '#d97706',
  },
  pie: {
    key: 'pie',
    label: 'Camembert',
    hint: 'Repartition simple',
    icon: PieChart,
    color: '#7c3aed',
  },
  target: {
    key: 'target',
    label: 'Objectif',
    hint: 'Progression vers une cible',
    icon: Target,
    color: '#16a34a',
  },
  line: {
    key: 'line',
    label: 'Courbe',
    hint: 'Tendance temporelle',
    icon: LineChart,
    color: '#0f766e',
  },
  treemap: {
    key: 'treemap',
    label: 'Treemap',
    hint: 'Comparer par surface',
    icon: BarChart3,
    color: '#0891b2',
  },
  heatmap: {
    key: 'heatmap',
    label: 'Heatmap',
    hint: 'Visualiser des intensites',
    icon: Target,
    color: '#ea580c',
  },
} as const

const searchBarRef = ref<{ focus: () => void } | null>(null)
const gridEl = ref<HTMLElement | null>(null)
const scrollEl = ref<HTMLElement | null>(null)

const query = ref('')
const debouncedQuery = ref('')
const activeCategory = ref('all')
const activeWidgetType = ref<string | null>(null)
const selectedWidgetType = ref<string | null>(null)
const optimisticWidgetType = ref<string | null>(null)

const formOpen = ref(false)
const pendingWidget = ref<WidgetPaletteItem | null>(null)
const activeDisplayKey = ref('')

let searchTimer: ReturnType<typeof window.setTimeout> | null = null
let optimisticTimer: ReturnType<typeof window.setTimeout> | null = null

const normalizedGroups = computed<WidgetPaletteGroup[]>(() =>
  (props.groups ?? []).map((group) => ({
    ...group,
    items: group.items ?? [],
  })),
)

const categoryChips = computed(() => [
  {
    label: 'Toutes',
    value: 'all',
    count: normalizedGroups.value.reduce((sum, group) => sum + (group.items?.length ?? 0), 0),
  },
  ...normalizedGroups.value.map((group) => ({
    label: group.title,
    value: group.title,
    color: group.color,
    count: group.items?.length ?? 0,
  })),
])

const visibleGroups = computed(() =>
  filterPaletteGroups(normalizedGroups.value, {
    query: debouncedQuery.value,
    category: activeCategory.value,
    dataType: 'all',
    sort: 'relevance',
  }),
)

const flatVisibleItems = computed(() => flattenPalette(visibleGroups.value))

const totalWidgets = computed(() =>
  normalizedGroups.value.reduce((sum, group) => sum + (group.items?.length ?? 0), 0),
)

const visibleWidgetsCount = computed(() => flatVisibleItems.value.length)

const formList = computed(() => {
  return resolveSelection(pendingWidget.value)
})

const activeDisplay = computed(() => {
  const groups = formList.value
  if (!groups.length) return null
  return groups.find((group) => group.key === activeDisplayKey.value) ?? groups[0]
})

const activeVariants = computed(() => activeDisplay.value?.variants ?? [])

const chooserStepText = computed(() => {
  const groups = formList.value.length
  const variants = activeVariants.value.length
  if (groups > 1 && variants > 1) return 'Choisis un affichage puis une variante fiable.'
  if (groups > 1) return 'Choisis la representation adaptee a ta metrique.'
  if (variants > 1) return 'Choisis la variante a placer sur le canvas.'
  return 'Confirme le widget a ajouter au canvas.'
})

watch(
  () => props.open,
  (open) => {
    if (!open) {
      closeFormPicker()
      return
    }

    query.value = ''
    debouncedQuery.value = ''
    activeCategory.value = 'all'
    selectedWidgetType.value = null
    optimisticWidgetType.value = null

    nextTick(() => {
      searchBarRef.value?.focus()
    })
  },
)

watch(query, (value) => {
  if (searchTimer) window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => {
    debouncedQuery.value = value
  }, 90)
})

watch(flatVisibleItems, (items) => {
  if (!items.length) {
    activeWidgetType.value = null
    return
  }

  const stillVisible = items.some((entry) => entry.item.type === activeWidgetType.value)
  if (!stillVisible) {
    activeWidgetType.value = items[0].item.type
  }
})

function closeFormPicker() {
  formOpen.value = false
  pendingWidget.value = null
  activeDisplayKey.value = ''
}

function setOptimistic(type: string | null) {
  optimisticWidgetType.value = type
  if (optimisticTimer) window.clearTimeout(optimisticTimer)
  if (!type) return
  optimisticTimer = window.setTimeout(() => {
    optimisticWidgetType.value = null
  }, 550)
}

function queueAdd(payload: string | { type: string; view?: string; props?: Record<string, unknown>; title?: string }) {
  emit('add', payload)
  emit('close')
}

function resolveSelection(widget: WidgetPaletteItem | null): WidgetDisplayGroup[] {
  if (!widget) return []
  if (widget.selection?.length) {
    return widget.selection
      .map((group) => ({
        ...group,
        variants: (group.variants ?? []).filter((variant) => variant?.key && variant?.label),
      }))
      .filter((group) => group.variants.length > 0)
  }

  const forms = widget.forms ?? []
  return forms
    .filter((key) => String(key || '').trim().length > 0)
    .map((key) => {
      const option = FORM_OPTIONS[key as keyof typeof FORM_OPTIONS] ?? FORM_OPTIONS.number
      return {
        key,
        label: option.label,
        hint: option.hint,
        variants: [
          {
            key,
            label: option.label,
            hint: option.hint,
            view: key,
            props: { view: key },
          },
        ],
      }
    })
}

function countSelectionVariants(groups: WidgetDisplayGroup[]) {
  return groups.reduce((sum, group) => sum + (group.variants?.length ?? 0), 0)
}

function displayIcon(key: string) {
  if (key === 'number' || key === 'target') return key === 'target' ? Target : Hash
  if (key === 'line') return LineChart
  if (key === 'pie') return PieChart
  if (key === 'list' || key === 'text') return LayoutList
  return BarChart3
}

function displayColor(key: string) {
  const option = FORM_OPTIONS[key as keyof typeof FORM_OPTIONS]
  if (option?.color) return option.color
  if (key === 'list') return '#14b8a6'
  if (key === 'text') return '#8b5cf6'
  return '#5b5ce2'
}

function onWidgetClick(widget: WidgetPaletteItem) {
  if (widget.disabled) return
  selectedWidgetType.value = widget.type
  activeWidgetType.value = widget.type
  setOptimistic(widget.type)

  const selection = resolveSelection(widget)
  const variantCount = countSelectionVariants(selection)

  if (widget.formPicker === false || variantCount <= 1) {
    const variant = selection[0]?.variants?.[0]
    if (variant) {
      queueAdd(buildVariantPayload(widget, variant))
      return
    }
    queueAdd(widget.type)
    return
  }

  pendingWidget.value = {
    ...widget,
    forms: widget.forms?.length ? [...widget.forms] : ['number'],
    selection,
  }
  activeDisplayKey.value = selection[0]?.key ?? ''
  formOpen.value = true
}

function onCategorySelect(value: string) {
  activeCategory.value = value
  nextTick(() => {
    const host = scrollEl.value
    if (!host) return
    if (value === 'all') {
      host.scrollTo({ top: 0, behavior: 'smooth' })
      return
    }
    const sections = host.querySelectorAll<HTMLElement>('[data-group]')
    const target = Array.from(sections).find((section) => section.dataset.group === value)
    if (!target) return
    host.scrollTo({
      top: Math.max(target.offsetTop - 8, 0),
      behavior: 'smooth',
    })
  })
}

function buildVariantPayload(widget: WidgetPaletteItem, variant: WidgetVariantOption) {
  const props = {
    ...(variant.props ?? {}),
  }
  if (variant.view) props.view = variant.view
  return {
    type: widget.type,
    view: variant.view,
    props,
    title: variant.widgetTitle ?? variant.label,
  }
}

function selectVariant(variant: WidgetVariantOption) {
  if (!pendingWidget.value) return
  const payload = buildVariantPayload(pendingWidget.value, variant)
  closeFormPicker()
  queueAdd(payload)
}

function focusCard(type: string) {
  if (!type) return
  const button = gridEl.value?.querySelector(`[data-widget-type="${type}"]`) as HTMLButtonElement | null
  if (!button) return
  button.focus()
  activeWidgetType.value = type
}

function activeIndexFromDom(): number {
  const current = document.activeElement as HTMLElement | null
  const focusedType = current?.dataset?.widgetType
  if (focusedType) {
    const idx = flatVisibleItems.value.findIndex((entry) => entry.item.type === focusedType)
    if (idx >= 0) return idx
  }
  return flatVisibleItems.value.findIndex((entry) => entry.item.type === activeWidgetType.value)
}

function onSearchKeydown(event: KeyboardEvent) {
  if (event.key === 'ArrowDown') {
    const first = flatVisibleItems.value[0]
    if (!first) return
    event.preventDefault()
    focusCard(first.item.type)
    return
  }

  if (event.key === 'Enter') {
    const first = flatVisibleItems.value[0]
    if (!first) return
    event.preventDefault()
    onWidgetClick(first.item)
    return
  }

  if (event.key === 'Escape') {
    event.preventDefault()
    emit('close')
  }
}

function onGridKeydown(event: KeyboardEvent) {
  const key = event.key
  if (key === 'Escape') {
    event.preventDefault()
    if (formOpen.value) {
      closeFormPicker()
    } else {
      emit('close')
    }
    return
  }

  if (!['ArrowRight', 'ArrowLeft', 'ArrowDown', 'ArrowUp', 'Home', 'End'].includes(key)) return
  if (!flatVisibleItems.value.length) return

  event.preventDefault()
  const currentIndex = Math.max(activeIndexFromDom(), 0)
  const nextIndex = moveGridIndexByKey({
    key,
    index: currentIndex,
    total: flatVisibleItems.value.length,
    columns: resolveGridColumns(gridEl.value?.clientWidth ?? window.innerWidth),
  })

  const next = flatVisibleItems.value[nextIndex]
  if (!next) return
  focusCard(next.item.type)
}

function onDialogKeydown(event: KeyboardEvent) {
  if (event.key !== 'Escape') return
  if (formOpen.value) {
    event.preventDefault()
    closeFormPicker()
  }
}

onBeforeUnmount(() => {
  if (searchTimer) window.clearTimeout(searchTimer)
  if (optimisticTimer) window.clearTimeout(optimisticTimer)
})
</script>

<style scoped>
.palette-shell {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  width: min(1200px, calc(100vw - 340px));
  min-width: 760px;
  height: calc(100dvh - 50px);
  overflow: hidden;
  border-radius: 18px;
  border: 1px solid rgba(113, 151, 195, 0.42);
  background:
    linear-gradient(180deg, rgba(247, 251, 255, 0.98), rgba(232, 240, 251, 0.98)),
    radial-gradient(circle at 100% 0, rgba(91, 92, 226, 0.1), transparent 40%);
  color: #0f172a;
  box-shadow:
    0 24px 52px rgba(2, 6, 23, 0.34),
    inset 0 1px 0 rgba(255, 255, 255, 0.78);
}

.palette-stage {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28px 40px;
}

.palette-cluster {
  display: grid;
  grid-template-columns: 214px minmax(0, 1fr);
  align-items: start;
  gap: 16px;
  width: fit-content;
  max-width: calc(100vw - 120px);
}

.palette-scroll {
  overflow: auto;
  min-height: 0;
  max-height: 100%;
  overscroll-behavior: contain;
  scroll-behavior: smooth;
  scrollbar-width: thin;
  scrollbar-color: rgba(91, 92, 226, 0.5) rgba(203, 213, 225, 0.36);
}

.palette-scroll::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

.palette-scroll::-webkit-scrollbar-track {
  border-radius: 999px;
  background: rgba(203, 213, 225, 0.36);
}

.palette-scroll::-webkit-scrollbar-thumb {
  border: 2px solid rgba(241, 247, 255, 0.9);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(91, 92, 226, 0.58), rgba(14, 165, 233, 0.5));
}

.palette-scroll::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, rgba(91, 92, 226, 0.72), rgba(14, 165, 233, 0.64));
}

.palette-float-side {
  align-self: start;
  display: grid;
  gap: 8px;
  padding: 12px;
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.3);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(241, 247, 255, 0.88)),
    radial-gradient(circle at 0 0, rgba(91, 92, 226, 0.1), transparent 52%);
  box-shadow:
    0 14px 30px rgba(31, 41, 55, 0.1),
    0 1px 0 rgba(255, 255, 255, 0.78) inset;
}

.palette-float-side__label {
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #64748b;
  padding: 2px 2px 6px;
  grid-column: 1 / -1;
}

.palette-float-side__item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-height: 36px;
  padding: 0 10px;
  border-radius: 11px;
  border: 1px solid rgba(148, 163, 184, 0.26);
  background: rgba(255, 255, 255, 0.7);
  color: #334155;
  font-size: 14px;
  font-weight: 600;
  text-align: left;
  transition: border-color 130ms ease, background 130ms ease, transform 130ms ease;
}

.palette-float-side__item:hover {
  border-color: rgba(91, 92, 226, 0.32);
  background: rgba(238, 242, 255, 0.74);
  transform: translateX(1px);
}

.palette-float-side__item.is-active {
  border-color: rgba(91, 92, 226, 0.42);
  background: rgba(238, 242, 255, 0.9);
  color: #111827;
}

.palette-float-side__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
}

.palette-float-side__text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.palette-float-side__count {
  margin-left: auto;
  min-width: 20px;
  height: 20px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  color: #334155;
  background: rgba(226, 232, 240, 0.78);
}

.palette-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 18px 20px 14px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.28);
  background: rgba(255, 255, 255, 0.62);
}

.palette-title {
  font-size: 22px;
  line-height: 1.1;
  font-weight: 800;
  letter-spacing: -0.01em;
}

.palette-subtitle {
  margin-top: 5px;
  font-size: 13px;
  color: #64748b;
}

.palette-close {
  margin-left: auto;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.72);
  background: rgba(226, 232, 240, 0.72);
  display: grid;
  place-items: center;
  color: #334155;
}

.palette-close:hover {
  border-color: rgba(59, 130, 246, 0.44);
  background: rgba(241, 245, 249, 0.86);
}

.palette-tools {
  position: sticky;
  top: 0;
  z-index: 2;
  display: grid;
  gap: 10px;
  padding: 12px 14px 10px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.24);
  background: rgba(241, 247, 255, 0.84);
  backdrop-filter: blur(8px);
}

.palette-body {
  padding: 12px 14px 18px;
  display: grid;
  gap: 14px;
  justify-items: center;
}

.palette-group {
  display: grid;
  gap: 8px;
  content-visibility: auto;
  contain-intrinsic-size: 360px;
  width: min(100%, 1040px);
}

.palette-group__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.palette-group__title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.palette-group__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
}

.palette-group__count {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  color: #334155;
  background: rgba(15, 23, 42, 0.12);
}

.palette-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.palette-loading {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  width: min(100%, 1040px);
}

.palette-loading__item {
  min-height: 200px;
  border-radius: 14px;
  background: linear-gradient(90deg, rgba(226, 232, 240, 0.7), rgba(241, 245, 249, 0.95), rgba(226, 232, 240, 0.7));
  background-size: 220% 100%;
  animation: palette-loading 1.2s linear infinite;
}

.palette-error,
.palette-empty {
  border: 1px dashed rgba(148, 163, 184, 0.5);
  border-radius: 13px;
  background: rgba(203, 213, 225, 0.44);
  padding: 18px;
  width: min(100%, 1040px);
}

.palette-error__title,
.palette-empty__title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.palette-error__copy,
.palette-empty__copy {
  margin-top: 5px;
  font-size: 13px;
  color: #64748b;
}

.palette-foot {
  display: flex;
  justify-content: flex-start;
  gap: 10px;
  flex-wrap: wrap;
  padding: 9px 14px 12px;
  border-top: 1px solid rgba(148, 163, 184, 0.26);
  font-size: 12px;
  color: #475569;
  background: rgba(241, 247, 255, 0.78);
}

.form-overlay {
  position: fixed;
  inset: 0;
  z-index: 70;
}

.form-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.54);
}

.form-shell {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: min(740px, calc(100vw - 24px));
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.36);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 248, 253, 0.98)),
    radial-gradient(circle at 100% 0, rgba(91, 92, 226, 0.1), transparent 42%);
  box-shadow:
    0 24px 58px rgba(15, 23, 42, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.76);
  padding: 16px;
}

.form-head {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.form-kicker {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #64748b;
}

.form-title {
  margin-top: 4px;
  font-size: 18px;
  font-weight: 750;
  color: #0f172a;
}

.form-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: #64748b;
}

.form-display-row {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.form-display {
  min-height: 38px;
  border-radius: 11px;
  border: 1px solid rgba(148, 163, 184, 0.34);
  background: rgba(255, 255, 255, 0.74);
  color: #475569;
  padding: 0 11px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 760;
  transition: border-color 130ms ease, background 130ms ease, color 130ms ease;
}

.form-display:hover,
.form-display.is-active {
  border-color: color-mix(in srgb, var(--display-color) 42%, rgba(148, 163, 184, 0.34));
  background: color-mix(in srgb, var(--display-color) 11%, #fff);
  color: #111827;
}

.form-variant-head {
  margin-top: 14px;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  padding-bottom: 7px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.22);
}

.form-variant-head span {
  color: #111827;
  font-size: 0.82rem;
  font-weight: 800;
}

.form-variant-head small {
  color: #64748b;
  font-size: 0.74rem;
  font-weight: 650;
  text-align: right;
}

.form-grid {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.form-card {
  position: relative;
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 10px;
  align-items: center;
  min-height: 74px;
  border: 1px solid rgba(148, 163, 184, 0.32);
  border-radius: 12px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(248, 250, 252, 0.9)),
    radial-gradient(circle at 100% 0, rgba(91, 92, 226, 0.06), transparent 42%);
  padding: 12px;
  text-align: left;
  transition: border-color 130ms ease, box-shadow 130ms ease, transform 130ms ease;
}

.form-card:hover {
  border-color: rgba(91, 92, 226, 0.36);
  box-shadow: 0 12px 26px rgba(31, 41, 55, 0.1);
  transform: translateY(-1px);
}

.form-card__icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  padding: 8px;
  background: rgba(238, 242, 255, 0.82);
}

.form-card__title {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.form-card__hint {
  margin-top: 2px;
  font-size: 12px;
  color: #64748b;
}

.form-card__badge {
  position: absolute;
  right: 10px;
  top: 10px;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.1);
  color: #4338ca;
  padding: 3px 7px;
  font-size: 10px;
  font-weight: 760;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

@keyframes palette-loading {
  0% {
    background-position: 0 0;
  }
  100% {
    background-position: -220% 0;
  }
}

@media (max-width: 1024px) {
  .palette-stage {
    justify-content: center;
    padding: 14px;
  }

  .palette-cluster {
    grid-template-columns: 176px minmax(0, 1fr);
    gap: 10px;
    max-width: calc(100vw - 24px);
  }

  .palette-shell {
    width: min(940px, calc(100vw - 210px));
    min-width: 0;
    height: calc(100dvh - 28px);
  }

  .palette-grid,
  .palette-loading {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .palette-float-side__item {
    min-height: 40px;
  }
}

@media (max-width: 820px) {
  .palette-stage {
    align-items: stretch;
    padding: 10px;
  }

  .palette-cluster {
    grid-template-columns: 1fr;
    grid-template-rows: auto minmax(0, 1fr);
    gap: 8px;
    width: 100%;
    max-width: none;
    height: calc(100dvh - 20px);
  }

  .palette-shell {
    width: 100%;
    height: 100%;
    min-height: 0;
    border-radius: 16px;
  }

  .palette-float-side {
    display: flex;
    align-items: center;
    gap: 8px;
    overflow-x: auto;
    padding: 8px;
    border-radius: 14px;
    box-shadow: none;
    scrollbar-width: none;
  }

  .palette-float-side::-webkit-scrollbar {
    display: none;
  }

  .palette-float-side__label {
    display: none;
  }

  .palette-float-side__item {
    width: auto;
    min-width: max-content;
    min-height: 40px;
    padding: 0 12px;
    border-radius: 999px;
    flex: 0 0 auto;
  }

  .palette-float-side__text {
    overflow: visible;
    text-overflow: clip;
  }
}

@media (max-width: 640px) {
  .palette-stage {
    padding: 0;
    align-items: flex-end;
  }

  .palette-cluster {
    gap: 6px;
    height: 100dvh;
  }

  .palette-shell {
    border-radius: 18px 18px 0 0;
    border-bottom: none;
    box-shadow:
      0 -16px 34px rgba(2, 6, 23, 0.28),
      0 -2px 0 rgba(148, 163, 184, 0.18);
  }

  .palette-head {
    padding: 14px 14px 12px;
  }

  .palette-title {
    font-size: 20px;
  }

  .palette-subtitle {
    font-size: 12px;
  }

  .palette-close {
    width: 40px;
    height: 40px;
    border-radius: 12px;
  }

  .palette-tools,
  .palette-foot {
    padding-left: 12px;
    padding-right: 12px;
  }

  .palette-body {
    padding: 10px 12px 14px;
    gap: 12px;
  }

  .palette-group {
    gap: 6px;
  }

  .palette-grid,
  .palette-loading {
    grid-template-columns: 1fr;
  }

  .palette-float-side {
    margin: 0 8px;
    border-radius: 12px;
  }

  .palette-float-side__item {
    min-height: 42px;
    font-size: 13px;
  }

  .palette-float-side__count {
    display: none;
  }

  .form-shell {
    left: 0;
    right: 0;
    top: auto;
    bottom: 0;
    transform: none;
    width: 100%;
    max-height: min(78dvh, 620px);
    border-radius: 16px 16px 0 0;
    padding: 14px;
    overflow: auto;
  }

  .form-head {
    position: sticky;
    top: 0;
    z-index: 2;
    background: #f8fafc;
    padding-bottom: 10px;
    margin-bottom: 6px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
