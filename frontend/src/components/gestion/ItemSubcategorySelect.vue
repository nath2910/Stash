<template>
  <div ref="rootEl" class="relative">
    <div class="mb-2 flex items-center justify-between gap-3">
      <div>
        <label :for="buttonId" class="block text-sm font-medium text-gray-200">
          Sous-categorie
        </label>
      </div>
      <button
        type="button"
        class="inline-flex items-center gap-1.5 rounded-full border border-purple-400/25 bg-purple-500/10 px-2.5 py-0.5 text-xs font-bold text-purple-200 transition hover:border-purple-300/50 hover:bg-purple-500/15 hover:text-purple-100"
        @click="openManager"
      >
        <Settings2 class="h-3.5 w-3.5" />
        <span>Gerer</span>
      </button>
    </div>

    <button
      :id="buttonId"
      type="button"
      class="subcategory-trigger"
      :class="{ 'is-open': menuOpen }"
      :aria-expanded="menuOpen ? 'true' : 'false'"
      @click="menuOpen = !menuOpen"
    >
      <span class="min-w-0 truncate" :class="modelValue ? 'text-gray-100' : 'text-gray-500'">
        {{ selectedLabel }}
      </span>
      <ChevronDown class="h-4 w-4 shrink-0 text-gray-400" :class="{ 'rotate-180': menuOpen }" />
    </button>

    <div v-if="menuOpen" class="subcategory-menu">
      <div class="max-h-56 overflow-y-auto p-1.5">
        <button type="button" class="subcategory-option is-empty" @click="selectValue('')">
          <span>Aucune sous-categorie</span>
          <Check v-if="!modelValue" class="h-4 w-4 text-purple-200" />
        </button>
        <button
          v-for="option in optionsForType"
          :key="option"
          type="button"
          class="subcategory-option"
          :class="{ 'is-active': option === modelValue }"
          @click="selectValue(option)"
        >
          <span class="min-w-0 truncate">{{ option }}</span>
          <Check v-if="option === modelValue" class="h-4 w-4 shrink-0 text-purple-200" />
        </button>
      </div>
      <div class="border-t border-gray-800/90 p-2">
        <button type="button" class="manage-menu-button" @click="openManager">
          <Settings2 class="h-3.5 w-3.5" />
          <span>Gerer les sous-categories</span>
        </button>
      </div>
    </div>

    <teleport to="body">
      <div
        v-if="managerOpen"
        class="fixed inset-0 z-[10000] flex items-end justify-center bg-black/75 p-0 backdrop-blur-sm sm:items-center sm:p-4"
        @click.self="closeManager"
      >
        <section
          class="w-full max-w-lg rounded-t-2xl border border-gray-700 bg-gray-900 shadow-2xl sm:rounded-2xl"
          @click.stop
        >
          <header class="flex items-start justify-between gap-3 border-b border-gray-800 p-4">
            <div>
              <p class="text-[11px] font-extrabold uppercase tracking-[0.08em] text-purple-300">
                {{ currentTypeLabel }}
              </p>
              <h3 class="mt-1 text-lg font-extrabold text-white">Sous-categories</h3>
              <p class="mt-1 text-xs text-gray-400">
                Ajoute les familles de detail qui te servent vraiment pour ce type d'article.
              </p>
            </div>
            <button
              type="button"
              class="rounded-lg p-2 text-gray-400 transition hover:bg-gray-800 hover:text-white"
              aria-label="Fermer"
              @click="closeManager"
            >
              <X class="h-4 w-4" />
            </button>
          </header>

          <div class="space-y-4 p-4">
            <form class="subcategory-add-form" @submit.prevent="addDraft">
              <input
                v-model.trim="draftName"
                type="text"
                maxlength="60"
                placeholder="Nouvelle sous-categorie"
                class="subcategory-add-input"
              />
              <button type="submit" class="subcategory-add-button" :disabled="!draftName">
                <Plus class="h-4 w-4" />
                <span>Ajouter</span>
              </button>
            </form>

            <div v-if="managerItems.length" class="max-h-72 space-y-2 overflow-y-auto pr-1">
              <div v-for="item in managerItems" :key="item" class="subcategory-row">
                <template v-if="renameFrom === item">
                  <input
                    v-model.trim="renameDraft"
                    type="text"
                    maxlength="60"
                    class="subcategory-row-input"
                    @keydown.enter.prevent="commitRename"
                    @keydown.esc.prevent="cancelRename"
                  />
                  <button
                    type="button"
                    class="icon-action text-emerald-200 hover:border-emerald-400/50 hover:bg-emerald-500/10"
                    aria-label="Valider"
                    @click="commitRename"
                  >
                    <Check class="h-4 w-4" />
                  </button>
                  <button
                    type="button"
                    class="icon-action text-gray-300 hover:border-gray-500 hover:bg-gray-800"
                    aria-label="Annuler"
                    @click="cancelRename"
                  >
                    <X class="h-4 w-4" />
                  </button>
                </template>
                <template v-else>
                  <button
                    type="button"
                    class="min-w-0 flex-1 truncate text-left text-sm font-bold text-gray-100"
                    @click="selectValue(item)"
                  >
                    {{ item }}
                  </button>
                  <button
                    type="button"
                    class="icon-action text-gray-300 hover:border-purple-400/50 hover:bg-purple-500/10 hover:text-purple-100"
                    aria-label="Renommer"
                    @click="startRename(item)"
                  >
                    <Pencil class="h-4 w-4" />
                  </button>
                  <button
                    type="button"
                    class="icon-action text-red-200 hover:border-red-400/50 hover:bg-red-500/10"
                    aria-label="Supprimer"
                    @click="removeItem(item)"
                  >
                    <Trash2 class="h-4 w-4" />
                  </button>
                </template>
              </div>
            </div>

            <p
              v-else
              class="rounded-xl border border-dashed border-gray-700 bg-gray-950/60 px-4 py-6 text-center text-sm text-gray-400"
            >
              Aucune sous-categorie definie.
            </p>
          </div>
        </section>
      </div>
    </teleport>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Check, ChevronDown, Pencil, Plus, Settings2, Trash2, X } from 'lucide-vue-next'
import { buildItemCategoryAliases, itemTypeLabel } from '@/RegleItem/itemCategoryStore'
import {
  addSubcategory,
  normalizeItemType,
  normalizeSubcategoryName,
  readStoredSubcategories,
  removeSubcategory,
  renameSubcategory,
  resolveSubcategoryOptions,
  writeStoredSubcategories,
} from '@/RegleItem/subcategoryStore'

const props = defineProps({
  modelValue: { type: String, default: '' },
  type: { type: String, default: 'SNEAKER' },
  userId: { type: [String, Number], default: 'guest' },
  discovered: { type: Object, default: () => ({}) },
  categoryLabels: { type: Object, default: null },
})

const emit = defineEmits(['update:modelValue'])

const rootEl = ref(null)
const menuOpen = ref(false)
const managerOpen = ref(false)
const draftName = ref('')
const renameFrom = ref('')
const renameDraft = ref('')
const storedMap = ref(
  readStoredSubcategories(props.userId, undefined, props.categoryLabels || undefined),
)

const buttonId = `subcategory-${Math.random().toString(16).slice(2)}`
const currentType = computed(() => normalizeItemType(props.type, props.categoryLabels || undefined))
const currentTypeLabel = computed(() =>
  itemTypeLabel(currentType.value, props.categoryLabels || undefined),
)
const mainCategoryAliases = computed(() =>
  buildItemCategoryAliases(props.categoryLabels || undefined),
)
const optionsForType = computed(() =>
  resolveSubcategoryOptions(currentType.value, {
    stored: storedMap.value,
    discovered: props.discovered,
    currentValue: props.modelValue,
    mainCategoryAliases: mainCategoryAliases.value,
    categoryLabels: props.categoryLabels || undefined,
  }),
)
const managerItems = computed(() =>
  (storedMap.value[currentType.value] || []).filter(
    (item) =>
      !mainCategoryAliases.value.has(normalizeSubcategoryName(item).toLocaleLowerCase('fr')),
  ),
)
const selectedLabel = computed(() =>
  mainCategoryAliases.value.has(normalizeSubcategoryName(props.modelValue).toLocaleLowerCase('fr'))
    ? 'Choisir une sous-categorie'
    : props.modelValue || 'Choisir une sous-categorie',
)

watch(
  () => props.userId,
  (userId) => {
    storedMap.value = readStoredSubcategories(userId, undefined, props.categoryLabels || undefined)
  },
)

watch(
  () => props.categoryLabels,
  (labels) => {
    storedMap.value = readStoredSubcategories(props.userId, undefined, labels || undefined)
  },
)

watch(
  () => [props.modelValue, mainCategoryAliases.value],
  () => {
    if (
      props.modelValue &&
      mainCategoryAliases.value.has(
        normalizeSubcategoryName(props.modelValue).toLocaleLowerCase('fr'),
      )
    ) {
      emit('update:modelValue', '')
    }
  },
  { immediate: true },
)

function persist(nextMap) {
  storedMap.value = writeStoredSubcategories(
    props.userId,
    nextMap,
    undefined,
    props.categoryLabels || undefined,
  )
}

function selectValue(value) {
  emit('update:modelValue', normalizeSubcategoryName(value))
  menuOpen.value = false
}

function openManager() {
  menuOpen.value = false
  managerOpen.value = true
}

function closeManager() {
  managerOpen.value = false
  draftName.value = ''
  cancelRename()
}

function addDraft() {
  const cleaned = normalizeSubcategoryName(draftName.value)
  if (!cleaned) return
  if (mainCategoryAliases.value.has(cleaned.toLocaleLowerCase('fr'))) return
  persist(
    addSubcategory(storedMap.value, currentType.value, cleaned, props.categoryLabels || undefined),
  )
  emit('update:modelValue', cleaned)
  draftName.value = ''
}

function removeItem(item) {
  persist(
    removeSubcategory(storedMap.value, currentType.value, item, props.categoryLabels || undefined),
  )
  if (
    normalizeSubcategoryName(props.modelValue).toLocaleLowerCase('fr') ===
    item.toLocaleLowerCase('fr')
  ) {
    emit('update:modelValue', '')
  }
}

function startRename(item) {
  renameFrom.value = item
  renameDraft.value = item
}

function cancelRename() {
  renameFrom.value = ''
  renameDraft.value = ''
}

function commitRename() {
  const from = renameFrom.value
  const to = normalizeSubcategoryName(renameDraft.value)
  if (!from || !to) {
    cancelRename()
    return
  }
  persist(
    renameSubcategory(
      storedMap.value,
      currentType.value,
      from,
      to,
      props.categoryLabels || undefined,
    ),
  )
  if (
    normalizeSubcategoryName(props.modelValue).toLocaleLowerCase('fr') ===
    from.toLocaleLowerCase('fr')
  ) {
    emit('update:modelValue', to)
  }
  cancelRename()
}

function onDocumentPointerDown(event) {
  if (!menuOpen.value) return
  if (rootEl.value?.contains(event.target)) return
  menuOpen.value = false
}

function onStoredSubcategoriesChange(event) {
  const detail = event?.detail || {}
  if (String(detail.userId || 'guest') !== String(props.userId || 'guest')) return
  storedMap.value = readStoredSubcategories(
    props.userId,
    undefined,
    props.categoryLabels || undefined,
  )
}

onMounted(() => {
  document.addEventListener('pointerdown', onDocumentPointerDown, true)
  if (typeof window !== 'undefined') {
    window.addEventListener('snk:item-subcategories-change', onStoredSubcategoriesChange)
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onDocumentPointerDown, true)
  if (typeof window !== 'undefined') {
    window.removeEventListener('snk:item-subcategories-change', onStoredSubcategoriesChange)
  }
})
</script>

<style scoped>
.subcategory-trigger {
  display: flex;
  min-height: 2.625rem;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  border: 1px solid rgb(75 85 99);
  border-radius: var(--radius-lg, 0.5rem);
  background: rgb(17 24 39);
  padding: 0.625rem 0.75rem;
  text-align: left;
  color: rgb(243 244 246);
  font-size: 0.875rem;
  line-height: 1.25rem;
  transition:
    background 140ms ease,
    border-color 140ms ease,
    box-shadow 140ms ease;
}

.subcategory-trigger:hover {
  border-color: rgba(148, 163, 184, 0.65);
  background: rgba(15, 23, 42, 0.92);
}

.subcategory-trigger:focus,
.subcategory-trigger.is-open {
  border-color: rgba(168, 85, 247, 0.78);
  box-shadow: 0 0 0 3px rgba(168, 85, 247, 0.18);
  outline: none;
}

.subcategory-menu {
  position: absolute;
  left: 0;
  right: 0;
  z-index: 50;
  margin-top: 0.5rem;
  overflow: hidden;
  border: 1px solid rgba(75, 85, 99, 0.92);
  border-radius: 1rem;
  background: rgba(2, 6, 23, 0.98);
  box-shadow: 0 18px 55px rgba(0, 0, 0, 0.38);
}

.subcategory-option {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  border-radius: 0.7rem;
  padding: 0.65rem 0.75rem;
  text-align: left;
  font-size: 0.875rem;
  font-weight: 650;
  color: rgb(209 213 219);
  transition:
    background 140ms ease,
    color 140ms ease;
}

.subcategory-option:hover,
.subcategory-option.is-active {
  background: rgba(124, 58, 237, 0.2);
  color: rgb(243 244 246);
}

.subcategory-option.is-empty {
  color: rgb(156 163 175);
}

.manage-menu-button {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  border: 1px solid rgba(168, 85, 247, 0.34);
  border-radius: 999px;
  background: rgba(88, 28, 135, 0.18);
  padding: 0.6rem 0.75rem;
  color: rgb(233 213 255);
  font-size: 0.78rem;
  font-weight: 800;
  transition:
    background 140ms ease,
    border-color 140ms ease;
}

.manage-menu-button:hover {
  border-color: rgba(216, 180, 254, 0.6);
  background: rgba(126, 34, 206, 0.24);
}

.subcategory-add-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.625rem;
}

.subcategory-add-input,
.subcategory-row-input {
  min-width: 0;
  border: 1px solid rgba(75, 85, 99, 0.95);
  border-radius: 0.85rem;
  background: rgba(2, 6, 23, 0.55);
  padding: 0.72rem 0.85rem;
  color: rgb(243 244 246);
  font-size: 0.875rem;
  outline: none;
}

.subcategory-add-input::placeholder {
  color: rgb(107 114 128);
}

.subcategory-add-input:focus,
.subcategory-row-input:focus {
  border-color: rgba(168, 85, 247, 0.78);
  box-shadow: 0 0 0 3px rgba(168, 85, 247, 0.18);
}

.subcategory-add-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  border-radius: 0.85rem;
  background: rgb(124 58 237);
  padding: 0.72rem 0.9rem;
  color: white;
  font-size: 0.86rem;
  font-weight: 800;
  transition:
    background 140ms ease,
    opacity 140ms ease;
}

.subcategory-add-button:hover:not(:disabled) {
  background: rgb(147 51 234);
}

.subcategory-add-button:disabled {
  cursor: not-allowed;
  opacity: 0.48;
}

.subcategory-row {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  border: 1px solid rgba(75, 85, 99, 0.78);
  border-radius: 0.9rem;
  background: rgba(2, 6, 23, 0.34);
  padding: 0.55rem;
}

.icon-action {
  display: inline-grid;
  height: 2.05rem;
  width: 2.05rem;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 0.7rem;
  border: 1px solid rgba(75, 85, 99, 0.9);
  background: rgba(15, 23, 42, 0.72);
  transition:
    border-color 140ms ease,
    background 140ms ease,
    color 140ms ease;
}

@media (max-width: 440px) {
  .subcategory-add-form {
    grid-template-columns: 1fr;
  }
}

:global(.gestion-page-light) .relative label,
:global(.gestion-page-light) .relative .text-gray-200,
:global(.home-page-light) .relative label,
:global(.home-page-light) .relative .text-gray-200 {
  color: #0f172a;
}

:global(.gestion-page-light) .relative .text-purple-200,
:global(.home-page-light) .relative .text-purple-200 {
  color: #0f766e;
}

.subcategory-trigger {
  border-color: rgba(148, 163, 184, 0.28);
  background: #ffffff;
  color: #0f172a;
}

.subcategory-trigger:hover,
.subcategory-trigger:focus,
.subcategory-trigger.is-open {
  border-color: rgba(20, 184, 166, 0.56);
  background: #f8fafc;
  box-shadow: 0 0 0 3px rgba(20, 184, 166, 0.14);
}

.subcategory-trigger :is(.text-gray-100, .text-gray-400, .text-gray-500) {
  color: #0f172a;
}

.subcategory-menu {
  border-color: rgba(125, 211, 252, 0.38);
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 18px 55px rgba(15, 23, 42, 0.14);
}

.subcategory-option {
  color: #334155;
}

.subcategory-option:hover,
.subcategory-option.is-active {
  background: #ecfdf5;
  color: #0f766e;
}

.subcategory-option.is-empty {
  color: #64748b;
}

.manage-menu-button,
.subcategory-add-button {
  border-color: rgba(20, 184, 166, 0.32);
  background: linear-gradient(135deg, #0f766e, #0e7490);
  color: #ffffff;
}

.manage-menu-button:hover,
.subcategory-add-button:hover:not(:disabled) {
  border-color: rgba(20, 184, 166, 0.54);
  background: linear-gradient(135deg, #0d9488, #0284c7);
}

.subcategory-add-input,
.subcategory-row-input {
  border-color: rgba(148, 163, 184, 0.28);
  background: #ffffff;
  color: #0f172a;
}

.subcategory-add-input::placeholder {
  color: #94a3b8;
}

.subcategory-add-input:focus,
.subcategory-row-input:focus {
  border-color: rgba(20, 184, 166, 0.72);
  box-shadow: 0 0 0 3px rgba(20, 184, 166, 0.14);
}

.subcategory-row {
  border-color: rgba(125, 211, 252, 0.32);
  background: rgba(255, 255, 255, 0.78);
}

.icon-action {
  border-color: rgba(125, 211, 252, 0.32);
  background: rgba(255, 255, 255, 0.8);
  color: #475569;
}
</style>
