<template>
  <teleport to="body">
    <div v-if="modelValue" class="fixed inset-0 z-[9999]">
      <!-- overlay -->
      <div class="absolute inset-0 bg-slate-950/55 backdrop-blur-sm" @click.self="close"></div>

      <!-- modal -->
      <div
        class="relative z-10 flex min-h-full items-end justify-center p-0 sm:items-center sm:p-4"
      >
        <div
          class="modal-card w-full max-w-4xl max-h-[100dvh] rounded-t-2xl rounded-b-none border border-gray-700 bg-gray-800 shadow-2xl sm:max-h-[92vh] sm:rounded-2xl"
        >
          <!-- Header -->
          <div class="modal-card-header flex items-start justify-between border-b border-gray-700 p-4 sm:p-5">
            <div>
              <h3 class="text-xl font-semibold text-gray-100">Modifier un item</h3>
              <p class="mt-1 text-sm text-gray-400">Mets a jour les informations, le statut et les pieces jointes.</p>
            </div>
            <div class="[&_button:hover]:bg-gray-600">
              <button
                type="button"
                @click="close"
                class="rounded-lg p-2 text-gray-300"
                aria-label="Fermer"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M6 18L18 6M6 6l12 12"
                  />
                </svg>
              </button>
            </div>
          </div>

          <!-- Erreur -->
          <div
            v-if="error"
            class="mx-4 mt-4 rounded-lg border border-red-500/40 bg-red-500/10 p-3 text-sm text-red-200 sm:mx-6"
          >
            {{ error }}
          </div>

          <!-- Succes -->
          <div
            v-if="success"
            class="mx-4 mt-4 rounded-lg border border-emerald-500/40 bg-emerald-500/10 p-3 text-sm text-emerald-200 sm:mx-6"
          >
            Modifications enregistrees.
          </div>

          <!-- Formulaire -->
          <form class="modal-form space-y-5 p-4 sm:p-6" @submit.prevent="save">
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <!-- Categorie principale -->
              <div class="metadata-section sm:col-span-2">
                <ItemCategorySelect
                  :model-value="form.type"
                  :user-id="currentUserId || 'guest'"
                  :labels="categoryLabels"
                  @update:modelValue="setType"
                  @labels-change="setCategoryLabels"
                />
              </div>

              <!-- Nom -->
              <div>
                <label class="block text-sm font-medium text-gray-200 mb-2">Nom de l'item</label>
                <input
                  type="text"
                  v-model="form.nomItem"
                  required
                  class="w-full rounded-lg border border-gray-600 bg-gray-900 text-gray-100 px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500/50 focus:border-purple-500"
                />
              </div>
              <!-- Sous-categorie -->
              <div>
                <ItemSubcategorySelect
                  v-model="form.categorie"
                  :type="form.type"
                  :user-id="currentUserId || 'guest'"
                  :category-labels="categoryLabels"
                />
              </div>

              <!-- Prix retail -->
              <div>
                <label class="block text-sm font-medium text-gray-200 mb-2"
                  >Prix d'achat (EUR)</label
                >
                <input
                  type="number"
                  v-model.number="form.prixRetail"
                  min="0"
                  step="0.01"
                  required
                  class="w-full rounded-lg border border-gray-600 bg-gray-900 text-gray-100 px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500/50 focus:border-purple-500"
                />
              </div>

              <!-- Prix revente -->
              <div>
                <label class="block text-sm font-medium text-gray-200 mb-2"
                  >Prix de vente (EUR)</label
                >
                <input
                  type="number"
                  v-model.number="form.prixResell"
                  min="0"
                  step="0.01"
                  class="w-full rounded-lg border border-gray-600 bg-gray-900 text-gray-100 px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500/50 focus:border-purple-500"
                />
              </div>

              <!-- Date achat -->
              <div>
                <label class="block text-sm font-medium text-gray-200 mb-2">Date d'achat</label>
                <CompactDateInput v-model="form.dateAchat" class="w-full" />
              </div>

              <!-- Date vente -->
              <div>
                <label class="block text-sm font-medium text-gray-200 mb-2">Date de vente</label>
                <CompactDateInput v-model="form.dateVente" class="w-full" />
                <p class="mt-1 text-xs text-gray-500">Laisse vide si pas vendue.</p>
              </div>

              <!-- Description -->
              <div class="sm:col-span-2">
                <label class="block text-sm font-medium text-gray-200 mb-2">Description</label>
                <textarea
                  v-model="form.description"
                  rows="3"
                  class="w-full rounded-lg border border-gray-600 bg-gray-900 text-gray-100 px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500/50 focus:border-purple-500"
                ></textarea>
              </div>

              <!-- Champs spécifiques -->
              <div class="sm:col-span-2">
                <div class="flex items-center justify-between mb-2">
                  <p class="text-sm font-medium text-gray-200">Champs spécifiques</p>
                  <p class="text-xs text-gray-400">
                    Adaptés au type {{ currentTypeLabel.toLowerCase() }}
                  </p>
                </div>
                <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div v-for="field in metadataFields" :key="field.key" class="space-y-1">
                    <label class="text-xs text-gray-300">{{ field.label }}</label>
                    <input
                      type="text"
                      v-model.trim="form.metadata[field.key]"
                      :placeholder="field.placeholder"
                      class="w-full rounded-lg border border-gray-600 bg-gray-900 text-gray-100 px-3 py-2 text-sm placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500/40 focus:border-purple-500"
                    />
                  </div>
                </div>
                <p v-if="form.type === 'TICKET'" class="mt-2 text-xs text-amber-200/80">
                  Ajoute une pièce jointe (PDF ou image) plus bas pour sécuriser le billet.
                </p>
              </div>
            </div>

            <!-- Attachments -->
            <div class="attachments-panel rounded-xl border border-gray-700 bg-gray-900/60 p-4 space-y-3">
              <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <p class="text-sm font-semibold text-gray-100">Pièces jointes</p>
                  <p class="text-xs text-gray-400">
                    PDF ou images (max 10MB). {{ form.type === 'TICKET' ? 'PDF recommandé.' : '' }}
                  </p>
                </div>
                <div class="flex items-center gap-2 sm:shrink-0">
                  <input
                    ref="fileInput"
                    type="file"
                    class="hidden"
                    :accept="fileAccept"
                    @change="onFileSelected"
                  />
                  <button
                    type="button"
                    class="attachment-upload-button px-3 py-2 text-xs rounded-lg border border-emerald-400/50 bg-emerald-500/10 text-emerald-100 hover:bg-emerald-500/20 transition disabled:opacity-60"
                    :disabled="uploading"
                    @click="fileInput?.click()"
                  >
                    {{ uploading ? 'Upload...' : 'Ajouter un fichier' }}
                  </button>
                </div>
              </div>

              <div v-if="attachmentError" class="text-xs text-red-300">
                {{ attachmentError }}
              </div>

              <ul v-if="attachments.length" class="space-y-2">
                <li
                  v-for="att in attachments"
                  :key="att.id"
                  class="flex flex-col gap-3 rounded-lg border border-gray-700 bg-gray-800/70 px-3 py-2 text-sm sm:flex-row sm:items-center sm:justify-between"
                >
                  <div class="min-w-0">
                    <p class="break-words text-gray-100">{{ att.filename }}</p>
                    <p class="text-[11px] text-gray-400">
                      {{ formatSize(att.sizeBytes) }} •
                      {{ att.mimeType || 'application/octet-stream' }}
                    </p>
                  </div>
                  <div class="flex flex-wrap items-center gap-2 sm:shrink-0">
                    <button
                      type="button"
                      class="px-2 py-1 text-xs rounded border border-blue-400/50 text-blue-200 hover:bg-blue-500/10"
                      @click="download(att)"
                    >
                      Télécharger
                    </button>
                    <button
                      type="button"
                      class="px-2 py-1 text-xs rounded border border-red-400/50 text-red-200 hover:bg-red-500/10"
                      @click="removeAttachment(att)"
                    >
                      Supprimer
                    </button>
                  </div>
                </li>
              </ul>

              <p v-else class="text-xs text-gray-400">Aucune pièce jointe pour le moment.</p>
            </div>

            <!-- Footer actions -->
            <div
              class="modal-footer-sticky flex flex-col-reverse gap-2 border-t border-gray-700 pt-4 sm:flex-row sm:items-center sm:justify-end"
            >
              <button
                type="button"
                @click="close"
                class="w-full rounded-lg border border-gray-600 px-4 py-2 text-sm text-gray-200 transition hover:bg-gray-700/50 sm:w-auto"
                :disabled="loading"
              >
                Annuler
              </button>

              <button
                type="submit"
                :disabled="loading"
                class="w-full whitespace-nowrap rounded-lg bg-purple-600 px-5 py-2 text-sm text-white transition hover:bg-purple-700 focus:outline-none focus:ring-2 focus:ring-purple-500/40 disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
              >
                {{ loading ? 'Enregistrement...' : 'Enregistrer' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import SnkVenteServices from '@/services/SnkVenteServices.js'
import CompactDateInput from '@/components/ui/CompactDateInput.vue'
import ItemCategorySelect from '@/components/gestion/ItemCategorySelect.vue'
import ItemSubcategorySelect from '@/components/gestion/ItemSubcategorySelect.vue'
import { METADATA_FIELDS } from '@/RegleItem/CategorieItem'
import { itemTypeLabel, readStoredItemCategories } from '@/RegleItem/itemCategoryStore'
import { useAuthStore } from '@/store/authStore'

const props = defineProps({
  modelValue: { type: Boolean, default: false }, // v-model
  vente: { type: Object, default: null },
})

const emit = defineEmits(['update:modelValue', 'saved'])
const authStore = useAuthStore()
const currentUserId = computed(() => authStore.user?.value?.id ?? authStore.user?.id ?? null)
const categoryLabels = ref(readStoredItemCategories(currentUserId.value || 'guest'))

const loading = ref(false)
const success = ref(false)
const error = ref(null)
const requireDateVente = ref(false)
const attachments = ref([])
const uploading = ref(false)
const attachmentError = ref(null)
const fileInput = ref(null)

const form = ref({
  id: null,
  nomItem: '',
  prixRetail: null,
  prixResell: null,
  dateAchat: '',
  dateVente: null,
  description: '',
  categorie: '',
  type: 'SNEAKER',
  metadata: {},
})

const setType = (type) => {
  if (form.value.type === type) return
  form.value.type = type
  form.value.categorie = ''
  form.value.metadata = {}
}

const setCategoryLabels = (labels) => {
  categoryLabels.value = labels
}

// quand on recoit une vente a editer => on pre-remplit
watch(
  () => props.vente,
  (v) => {
    if (!v) return
    form.value = {
      id: v.id,
      nomItem: v.nomItem ?? v.nom_item ?? '',
      prixRetail: v.prixRetail ?? v.prix_retail ?? null,
      prixResell: v.prixResell ?? v.prix_resell ?? null,
      dateAchat: (v.dateAchat ?? v.date_achat ?? '').slice(0, 10),
      dateVente:
        (v.dateVente ?? v.date_vente ?? '') ? (v.dateVente ?? v.date_vente).slice(0, 10) : null,
      description: v.description ?? '',
      categorie: v.categorie ?? '',
      type: v.type ?? 'SNEAKER',
      metadata: { ...(v.metadata || {}) },
    }
    success.value = false
    error.value = null
    requireDateVente.value =
      form.value.prixResell !== null &&
      form.value.prixResell !== '' &&
      form.value.prixResell !== undefined
    if (v.id) {
      loadAttachments(v.id)
    } else {
      attachments.value = []
    }
  },
  { immediate: true },
)

const close = () => {
  emit('update:modelValue', false)
}

const metadataFields = computed(() => METADATA_FIELDS[form.value.type] || [])
const cleanedMetadata = computed(() => {
  const out = {}
  for (const f of metadataFields.value) {
    const v = form.value.metadata?.[f.key]
    if (v !== undefined && v !== null && String(v).trim() !== '') out[f.key] = String(v).trim()
  }
  return out
})
const currentTypeLabel = computed(() => itemTypeLabel(form.value.type, categoryLabels.value))
const fileAccept = computed(() =>
  form.value.type === 'TICKET' ? 'application/pdf,image/*' : 'application/pdf,image/*',
)

watch(
  () => currentUserId.value,
  (userId) => {
    categoryLabels.value = readStoredItemCategories(userId || 'guest')
  },
)

const save = async () => {
  requireDateVente.value =
    form.value.prixResell !== null &&
    form.value.prixResell !== '' &&
    form.value.prixResell !== undefined

  if (requireDateVente.value && !form.value.dateVente) {
    error.value = 'Ajoute une date de vente si tu saisis un prix de revente.'
    return
  }

  if (form.value.dateVente && (form.value.prixResell === null || form.value.prixResell === '')) {
    form.value.prixResell = 0
  }

  loading.value = true
  success.value = false
  error.value = null

  try {
    const { data } = await SnkVenteServices.update(form.value.id, {
      nomItem: form.value.nomItem,
      prixRetail: form.value.prixRetail,
      prixResell: form.value.prixResell,
      dateAchat: form.value.dateAchat,
      dateVente: form.value.dateVente,
      description: form.value.description,
      categorie: form.value.categorie,
      type: form.value.type,
      metadata: cleanedMetadata.value,
    })

    success.value = true
    emit('saved', data)

    setTimeout(() => {
      close()
      success.value = false
    }, 600)
  } catch (err) {
    error.value = err.response?.data?.message || 'Erreur lors de la modification'
    console.error(err)
  } finally {
    loading.value = false
  }
}

const loadAttachments = async (id) => {
  try {
    const { data } = await SnkVenteServices.listAttachments(id)
    attachments.value = Array.isArray(data) ? data : []
  } catch (e) {
    console.error('Load attachments failed', e)
    attachmentError.value = 'Impossible de charger les pièces jointes'
  }
}

const onFileSelected = async (event) => {
  const file = event.target.files?.[0]
  if (!file || !form.value.id) return
  attachmentError.value = null
  uploading.value = true
  try {
    await SnkVenteServices.uploadAttachment(form.value.id, file)
    await loadAttachments(form.value.id)
  } catch (e) {
    attachmentError.value =
      e?.response?.data?.message || 'Erreur pendant le téléversement (max 10MB, PDF ou image)'
  } finally {
    uploading.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}

const download = async (att) => {
  try {
    const { data } = await SnkVenteServices.downloadAttachment(form.value.id, att.id)
    const blob = new Blob([data], { type: att.mimeType || 'application/octet-stream' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = att.filename || 'attachment'
    a.click()
    window.URL.revokeObjectURL(url)
  } catch {
    attachmentError.value = 'Téléchargement impossible'
  }
}

const removeAttachment = async (att) => {
  if (!confirm('Supprimer cette pièce jointe ?')) return
  try {
    await SnkVenteServices.deleteAttachment(form.value.id, att.id)
    attachments.value = attachments.value.filter((a) => a.id !== att.id)
  } catch {
    attachmentError.value = 'Suppression impossible'
  }
}

const formatSize = (bytes) => {
  if (!bytes && bytes !== 0) return '-'
  if (bytes < 1024) return `${bytes} o`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} ko`
  return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`
}
</script>

<style scoped>
.modal-card {
  border-color: rgba(125, 211, 252, 0.38);
  background:
    linear-gradient(135deg, rgba(14, 165, 233, 0.08), transparent 42%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 244, 238, 0.98)),
    #fbfaf6;
  color: #0f172a;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: none;
  box-shadow: 0 28px 90px rgba(15, 23, 42, 0.24);
}
.modal-card::before {
  content: '';
  position: sticky;
  top: 0;
  z-index: 4;
  display: block;
  height: 4px;
  background: linear-gradient(90deg, #0ea5e9, #14b8a6, #f59e0b);
}
.modal-card::-webkit-scrollbar {
  display: none;
}
.modal-card-header {
  position: sticky;
  top: 4px;
  z-index: 3;
  background:
    linear-gradient(135deg, rgba(236, 253, 245, 0.94), rgba(224, 242, 254, 0.78)),
    rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(10px);
}
.modal-card-header h3 {
  color: #0f172a;
  font-size: clamp(1.25rem, 2vw, 1.55rem);
  font-weight: 950;
  letter-spacing: 0;
}
.modal-form {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.5), rgba(247, 244, 238, 0.92)),
    #fbfaf6;
}
.modal-footer-sticky {
  position: sticky;
  bottom: 0;
  z-index: 2;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.86), rgba(255, 255, 255, 0.98));
  backdrop-filter: blur(6px);
}

.modal-card :is(.border-gray-700, .border-gray-600) {
  border-color: rgba(14, 165, 233, 0.38);
}

.modal-card :is(.bg-gray-900\/60, .bg-gray-900, .bg-gray-800) {
  background: #f8fafc;
}

.modal-card [class*='bg-slate-'],
.modal-card [class*='bg-white/'] {
  background: rgba(255, 255, 255, 0.78);
}

.modal-card :is(.text-gray-100, .text-gray-200, .text-gray-300) {
  color: #0f172a;
}

.modal-card :is(.text-gray-400, .text-gray-500) {
  color: #64748b;
}

.modal-card :is(input, textarea) {
  border-color: rgba(14, 165, 233, 0.34);
  background: #f8fafc;
  color: #0f172a;
  min-height: 2.65rem;
  border-radius: 0.9rem;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.95);
}

.modal-card :is(input, textarea)::placeholder {
  color: #94a3b8;
}

.modal-card :is(input, textarea):focus {
  border-color: rgba(20, 184, 166, 0.72);
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(20, 184, 166, 0.14);
}

.metadata-section,
.attachments-panel {
  border: 1px solid rgba(14, 165, 233, 0.34);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.86), rgba(248, 250, 252, 0.92)),
    #f8fafc;
  padding: 1rem;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.95);
}

.metadata-section {
  margin-top: 0.15rem;
}

.attachments-panel {
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.95),
    0 10px 24px rgba(15, 23, 42, 0.05);
}

.modal-card button[type='submit'] {
  border-color: rgba(15, 118, 110, 0.2);
  background: linear-gradient(135deg, #0f766e, #0e7490);
  color: #ffffff;
  border-radius: 999px;
  font-weight: 900;
  min-height: 2.55rem;
}
.modal-card button:not([type='submit']) {
  border-radius: 999px;
  font-weight: 800;
}
.modal-card :is(.text-blue-200) {
  color: #0369a1;
}
.modal-card :is(.text-emerald-100, .text-emerald-200) {
  color: #047857;
}
.modal-card :is(.text-red-200, .text-red-300) {
  color: #b91c1c;
}

.attachment-upload-button {
  background: #ecfdf5;
  color: #047857;
}

.attachment-upload-button:hover:not(:disabled) {
  background: #ccfbf1;
}

.attachment-upload-button:disabled {
  opacity: 1;
  background: #d1fae5;
  color: #0f766e;
}
@media (max-width: 639px) {
  .modal-card {
    padding-bottom: max(env(safe-area-inset-bottom), 0.75rem);
  }
  .modal-footer-sticky {
    padding-bottom: calc(1rem + env(safe-area-inset-bottom, 0px));
  }
}
</style>
