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
                    <p class="palette-subtitle">Recherche un widget puis ajoute-le en un clic.</p>
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
              <div class="form-kicker">Affichage</div>
              <h3 class="form-title">Choisir une vue</h3>
              <p class="form-subtitle">{{ pendingWidget?.title }} existe en plusieurs representations.</p>
            </div>
            <button class="palette-close" type="button" aria-label="Fermer" @click="closeFormPicker">
              <X :size="16" aria-hidden="true" />
            </button>
          </header>

          <div class="form-grid">
            <button v-for="form in formList" :key="form.key" type="button" class="form-card" @click="selectForm(form.key)">
              <component :is="form.icon" :size="18" class="form-card__icon" :style="{ color: form.color }" />
              <div class="form-card__body">
                <div class="form-card__title">{{ form.label }}</div>
                <div class="form-card__hint">{{ form.hint }}</div>
              </div>
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
import { BarChart3, Hash, LineChart, PieChart, Target, X } from 'lucide-vue-next'
import WidgetPaletteSearchBar from './palette/WidgetPaletteSearchBar.vue'
import WidgetPaletteCard from './palette/WidgetPaletteCard.vue'
import { filterPaletteGroups, flattenPalette, moveGridIndexByKey, resolveGridColumns } from './palette/paletteUtils'
import type { WidgetPaletteGroup, WidgetPaletteItem } from './palette/types'

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
  (event: 'add', payload: string | { type: string; view?: string }): void
}>()

const FORM_OPTIONS = {
  number: {
    key: 'number',
    label: 'Chiffre',
    hint: 'KPI compact',
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
    label: 'Graphe',
    hint: 'Courbe temporelle',
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

let searchTimer: ReturnType<typeof window.setTimeout> | null = null
let addTimer: number | null = null
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
  const forms = pendingWidget.value?.forms ?? []
  if (!forms.length) return []
  return forms.map((key) => FORM_OPTIONS[key as keyof typeof FORM_OPTIONS] ?? FORM_OPTIONS.number)
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
}

function setOptimistic(type: string | null) {
  optimisticWidgetType.value = type
  if (optimisticTimer) window.clearTimeout(optimisticTimer)
  if (!type) return
  optimisticTimer = window.setTimeout(() => {
    optimisticWidgetType.value = null
  }, 550)
}

function queueAdd(payload: string | { type: string; view?: string }) {
  emit('close')
  if (addTimer) window.cancelAnimationFrame(addTimer)
  addTimer = window.requestAnimationFrame(() => {
    emit('add', payload)
    addTimer = null
  })
}

function onWidgetClick(widget: WidgetPaletteItem) {
  if (widget.disabled) return
  selectedWidgetType.value = widget.type
  activeWidgetType.value = widget.type
  setOptimistic(widget.type)

  if (widget.formPicker === false || (widget.forms?.length ?? 0) <= 1) {
    queueAdd(widget.type)
    return
  }

  pendingWidget.value = {
    ...widget,
    forms: widget.forms?.length ? [...widget.forms] : ['number'],
  }
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

function selectForm(key: string) {
  if (!pendingWidget.value) return
  const payload = { type: pendingWidget.value.type, view: key }
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
  if (addTimer) window.cancelAnimationFrame(addTimer)
})
</script>

<style scoped>
.palette-shell {
  display: grid;
  grid-template-rows: auto 1fr auto;
  width: min(1200px, calc(100vw - 340px));
  min-width: 760px;
  height: calc(100dvh - 50px);
  overflow: hidden;
  border-radius: 18px;
  border: 1px solid rgba(100, 116, 139, 0.46);
  background:
    linear-gradient(180deg, #c8d3e4, #bcc9dc),
    radial-gradient(circle at 100% 0, rgba(59, 130, 246, 0.12), transparent 40%);
  color: #0f172a;
  box-shadow: 0 24px 52px rgba(2, 6, 23, 0.4);
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
  overscroll-behavior: contain;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.palette-scroll::-webkit-scrollbar {
  width: 0;
  height: 0;
  display: none;
}

.palette-float-side {
  align-self: start;
  display: grid;
  gap: 8px;
  padding: 12px;
  border-radius: 16px;
  border: 1px solid rgba(100, 116, 139, 0.42);
  background:
    linear-gradient(180deg, rgba(30, 41, 59, 0.86), rgba(15, 23, 42, 0.84)),
    radial-gradient(circle at 0 0, rgba(59, 130, 246, 0.24), transparent 52%);
  box-shadow:
    0 16px 36px rgba(2, 6, 23, 0.46),
    0 1px 0 rgba(255, 255, 255, 0.08) inset;
}

.palette-float-side__label {
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: rgba(203, 213, 225, 0.95);
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
  border: 1px solid rgba(100, 116, 139, 0.65);
  background: rgba(30, 41, 59, 0.66);
  color: #e2e8f0;
  font-size: 14px;
  font-weight: 600;
  text-align: left;
  transition: border-color 130ms ease, background 130ms ease, transform 130ms ease;
}

.palette-float-side__item:hover {
  border-color: rgba(96, 165, 250, 0.7);
  background: rgba(30, 58, 138, 0.36);
  transform: translateX(1px);
}

.palette-float-side__item.is-active {
  border-color: rgba(96, 165, 250, 0.92);
  background: rgba(37, 99, 235, 0.45);
  color: #eff6ff;
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
  color: rgba(226, 232, 240, 0.92);
  background: rgba(15, 23, 42, 0.46);
}

.palette-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 18px 20px 14px;
  border-bottom: 1px solid rgba(100, 116, 139, 0.46);
  background: rgba(214, 224, 237, 0.64);
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
  border-bottom: 1px solid rgba(100, 116, 139, 0.38);
  background: rgba(203, 213, 225, 0.58);
  backdrop-filter: blur(4px);
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
  border-top: 1px solid rgba(100, 116, 139, 0.46);
  font-size: 12px;
  color: #475569;
  background: rgba(203, 213, 225, 0.56);
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
  width: min(680px, calc(100vw - 24px));
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.3);
  background: #f8fafc;
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.32);
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

.form-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.form-card {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 10px;
  align-items: center;
  border: 1px solid rgba(203, 213, 225, 0.86);
  border-radius: 12px;
  background: #ffffff;
  padding: 12px;
  text-align: left;
}

.form-card:hover {
  border-color: rgba(59, 130, 246, 0.42);
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
    padding: 16px 18px;
  }

  .palette-cluster {
    grid-template-columns: 184px minmax(0, 1fr);
    gap: 12px;
    max-width: calc(100vw - 36px);
  }

  .palette-shell {
    width: min(980px, calc(100vw - 232px));
    min-width: 0;
  }

  .palette-grid,
  .palette-loading {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .palette-shell {
    width: calc(100vw - 10px);
    height: calc(100dvh - 10px);
    border-radius: 14px;
  }

  .palette-stage {
    padding: 8px;
  }

  .palette-cluster {
    grid-template-columns: 1fr;
    gap: 8px;
    width: 100%;
  }

  .palette-head,
  .palette-tools,
  .palette-foot {
    padding-left: 12px;
    padding-right: 12px;
  }

  .palette-body {
    padding-left: 12px;
    padding-right: 12px;
    padding-bottom: 16px;
  }

  .palette-float-side {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    padding: 9px;
    border-radius: 12px;
    gap: 7px;
    box-shadow: none;
  }

  .palette-float-side__count {
    display: none;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .palette-grid,
  .palette-loading {
    grid-template-columns: 1fr;
  }
}
</style>
