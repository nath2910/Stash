<template>
  <div ref="rootEl" class="subcategory-field">
    <div class="subcategory-field-header">
      <div>
        <label :for="buttonId" class="subcategory-label">
          Sous-categorie
        </label>
      </div>
      <button
        type="button"
        class="subcategory-manage-button"
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
      <span class="subcategory-trigger__text" :class="{ 'is-placeholder': !modelValue }">
        {{ selectedLabel }}
      </span>
      <ChevronDown class="subcategory-trigger__chevron" :class="{ 'is-open': menuOpen }" />
    </button>

    <div v-if="menuOpen" class="subcategory-menu">
      <div class="max-h-56 overflow-y-auto p-1.5">
        <button type="button" class="subcategory-option is-empty" @click="selectValue('')">
          <span>Aucune sous-categorie</span>
          <Check v-if="!modelValue" class="h-4 w-4" />
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
          <Check v-if="option === modelValue" class="h-4 w-4 shrink-0" />
        </button>
      </div>
      <div class="subcategory-menu-footer">
        <button type="button" class="manage-menu-button" @click="openManager">
          <Settings2 class="h-3.5 w-3.5" />
          <span>Gerer les sous-categories</span>
        </button>
      </div>
    </div>

    <teleport to="body">
      <div
        v-if="managerOpen"
        class="subcategory-manager-backdrop"
        @click.self="closeManager"
      >
        <section
          class="subcategory-manager-panel"
          @click.stop
        >
          <header class="subcategory-manager-header">
            <div>
              <p class="subcategory-manager-kicker">
                {{ currentTypeLabel }}
              </p>
              <h3 class="subcategory-manager-title">Sous-categories</h3>
              <p class="subcategory-manager-subtitle">
                Ajoute les familles de detail qui te servent vraiment pour ce type d'article.
              </p>
            </div>
            <button
              type="button"
              class="subcategory-manager-close"
              aria-label="Fermer"
              @click="closeManager"
            >
              <X class="h-4 w-4" />
            </button>
          </header>

          <div class="subcategory-manager-body">
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

            <div v-if="managerMessage" class="subcategory-feedback">
              {{ managerMessage }}
            </div>

            <div v-if="managerItems.length" class="subcategory-manager-list">
              <div v-for="item in managerItems" :key="item" class="subcategory-row">
                <template v-if="renameFrom === item">
                  <div class="subcategory-row-edit">
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
                      class="subcategory-action is-confirm"
                      @click="commitRename"
                    >
                      <Check class="h-4 w-4" />
                      <span>Valider</span>
                    </button>
                    <button
                      type="button"
                      class="subcategory-action"
                      @click="cancelRename"
                    >
                      <X class="h-4 w-4" />
                      <span>Annuler</span>
                    </button>
                  </div>
                </template>
                <template v-else>
                  <button
                    type="button"
                    class="subcategory-row-main"
                    @click="selectValue(item)"
                  >
                    <span>{{ item }}</span>
                    <small v-if="subcategoryUseCount(item)">
                      {{ subcategoryMeta(item) }}
                    </small>
                  </button>
                  <div class="subcategory-row-actions">
                    <button
                      type="button"
                      class="subcategory-action"
                      @click="startRename(item)"
                    >
                      <Pencil class="h-4 w-4" />
                      <span>Modifier</span>
                    </button>
                    <button
                      type="button"
                      class="subcategory-action is-danger"
                      :title="removeTitle(item)"
                      @click="removeItem(item)"
                    >
                      <Trash2 class="h-4 w-4" />
                      <span>Supprimer</span>
                    </button>
                  </div>
                </template>
              </div>
            </div>

            <p
              v-else
              class="subcategory-empty-state"
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
import { getField, typeOf } from '@/utils/snkVente'

const props = defineProps({
  modelValue: { type: String, default: '' },
  type: { type: String, default: 'SNEAKER' },
  userId: { type: [String, Number], default: 'guest' },
  discovered: { type: Object, default: () => ({}) },
  items: { type: Array, default: () => [] },
  categoryLabels: { type: Object, default: null },
})

const emit = defineEmits(['update:modelValue'])

const rootEl = ref(null)
const menuOpen = ref(false)
const managerOpen = ref(false)
const draftName = ref('')
const renameFrom = ref('')
const renameDraft = ref('')
const managerMessage = ref('')
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
const subcategoryUseCounts = computed(() => {
  const counts = new Map()
  for (const item of Array.isArray(props.items) ? props.items : []) {
    if (normalizeItemType(typeOf(item || {})) !== currentType.value) {
      continue
    }
    const name = normalizeSubcategoryName(getField(item, 'categorie', ''))
    if (!name) continue
    const key = name.toLocaleLowerCase('fr')
    counts.set(key, (counts.get(key) || 0) + 1)
  }
  return counts
})
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
  managerMessage.value = ''
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
  managerMessage.value = ''
  cancelRename()
}

function addDraft() {
  const cleaned = normalizeSubcategoryName(draftName.value)
  if (!cleaned) return
  if (mainCategoryAliases.value.has(cleaned.toLocaleLowerCase('fr'))) return
  managerMessage.value = ''
  persist(
    addSubcategory(storedMap.value, currentType.value, cleaned, props.categoryLabels || undefined),
  )
  emit('update:modelValue', cleaned)
  draftName.value = ''
}

function subcategoryUseCount(item) {
  const key = normalizeSubcategoryName(item).toLocaleLowerCase('fr')
  return subcategoryUseCounts.value.get(key) || 0
}

function subcategoryMeta(item) {
  const count = subcategoryUseCount(item)
  return `Utilisee par ${count} item${count > 1 ? 's' : ''}`
}

function removeTitle(item) {
  const count = subcategoryUseCount(item)
  if (count) return `Sous-categorie utilisee par ${count} item${count > 1 ? 's' : ''}`
  return `Supprimer ${item}`
}

function removeItem(item) {
  const count = subcategoryUseCount(item)
  if (count) {
    managerMessage.value =
      `Sous-categorie utilisee par ${count} item${count > 1 ? 's' : ''}. ` +
      'Modifie ou reassigne ces items avant de la supprimer.'
    return
  }
  if (typeof window !== 'undefined' && !window.confirm(`Supprimer "${item}" ?`)) return
  persist(
    removeSubcategory(storedMap.value, currentType.value, item, props.categoryLabels || undefined),
  )
  if (
    normalizeSubcategoryName(props.modelValue).toLocaleLowerCase('fr') ===
    item.toLocaleLowerCase('fr')
  ) {
    emit('update:modelValue', '')
  }
  managerMessage.value = 'Sous-categorie supprimee.'
}

function startRename(item) {
  managerMessage.value = ''
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
  managerMessage.value = ''
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
.subcategory-field {
  position: relative;
  display: grid;
  width: 100%;
  min-width: 0;
  gap: 0.3rem;
}

.subcategory-field-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 0.45rem;
  min-width: 0;
}

.subcategory-label {
  display: block;
  color: #334155;
  font-size: 0.76rem;
  font-weight: 800;
}

.subcategory-manage-button {
  display: inline-flex;
  min-height: 2rem;
  max-width: 100%;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  border: 1px solid rgba(20, 184, 166, 0.32);
  border-radius: 999px;
  background: rgba(240, 253, 250, 0.9);
  padding: 0.25rem 0.62rem;
  color: #0f766e;
  font-size: 0.74rem;
  font-weight: 850;
  justify-self: end;
  white-space: nowrap;
  transition:
    border-color 140ms ease,
    background 140ms ease,
    color 140ms ease;
}

.subcategory-manage-button:hover {
  border-color: rgba(20, 184, 166, 0.55);
  background: #ccfbf1;
  color: #0f766e;
}

.subcategory-trigger {
  display: flex;
  min-height: 2.5rem;
  width: 100%;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  border: 1px solid rgba(100, 116, 139, 0.24);
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.92);
  padding: 0.5rem 0.72rem;
  color: #0f172a;
  text-align: left;
  outline: none;
  transition:
    background 140ms ease,
    border-color 140ms ease,
    box-shadow 140ms ease;
}

.subcategory-trigger:hover,
.subcategory-trigger:focus,
.subcategory-trigger.is-open {
  border-color: rgba(20, 184, 166, 0.56);
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(20, 184, 166, 0.12);
}

.subcategory-trigger__text {
  min-width: 0;
  overflow: hidden;
  color: #0f172a;
  font-size: 0.88rem;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.subcategory-trigger__text.is-placeholder {
  color: #94a3b8;
}

.subcategory-trigger__chevron {
  width: 1rem;
  height: 1rem;
  flex: 0 0 auto;
  color: #475569;
  transition: transform 140ms ease;
}

.subcategory-trigger__chevron.is-open {
  transform: rotate(180deg);
}

.subcategory-menu {
  position: absolute;
  left: 0;
  right: 0;
  z-index: 80;
  margin-top: 0.5rem;
  overflow: hidden;
  border: 1px solid rgba(125, 211, 252, 0.38);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 18px 55px rgba(15, 23, 42, 0.14);
}

.subcategory-option {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  border-radius: 0.7rem;
  padding: 0.65rem 0.75rem;
  color: #334155;
  text-align: left;
  font-size: 0.875rem;
  font-weight: 700;
  transition:
    background 140ms ease,
    color 140ms ease;
}

.subcategory-option:hover,
.subcategory-option.is-active {
  background: #ecfdf5;
  color: #0f766e;
}

.subcategory-option.is-empty {
  color: #64748b;
}

.subcategory-option svg {
  color: #0f766e;
}

.subcategory-menu-footer {
  border-top: 1px solid rgba(226, 232, 240, 0.9);
  padding: 0.5rem;
}

.manage-menu-button {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  border: 1px solid rgba(20, 184, 166, 0.32);
  border-radius: 999px;
  background: rgba(240, 253, 250, 0.9);
  padding: 0.6rem 0.75rem;
  color: #0f766e;
  font-size: 0.78rem;
  font-weight: 850;
  transition:
    background 140ms ease,
    border-color 140ms ease;
}

.manage-menu-button:hover {
  border-color: rgba(20, 184, 166, 0.54);
  background: #ccfbf1;
}

.subcategory-manager-backdrop {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: rgba(15, 23, 42, 0.48);
  padding: 0;
  backdrop-filter: blur(10px);
}

.subcategory-manager-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  width: 100%;
  max-height: min(calc(100dvh - 1.5rem), 42rem);
  max-width: 44rem;
  min-height: 0;
  overflow: hidden;
  border: 1px solid rgba(125, 211, 252, 0.4);
  border-radius: 20px 20px 0 0;
  background:
    linear-gradient(135deg, rgba(236, 253, 245, 0.76), rgba(224, 242, 254, 0.58) 42%, transparent),
    #fbfaf6;
  color: #0f172a;
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.24);
}

.subcategory-manager-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  border-bottom: 1px solid rgba(203, 213, 225, 0.82);
  padding: 1.15rem 1.25rem 1rem;
}

.subcategory-manager-kicker {
  margin: 0 0 0.25rem;
  color: #0f766e;
  font-size: 0.7rem;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.subcategory-manager-title {
  margin: 0;
  color: #0f172a;
  font-size: clamp(1.15rem, 2vw, 1.45rem);
  font-weight: 950;
}

.subcategory-manager-subtitle {
  margin: 0.35rem 0 0;
  max-width: 28rem;
  color: #64748b;
  font-size: 0.85rem;
  font-weight: 650;
  line-height: 1.35;
}

.subcategory-manager-close {
  display: inline-grid;
  width: 2.25rem;
  height: 2.25rem;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid rgba(148, 163, 184, 0.32);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.84);
  color: #475569;
  transition:
    background 140ms ease,
    border-color 140ms ease,
    color 140ms ease;
}

.subcategory-manager-close:hover {
  border-color: rgba(20, 184, 166, 0.5);
  background: #ecfdf5;
  color: #0f766e;
}

.subcategory-manager-body {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
  min-height: 0;
  overflow: hidden;
  padding: 1rem 1.25rem 1.25rem;
}

.subcategory-add-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.7rem;
}

.subcategory-add-input,
.subcategory-row-input {
  min-width: 0;
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 14px;
  background: #ffffff;
  padding: 0.78rem 0.9rem;
  color: #0f172a;
  font-size: 0.92rem;
  font-weight: 700;
  outline: none;
}

.subcategory-add-input::placeholder {
  color: #94a3b8;
}

.subcategory-add-input:focus,
.subcategory-row-input:focus {
  border-color: rgba(20, 184, 166, 0.72);
  box-shadow: 0 0 0 3px rgba(20, 184, 166, 0.14);
}

.subcategory-add-button {
  display: inline-flex;
  min-height: 2.8rem;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  border: 1px solid rgba(15, 118, 110, 0.2);
  border-radius: 14px;
  background: linear-gradient(135deg, #0f766e, #0e7490);
  padding: 0.72rem 1rem;
  color: #ffffff;
  font-size: 0.88rem;
  font-weight: 900;
  transition:
    background 140ms ease,
    opacity 140ms ease;
}

.subcategory-add-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #0d9488, #0284c7);
}

.subcategory-add-button:disabled {
  cursor: not-allowed;
  opacity: 0.48;
}

.subcategory-feedback {
  border: 1px solid rgba(14, 165, 233, 0.28);
  border-radius: 14px;
  background: rgba(240, 253, 250, 0.9);
  color: #0f766e;
  padding: 0.65rem 0.75rem;
  font-size: 0.82rem;
  font-weight: 800;
}

.subcategory-manager-list {
  display: grid;
  flex: 1 1 auto;
  gap: 0.6rem;
  min-height: 0;
  max-height: 100%;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding-right: 0.1rem;
  scrollbar-gutter: stable;
}

.subcategory-row {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 0.6rem;
  border: 1px solid rgba(125, 211, 252, 0.32);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.82);
  padding: 0.62rem;
}

.subcategory-row-main {
  display: grid;
  gap: 0.12rem;
  min-width: 0;
  flex: 1;
  overflow: hidden;
  color: #0f172a;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.9rem;
  font-weight: 850;
}

.subcategory-row-main span,
.subcategory-row-main small {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.subcategory-row-main small {
  color: #64748b;
  font-size: 0.72rem;
  font-weight: 750;
}

.subcategory-row-actions,
.subcategory-row-edit {
  display: inline-flex;
  min-width: 0;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 0.45rem;
}

.subcategory-row-edit {
  flex: 1;
}

.subcategory-action {
  display: inline-flex;
  min-height: 2.15rem;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  border: 1px solid rgba(125, 211, 252, 0.34);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.88);
  color: #475569;
  padding: 0 0.65rem;
  font-size: 0.76rem;
  font-weight: 850;
  white-space: nowrap;
  transition:
    border-color 140ms ease,
    background 140ms ease,
    color 140ms ease;
}

.subcategory-action:hover:not(:disabled),
.subcategory-action.is-confirm:hover:not(:disabled) {
  border-color: rgba(20, 184, 166, 0.5);
  background: #ecfdf5;
  color: #0f766e;
}

.subcategory-action.is-confirm {
  color: #047857;
}

.subcategory-action.is-danger {
  color: #b91c1c;
}

.subcategory-action.is-danger:hover:not(:disabled) {
  border-color: rgba(248, 113, 113, 0.42);
  background: #fef2f2;
  color: #b91c1c;
}

.subcategory-empty-state {
  border: 1px dashed rgba(125, 211, 252, 0.55);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.68);
  padding: 1.2rem;
  color: #64748b;
  text-align: center;
  font-size: 0.88rem;
  font-weight: 700;
}

@media (min-width: 640px) {
  .subcategory-manager-backdrop {
    align-items: center;
    padding: 1rem;
  }

  .subcategory-manager-panel {
    border-radius: 20px;
  }
}

@media (max-width: 440px) {
  .subcategory-field-header,
  .subcategory-add-form {
    grid-template-columns: 1fr;
  }

  .subcategory-field-header {
    display: grid;
  }

  .subcategory-row {
    display: grid;
  }

  .subcategory-row-actions,
  .subcategory-row-edit,
  .subcategory-action {
    width: 100%;
  }

  .subcategory-manager-panel {
    max-height: calc(100dvh - 0.75rem);
  }

  .subcategory-manage-button,
  .subcategory-add-button {
    width: 100%;
  }
}

@media (max-width: 720px) {
  .subcategory-field-header {
    grid-template-columns: 1fr;
  }

  .subcategory-manage-button {
    justify-self: start;
  }
}
</style>
