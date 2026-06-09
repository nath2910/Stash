<template>
  <section class="quick-add-panel">
    <div class="panel-heading">
      <div>
        <p class="panel-eyebrow">Ajout rapide</p>
        <h2>Ajouter un item</h2>
      </div>
      <button
        type="button"
        class="ghost-button"
        :class="{ 'is-active': showDetails }"
        :aria-expanded="showDetails"
        @click="showDetails = !showDetails"
      >
        <SlidersHorizontal class="h-4 w-4" aria-hidden="true" />
        <span>Détails</span>
      </button>
    </div>

    <form class="quick-form" @submit.prevent="submit">
      <ItemCategorySelect
        class="field field--category"
        label="Catégorie"
        :model-value="form.type"
        display="dropdown"
        placeholder="Choisir une catégorie"
        :user-id="currentUserId"
        :labels="categoryLabels"
        @update:modelValue="setType"
        @labels-change="setCategoryLabels"
      />

      <label class="field field--name">
        <span>Nom de l'item</span>
        <input
          v-model.trim="form.nomItem"
          type="text"
          autocomplete="off"
          placeholder="Nom, modèle, référence..."
          required
        />
      </label>

      <ItemSubcategorySelect
        v-model="form.categorie"
        class="field field--subcategory"
        label="Sous-catégorie"
        placeholder="Marque, famille..."
        empty-label="Aucune sous-catégorie"
        :type="form.type"
        :user-id="currentUserId"
        :discovered="discoveredSubcategories"
        :category-labels="categoryLabels"
      />

      <label class="field field--price">
        <span>Prix achat</span>
        <input
          v-model.number="form.prixRetail"
          type="number"
          min="0"
          step="0.01"
          placeholder="110"
          required
        />
      </label>

      <div class="field field--date">
        <span>Date d'achat</span>
        <CompactDateInput v-model="form.dateAchat" light size="md" />
      </div>

      <div class="field field--quantity">
        <span>Quantite</span>
        <div class="quantity-stepper">
          <button
            type="button"
            class="quantity-button"
            :disabled="Number(form.quantity) <= 1"
            aria-label="Diminuer la quantite"
            @click="decreaseQuantity"
          >
            <Minus class="h-4 w-4" aria-hidden="true" />
          </button>
          <input
            v-model.number="form.quantity"
            class="quantity-input"
            type="number"
            min="1"
            max="50"
            step="1"
            required
            aria-label="Quantite"
            @blur="setQuantity(form.quantity)"
          />
          <button
            type="button"
            class="quantity-button"
            :disabled="Number(form.quantity) >= 50"
            aria-label="Augmenter la quantite"
            @click="increaseQuantity"
          >
            <Plus class="h-4 w-4" aria-hidden="true" />
          </button>
        </div>
      </div>

      <template v-if="showDetails">
        <div class="field">
          <span>Date de vente</span>
          <CompactDateInput v-model="form.dateVente" light size="md" />
        </div>

        <label class="field">
          <span>Prix de vente</span>
          <input v-model.number="form.prixResell" type="number" min="0" step="0.01" placeholder="180" />
        </label>

        <label class="field field--3">
          <span>Notes</span>
          <input
            v-model.trim="form.description"
            type="text"
            placeholder="Lieu d'achat, état, accessoires inclus..."
          />
        </label>

        <label v-for="field in metadataFields" :key="field.key" class="field">
          <span>{{ field.label }}</span>
          <input
            v-model.trim="form.metadata[field.key]"
            type="text"
            :placeholder="field.placeholder"
          />
        </label>
      </template>

      <div class="form-actions">
        <button type="submit" class="submit-button" :disabled="saving">
          <Plus class="h-4 w-4" aria-hidden="true" />
          <span>{{ saving ? 'Enregistrement...' : 'Ajouter' }}</span>
        </button>
      </div>
    </form>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { Minus, Plus, SlidersHorizontal } from 'lucide-vue-next'
import CompactDateInput from '@/components/ui/CompactDateInput.vue'
import ItemCategorySelect from '@/components/gestion/ItemCategorySelect.vue'
import ItemSubcategorySelect from '@/components/gestion/ItemSubcategorySelect.vue'
import { useAuthStore } from '@/store/authStore'
import { METADATA_FIELDS } from '@/RegleItem/CategorieItem'
import {
  normalizeItemType,
  readStoredItemCategories,
  resolveItemTypeOptions,
} from '@/RegleItem/itemCategoryStore'
import { extractSubcategoriesByType } from '@/RegleItem/subcategoryStore'
import { numberOrNull, toYmdLocal } from '@/utils/homeDashboard'

const props = defineProps({
  saving: { type: Boolean, default: false },
  successKey: { type: Number, default: 0 },
  items: { type: Array, default: () => [] },
})

const emit = defineEmits(['submit', 'error'])

const LAST_TYPE_PREFIX = 'snk_home_last_item_type_v1'
const auth = useAuthStore()
const currentUserId = computed(() => auth.user?.value?.id ?? auth.user?.id ?? 'guest')
const categoryLabels = ref(readStoredItemCategories(currentUserId.value))
const showDetails = ref(false)

const itemTypes = computed(() => resolveItemTypeOptions(categoryLabels.value))
const discoveredSubcategories = computed(() =>
  extractSubcategoriesByType(props.items, categoryLabels.value),
)
const metadataFields = computed(() => METADATA_FIELDS[form.value.type] || [])

function lastTypeStorageKey(userId = currentUserId.value) {
  return `${LAST_TYPE_PREFIX}_${String(userId || 'guest')}`
}

function readLastType() {
  try {
    return localStorage.getItem(lastTypeStorageKey()) || ''
  } catch {
    return ''
  }
}

function writeLastType(type) {
  try {
    localStorage.setItem(lastTypeStorageKey(), type)
  } catch {
    // Local storage can be unavailable in private contexts.
  }
}

function resolveDefaultType() {
  const options = itemTypes.value
  const lastType = normalizeItemType(readLastType())
  if (options.some((option) => option.value === lastType)) return lastType
  return options[0]?.value || 'OTHER'
}

function defaultMetadata(type) {
  const fields = METADATA_FIELDS[type] || []
  return fields.some((field) => field.key === 'condition') ? { condition: 'Neuf' } : {}
}

const emptyForm = () => {
  const type = resolveDefaultType()
  return {
    nomItem: '',
    prixRetail: null,
    prixResell: null,
    dateAchat: toYmdLocal(new Date()),
    dateVente: '',
    description: '',
    categorie: '',
    type,
    quantity: 1,
    metadata: defaultMetadata(type),
  }
}

const form = ref(emptyForm())

watch(
  () => props.successKey,
  () => {
    if (!props.successKey) return
    form.value = emptyForm()
    showDetails.value = false
  },
)

watch(
  () => currentUserId.value,
  (userId) => {
    categoryLabels.value = readStoredItemCategories(userId)
    form.value = emptyForm()
  },
)

watch(
  () => form.value.dateVente,
  (value) => {
    if (value && (form.value.prixResell === null || form.value.prixResell === '')) {
      form.value.prixResell = 0
    }
  },
)

function setType(type) {
  const nextType = normalizeItemType(type)
  if (form.value.type === nextType) return
  form.value.type = nextType
  form.value.categorie = ''
  form.value.metadata = defaultMetadata(nextType)
}

function setCategoryLabels(labels) {
  categoryLabels.value = labels || readStoredItemCategories(currentUserId.value)
}

function onCategoryLabelsChange(event) {
  const detail = event?.detail || {}
  if (String(detail.userId || 'guest') !== String(currentUserId.value || 'guest')) return
  categoryLabels.value = readStoredItemCategories(currentUserId.value)
}

function onSubcategoriesChange(event) {
  const detail = event?.detail || {}
  if (String(detail.userId || 'guest') !== String(currentUserId.value || 'guest')) return
}

if (typeof window !== 'undefined') {
  window.addEventListener('snk:item-categories-change', onCategoryLabelsChange)
  window.addEventListener('snk:item-subcategories-change', onSubcategoriesChange)
}

onBeforeUnmount(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('snk:item-categories-change', onCategoryLabelsChange)
    window.removeEventListener('snk:item-subcategories-change', onSubcategoriesChange)
  }
})

function cleanedMetadata() {
  const out = {}
  for (const field of metadataFields.value) {
    const value = form.value.metadata?.[field.key]
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      out[field.key] = String(value).trim()
    }
  }
  return out
}

function clampQuantity(value) {
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) return 1
  return Math.min(50, Math.max(1, Math.trunc(numberValue)))
}

function setQuantity(value) {
  form.value.quantity = clampQuantity(value)
}

function decreaseQuantity() {
  setQuantity(Number(form.value.quantity || 1) - 1)
}

function increaseQuantity() {
  setQuantity(Number(form.value.quantity || 1) + 1)
}

function validate() {
  const today = toYmdLocal(new Date())
  if (!form.value.type) return 'Choisis une categorie.'
  if (!form.value.nomItem.trim()) return "Le nom de l'item est obligatoire."
  const retail = numberOrNull(form.value.prixRetail)
  if (retail === null) return "Le prix d'achat est obligatoire."
  if (retail < 0) return "Le prix d'achat doit etre positif."
  const resell = numberOrNull(form.value.prixResell)
  if (resell !== null && resell < 0) return 'Le prix de vente doit etre positif.'
  const quantity = Number(form.value.quantity)
  if (!Number.isInteger(quantity) || quantity < 1 || quantity > 50) {
    return 'La quantite doit etre comprise entre 1 et 50.'
  }
  if (form.value.dateAchat && form.value.dateAchat > today) {
    return "La date d'achat ne peut pas etre apres aujourd'hui."
  }
  if (form.value.dateVente && form.value.dateVente > today) {
    return "La date de vente ne peut pas etre apres aujourd'hui."
  }
  if (form.value.dateAchat && form.value.dateVente && form.value.dateVente < form.value.dateAchat) {
    return "La date de vente doit etre apres la date d'achat."
  }
  return ''
}

function submit() {
  setQuantity(form.value.quantity)
  const validationError = validate()
  if (validationError) {
    emit('error', validationError)
    return
  }

  const itemType = normalizeItemType(form.value.type || resolveDefaultType())
  writeLastType(itemType)

  emit('submit', {
    quantity: Number(form.value.quantity),
    payload: {
      nomItem: form.value.nomItem.trim(),
      prixRetail: numberOrNull(form.value.prixRetail),
      prixResell: numberOrNull(form.value.prixResell),
      dateAchat: form.value.dateAchat || null,
      dateVente: form.value.dateVente || null,
      description: form.value.description.trim(),
      categorie: form.value.categorie.trim() || null,
      type: itemType,
      metadata: cleanedMetadata(),
    },
  })
}
</script>

<style scoped>
.quick-add-panel {
  position: relative;
  display: grid;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  gap: 0.8rem;
  border: 1px solid rgba(203, 213, 225, 0.72);
  border-radius: 20px;
  background: #ffffff;
  padding: clamp(0.9rem, 1.6vw, 1.15rem);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.07);
}

.quick-add-panel::before {
  content: '';
  position: absolute;
  inset: 0 1rem auto;
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, #14b8a6, #0ea5e9, #f59e0b);
}

.panel-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.panel-eyebrow {
  color: #0f766e;
  font-size: 0.7rem;
  font-weight: 800;
  text-transform: uppercase;
}

h2 {
  margin-top: 0.1rem;
  color: #0f172a;
  font-size: clamp(1.05rem, 2vw, 1.3rem);
  font-weight: 800;
}

.quick-form {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 0.72rem;
  align-items: end;
  width: 100%;
  max-width: 100%;
}

.field {
  display: grid;
  grid-column: span 3;
  min-width: 0;
  gap: 0.3rem;
}

.field--category {
  grid-column: span 4;
}

.field--name {
  grid-column: span 5;
}

.field--subcategory {
  grid-column: span 3;
}

.field--date {
  grid-column: span 3;
}

.field--quantity {
  grid-column: span 3;
}

.form-actions {
  grid-column: span 3;
}

.field--3 {
  grid-column: span 6;
}

.field span {
  color: #334155;
  font-size: 0.76rem;
  font-weight: 800;
}

input {
  width: 100%;
  min-height: 40px;
  border: 1px solid rgba(100, 116, 139, 0.24);
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.88);
  color: #0f172a;
  padding: 0.5rem 0.72rem;
  font-size: 0.88rem;
  font-weight: 650;
  outline: none;
}

input[type='number'] {
  appearance: textfield;
  -moz-appearance: textfield;
}

input[type='number']::-webkit-inner-spin-button,
input[type='number']::-webkit-outer-spin-button {
  margin: 0;
  -webkit-appearance: none;
}

input::placeholder {
  color: #94a3b8;
}

input:focus {
  border-color: rgba(20, 184, 166, 0.62);
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(20, 184, 166, 0.12), 0 10px 24px rgba(20, 184, 166, 0.08);
}

.quantity-stepper {
  display: grid;
  grid-template-columns: 2.45rem minmax(3.8rem, 1fr) 2.45rem;
  align-items: center;
  min-height: 40px;
  overflow: hidden;
  border: 1px solid rgba(100, 116, 139, 0.24);
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.88);
}

.quantity-stepper:focus-within {
  border-color: rgba(20, 184, 166, 0.62);
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(20, 184, 166, 0.12), 0 10px 24px rgba(20, 184, 166, 0.08);
}

.quantity-button {
  display: inline-flex;
  height: 100%;
  min-height: 40px;
  align-items: center;
  justify-content: center;
  border: 0;
  background: transparent;
  color: #0f766e;
}

.quantity-button:hover:not(:disabled) {
  background: rgba(20, 184, 166, 0.1);
}

.quantity-button:disabled {
  cursor: not-allowed;
  color: #94a3b8;
  opacity: 0.65;
}

.quantity-input {
  min-height: 40px;
  border: 0;
  border-inline: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 0;
  background: transparent;
  padding: 0 0.35rem;
  text-align: center;
}

.quantity-input:focus {
  background: transparent;
  box-shadow: none;
}

.ghost-button,
.submit-button {
  display: inline-flex;
  min-height: 2.45rem;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  border-radius: 12px;
  padding: 0 0.9rem;
  font-size: 0.84rem;
  font-weight: 800;
  white-space: nowrap;
}

.ghost-button {
  border: 1px solid rgba(148, 163, 184, 0.3);
  background: rgba(248, 250, 252, 0.86);
  color: #334155;
}

.ghost-button:hover,
.ghost-button.is-active {
  border-color: rgba(15, 118, 110, 0.3);
  background: linear-gradient(135deg, #ecfdf5, #e0f2fe);
  color: #0f766e;
}

.form-actions {
  display: flex;
  min-width: 0;
  align-self: end;
  justify-content: flex-end;
}

.submit-button {
  width: 100%;
  border: 1px solid rgba(15, 118, 110, 0.2);
  background: linear-gradient(135deg, #0f766e, #0e7490);
  color: #ffffff;
  box-shadow: 0 14px 28px rgba(15, 118, 110, 0.2), 0 8px 20px rgba(14, 116, 144, 0.14);
}

.submit-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #0d9488, #0284c7);
}

.submit-button:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

:deep(.cd-input) {
  min-height: 40px;
  border-color: rgba(100, 116, 139, 0.28);
  border-radius: 12px;
  background: #f8fafc;
  color: #0f172a;
  font-size: 0.88rem;
  font-weight: 650;
}

.quick-add-panel :deep(.category-field) {
  gap: 0.35rem;
}

.quick-add-panel :deep(.field-heading) {
  margin-bottom: 0.35rem;
  min-width: 0;
}

.quick-add-panel :deep(.field-helper),
.quick-add-panel :deep(.category-choice__base) {
  display: none;
}

.quick-add-panel :deep(.field-label),
.quick-add-panel :deep(.subcategory-label) {
  min-width: 0;
  color: #334155;
  font-size: 0.76rem;
  font-weight: 800;
}

.quick-add-panel :deep(.category-grid) {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.42rem;
  max-height: 10.2rem;
  overflow-y: auto;
  padding-right: 0.15rem;
}

.quick-add-panel :deep(.category-choice) {
  min-height: 40px;
  border-radius: 12px;
  padding: 0.48rem 0.58rem;
  gap: 0.5rem;
}

.quick-add-panel :deep(.category-choice__icon) {
  width: 1.8rem;
  height: 1.8rem;
}

.quick-add-panel :deep(.category-choice__label) {
  font-size: 0.78rem;
  line-height: 1.1;
}

.quick-add-panel :deep(.manage-link) {
  min-height: 1.85rem;
  max-width: 100%;
  padding: 0.25rem 0.55rem;
}

.quick-add-panel :deep(.subcategory-field-header) {
  margin-bottom: 0.35rem;
  min-width: 0;
}

.quick-add-panel :deep(.subcategory-trigger) {
  width: 100%;
  min-width: 0;
  min-height: 40px;
  border-radius: 12px;
}

.quick-add-panel :deep(.subcategory-manage-button) {
  max-width: 100%;
  flex-shrink: 0;
  padding-inline: 0.55rem;
}

@media (max-width: 980px) {
  .quick-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .field,
  .field--category,
  .field--subcategory,
  .field--price,
  .field--date,
  .field--quantity {
    grid-column: span 1;
  }

  .field--name,
  .field--3,
  .form-actions {
    grid-column: span 2;
  }

  .submit-button {
    justify-self: end;
    width: min(100%, 14rem);
  }
}

@media (max-width: 640px) {
  .panel-heading {
    display: grid;
  }

  .ghost-button {
    justify-self: start;
  }

  .quick-form {
    grid-template-columns: 1fr;
  }

  .field,
  .field--category,
  .field--name,
  .field--subcategory,
  .field--price,
  .field--date,
  .field--quantity,
  .field--3,
  .form-actions {
    grid-column: auto;
  }

  .submit-button {
    width: 100%;
  }
}

@media (max-width: 420px) {
  .quick-add-panel {
    gap: 0.7rem;
    border-radius: 16px;
    padding: 0.85rem;
  }

  .panel-heading {
    gap: 0.7rem;
  }

  .ghost-button,
  .submit-button {
    min-height: 2.5rem;
    border-radius: 11px;
    padding-inline: 0.75rem;
  }

  input,
  :deep(.cd-input),
  .quantity-stepper,
  .quantity-button,
  .quantity-input {
    min-height: 42px;
  }
}
</style>
