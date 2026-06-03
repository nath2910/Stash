<template>
  <teleport to="body">
    <div v-if="open" class="modal-root" @keydown.esc.prevent="$emit('close')">
      <div class="modal-backdrop" @click="$emit('close')"></div>
      <div class="modal-position">
        <section class="modal-card" role="dialog" aria-modal="true" aria-labelledby="quick-item-title">
          <header class="modal-header">
            <div>
              <p class="modal-eyebrow">{{ statusLabel }}</p>
              <h2 id="quick-item-title">{{ form.nomItem || 'Modifier un item' }}</h2>
            </div>
            <div class="header-actions">
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
              <button type="button" class="icon-button" aria-label="Fermer" @click="$emit('close')">
                <X class="h-5 w-5" aria-hidden="true" />
              </button>
            </div>
          </header>

          <div v-if="localError || error" class="modal-alert modal-alert--error">
            {{ localError || error }}
          </div>
          <div v-if="successText" class="modal-alert modal-alert--success">
            {{ successText }}
          </div>

          <form class="modal-form" @submit.prevent="submit">
            <ItemCategorySelect
              class="field"
              label="Catégorie"
              :model-value="form.type"
              placeholder="Choisir une catégorie"
              :user-id="currentUserId"
              :labels="categoryLabels"
              @update:modelValue="setType"
              @labels-change="setCategoryLabels"
            />

            <label class="field field--2">
              <span>Nom de l'item</span>
              <input v-model.trim="form.nomItem" type="text" autocomplete="off" required />
            </label>

            <ItemSubcategorySelect
              v-model="form.categorie"
              class="field"
              label="Sous-catégorie"
              placeholder="Marque, famille..."
              empty-label="Aucune sous-catégorie"
              :type="form.type"
              :user-id="currentUserId"
              :discovered="discoveredSubcategories"
              :category-labels="categoryLabels"
            />

            <label class="field">
              <span>Prix achat</span>
              <input v-model.number="form.prixRetail" type="number" min="0" step="0.01" required />
            </label>

            <label class="field">
              <span>Prix vente</span>
              <input v-model.number="form.prixResell" type="number" min="0" step="0.01" />
            </label>

            <div class="field">
              <span>Date d'achat</span>
              <CompactDateInput v-model="form.dateAchat" light size="md" class="date-control" />
            </div>

            <div class="field">
              <span>Date de vente</span>
              <CompactDateInput v-model="form.dateVente" light size="md" class="date-control" />
            </div>

            <template v-if="showDetails">
              <label class="field field--4">
                <span>Notes</span>
                <textarea
                  v-model.trim="form.description"
                  rows="2"
                  placeholder="État, lieu d'achat, accessoires inclus..."
                ></textarea>
              </label>

              <div class="field field--4 details-heading">
                <p>Champs spécifiques</p>
                <span>Adaptés au type {{ currentTypeLabel.toLowerCase() }}</span>
              </div>

              <label v-for="field in metadataFields" :key="field.key" class="field">
                <span>{{ field.label }}</span>
                <input
                  v-model.trim="form.metadata[field.key]"
                  type="text"
                  :placeholder="field.placeholder"
                />
              </label>

              <p v-if="!metadataFields.length" class="field field--4 metadata-empty">
                Aucun champ spécifique pour cette catégorie.
              </p>

              <p v-if="form.type === 'TICKET'" class="field field--4 ticket-note">
                Les pièces jointes restent disponibles depuis la fiche Gestion.
              </p>
            </template>

            <footer class="modal-actions">
              <button type="button" class="secondary-button" :disabled="saving" @click="$emit('close')">
                Annuler
              </button>
              <button type="submit" class="primary-button" :disabled="saving">
                <Save class="h-4 w-4" aria-hidden="true" />
                <span>{{ saving ? 'Enregistrement...' : 'Enregistrer' }}</span>
              </button>
            </footer>
          </form>
        </section>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { Save, SlidersHorizontal, X } from 'lucide-vue-next'
import CompactDateInput from '@/components/ui/CompactDateInput.vue'
import ItemCategorySelect from '@/components/gestion/ItemCategorySelect.vue'
import ItemSubcategorySelect from '@/components/gestion/ItemSubcategorySelect.vue'
import { useAuthStore } from '@/store/authStore'
import { METADATA_FIELDS } from '@/RegleItem/CategorieItem'
import {
  itemTypeLabel,
  normalizeItemType,
  readStoredItemCategories,
} from '@/RegleItem/itemCategoryStore'
import { extractSubcategoriesByType } from '@/RegleItem/subcategoryStore'
import { getField, typeOf } from '@/utils/snkVente'
import { numberOrNull } from '@/utils/homeDashboard'
import { normalizeItemStatus } from '@/constants/statuses'

const props = defineProps({
  open: { type: Boolean, default: false },
  item: { type: Object, default: null },
  items: { type: Array, default: () => [] },
  saving: { type: Boolean, default: false },
  error: { type: String, default: '' },
  successKey: { type: Number, default: 0 },
})

const emit = defineEmits(['close', 'save'])

const auth = useAuthStore()
const currentUserId = computed(() => auth.user?.value?.id ?? auth.user?.id ?? 'guest')
const categoryLabels = ref(readStoredItemCategories(currentUserId.value))
const localError = ref('')
const successText = ref('')
const showDetails = ref(false)
const form = ref({
  id: null,
  nomItem: '',
  prixRetail: null,
  prixResell: null,
  dateAchat: '',
  dateVente: '',
  description: '',
  categorie: '',
  type: 'OTHER',
  metadata: {},
})

const statusLabel = computed(() =>
  props.item ? normalizeItemStatus(props.item).label : 'En stock',
)
const discoveredSubcategories = computed(() =>
  extractSubcategoriesByType(props.items, categoryLabels.value),
)
const metadataFields = computed(() => METADATA_FIELDS[form.value.type] || [])
const currentTypeLabel = computed(() => itemTypeLabel(form.value.type, categoryLabels.value))

function defaultMetadata(type) {
  const fields = METADATA_FIELDS[type] || []
  return fields.some((field) => field.key === 'condition') ? { condition: 'Neuf' } : {}
}

watch(
  () => props.item,
  (item) => {
    if (!item) return
    const itemType = normalizeItemType(typeOf(item))
    form.value = {
      id: item.id,
      nomItem: getField(item, 'nomItem', ''),
      prixRetail: getField(item, 'prixRetail', '') ?? null,
      prixResell: getField(item, 'prixResell', '') ?? null,
      dateAchat: String(getField(item, 'dateAchat', '') || '').slice(0, 10),
      dateVente: String(getField(item, 'dateVente', '') || '').slice(0, 10),
      description: getField(item, 'description', ''),
      categorie: getField(item, 'categorie', ''),
      type: itemType,
      metadata: { ...defaultMetadata(itemType), ...(item.metadata || {}) },
    }
    showDetails.value = false
    localError.value = ''
    successText.value = ''
  },
  { immediate: true },
)

watch(
  () => props.successKey,
  () => {
    if (!props.successKey) return
    successText.value = 'Modifications enregistrées.'
    window.setTimeout(() => {
      successText.value = ''
    }, 2400)
  },
)

watch(
  () => currentUserId.value,
  (userId) => {
    categoryLabels.value = readStoredItemCategories(userId)
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

function validate() {
  if (!form.value.nomItem.trim()) return "Le nom de l'item est obligatoire."
  const retail = numberOrNull(form.value.prixRetail)
  const resell = numberOrNull(form.value.prixResell)
  if (retail === null) return "Le prix d'achat est obligatoire."
  if (retail < 0) return "Le prix d'achat doit être positif."
  if (resell !== null && resell < 0) return 'Le prix de vente doit être positif.'
  return ''
}

function submit() {
  localError.value = validate()
  successText.value = ''
  if (localError.value || !form.value.id) return

  emit('save', {
    id: form.value.id,
    payload: {
      nomItem: form.value.nomItem.trim(),
      prixRetail: numberOrNull(form.value.prixRetail),
      prixResell: numberOrNull(form.value.prixResell),
      dateAchat: form.value.dateAchat || null,
      dateVente: form.value.dateVente || null,
      description: form.value.description.trim(),
      categorie: form.value.categorie.trim() || null,
      type: normalizeItemType(form.value.type || 'OTHER'),
      metadata: cleanedMetadata(),
    },
  })
}
</script>

<style scoped>
.modal-root {
  position: fixed;
  inset: 0;
  z-index: 9999;
}

.modal-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.52);
  backdrop-filter: blur(5px);
}

.modal-position {
  position: relative;
  z-index: 1;
  display: flex;
  min-height: 100%;
  align-items: center;
  justify-content: center;
  padding: clamp(0.75rem, 2vw, 1.25rem);
}

.modal-card {
  width: min(100%, 940px);
  max-height: calc(100dvh - 2rem);
  overflow: auto;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.26);
}

.modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  border-bottom: 1px solid rgba(226, 232, 240, 0.96);
  padding: 0.95rem 1rem;
}

.modal-eyebrow {
  color: #0f766e;
  font-size: 0.7rem;
  font-weight: 800;
  text-transform: uppercase;
}

h2 {
  margin-top: 0.1rem;
  color: #0f172a;
  font-size: clamp(1.05rem, 2.2vw, 1.4rem);
  font-weight: 800;
}

.header-actions {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
}

.icon-button {
  display: inline-flex;
  width: 2.4rem;
  height: 2.4rem;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 12px;
  background: #f8fafc;
  color: #334155;
}

.modal-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) minmax(0, 1fr) minmax(0, 1fr);
  gap: 0.75rem;
  align-items: end;
  padding: 1rem;
}

.field {
  display: grid;
  min-width: 0;
  gap: 0.32rem;
}

.field--2 {
  grid-column: span 2;
}

.field--4,
.modal-actions {
  grid-column: 1 / -1;
}

.field span,
.details-heading p {
  color: #334155;
  font-size: 0.76rem;
  font-weight: 800;
}

input,
textarea {
  width: 100%;
  min-height: 40px;
  border: 1px solid rgba(100, 116, 139, 0.28);
  border-radius: 12px;
  background: #f8fafc;
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

textarea {
  min-height: 68px;
  resize: vertical;
}

input::placeholder,
textarea::placeholder {
  color: #94a3b8;
}

input:focus,
textarea:focus {
  border-color: rgba(20, 184, 166, 0.62);
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(20, 184, 166, 0.12);
}

.ghost-button,
.secondary-button,
.primary-button {
  display: inline-flex;
  min-height: 2.4rem;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  border-radius: 12px;
  padding: 0 0.9rem;
  font-size: 0.84rem;
  font-weight: 800;
  white-space: nowrap;
}

.ghost-button,
.secondary-button {
  border: 1px solid rgba(148, 163, 184, 0.34);
  background: #f8fafc;
  color: #334155;
}

.ghost-button:hover,
.ghost-button.is-active,
.secondary-button:hover {
  border-color: rgba(15, 118, 110, 0.28);
  background: #ecfdf5;
  color: #0f766e;
}

.details-heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.75rem;
  border-top: 1px solid rgba(226, 232, 240, 0.92);
  padding-top: 0.85rem;
}

.details-heading span,
.metadata-empty,
.ticket-note {
  color: #64748b;
  font-size: 0.78rem;
  font-weight: 650;
}

.ticket-note {
  color: #92400e;
}

.modal-alert {
  margin: 0.85rem 1rem 0;
  border-radius: 12px;
  padding: 0.7rem 0.85rem;
  font-size: 0.84rem;
  font-weight: 700;
}

.modal-alert--error {
  border: 1px solid rgba(239, 68, 68, 0.22);
  background: #fef2f2;
  color: #b91c1c;
}

.modal-alert--success {
  border: 1px solid rgba(16, 185, 129, 0.24);
  background: #ecfdf5;
  color: #047857;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.65rem;
  border-top: 1px solid rgba(226, 232, 240, 0.96);
  padding-top: 0.9rem;
}

.primary-button {
  border: 1px solid rgba(15, 118, 110, 0.2);
  background: #0f766e;
  color: #ffffff;
  box-shadow: 0 14px 28px rgba(15, 118, 110, 0.18);
}

.primary-button:disabled,
.secondary-button:disabled {
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

.modal-form :deep(.category-field) {
  gap: 0.35rem;
}

.modal-form :deep(.field-heading) {
  margin-bottom: 0.35rem;
}

.modal-form :deep(.field-helper),
.modal-form :deep(.category-choice__base) {
  display: none;
}

.modal-form :deep(.field-label),
.modal-form :deep(.relative label) {
  color: #334155;
  font-size: 0.76rem;
  font-weight: 800;
}

.modal-form :deep(.category-grid) {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.42rem;
  max-height: 8.5rem;
  overflow-y: auto;
  padding-right: 0.15rem;
}

.modal-form :deep(.category-choice) {
  min-height: 40px;
  border-radius: 12px;
  padding: 0.48rem 0.58rem;
  gap: 0.5rem;
}

.modal-form :deep(.category-choice__icon) {
  width: 1.8rem;
  height: 1.8rem;
}

.modal-form :deep(.category-choice__label) {
  font-size: 0.78rem;
  line-height: 1.1;
}

.modal-form :deep(.manage-link) {
  min-height: 1.85rem;
  padding: 0.25rem 0.55rem;
}

.modal-form :deep(.relative > .mb-2) {
  margin-bottom: 0.35rem;
}

.modal-form :deep(.subcategory-trigger) {
  min-height: 40px;
  border-radius: 12px;
}

@media (max-width: 760px) {
  .modal-position {
    align-items: flex-end;
    padding: 0;
  }

  .modal-card {
    max-height: 100dvh;
    border-radius: 20px 20px 0 0;
  }

  .modal-header,
  .header-actions,
  .details-heading,
  .modal-actions {
    display: grid;
  }

  .modal-form {
    grid-template-columns: 1fr;
  }

  .field--2,
  .field--4 {
    grid-column: auto;
  }

  .secondary-button,
  .primary-button {
    width: 100%;
  }
}
</style>
