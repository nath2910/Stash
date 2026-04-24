<template>
  <TransitionRoot appear :show="open" as="template">
    <Dialog as="div" class="relative z-[64]" @close="emit('close')">
      <TransitionChild
        as="template"
        enter="duration-180 ease-out"
        enter-from="opacity-0"
        enter-to="opacity-100"
        leave="duration-140 ease-in"
        leave-from="opacity-100"
        leave-to="opacity-0"
      >
        <div class="fixed inset-0 bg-slate-950/70 backdrop-blur-[2px]" />
      </TransitionChild>

      <div class="fixed inset-0 overflow-hidden">
        <div class="template-library-stage">
          <TransitionChild
            as="template"
            enter="duration-200 ease-[cubic-bezier(0.22,1,0.36,1)]"
            enter-from="opacity-0 translate-y-2"
            enter-to="opacity-100 translate-y-0"
            leave="duration-140 ease-in"
            leave-from="opacity-100 translate-y-0"
            leave-to="opacity-0 translate-y-2"
          >
            <DialogPanel class="template-library-shell">
              <aside class="template-library-side">
                <div class="template-side-label">Categories</div>
                <button
                  v-for="category in categoryChips"
                  :key="`cat-${category.value}`"
                  type="button"
                  class="template-side-chip"
                  :class="{ 'is-active': selectedCategory === category.value }"
                  @click="selectedCategory = category.value"
                >
                  <span>{{ category.label }}</span>
                  <span class="template-side-chip__count">{{ category.count }}</span>
                </button>
              </aside>

              <section class="template-library-main">
                <header class="template-head">
                  <div>
                    <DialogTitle class="template-title">Template Gallery</DialogTitle>
                    <p class="template-subtitle">
                      Choisis un preset premium, applique-le, et repars d'un dashboard deja fini.
                    </p>
                  </div>
                  <button
                    type="button"
                    class="template-close"
                    aria-label="Fermer la bibliotheque templates"
                    @click="emit('close')"
                  >
                    <X :size="16" />
                  </button>
                </header>

                <div class="template-toolbar">
                  <div class="template-search-wrap">
                    <Search class="template-search-icon" :size="15" />
                    <input
                      ref="searchInputRef"
                      v-model="query"
                      type="search"
                      class="template-search"
                      placeholder="Recherche template..."
                    />
                  </div>

                  <div class="template-toolbar-controls">
                    <div class="template-chip-row">
                      <button
                        v-for="entry in sourceOptions"
                        :key="`source-${entry.value}`"
                        type="button"
                        class="template-inline-chip"
                        :class="{ 'is-active': sourceFilter === entry.value }"
                        @click="sourceFilter = entry.value"
                      >
                        {{ entry.label }}
                      </button>
                    </div>

                    <div class="template-chip-row">
                      <button
                        v-for="entry in densityOptions"
                        :key="`density-${entry.value}`"
                        type="button"
                        class="template-inline-chip"
                        :class="{ 'is-active': densityFilter === entry.value }"
                        @click="densityFilter = entry.value"
                      >
                        {{ entry.label }}
                      </button>
                    </div>

                    <select v-model="sortMode" class="template-sort">
                      <option v-for="entry in sortOptions" :key="`sort-${entry.value}`" :value="entry.value">
                        {{ entry.label }}
                      </option>
                    </select>
                  </div>

                  <div class="template-toolbar-meta">
                    <span>{{ filteredTemplates.length }}/{{ templates.length }}</span>
                    <button type="button" class="template-reset-btn" @click="resetFilters">Reset</button>
                  </div>
                </div>

                <div v-if="visibleTags.length" class="template-tags-row">
                  <button
                    v-for="tag in visibleTags"
                    :key="`tag-${tag}`"
                    type="button"
                    class="template-tag-filter"
                    :class="{ 'is-active': activeTags.includes(tag) }"
                    @click="toggleTag(tag)"
                  >
                    {{ tag }}
                  </button>
                </div>

                <div class="template-save-bar">
                  <div class="template-save-bar__copy">
                    <div class="template-save-bar__title">Template perso</div>
                    <p class="template-save-bar__text">
                      Sauvegarde le canvas actuel pour reutiliser la mise en page.
                    </p>
                  </div>
                  <button
                    type="button"
                    class="template-save-btn"
                    :disabled="!canSaveCurrent"
                    @click="openCreateForm"
                  >
                    <Save :size="14" />
                    <span>Nouveau template</span>
                  </button>
                </div>

                <div class="template-scroll">
                  <div v-if="!filteredTemplates.length" class="template-empty">
                    <div class="template-empty__title">Aucun template</div>
                    <p class="template-empty__text">
                      Ajuste la recherche, les tags ou la densite pour retrouver un preset.
                    </p>
                  </div>

                  <article
                    v-for="template in filteredTemplates"
                    :key="template.id"
                    class="template-card"
                    :style="{ '--template-card-accent': template.accent }"
                    role="button"
                    tabindex="0"
                    @click="applyFromCard(template)"
                    @keydown.enter.prevent="applyFromCard(template)"
                    @keydown.space.prevent="applyFromCard(template)"
                  >
                    <header class="template-card__head">
                      <div>
                        <div class="template-card__kicker">
                          <span class="template-card__category">{{ template.category }}</span>
                          <span class="template-card__dot"></span>
                          <span class="template-card__source">{{ template.source === 'system' ? 'Systeme' : 'Perso' }}</span>
                        </div>
                        <h3 class="template-card__title">{{ template.name }}</h3>
                        <p class="template-card__desc">{{ template.description }}</p>
                      </div>

                      <div class="template-card__actions">
                        <span class="template-density-pill">{{ densityLabel(template.density) }}</span>
                        <button
                          type="button"
                          class="template-apply-btn"
                          :disabled="applyingTemplateId === template.id"
                          @click.stop="applyFromCard(template)"
                        >
                          <Loader2 v-if="applyingTemplateId === template.id" :size="14" class="spin" />
                          <span>{{ applyingTemplateId === template.id ? 'Application...' : 'Appliquer' }}</span>
                        </button>
                      </div>
                    </header>

                    <div class="template-card__range">{{ rangeLabel(template.recommendedRange) }}</div>

                    <div class="template-card__preview">
                      <WidgetPreview
                        v-for="(entry, idx) in previewEntries(template)"
                        :key="`${template.id}-${entry.type}-${idx}`"
                        :model="entry.preview"
                        :accent="template.accent"
                      />
                    </div>

                    <footer class="template-card__footer">
                      <div class="template-card__tagline">{{ template.tags.slice(0, 3).join(' • ') }}</div>

                      <div v-if="template.source === 'custom'" class="template-card__custom-actions">
                        <button type="button" class="template-icon-btn" @click.stop="openEditForm(template)">
                          <Pencil :size="13" />
                        </button>
                        <button type="button" class="template-icon-btn is-danger" @click.stop="emit('delete-custom', template.id)">
                          <Trash2 :size="13" />
                        </button>
                      </div>
                    </footer>
                  </article>
                </div>
              </section>
            </DialogPanel>
          </TransitionChild>
        </div>
      </div>
    </Dialog>
  </TransitionRoot>

  <teleport to="body">
    <Transition
      enter-active-class="duration-180 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="duration-120 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div v-if="formOpen" class="template-form-overlay" role="dialog" aria-modal="true">
        <div class="template-form-backdrop" @click="formOpen = false"></div>
        <div class="template-form-shell" @click.stop>
          <header class="template-form-head">
            <div>
              <div class="template-form-kicker">{{ formMode === 'create' ? 'Nouveau template' : 'Edition template' }}</div>
              <h3 class="template-form-title">
                {{ formMode === 'create' ? 'Enregistrer le canvas actuel' : 'Modifier le template' }}
              </h3>
            </div>
            <button type="button" class="template-close" aria-label="Fermer" @click="formOpen = false">
              <X :size="16" />
            </button>
          </header>

          <form class="template-form-grid" @submit.prevent="submitForm">
            <label class="template-field">
              <span>Nom</span>
              <input v-model.trim="form.name" type="text" maxlength="70" required />
            </label>

            <label class="template-field">
              <span>Categorie</span>
              <input v-model.trim="form.category" type="text" maxlength="40" />
            </label>

            <label class="template-field template-field--full">
              <span>Description</span>
              <textarea v-model.trim="form.description" rows="3" maxlength="240"></textarea>
            </label>

            <label class="template-field">
              <span>Accent</span>
              <div class="template-color-field">
                <input v-model.trim="form.accent" type="text" placeholder="#38bdf8" />
                <input v-model="form.accent" type="color" />
              </div>
            </label>

            <label class="template-field">
              <span>Tags (separes par ,)</span>
              <input v-model.trim="form.tagsText" type="text" placeholder="executive, mensuel, board" />
            </label>

            <label class="template-field">
              <span>Plage recommandee</span>
              <select v-model="form.rangeKind">
                <option value="month">Mois</option>
                <option value="ytd">YTD</option>
                <option value="year">Annee</option>
                <option value="custom">Custom</option>
              </select>
            </label>

            <template v-if="form.rangeKind === 'custom'">
              <label class="template-field">
                <span>Du</span>
                <input v-model="form.from" type="date" />
              </label>
              <label class="template-field">
                <span>Au</span>
                <input v-model="form.to" type="date" />
              </label>
            </template>

            <p v-if="formError" class="template-form-error">{{ formError }}</p>

            <div class="template-form-actions">
              <button type="button" class="template-form-btn" @click="formOpen = false">Annuler</button>
              <button type="submit" class="template-form-btn is-primary">
                {{ formMode === 'create' ? 'Enregistrer' : 'Mettre a jour' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Transition>
  </teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { Dialog, DialogPanel, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue'
import { Loader2, Pencil, Save, Search, Trash2, X } from 'lucide-vue-next'
import WidgetPreview from '../palette/WidgetPreview.vue'
import { filterTemplates, mapTemplatePreview } from './statsTemplateUtils'
import type {
  EditableTemplateFields,
  StatsTemplateDensity,
  StatsTemplateRange,
  StatsTemplateRecord,
  StatsTemplateSort,
} from './templateTypes'
const props = withDefaults(
  defineProps<{
    open: boolean
    templates: StatsTemplateRecord[]
    canSaveCurrent?: boolean
    applyingTemplateId?: string | null
  }>(),
  {
    canSaveCurrent: false,
    applyingTemplateId: null,
  },
)

const emit = defineEmits<{
  (event: 'close'): void
  (event: 'apply', payload: StatsTemplateRecord): void
  (event: 'create-custom', payload: EditableTemplateFields): void
  (event: 'update-custom', payload: { id: string; changes: Partial<EditableTemplateFields> }): void
  (event: 'delete-custom', payload: string): void
}>()

type SourceFilter = 'all' | 'system' | 'custom'
type DensityFilter = 'all' | StatsTemplateDensity

type TemplateFormState = {
  name: string
  description: string
  category: string
  accent: string
  tagsText: string
  rangeKind: StatsTemplateRange['kind']
  from: string
  to: string
}

const sourceOptions: Array<{ value: SourceFilter; label: string }> = [
  { value: 'all', label: 'Tous' },
  { value: 'system', label: 'Systeme' },
  { value: 'custom', label: 'Perso' },
]

const densityOptions: Array<{ value: DensityFilter; label: string }> = [
  { value: 'all', label: 'Tous' },
  { value: 'dense', label: 'Dense' },
  { value: 'balanced', label: 'Balanced' },
  { value: 'minimal', label: 'Minimal' },
]

const sortOptions: Array<{ value: StatsTemplateSort; label: string }> = [
  { value: 'relevance', label: 'Pertinence' },
  { value: 'name', label: 'Nom' },
  { value: 'updated', label: 'Maj' },
  { value: 'density', label: 'Densite' },
]

const query = ref('')
const debouncedQuery = ref('')
const sourceFilter = ref<SourceFilter>('all')
const densityFilter = ref<DensityFilter>('all')
const sortMode = ref<StatsTemplateSort>('relevance')
const selectedCategory = ref('all')
const activeTags = ref<string[]>([])
const searchInputRef = ref<HTMLInputElement | null>(null)

const formOpen = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const editingTemplateId = ref<string | null>(null)
const formError = ref('')
const form = ref<TemplateFormState>({
  name: '',
  description: '',
  category: '',
  accent: '#38bdf8',
  tagsText: '',
  rangeKind: 'month',
  from: '',
  to: '',
})

let queryTimer: ReturnType<typeof window.setTimeout> | null = null

watch(query, (value) => {
  if (queryTimer) window.clearTimeout(queryTimer)
  queryTimer = window.setTimeout(() => {
    debouncedQuery.value = value
  }, 80)
})

watch(
  () => props.open,
  async (open) => {
    if (!open) {
      formOpen.value = false
      formError.value = ''
      return
    }
    await nextTick()
    searchInputRef.value?.focus()
  },
)

const templates = computed(() => props.templates ?? [])

const preCategoryTemplates = computed(() =>
  filterTemplates(templates.value, {
    query: debouncedQuery.value,
    category: 'all',
    source: sourceFilter.value,
    density: densityFilter.value,
    sort: sortMode.value,
    tags: activeTags.value,
  }),
)

const categoryChips = computed(() => {
  const counts = new Map<string, number>()
  for (const template of preCategoryTemplates.value) {
    const key = template.category || 'General'
    counts.set(key, (counts.get(key) ?? 0) + 1)
  }
  return [
    { label: 'Toutes', value: 'all', count: preCategoryTemplates.value.length },
    ...Array.from(counts.entries())
      .sort((a, b) => a[0].localeCompare(b[0], 'fr'))
      .map(([label, count]) => ({ label, value: label, count })),
  ]
})

watch(categoryChips, (chips) => {
  const exists = chips.some((entry) => entry.value === selectedCategory.value)
  if (!exists) selectedCategory.value = 'all'
})

const filteredTemplates = computed(() =>
  filterTemplates(templates.value, {
    query: debouncedQuery.value,
    category: selectedCategory.value,
    source: sourceFilter.value,
    density: densityFilter.value,
    sort: sortMode.value,
    tags: activeTags.value,
  }),
)

const visibleTags = computed(() => {
  const counts = new Map<string, number>()
  for (const template of preCategoryTemplates.value) {
    for (const tag of template.tags ?? []) {
      counts.set(tag, (counts.get(tag) ?? 0) + 1)
    }
  }
  return Array.from(counts.entries())
    .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0], 'fr'))
    .slice(0, 8)
    .map(([tag]) => tag)
})

function applyFromCard(template: StatsTemplateRecord) {
  emit('apply', template)
}

function toggleTag(tag: string) {
  const next = new Set(activeTags.value)
  if (next.has(tag)) next.delete(tag)
  else next.add(tag)
  activeTags.value = Array.from(next)
}

function previewEntries(template: StatsTemplateRecord) {
  return mapTemplatePreview(template, 2)
}

function densityLabel(value?: StatsTemplateDensity) {
  if (value === 'dense') return 'Dense'
  if (value === 'minimal') return 'Minimal'
  return 'Balanced'
}

function rangeLabel(range: StatsTemplateRange) {
  if (range.kind === 'month') return 'Plage recommandee: 30 jours'
  if (range.kind === 'ytd') return 'Plage recommandee: debut annee a aujourd hui'
  if (range.kind === 'year') return 'Plage recommandee: 12 derniers mois'
  if (range.from && range.to) return `Plage recommandee: ${range.from} -> ${range.to}`
  return 'Plage recommandee: libre'
}
function resetFilters() {
  query.value = ''
  debouncedQuery.value = ''
  sourceFilter.value = 'all'
  densityFilter.value = 'all'
  selectedCategory.value = 'all'
  sortMode.value = 'relevance'
  activeTags.value = []
}

function openCreateForm() {
  if (!props.canSaveCurrent) return
  formMode.value = 'create'
  editingTemplateId.value = null
  formError.value = ''
  form.value = {
    name: '',
    description: '',
    category: '',
    accent: '#38bdf8',
    tagsText: '',
    rangeKind: 'month',
    from: '',
    to: '',
  }
  formOpen.value = true
}

function openEditForm(template: StatsTemplateRecord) {
  if (template.source !== 'custom') return
  formMode.value = 'edit'
  editingTemplateId.value = template.id
  formError.value = ''
  form.value = {
    name: template.name,
    description: template.description,
    category: template.category,
    accent: template.accent,
    tagsText: (template.tags ?? []).join(', '),
    rangeKind: template.recommendedRange.kind,
    from: template.recommendedRange.from ?? '',
    to: template.recommendedRange.to ?? '',
  }
  formOpen.value = true
}

function normalizeTagsInput(raw: string) {
  const unique = new Map<string, string>()
  for (const part of raw.split(',')) {
    const tag = part.trim()
    if (!tag) continue
    const key = tag.toLowerCase()
    if (!unique.has(key)) unique.set(key, tag)
  }
  return Array.from(unique.values()).slice(0, 12)
}

function buildFormPayload(): EditableTemplateFields | null {
  const name = form.value.name.trim()
  if (!name) {
    formError.value = 'Le nom est obligatoire.'
    return null
  }

  const rangeKind = form.value.rangeKind
  let recommendedRange: StatsTemplateRange = { kind: rangeKind }
  if (rangeKind === 'custom') {
    if (!form.value.from || !form.value.to) {
      formError.value = 'Completer les dates pour la plage custom.'
      return null
    }
    const from = form.value.from <= form.value.to ? form.value.from : form.value.to
    const to = form.value.from <= form.value.to ? form.value.to : form.value.from
    recommendedRange = { kind: 'custom', from, to }
  }

  const accent = /^#[0-9a-fA-F]{6}$/.test(form.value.accent.trim())
    ? form.value.accent.trim()
    : '#38bdf8'

  return {
    name,
    description: form.value.description.trim(),
    category: form.value.category.trim() || 'General',
    tags: normalizeTagsInput(form.value.tagsText),
    accent,
    recommendedRange,
  }
}

function submitForm() {
  formError.value = ''
  const payload = buildFormPayload()
  if (!payload) return

  if (formMode.value === 'create') {
    emit('create-custom', payload)
    formOpen.value = false
    return
  }

  if (!editingTemplateId.value) {
    formError.value = 'Template a modifier introuvable.'
    return
  }
  emit('update-custom', { id: editingTemplateId.value, changes: payload })
  formOpen.value = false
}
</script>

<style scoped src="./StatsTemplateLibrary.css"></style>

