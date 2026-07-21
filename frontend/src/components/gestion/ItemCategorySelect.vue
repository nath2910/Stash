<template>
  <div
    ref="rootEl"
    class="category-field"
    :class="{ 'category-field--dropdown': display === 'dropdown' }"
  >
    <div class="field-heading">
      <div>
        <label class="field-label">{{ label }}</label>
        <p class="field-helper">Famille globale de l'article</p>
      </div>
      <button type="button" class="manage-link" @click="openManager">
        <Settings2 class="h-3.5 w-3.5" />
        <span>Personnaliser</span>
      </button>
    </div>

    <template v-if="display === 'dropdown'">
      <button
        type="button"
        class="category-dropdown-trigger"
        :class="{ 'is-open': menuOpen }"
        :aria-expanded="menuOpen ? 'true' : 'false'"
        @click="toggleMenu"
      >
        <span
          class="category-dropdown-trigger__icon"
          :class="`type-${selectedOption.value.toLowerCase()}`"
        >
          <component :is="categoryIcon(selectedOption.value)" class="h-4 w-4" />
        </span>
        <span class="category-dropdown-trigger__text">
          {{ selectedOption.label || placeholder }}
        </span>
        <ChevronDown class="h-4 w-4 category-dropdown-trigger__chevron" />
      </button>

      <div v-if="menuOpen" class="category-dropdown-menu">
        <button
          v-for="option in itemTypeOptions"
          :key="option.value"
          type="button"
          class="category-dropdown-option"
          :class="[`type-${option.value.toLowerCase()}`, { 'is-active': modelValue === option.value }]"
          @click="selectType(option.value)"
        >
          <span class="category-choice__icon">
            <component :is="categoryIcon(option.value)" class="h-4 w-4" />
          </span>
          <span class="category-choice__text">
            <span class="category-choice__label">{{ option.label }}</span>
            <span v-if="option.label !== option.defaultLabel" class="category-choice__base">
              {{ option.defaultLabel }}
            </span>
          </span>
          <Check v-if="modelValue === option.value" class="h-4 w-4 category-dropdown-check" />
        </button>
      </div>
    </template>

    <div v-else class="category-grid" role="radiogroup" aria-label="Categorie principale">
      <button
        v-for="option in itemTypeOptions"
        :key="option.value"
        type="button"
        class="category-choice"
        :class="[`type-${option.value.toLowerCase()}`, { 'is-active': modelValue === option.value }]"
        :aria-pressed="modelValue === option.value"
        @click="selectType(option.value)"
      >
        <span class="category-choice__icon">
          <component :is="categoryIcon(option.value)" class="h-4 w-4" />
        </span>
        <span class="category-choice__text">
          <span class="category-choice__label">{{ option.label }}</span>
          <span v-if="option.label !== option.defaultLabel" class="category-choice__base">
            {{ option.defaultLabel }}
          </span>
        </span>
      </button>
    </div>

    <teleport to="body">
      <div
        v-if="managerOpen"
        class="category-manager-backdrop"
        role="dialog"
        aria-modal="true"
        @click.self="closeManager"
      >
        <section class="category-manager-panel" @click.stop>
          <header class="manager-header">
            <div>
              <p class="manager-kicker">Organisation</p>
              <h3 class="manager-title">Categories principales</h3>
              <p class="manager-subtitle">
                Ajoute tes familles globales et renomme les categories existantes.
              </p>
            </div>
            <button
              type="button"
              class="manager-close"
              aria-label="Fermer"
              @click="closeManager"
            >
              <X class="h-4 w-4" />
            </button>
          </header>

          <form class="manager-add-form" @submit.prevent="addCategory">
            <input
              v-model.trim="newCategoryDraft"
              type="text"
              maxlength="40"
              class="manager-input"
              placeholder="Nouvelle categorie principale"
            />
            <button
              type="submit"
              class="manager-add-button"
              :disabled="!newCategoryDraft"
            >
              <Plus class="h-4 w-4" />
              <span>Ajouter</span>
            </button>
          </form>

          <div v-if="managerMessage" class="manager-feedback">
            {{ managerMessage }}
          </div>

          <div class="manager-list">
            <div
              v-for="option in itemTypeOptions"
              :key="option.value"
              class="manager-row"
              :class="{ 'is-selected': modelValue === option.value }"
            >
              <span class="manager-row__icon" :class="`type-${option.value.toLowerCase()}`">
                <component :is="categoryIcon(option.value)" class="h-4 w-4" />
              </span>

              <template v-if="editingType === option.value">
                <div class="manager-row__edit">
                  <input
                    v-model.trim="editDraft"
                    type="text"
                    maxlength="40"
                    class="manager-input"
                    @keydown.enter.prevent="commitEdit"
                    @keydown.esc.prevent="cancelEdit"
                  />
                  <button
                    type="button"
                    class="manager-action is-confirm"
                    :disabled="!editDraft"
                    @click="commitEdit"
                  >
                    <Check class="h-4 w-4" />
                    <span>Valider</span>
                  </button>
                  <button
                    type="button"
                    class="manager-action"
                    @click="cancelEdit"
                  >
                    <X class="h-4 w-4" />
                    <span>Annuler</span>
                  </button>
                </div>
              </template>

              <template v-else>
                <button type="button" class="manager-row__main" @click="selectType(option.value)">
                  <span class="manager-row__label">{{ option.label }}</span>
                  <span class="manager-row__meta">
                    {{ categoryMeta(option) }}
                  </span>
                </button>
                <div class="manager-row__actions">
                  <button
                    type="button"
                    class="manager-action"
                    @click="startEdit(option)"
                  >
                    <Pencil class="h-4 w-4" />
                    <span>Modifier</span>
                  </button>
                  <button
                    type="button"
                    class="manager-action is-danger"
                    :disabled="!canDeleteCategory(option)"
                    :title="deleteCategoryTitle(option)"
                    @click="deleteCategory(option)"
                  >
                    <Trash2 class="h-4 w-4" />
                    <span>Supprimer</span>
                  </button>
                </div>
              </template>
            </div>
          </div>
        </section>
      </div>
    </teleport>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  Check,
  ChevronDown,
  CircleHelp,
  Package,
  Pencil,
  Plus,
  Settings2,
  Tag,
  Ticket,
  Trash2,
  X,
} from 'lucide-vue-next'
import {
  addItemCategory,
  canRemoveItemCategory,
  itemTypeLabel,
  normalizeCategoryLabel,
  normalizeItemType,
  readStoredItemCategories,
  removeItemCategory,
  renameItemCategory,
  resolveItemTypeOptions,
  writeStoredItemCategories,
} from '@/RegleItem/itemCategoryStore'
import { typeOf } from '@/utils/snkVente'

const props = defineProps({
  label: { type: String, default: 'Categorie principale' },
  modelValue: { type: String, default: 'SNEAKER' },
  items: { type: Array, default: () => [] },
  userId: { type: [String, Number], default: 'guest' },
  labels: { type: Object, default: null },
  display: { type: String, default: 'grid' },
  placeholder: { type: String, default: 'Choisir une categorie' },
})

const emit = defineEmits(['update:modelValue', 'labelsChange'])

const rootEl = ref(null)
const managerOpen = ref(false)
const menuOpen = ref(false)
const storedLabels = ref(readStoredItemCategories(props.userId))
const editingType = ref('')
const editDraft = ref('')
const newCategoryDraft = ref('')
const managerMessage = ref('')

const effectiveLabels = computed(() => props.labels || storedLabels.value)
const optionLabels = computed(() => {
  const labels = { ...(effectiveLabels.value || {}) }
  const currentType = normalizeItemType(props.modelValue)
  if (currentType && !labels[currentType]) {
    labels[currentType] = itemTypeLabel(currentType, labels)
  }
  return labels
})
const itemTypeOptions = computed(() => resolveItemTypeOptions(optionLabels.value))
const itemTypeUseCounts = computed(() => {
  const counts = new Map()
  for (const item of Array.isArray(props.items) ? props.items : []) {
    const itemType = normalizeItemType(typeOf(item || {}))
    if (!itemType) continue
    counts.set(itemType, (counts.get(itemType) || 0) + 1)
  }
  return counts
})
const selectedOption = computed(
  () =>
    itemTypeOptions.value.find((option) => modelValueIs(option.value)) ||
    itemTypeOptions.value[0] || {
      value: 'OTHER',
      label: props.placeholder,
      defaultLabel: props.placeholder,
    },
)

watch(
  () => props.userId,
  (userId) => {
    storedLabels.value = readStoredItemCategories(userId)
  },
)

function persist(nextLabels) {
  storedLabels.value = writeStoredItemCategories(props.userId, nextLabels)
  emit('labelsChange', storedLabels.value)
}

function selectType(type) {
  managerMessage.value = ''
  emit('update:modelValue', normalizeItemType(type))
  menuOpen.value = false
}

function toggleMenu() {
  menuOpen.value = !menuOpen.value
}

function openManager() {
  menuOpen.value = false
  managerOpen.value = true
}

function closeManager() {
  managerOpen.value = false
  newCategoryDraft.value = ''
  managerMessage.value = ''
  cancelEdit()
}

function startEdit(option) {
  managerMessage.value = ''
  editingType.value = option.value
  editDraft.value = option.label
}

function cancelEdit() {
  editingType.value = ''
  editDraft.value = ''
}

function commitEdit() {
  const label = normalizeCategoryLabel(editDraft.value)
  if (!editingType.value || !label) {
    cancelEdit()
    return
  }
  managerMessage.value = ''
  persist(renameItemCategory(effectiveLabels.value, editingType.value, label))
  cancelEdit()
}

function addCategory() {
  const result = addItemCategory(effectiveLabels.value, newCategoryDraft.value)
  if (!result.type) return
  managerMessage.value = ''
  persist(result.labels)
  emit('update:modelValue', result.type)
  newCategoryDraft.value = ''
}

function canDeleteCategory(option) {
  return canRemoveItemCategory(option?.value) && itemTypeOptions.value.length > 1
}

function categoryUseCount(option) {
  return itemTypeUseCounts.value.get(normalizeItemType(option?.value)) || 0
}

function categoryMeta(option) {
  const count = categoryUseCount(option)
  if (count) return `Utilisee par ${count} item${count > 1 ? 's' : ''}`
  return option.custom ? 'Personnalisee' : `Base ${option.defaultLabel}`
}

function deleteCategoryTitle(option) {
  if (!canDeleteCategory(option)) return 'Garde au moins une categorie'
  const count = categoryUseCount(option)
  if (count) return `Categorie utilisee par ${count} item${count > 1 ? 's' : ''}`
  return `Supprimer ${option?.label || 'la categorie'}`
}

function deleteCategory(option) {
  if (!canDeleteCategory(option)) {
    managerMessage.value = 'Garde au moins une categorie principale.'
    return
  }
  const count = categoryUseCount(option)
  if (count) {
    managerMessage.value =
      `Categorie utilisee par ${count} item${count > 1 ? 's' : ''}. ` +
      'Modifie ou reassigne ces items avant de la supprimer.'
    return
  }
  const label = option?.label || 'cette categorie'
  if (typeof window !== 'undefined' && !window.confirm(`Supprimer "${label}" ?`)) return
  const nextLabels = removeItemCategory(effectiveLabels.value, option.value)
  persist(nextLabels)
  const nextOptions = resolveItemTypeOptions(nextLabels)
  if (modelValueIs(option.value) || !nextOptions.some((item) => modelValueIs(item.value))) {
    emit('update:modelValue', nextOptions[0]?.value || '')
  }
  if (editingType.value === option.value) cancelEdit()
  managerMessage.value = 'Categorie supprimee.'
}

function modelValueIs(type) {
  return normalizeItemType(props.modelValue) === normalizeItemType(type)
}

function categoryIcon(type) {
  if (type === 'TICKET') return Ticket
  if (type === 'POKEMON_CARD') return Tag
  if (type === 'OTHER') return CircleHelp
  return Package
}

function onDocumentPointerDown(event) {
  if (!menuOpen.value) return
  if (rootEl.value?.contains(event.target)) return
  menuOpen.value = false
}

function onStoredLabelsChange(event) {
  const detail = event?.detail || {}
  if (String(detail.userId || 'guest') !== String(props.userId || 'guest')) return
  storedLabels.value = readStoredItemCategories(props.userId)
}

onMounted(() => {
  document.addEventListener('pointerdown', onDocumentPointerDown, true)
  if (typeof window !== 'undefined') {
    window.addEventListener('snk:item-categories-change', onStoredLabelsChange)
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onDocumentPointerDown, true)
  if (typeof window !== 'undefined') {
    window.removeEventListener('snk:item-categories-change', onStoredLabelsChange)
  }
})
</script>

<style scoped>
.category-field {
  position: relative;
  display: grid;
  min-width: 0;
  gap: 0.35rem;
}

.category-field--dropdown {
  z-index: 20;
}

.field-heading {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 0.45rem;
  margin-bottom: 0.35rem;
  min-width: 0;
}

.field-heading > div {
  min-width: 0;
}

.field-label {
  display: block;
  color: rgb(229 231 235);
  font-size: 0.875rem;
  font-weight: 700;
}

.field-helper {
  margin-top: 0.125rem;
  color: rgb(156 163 175);
  font-size: 0.75rem;
}

.manage-link {
  display: inline-flex;
  min-height: 2rem;
  align-items: center;
  justify-content: center;
  gap: 0.375rem;
  border-radius: 999px;
  border: 1px solid rgba(168, 85, 247, 0.28);
  background: rgba(88, 28, 135, 0.2);
  padding: 0.28rem 0.62rem;
  color: rgb(216 180 254);
  font-size: 0.75rem;
  font-weight: 700;
  justify-self: end;
  transition:
    background 140ms ease,
    border-color 140ms ease,
    color 140ms ease;
}

.manage-link:hover {
  border-color: rgba(216, 180, 254, 0.58);
  background: rgba(126, 34, 206, 0.24);
  color: rgb(243 232 255);
}

.category-dropdown-trigger {
  display: flex;
  min-height: 2.625rem;
  width: 100%;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 0.65rem;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 0.85rem;
  background: #ffffff;
  padding: 0.45rem 0.7rem;
  color: #0f172a;
  text-align: left;
  font-size: 0.88rem;
  font-weight: 750;
  transition:
    border-color 140ms ease,
    background 140ms ease,
    box-shadow 140ms ease;
}

.category-dropdown-trigger:hover,
.category-dropdown-trigger.is-open {
  border-color: rgba(20, 184, 166, 0.56);
  background: #f8fafc;
  box-shadow: 0 0 0 3px rgba(20, 184, 166, 0.14);
}

.category-dropdown-trigger__icon {
  display: inline-grid;
  height: 1.9rem;
  width: 1.9rem;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 0.65rem;
  border: 1px solid rgba(20, 184, 166, 0.28);
  background: #ecfdf5;
  color: #0f766e;
}

.category-dropdown-trigger__text {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-dropdown-trigger__chevron {
  flex: 0 0 auto;
  color: #64748b;
  transition: transform 140ms ease;
}

.category-dropdown-trigger.is-open .category-dropdown-trigger__chevron {
  transform: rotate(180deg);
}

.category-dropdown-menu {
  position: absolute;
  left: 0;
  top: calc(100% + 0.45rem);
  z-index: 220;
  display: grid;
  width: max(100%, min(34rem, calc(100vw - 2rem)));
  max-height: min(430px, calc(100dvh - 11rem));
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.45rem;
  align-content: start;
  overflow-y: auto;
  overscroll-behavior: contain;
  border: 1px solid rgba(203, 213, 225, 0.9);
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.99);
  padding: 0.55rem;
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.18);
  scrollbar-width: thin;
  scrollbar-color: rgba(15, 118, 110, 0.42) rgba(241, 245, 249, 0.9);
}

.category-dropdown-menu::-webkit-scrollbar {
  width: 7px;
}

.category-dropdown-menu::-webkit-scrollbar-track {
  border-radius: 999px;
  background: rgba(241, 245, 249, 0.9);
}

.category-dropdown-menu::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.42);
}

.category-dropdown-option {
  display: flex;
  min-width: 0;
  min-height: 2.8rem;
  align-items: center;
  gap: 0.6rem;
  border: 1px solid rgba(203, 213, 225, 0.76);
  border-radius: 0.85rem;
  background: #ffffff;
  padding: 0.5rem 0.6rem;
  text-align: left;
  transition:
    border-color 140ms ease,
    background 140ms ease,
    box-shadow 140ms ease;
}

.category-dropdown-option:hover,
.category-dropdown-option.is-active {
  border-color: rgba(20, 184, 166, 0.5);
  background: #ecfdf5;
}

.category-dropdown-option.is-active {
  box-shadow: inset 0 0 0 1px rgba(20, 184, 166, 0.22);
}

.category-dropdown-option .category-choice__label {
  color: #0f172a;
  font-size: 0.84rem;
}

.category-dropdown-option .category-choice__base {
  color: #64748b;
}

.category-dropdown-option .category-choice__icon {
  height: 1.85rem;
  width: 1.85rem;
}

.category-dropdown-check {
  flex: 0 0 auto;
  color: #0f766e;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.625rem;
}

.category-choice {
  display: flex;
  min-height: 3.375rem;
  align-items: center;
  gap: 0.75rem;
  border-radius: 0.875rem;
  border: 1px solid rgba(75, 85, 99, 0.95);
  background: rgba(17, 24, 39, 0.7);
  padding: 0.7rem 0.85rem;
  text-align: left;
  transition:
    transform 140ms ease,
    border-color 140ms ease,
    background 140ms ease,
    box-shadow 140ms ease;
}

.category-choice:hover {
  border-color: rgba(148, 163, 184, 0.65);
  background: rgba(15, 23, 42, 0.92);
}

.category-choice.is-active {
  border-color: rgba(168, 85, 247, 0.82);
  background: rgba(88, 28, 135, 0.28);
  box-shadow:
    0 0 0 1px rgba(168, 85, 247, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.category-choice__icon,
.manager-row__icon {
  display: inline-grid;
  height: 2rem;
  width: 2rem;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 0.7rem;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(148, 163, 184, 0.1);
  color: rgb(226 232 240);
}

.type-sneaker .category-choice__icon,
.manager-row__icon.type-sneaker {
  background: rgba(168, 85, 247, 0.16);
  color: rgb(221 214 254);
}

.type-pokemon_card .category-choice__icon,
.manager-row__icon.type-pokemon_card {
  background: rgba(34, 211, 238, 0.14);
  color: rgb(165 243 252);
}

.type-ticket .category-choice__icon,
.manager-row__icon.type-ticket {
  background: rgba(245, 158, 11, 0.16);
  color: rgb(253 230 138);
}

.type-other .category-choice__icon,
.manager-row__icon.type-other {
  background: rgba(148, 163, 184, 0.14);
  color: rgb(226 232 240);
}

.category-choice__text {
  display: grid;
  min-width: 0;
  gap: 0.125rem;
}

.category-choice__label {
  overflow: hidden;
  color: rgb(243 244 246);
  font-size: 0.9rem;
  font-weight: 750;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-choice__base {
  overflow: hidden;
  color: rgb(156 163 175);
  font-size: 0.68rem;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-manager-backdrop {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: rgba(2, 6, 23, 0.74);
  padding: 0;
  backdrop-filter: blur(10px);
}

.category-manager-panel {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-height: min(calc(100dvh - 1.5rem), 42rem);
  max-width: 36rem;
  min-height: 0;
  overflow: hidden;
  border: 1px solid rgba(71, 85, 105, 0.85);
  border-radius: 1.25rem 1.25rem 0 0;
  background:
    linear-gradient(180deg, rgba(17, 24, 39, 0.98), rgba(15, 23, 42, 0.98)),
    rgb(17 24 39);
  box-shadow: 0 22px 70px rgba(0, 0, 0, 0.46);
}

.manager-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  border-bottom: 1px solid rgba(55, 65, 81, 0.82);
  padding: 1.1rem;
}

.manager-kicker {
  margin: 0 0 0.25rem;
  color: rgb(192 132 252);
  font-size: 0.7rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.manager-title {
  margin: 0;
  color: white;
  font-size: 1.05rem;
  font-weight: 800;
}

.manager-subtitle {
  margin: 0.35rem 0 0;
  max-width: 28rem;
  color: rgb(156 163 175);
  font-size: 0.8rem;
  line-height: 1.35;
}

.manager-close {
  display: inline-grid;
  height: 2rem;
  width: 2rem;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 0.65rem;
  color: rgb(156 163 175);
  transition:
    background 140ms ease,
    color 140ms ease;
}

.manager-close:hover {
  background: rgba(55, 65, 81, 0.7);
  color: white;
}

.manager-list {
  display: grid;
  flex: 1 1 auto;
  gap: 0.625rem;
  min-height: 0;
  max-height: 100%;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 1rem;
  scrollbar-gutter: stable;
}

.manager-add-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.625rem;
  border-bottom: 1px solid rgba(55, 65, 81, 0.82);
  padding: 1rem;
}

.manager-add-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  border-radius: 0.75rem;
  background: rgb(124 58 237);
  padding: 0.65rem 0.9rem;
  color: white;
  font-size: 0.84rem;
  font-weight: 800;
  transition:
    background 140ms ease,
    opacity 140ms ease;
}

.manager-add-button:hover:not(:disabled) {
  background: rgb(147 51 234);
}

.manager-add-button:disabled {
  cursor: not-allowed;
  opacity: 0.46;
}

.manager-feedback {
  margin: 0.75rem 1rem 0;
  border: 1px solid rgba(14, 165, 233, 0.28);
  border-radius: 0.85rem;
  background: rgba(240, 253, 250, 0.9);
  color: #0f766e;
  padding: 0.65rem 0.75rem;
  font-size: 0.8rem;
  font-weight: 800;
}

.manager-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  border: 1px solid rgba(75, 85, 99, 0.78);
  border-radius: 0.95rem;
  background: rgba(2, 6, 23, 0.34);
  padding: 0.625rem;
}

.manager-row.is-selected {
  border-color: rgba(168, 85, 247, 0.5);
  background: rgba(88, 28, 135, 0.16);
}

.manager-row__main {
  display: grid;
  min-width: 0;
  flex: 1;
  gap: 0.125rem;
  text-align: left;
}

.manager-row__actions,
.manager-row__edit {
  display: inline-flex;
  min-width: 0;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 0.45rem;
}

.manager-row__edit {
  flex: 1;
}

.manager-row__label {
  overflow: hidden;
  color: rgb(243 244 246);
  font-size: 0.9rem;
  font-weight: 750;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.manager-row__meta {
  color: rgb(148 163 184);
  font-size: 0.72rem;
  font-weight: 600;
}

.manager-input {
  min-width: 0;
  flex: 1;
  border-radius: 0.75rem;
  border: 1px solid rgba(107, 114, 128, 0.85);
  background: rgba(2, 6, 23, 0.62);
  padding: 0.65rem 0.75rem;
  color: rgb(243 244 246);
  font-size: 0.875rem;
  outline: none;
}

.manager-input:focus {
  border-color: rgba(168, 85, 247, 0.8);
  box-shadow: 0 0 0 3px rgba(168, 85, 247, 0.18);
}

.manager-action {
  display: inline-flex;
  min-height: 2.1rem;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  border: 1px solid rgba(75, 85, 99, 0.9);
  border-radius: 0.7rem;
  padding: 0 0.65rem;
  color: rgb(209 213 219);
  font-size: 0.76rem;
  font-weight: 850;
  white-space: nowrap;
  transition:
    background 140ms ease,
    border-color 140ms ease,
    color 140ms ease;
}

.manager-action:hover:not(:disabled) {
  border-color: rgba(168, 85, 247, 0.5);
  background: rgba(126, 34, 206, 0.18);
  color: rgb(243 232 255);
}

.manager-action.is-confirm {
  color: rgb(167 243 208);
}

.manager-action.is-danger {
  color: rgb(254 202 202);
}

.manager-action.is-danger:hover:not(:disabled) {
  border-color: rgba(248, 113, 113, 0.55);
  background: rgba(239, 68, 68, 0.12);
  color: rgb(254 226 226);
}

.manager-action:disabled {
  cursor: not-allowed;
  opacity: 0.42;
}

@media (min-width: 640px) {
  .category-manager-backdrop {
    align-items: center;
    padding: 1rem;
  }

  .category-manager-panel {
    border-radius: 1.25rem;
  }
}

@media (max-width: 420px) {
  .category-grid {
    grid-template-columns: 1fr;
  }

  .category-dropdown-menu {
    width: 100%;
    grid-template-columns: 1fr;
  }

  .field-heading {
    align-items: stretch;
    flex-direction: column;
  }

  .manage-link {
    justify-content: center;
  }

  .manager-add-form,
  .manager-row {
    grid-template-columns: 1fr;
  }

  .manager-row {
    display: grid;
  }

  .manager-row__actions,
  .manager-row__edit,
  .manager-action {
    width: 100%;
  }

  .category-manager-panel {
    max-height: calc(100dvh - 0.75rem);
  }
}

@media (max-width: 720px) {
  .field-heading {
    grid-template-columns: 1fr;
  }

  .manage-link {
    justify-self: start;
  }
}

.field-label,
.category-choice__label,
.manager-row__label {
  color: #0f172a;
}

.field-helper,
.category-choice__base,
.manager-subtitle,
.manager-row__meta {
  color: #64748b;
}

.manage-link {
  border-color: rgba(20, 184, 166, 0.32);
  background: rgba(240, 253, 250, 0.86);
  color: #0f766e;
}

.manage-link:hover {
  border-color: rgba(20, 184, 166, 0.54);
  background: #ccfbf1;
  color: #0f766e;
}

.category-choice,
.manager-row {
  border-color: rgba(125, 211, 252, 0.34);
  background: rgba(255, 255, 255, 0.84);
}

.category-choice:hover {
  border-color: rgba(20, 184, 166, 0.46);
  background: #f0fdfa;
}

.category-choice.is-active,
.manager-row.is-selected {
  border-color: rgba(20, 184, 166, 0.62);
  background: linear-gradient(135deg, #ccfbf1, #e0f2fe);
  box-shadow: 0 0 0 1px rgba(20, 184, 166, 0.16), 0 12px 24px rgba(14, 116, 144, 0.08);
}

.category-choice__icon,
.manager-row__icon,
.type-sneaker .category-choice__icon,
.manager-row__icon.type-sneaker,
.type-pokemon_card .category-choice__icon,
.manager-row__icon.type-pokemon_card,
.type-ticket .category-choice__icon,
.manager-row__icon.type-ticket,
.type-other .category-choice__icon,
.manager-row__icon.type-other {
  border-color: rgba(20, 184, 166, 0.26);
  background: rgba(236, 253, 245, 0.9);
  color: #0f766e;
}

.category-manager-panel {
  border-color: rgba(125, 211, 252, 0.38);
  background:
    linear-gradient(135deg, rgba(14, 165, 233, 0.08), transparent 40%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.96)),
    #ffffff;
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.18);
}

.manager-header,
.manager-add-form {
  border-color: rgba(125, 211, 252, 0.26);
}

.manager-kicker {
  color: #0f766e;
}

.manager-title {
  color: #0f172a;
}

.manager-close,
.manager-action {
  border-color: rgba(125, 211, 252, 0.32);
  background: rgba(255, 255, 255, 0.78);
  color: #475569;
}

.manager-close:hover,
.manager-action:hover:not(:disabled) {
  border-color: rgba(20, 184, 166, 0.48);
  background: #ecfdf5;
  color: #0f766e;
}

.manager-action.is-danger {
  border-color: rgba(220, 38, 38, 0.9);
  background: #ef4444;
  color: #ffffff;
  box-shadow: 0 10px 18px rgba(220, 38, 38, 0.16);
}

.manager-action.is-danger:hover:not(:disabled) {
  border-color: #b91c1c;
  background: #dc2626;
  color: #ffffff;
}

.manager-action.is-danger:disabled {
  border-color: rgba(252, 165, 165, 0.9);
  background: #fecaca;
  color: #ffffff;
  box-shadow: none;
}

.manager-add-button {
  background: linear-gradient(135deg, #0f766e, #0e7490);
}

.manager-add-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #0d9488, #0284c7);
}

.manager-input {
  border-color: rgba(148, 163, 184, 0.28);
  background: #ffffff;
  color: #0f172a;
}

.manager-input:focus {
  border-color: rgba(20, 184, 166, 0.72);
  box-shadow: 0 0 0 3px rgba(20, 184, 166, 0.14);
}
</style>
