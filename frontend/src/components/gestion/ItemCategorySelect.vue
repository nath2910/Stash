<template>
  <div class="category-field">
    <div class="field-heading">
      <div>
        <label class="field-label">Categorie principale</label>
        <p class="field-helper">Famille globale de l'article</p>
      </div>
      <button type="button" class="manage-link" @click="openManager">
        <Settings2 class="h-3.5 w-3.5" />
        <span>Personnaliser</span>
      </button>
    </div>

    <div class="category-grid" role="radiogroup" aria-label="Categorie principale">
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
                  class="manager-icon is-confirm"
                  aria-label="Valider"
                  :disabled="!editDraft"
                  @click="commitEdit"
                >
                  <Check class="h-4 w-4" />
                </button>
                <button
                  type="button"
                  class="manager-icon"
                  aria-label="Annuler"
                  @click="cancelEdit"
                >
                  <X class="h-4 w-4" />
                </button>
              </template>

              <template v-else>
                <button type="button" class="manager-row__main" @click="selectType(option.value)">
                  <span class="manager-row__label">{{ option.label }}</span>
                  <span class="manager-row__meta">
                    {{ option.custom ? 'Personnalisee' : `Base ${option.defaultLabel}` }}
                  </span>
                </button>
                <button
                  type="button"
                  class="manager-icon"
                  aria-label="Renommer"
                  @click="startEdit(option)"
                >
                  <Pencil class="h-4 w-4" />
                </button>
                <button
                  v-if="!option.custom"
                  type="button"
                  class="manager-icon"
                  aria-label="Reinitialiser"
                  :disabled="option.label === option.defaultLabel"
                  @click="resetLabel(option.value)"
                >
                  <RotateCcw class="h-4 w-4" />
                </button>
                <button
                  v-else
                  type="button"
                  class="manager-icon is-danger"
                  aria-label="Supprimer"
                  @click="deleteCategory(option)"
                >
                  <Trash2 class="h-4 w-4" />
                </button>
              </template>
            </div>
          </div>
        </section>
      </div>
    </teleport>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import {
  Check,
  CircleHelp,
  Package,
  Pencil,
  Plus,
  RotateCcw,
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
  resetItemCategory,
  resolveItemTypeOptions,
  writeStoredItemCategories,
} from '@/RegleItem/itemCategoryStore'

const props = defineProps({
  modelValue: { type: String, default: 'SNEAKER' },
  userId: { type: [String, Number], default: 'guest' },
  labels: { type: Object, default: null },
})

const emit = defineEmits(['update:modelValue', 'labelsChange'])

const managerOpen = ref(false)
const storedLabels = ref(readStoredItemCategories(props.userId))
const editingType = ref('')
const editDraft = ref('')
const newCategoryDraft = ref('')

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
  emit('update:modelValue', normalizeItemType(type))
}

function openManager() {
  managerOpen.value = true
}

function closeManager() {
  managerOpen.value = false
  newCategoryDraft.value = ''
  cancelEdit()
}

function startEdit(option) {
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
  persist(renameItemCategory(effectiveLabels.value, editingType.value, label))
  cancelEdit()
}

function addCategory() {
  const result = addItemCategory(effectiveLabels.value, newCategoryDraft.value)
  if (!result.type) return
  persist(result.labels)
  emit('update:modelValue', result.type)
  newCategoryDraft.value = ''
}

function resetLabel(type) {
  persist(resetItemCategory(effectiveLabels.value, type))
}

function deleteCategory(option) {
  if (!canRemoveItemCategory(option?.value)) return
  const nextLabels = removeItemCategory(effectiveLabels.value, option.value)
  persist(nextLabels)
  if (modelValueIs(option.value)) {
    emit('update:modelValue', 'SNEAKER')
  }
  if (editingType.value === option.value) cancelEdit()
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
</script>

<style scoped>
.category-field {
  position: relative;
}

.field-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.625rem;
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
  align-items: center;
  gap: 0.375rem;
  border-radius: 999px;
  border: 1px solid rgba(168, 85, 247, 0.28);
  background: rgba(88, 28, 135, 0.2);
  padding: 0.35rem 0.625rem;
  color: rgb(216 180 254);
  font-size: 0.75rem;
  font-weight: 700;
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
  width: 100%;
  max-height: min(88vh, 42rem);
  max-width: 36rem;
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
  gap: 0.625rem;
  overflow-y: auto;
  padding: 1rem;
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

.manager-icon {
  display: inline-grid;
  height: 2.1rem;
  width: 2.1rem;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid rgba(75, 85, 99, 0.9);
  border-radius: 0.7rem;
  color: rgb(209 213 219);
  transition:
    background 140ms ease,
    border-color 140ms ease,
    color 140ms ease;
}

.manager-icon:hover:not(:disabled) {
  border-color: rgba(168, 85, 247, 0.5);
  background: rgba(126, 34, 206, 0.18);
  color: rgb(243 232 255);
}

.manager-icon.is-confirm {
  color: rgb(167 243 208);
}

.manager-icon.is-danger {
  color: rgb(254 202 202);
}

.manager-icon.is-danger:hover:not(:disabled) {
  border-color: rgba(248, 113, 113, 0.55);
  background: rgba(239, 68, 68, 0.12);
  color: rgb(254 226 226);
}

.manager-icon:disabled {
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

  .field-heading {
    align-items: stretch;
    flex-direction: column;
  }

  .manage-link {
    justify-content: center;
  }

  .manager-add-form {
    grid-template-columns: 1fr;
  }
}
</style>
