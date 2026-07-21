<template>
  <form class="item-form" :class="[`item-form--${surface}`, { 'has-cancel': showCancel }]" @submit.prevent="submit">
    <div v-if="showDetailsToggle" class="item-form-tools">
      <button
        type="button"
        class="item-details-toggle"
        :class="{ 'is-active': showDetails }"
        :aria-expanded="showDetails"
        @click="showDetails = !showDetails"
      >
        <SlidersHorizontal class="h-4 w-4" aria-hidden="true" />
        <span>Details</span>
      </button>
    </div>

    <div class="item-form-grid">
      <ItemCategorySelect
        class="item-field item-field--category"
        label="Categorie"
        :model-value="form.type"
        :items="items"
        display="dropdown"
        placeholder="Choisir une categorie"
        :user-id="currentUserId"
        :labels="categoryLabels"
        @update:modelValue="setType"
        @labels-change="setCategoryLabels"
      />

      <label class="item-field item-field--name">
        <span>Nom de l'item</span>
        <input
          ref="nameInputRef"
          v-model.trim="form.nomItem"
          type="text"
          autocomplete="off"
          placeholder="Nom, modele, reference..."
          required
        />
      </label>

      <ItemSubcategorySelect
        :model-value="form.categorie"
        class="item-field item-field--subcategory"
        label="Sous-categorie"
        placeholder="Marque, famille..."
        empty-label="Aucune sous-categorie"
        :type="form.type"
        :user-id="currentUserId"
        :discovered="discoveredSubcategories"
        :items="items"
        :category-labels="categoryLabels"
        @update:modelValue="setSubcategory"
      />

      <label class="item-field item-field--price">
        <span>Prix achat</span>
        <input
          :value="priceInputs.prixRetail"
          type="text"
          inputmode="decimal"
          placeholder="110"
          required
          @input="updatePriceField('prixRetail', $event)"
          @blur="formatPriceField('prixRetail')"
        />
      </label>

      <div class="item-field item-field--date">
        <span>Date d'achat</span>
        <CompactDateInput v-model="form.dateAchat" light size="md" />
      </div>

      <div v-if="quantityEnabled && (!showDetails || isQuickSurface)" class="item-field item-field--quantity">
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

      <template v-if="showInlineDetails">
        <div class="item-field item-field--date">
          <span>Date de vente</span>
          <CompactDateInput v-model="form.dateVente" light size="md" />
        </div>

        <label class="item-field item-field--price">
          <span>Prix de vente</span>
          <input
            :value="priceInputs.prixResell"
            type="text"
            inputmode="decimal"
            placeholder="180"
            @input="updatePriceField('prixResell', $event)"
            @blur="formatPriceField('prixResell')"
          />
        </label>

        <label class="item-field item-field--notes">
          <span>Notes</span>
          <textarea
            v-model.trim="form.description"
            rows="2"
            placeholder="Etat, lieu d'achat, accessoires inclus..."
          ></textarea>
        </label>

        <div v-if="metadataFields.length" class="item-field item-field--section">
          <p>Champs specifiques</p>
          <span>Adaptes au type {{ currentTypeLabel.toLowerCase() }}</span>
        </div>

        <label v-for="field in metadataFields" :key="field.key" class="item-field">
          <span>{{ field.label }}</span>
          <input
            v-model.trim="form.metadata[field.key]"
            type="text"
            :placeholder="field.placeholder"
          />
        </label>
      </template>

      <div v-if="quantityEnabled && showInlineDetails" class="item-field item-field--quantity">
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

      <slot name="after-fields" :form="form" :show-details="showDetails"></slot>

      <footer class="item-form-actions">
        <button v-if="showCancel" type="button" class="item-secondary-button" :disabled="saving" @click="$emit('cancel')">
          {{ cancelLabel }}
        </button>
        <button type="submit" class="item-primary-button" :disabled="saving">
          <Save v-if="mode === 'edit'" class="h-4 w-4" aria-hidden="true" />
          <Plus v-else class="h-4 w-4" aria-hidden="true" />
          <span>{{ saving ? savingLabel : submitLabel }}</span>
        </button>
      </footer>
    </div>
  </form>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { Minus, Plus, Save, SlidersHorizontal } from 'lucide-vue-next'
import CompactDateInput from '@/components/ui/CompactDateInput.vue'
import ItemCategorySelect from '@/components/gestion/ItemCategorySelect.vue'
import ItemSubcategorySelect from '@/components/gestion/ItemSubcategorySelect.vue'
import { useAuthStore } from '@/store/authStore'
import { METADATA_FIELDS } from '@/RegleItem/CategorieItem'
import {
  buildItemCategoryAliases,
  itemTypeLabel,
  normalizeItemType,
  readStoredItemCategories,
  resolveItemTypeOptions,
} from '@/RegleItem/itemCategoryStore'
import {
  extractSubcategoriesByType,
  normalizeSubcategoryName,
  readStoredSubcategories,
} from '@/RegleItem/subcategoryStore'
import { inferItemClassificationFromName } from '@/RegleItem/itemNameInference'
import { getField, typeOf } from '@/utils/snkVente'
import { numberOrNull, toYmdLocal } from '@/utils/homeDashboard'

const props = defineProps({
  mode: { type: String, default: 'create' },
  item: { type: Object, default: null },
  items: { type: Array, default: () => [] },
  saving: { type: Boolean, default: false },
  resetKey: { type: Number, default: 0 },
  quantityEnabled: { type: Boolean, default: false },
  showCancel: { type: Boolean, default: true },
  showDetailsToggle: { type: Boolean, default: true },
  detailsDefaultOpen: { type: Boolean, default: false },
  autoInferFromName: { type: Boolean, default: false },
  autoFocusFirstField: { type: Boolean, default: false },
  submitLabel: { type: String, default: 'Enregistrer' },
  savingLabel: { type: String, default: 'Enregistrement...' },
  cancelLabel: { type: String, default: 'Annuler' },
  surface: { type: String, default: 'modal' },
})

const emit = defineEmits(['submit', 'error', 'cancel', 'form-change'])

const LAST_TYPE_PREFIX = 'snk_home_last_item_type_v1'
const auth = useAuthStore()
const currentUserId = computed(() => auth.user?.value?.id ?? auth.user?.id ?? 'guest')
const categoryLabels = ref(readStoredItemCategories(currentUserId.value))
const storedSubcategories = ref(
  readStoredSubcategories(currentUserId.value, undefined, categoryLabels.value),
)
const showDetails = ref(props.detailsDefaultOpen)
const manualTypeOverride = ref(false)
const manualSubcategoryOverride = ref(false)
const manualRetailOverride = ref(false)
const manualResellOverride = ref(false)
const autoFilledSubcategory = ref(false)
const autoFilledRetail = ref(false)
const autoFilledResell = ref(false)
const nameInputRef = ref(null)
const priceInputs = ref({
  prixRetail: '',
  prixResell: '',
})

const itemTypes = computed(() => resolveItemTypeOptions(categoryLabels.value))
const discoveredSubcategories = computed(() =>
  extractSubcategoriesByType(props.items, categoryLabels.value),
)
const isQuickSurface = computed(() => props.surface === 'quick')
const showInlineDetails = computed(() => showDetails.value)
const mainCategoryAliases = computed(() => buildItemCategoryAliases(categoryLabels.value))
const metadataFields = computed(() => METADATA_FIELDS[form.value.type] || [])
const currentTypeLabel = computed(() => itemTypeLabel(form.value.type, categoryLabels.value))

const suggestedFromHistory = computed(() => {
  if (props.mode !== 'create') return null

  const normalizedName = normalizeNameForSuggestion(form.value.nomItem)
  const candidates = [...(props.items || [])]
    .filter((item) => normalizeItemType(typeOf(item)) === form.value.type)
    .map((item) => ({
      item,
      score: suggestionScore(item, normalizedName, form.value.categorie),
    }))
    .filter((entry) => entry.score > 0)
    .sort((a, b) => {
      if (b.score !== a.score) return b.score - a.score
      const aDate = String(getField(a.item, 'dateAchat', '') || getField(a.item, 'dateVente', '') || '')
      const bDate = String(getField(b.item, 'dateAchat', '') || getField(b.item, 'dateVente', '') || '')
      return bDate.localeCompare(aDate)
    })

  return candidates[0]?.item || null
})
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

function normalizeNameForSuggestion(value) {
  return String(value || '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, ' ')
    .trim()
}

function suggestionScore(item, normalizedName, category) {
  let score = 0
  const itemName = normalizeNameForSuggestion(getField(item, 'nomItem', ''))
  const itemCategory = normalizeSubcategoryName(getField(item, 'categorie', ''))

  if (category && itemCategory === normalizeSubcategoryName(category)) score += 5
  if (normalizedName && itemName === normalizedName) score += 6
  if (normalizedName && itemName.includes(normalizedName)) score += 4
  if (normalizedName) {
    const firstToken = normalizedName.split(' ').filter(Boolean)[0]
    if (firstToken && itemName.includes(firstToken)) score += 2
  }

  const retail = numberOrNull(getField(item, 'prixRetail', null))
  const resell = numberOrNull(getField(item, 'prixResell', null))
  if (retail !== null) score += 1
  if (resell !== null) score += 1
  return score
}

function formatPriceValue(value) {
  const parsed = numberOrNull(value)
  return parsed === null ? '' : parsed.toFixed(2).replace('.', ',')
}

function parsePriceValue(raw) {
  const normalized = String(raw || '')
    .replace(/\s/g, '')
    .replace(/[€]/g, '')
    .replace(',', '.')
  return numberOrNull(normalized)
}

function syncPriceInputsFromForm() {
  priceInputs.value.prixRetail = formatPriceValue(form.value.prixRetail)
  priceInputs.value.prixResell = formatPriceValue(form.value.prixResell)
}

function emptyForm(prefill = {}) {
  const type = normalizeItemType(prefill.type || resolveDefaultType())
  return {
    id: null,
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
    ...prefill,
    type,
    metadata: { ...defaultMetadata(type), ...(prefill.metadata || {}) },
  }
}

function formFromItem(item) {
  if (!item) return emptyForm()
  const itemType = normalizeItemType(typeOf(item))
  return emptyForm({
    id: item.id,
    nomItem: getField(item, 'nomItem', ''),
    prixRetail: getField(item, 'prixRetail', '') ?? null,
    prixResell: getField(item, 'prixResell', '') ?? null,
    dateAchat: String(getField(item, 'dateAchat', '') || '').slice(0, 10),
    dateVente: String(getField(item, 'dateVente', '') || '').slice(0, 10),
    description: getField(item, 'description', ''),
    categorie: getField(item, 'categorie', ''),
    type: itemType,
    metadata: item.metadata || {},
  })
}

const form = ref(props.mode === 'edit' ? formFromItem(props.item) : emptyForm())

function hasDetailedValues(value) {
  if (!value) return false
  if (value.dateVente || value.prixResell || value.description) return true
  return Object.keys(value.metadata || {}).some((key) => {
    const metadataValue = value.metadata[key]
    return metadataValue !== null && metadataValue !== undefined && String(metadataValue).trim() !== ''
  })
}

function resetForm() {
  form.value = props.mode === 'edit' ? formFromItem(props.item) : emptyForm()
  showDetails.value = props.detailsDefaultOpen || (props.mode === 'edit' && hasDetailedValues(form.value))
  manualTypeOverride.value = false
  manualSubcategoryOverride.value = false
  manualRetailOverride.value = false
  manualResellOverride.value = false
  autoFilledSubcategory.value = false
  autoFilledRetail.value = false
  autoFilledResell.value = false
  syncPriceInputsFromForm()
  applyNameInference()
  applySmartAutofill()
  queueAutoFocus()
}

function queueAutoFocus() {
  if (!props.autoFocusFirstField) return
  focusFirstField()
}

function focusFirstField() {
  nextTick(() => {
    nameInputRef.value?.focus?.()
    nameInputRef.value?.select?.()
  })
}

function applySuggestedPrice(key, value) {
  const parsed = numberOrNull(value)
  if (parsed === null) return false
  form.value[key] = Number(parsed)
  priceInputs.value[key] = formatPriceValue(parsed)
  if (key === 'prixRetail') autoFilledRetail.value = true
  if (key === 'prixResell') autoFilledResell.value = true
  return true
}

function clearAutoSuggestedPrice(key) {
  form.value[key] = null
  priceInputs.value[key] = ''
  if (key === 'prixRetail') autoFilledRetail.value = false
  if (key === 'prixResell') autoFilledResell.value = false
}

function applySmartAutofill() {
  if (props.mode !== 'create') return
  const source = suggestedFromHistory.value
  if (!source) {
    if (!manualRetailOverride.value && autoFilledRetail.value) clearAutoSuggestedPrice('prixRetail')
    if (!manualResellOverride.value && autoFilledResell.value) clearAutoSuggestedPrice('prixResell')
    if (!manualSubcategoryOverride.value && autoFilledSubcategory.value) {
      form.value.categorie = ''
      autoFilledSubcategory.value = false
    }
    return
  }

  if (!manualRetailOverride.value && (numberOrNull(form.value.prixRetail) === null || autoFilledRetail.value)) {
    if (!applySuggestedPrice('prixRetail', getField(source, 'prixRetail', null)) && autoFilledRetail.value) {
      clearAutoSuggestedPrice('prixRetail')
    }
  }

  if (!manualResellOverride.value && (numberOrNull(form.value.prixResell) === null || autoFilledResell.value)) {
    if (!applySuggestedPrice('prixResell', getField(source, 'prixResell', null)) && autoFilledResell.value) {
      clearAutoSuggestedPrice('prixResell')
    }
  }

  if (!manualSubcategoryOverride.value && (!form.value.categorie || autoFilledSubcategory.value)) {
    form.value.categorie = normalizeSubcategoryName(getField(source, 'categorie', ''))
    autoFilledSubcategory.value = Boolean(form.value.categorie)
  }
}

watch(
  () => props.item,
  () => {
    if (props.mode === 'edit') resetForm()
  },
  { immediate: true },
)

watch(
  () => props.resetKey,
  () => {
    if (!props.resetKey || props.mode === 'edit') return
    resetForm()
  },
)

watch(
  () => currentUserId.value,
  (userId) => {
    categoryLabels.value = readStoredItemCategories(userId)
    storedSubcategories.value = readStoredSubcategories(userId, undefined, categoryLabels.value)
    if (props.mode !== 'edit') resetForm()
  },
)

watch(
  () => form.value.dateVente,
  (value) => {
    if (value && (form.value.prixResell === null || form.value.prixResell === '')) {
      form.value.prixResell = 0
      priceInputs.value.prixResell = formatPriceValue(0)
    }
  },
)

watch(
  form,
  (value) => {
    emit('form-change', value)
  },
  { deep: true, immediate: true },
)

function applyType(type) {
  const nextType = normalizeItemType(type)
  if (form.value.type === nextType) return false
  form.value.type = nextType
  form.value.categorie = ''
  form.value.metadata = defaultMetadata(nextType)
  return true
}

function setType(type) {
  manualTypeOverride.value = true
  manualSubcategoryOverride.value = false
  autoFilledSubcategory.value = false
  applyType(type)
  applySmartAutofill()
}

function setSubcategory(value) {
  manualSubcategoryOverride.value = true
  autoFilledSubcategory.value = false
  form.value.categorie = normalizeSubcategoryName(value)
  applySmartAutofill()
}

function applyNameInference() {
  if (!props.autoInferFromName || props.mode === 'edit') return
  const prediction = inferItemClassificationFromName(form.value.nomItem, {
    categoryLabels: categoryLabels.value,
    storedSubcategories: storedSubcategories.value,
    discoveredSubcategories: discoveredSubcategories.value,
    currentType: form.value.type,
    currentSubcategory: form.value.categorie,
    mainCategoryAliases: mainCategoryAliases.value,
  })

  if (!manualTypeOverride.value && prediction.type) {
    applyType(prediction.type)
  }

  if (!manualSubcategoryOverride.value && prediction.subcategory) {
    form.value.categorie = prediction.subcategory
  }
}

function setCategoryLabels(labels) {
  categoryLabels.value = labels || readStoredItemCategories(currentUserId.value)
  storedSubcategories.value = readStoredSubcategories(
    currentUserId.value,
    undefined,
    categoryLabels.value,
  )
  applyNameInference()
  applySmartAutofill()
}

function onCategoryLabelsChange(event) {
  const detail = event?.detail || {}
  if (String(detail.userId || 'guest') !== String(currentUserId.value || 'guest')) return
  categoryLabels.value = readStoredItemCategories(currentUserId.value)
  storedSubcategories.value = readStoredSubcategories(
    currentUserId.value,
    undefined,
    categoryLabels.value,
  )
  applyNameInference()
  applySmartAutofill()
}

function onSubcategoriesChange(event) {
  const detail = event?.detail || {}
  if (String(detail.userId || 'guest') !== String(currentUserId.value || 'guest')) return
  storedSubcategories.value = readStoredSubcategories(
    currentUserId.value,
    undefined,
    categoryLabels.value,
  )
  applyNameInference()
  applySmartAutofill()
}

watch(
  () => form.value.nomItem,
  () => {
    applyNameInference()
    applySmartAutofill()
  },
)

watch(
  () => [form.value.type, form.value.categorie],
  () => {
    applySmartAutofill()
  },
)

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

function updatePriceField(key, event) {
  const raw = typeof event === 'string' ? event : event?.target?.value
  priceInputs.value[key] = String(raw ?? '')

  if (key === 'prixRetail') {
    manualRetailOverride.value = true
    autoFilledRetail.value = false
  }
  if (key === 'prixResell') {
    manualResellOverride.value = true
    autoFilledResell.value = false
  }

  const parsed = parsePriceValue(priceInputs.value[key])
  form.value[key] = parsed
}

function formatPriceField(key) {
  const parsed = parsePriceValue(priceInputs.value[key])
  form.value[key] = parsed
  priceInputs.value[key] = formatPriceValue(parsed)
}

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
  if (resell !== null && !form.value.dateVente) {
    return 'Ajoute une date de vente si tu saisis un prix de revente.'
  }
  if (props.quantityEnabled) {
    const quantity = Number(form.value.quantity)
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > 50) {
      return 'La quantite doit etre comprise entre 1 et 50.'
    }
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

function buildPayload() {
  const itemType = normalizeItemType(form.value.type || resolveDefaultType())
  writeLastType(itemType)
  return {
    nomItem: form.value.nomItem.trim(),
    prixRetail: numberOrNull(form.value.prixRetail),
    prixResell: numberOrNull(form.value.prixResell),
    dateAchat: form.value.dateAchat || null,
    dateVente: form.value.dateVente || null,
    description: form.value.description.trim(),
    categorie: form.value.categorie.trim() || null,
    type: itemType,
    metadata: cleanedMetadata(),
  }
}

function submit() {
  if (props.quantityEnabled) setQuantity(form.value.quantity)
  const validationError = validate()
  if (validationError) {
    emit('error', validationError)
    return
  }
  const payload = buildPayload()
  if (props.mode === 'edit') {
    emit('submit', { id: form.value.id, payload })
    return
  }
  emit('submit', { quantity: props.quantityEnabled ? Number(form.value.quantity) : 1, payload })
}

defineExpose({
  focusFirstField,
})
</script>

<style scoped>
.item-form {
  display: grid;
  gap: 0.75rem;
  min-width: 0;
}

.item-form-tools {
  display: flex;
  justify-content: flex-end;
}

.item-details-toggle,
.item-secondary-button,
.item-primary-button {
  display: inline-flex;
  min-height: 2.45rem;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  border-radius: 12px;
  padding: 0 0.9rem;
  font-size: 0.84rem;
  font-weight: 850;
  white-space: nowrap;
}

.item-details-toggle,
.item-secondary-button {
  border: 1px solid rgba(148, 163, 184, 0.34);
  background: #f8fafc;
  color: #334155;
}

.item-details-toggle:hover,
.item-details-toggle.is-active,
.item-secondary-button:hover {
  border-color: rgba(15, 118, 110, 0.28);
  background: #ecfdf5;
  color: #0f766e;
}

.item-form-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 0.72rem;
  align-items: end;
  min-width: 0;
}

.item-field {
  display: grid;
  grid-column: span 3;
  min-width: 0;
  gap: 0.3rem;
}

.item-field--category {
  grid-column: span 4;
}

.item-field--name {
  grid-column: span 5;
}

.item-field--subcategory,
.item-field--date,
.item-field--quantity,
.item-field--price {
  grid-column: span 3;
}

.item-field--notes,
.item-field--section {
  grid-column: span 6;
}

.item-field--section {
  align-self: stretch;
  align-content: center;
  border-top: 1px solid rgba(226, 232, 240, 0.92);
  padding-top: 0.65rem;
}

.item-field > span,
.item-field--section p {
  color: #334155;
  font-size: 0.76rem;
  font-weight: 850;
}

.item-field small {
  color: #64748b;
  font-size: 0.72rem;
  font-weight: 650;
  line-height: 1.35;
}

.item-field--section span {
  color: #64748b;
  font-size: 0.76rem;
  font-weight: 650;
}

.item-field input,
.item-field textarea {
  width: 100%;
  min-height: 40px;
  border: 1px solid rgba(100, 116, 139, 0.26);
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.92);
  color: #0f172a;
  padding: 0.5rem 0.72rem;
  font-size: 0.88rem;
  font-weight: 650;
  outline: none;
  transition:
    border-color 120ms ease,
    background 120ms ease,
    box-shadow 120ms ease;
}

.item-field textarea {
  min-height: 68px;
  resize: vertical;
}

.item-field input[type='number'] {
  appearance: textfield;
  -moz-appearance: textfield;
}

.item-field input[type='number']::-webkit-inner-spin-button,
.item-field input[type='number']::-webkit-outer-spin-button {
  margin: 0;
  -webkit-appearance: none;
}

.item-field input::placeholder,
.item-field textarea::placeholder {
  color: #94a3b8;
}

.item-field input:focus,
.item-field textarea:focus {
  border-color: rgba(20, 184, 166, 0.62);
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(20, 184, 166, 0.12);
}

.quantity-stepper {
  display: grid;
  grid-template-columns: 2.45rem minmax(3.8rem, 1fr) 2.45rem;
  align-items: center;
  min-height: 40px;
  overflow: hidden;
  border: 1px solid rgba(100, 116, 139, 0.24);
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.92);
}

.quantity-stepper:focus-within {
  border-color: rgba(20, 184, 166, 0.62);
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(20, 184, 166, 0.12);
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
  min-height: 40px !important;
  border: 0 !important;
  border-inline: 1px solid rgba(148, 163, 184, 0.2) !important;
  border-radius: 0 !important;
  background: transparent !important;
  padding: 0 0.35rem !important;
  text-align: center;
}

.quantity-input:focus {
  background: transparent !important;
  box-shadow: none !important;
}

.item-form-actions {
  display: flex;
  grid-column: 1 / -1;
  justify-content: flex-end;
  gap: 0.65rem;
  border-top: 1px solid rgba(226, 232, 240, 0.96);
  padding-top: 0.9rem;
}

.item-form--quick .item-form-actions {
  border-top: 0;
  padding-top: 0;
}

.item-primary-button {
  border: 1px solid rgba(15, 118, 110, 0.2);
  background: linear-gradient(135deg, #0f766e, #0e7490);
  color: #ffffff;
  box-shadow: 0 12px 24px rgba(15, 118, 110, 0.16);
}

.item-primary-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #0d9488, #0284c7);
}

.item-primary-button:disabled,
.item-secondary-button:disabled {
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

:deep(.category-field),
:deep(.subcategory-field) {
  gap: 0.35rem;
}

:deep(.field-heading),
:deep(.subcategory-field-header) {
  margin-bottom: 0.35rem;
  min-width: 0;
}

:deep(.field-helper),
:deep(.category-choice__base) {
  display: none;
}

:deep(.field-label),
:deep(.subcategory-label) {
  min-width: 0;
  color: #334155;
  font-size: 0.76rem;
  font-weight: 850;
}

:deep(.manage-link),
:deep(.subcategory-manage-button) {
  min-height: 1.85rem;
  max-width: 100%;
  padding: 0.25rem 0.55rem;
}

:deep(.category-dropdown-trigger),
:deep(.subcategory-trigger) {
  min-height: 40px;
  border-radius: 12px;
}

@media (max-width: 980px) {
  .item-form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .item-field,
  .item-field--category,
  .item-field--subcategory,
  .item-field--price,
  .item-field--date,
  .item-field--quantity {
    grid-column: span 1;
  }

  .item-field--name,
  .item-field--notes,
  .item-field--section,
  .item-form-actions {
    grid-column: span 2;
  }
}

@media (max-width: 640px) {
  .item-form-tools {
    justify-content: stretch;
  }

  .item-details-toggle {
    width: 100%;
  }

  .item-form-grid {
    grid-template-columns: 1fr;
  }

  .item-field,
  .item-field--category,
  .item-field--name,
  .item-field--subcategory,
  .item-field--price,
  .item-field--date,
  .item-field--quantity,
  .item-field--notes,
  .item-field--section,
  .item-form-actions {
    grid-column: auto;
  }

  .item-form-actions {
    display: grid;
  }

  .item-secondary-button,
  .item-primary-button {
    width: 100%;
  }

}

@media (hover: none) and (pointer: coarse) {
  .item-field input,
  .item-field textarea,
  .quantity-input,
  :deep(.cd-input) {
    font-size: 16px !important;
  }
}
</style>
