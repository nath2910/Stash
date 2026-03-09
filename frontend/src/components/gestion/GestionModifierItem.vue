<template>
  <teleport to="body">
    <div v-if="modelValue" class="fixed inset-0 z-[9999]">
      <!-- overlay -->
      <div class="absolute inset-0 bg-black/80 backdrop-blur-sm" @click.self="close"></div>

      <!-- modal -->
      <div class="relative z-10 flex items-center justify-center min-h-full p-4">
        <div
          class="modal-card w-full max-w-3xl max-h-[85vh] rounded-2xl bg-gray-800 border border-gray-700 shadow-2xl"
        >
          <!-- Header -->
          <div class="flex items-start justify-between p-5 border-b border-gray-700">
            <div>
              <h3 class="text-xl font-semibold text-gray-100">Modifier une vente</h3>
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
            class="mx-6 mt-4 rounded-lg border border-red-500/40 bg-red-500/10 p-3 text-sm text-red-200"
          >
            {{ error }}
          </div>

          <!-- Succes -->
          <div
            v-if="success"
            class="mx-6 mt-4 rounded-lg border border-emerald-500/40 bg-emerald-500/10 p-3 text-sm text-emerald-200"
          >
            Modifications enregistrees.
          </div>

          <!-- Formulaire -->
          <form class="p-6 space-y-6 overflow-hidden" @submit.prevent="save">
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <!-- Type -->
              <div class="sm:col-span-2">
                <label class="block text-sm font-medium text-gray-200 mb-2">Type d'item</label>
                <div class="flex flex-wrap gap-2">
                  <button
                    v-for="option in ITEM_TYPES"
                    :key="option.value"
                    type="button"
                    @click="form.type = option.value"
                    class="px-3 py-2 rounded-lg border text-sm transition"
                    :class="
                      form.type === option.value
                        ? 'border-emerald-400 bg-emerald-400/10 text-emerald-100'
                        : 'border-gray-600 bg-gray-900 text-gray-300 hover:border-gray-500'
                    "
                  >
                    {{ option.label }}
                  </button>
                </div>
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
              <!-- Categorie -->
              <div>
                <label class="block text-sm font-medium text-gray-200 mb-2">Categorie</label>
                <input
                  type="text"
                  v-model="form.categorie"
                  class="w-full rounded-lg border border-gray-600 bg-gray-900 text-gray-100 px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500/50 focus:border-purple-500"
                />
              </div>

              <!-- Prix retail -->
              <div>
                <label class="block text-sm font-medium text-gray-200 mb-2">Prix Retail (EUR)</label>
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
                <label class="block text-sm font-medium text-gray-200 mb-2">Prix Revente (EUR)</label>
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
            <div class="rounded-xl border border-gray-700 bg-gray-900/60 p-4 space-y-3">
              <div class="flex items-start justify-between gap-2">
                <div>
                  <p class="text-sm font-semibold text-gray-100">Pièces jointes</p>
                  <p class="text-xs text-gray-400">
                    PDF ou images (max 10MB). {{ form.type === 'TICKET' ? 'PDF recommandé.' : '' }}
                  </p>
                </div>
                <div class="flex items-center gap-2">
                  <input
                    ref="fileInput"
                    type="file"
                    class="hidden"
                    :accept="fileAccept"
                    @change="onFileSelected"
                  />
                  <button
                    type="button"
                    class="px-3 py-2 text-xs rounded-lg border border-emerald-400/50 bg-emerald-500/10 text-emerald-100 hover:bg-emerald-500/20 transition disabled:opacity-60"
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
                  class="flex items-center justify-between gap-3 rounded-lg border border-gray-700 bg-gray-800/70 px-3 py-2 text-sm"
                >
                  <div>
                    <p class="text-gray-100">{{ att.filename }}</p>
                    <p class="text-[11px] text-gray-400">
                      {{ formatSize(att.sizeBytes) }} • {{ att.mimeType || 'application/octet-stream' }}
                    </p>
                  </div>
                  <div class="flex items-center gap-2">
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
            <div class="pt-4 border-t border-gray-700 flex items-center justify-end gap-2">
              <button
                type="button"
                @click="close"
                class="px-4 py-2 text-sm rounded-lg border border-gray-600 text-gray-200 hover:bg-gray-700/50 transition"
                :disabled="loading"
              >
                Annuler
              </button>

              <button
                type="submit"
                :disabled="loading"
                class="px-5 py-2 text-sm rounded-lg text-white bg-purple-600 hover:bg-purple-700 focus:outline-none focus:ring-2 focus:ring-purple-500/40 disabled:opacity-60 disabled:cursor-not-allowed transition whitespace-nowrap"
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
import { ITEM_TYPES, METADATA_FIELDS, typeLabel } from '@/constants/itemTypes'

const props = defineProps({
  modelValue: { type: Boolean, default: false }, // v-model
  vente: { type: Object, default: null },
})

const emit = defineEmits(['update:modelValue', 'saved'])

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
const currentTypeLabel = computed(() => typeLabel(form.value.type))
const fileAccept = computed(() =>
  form.value.type === 'TICKET' ? 'application/pdf,image/*' : 'application/pdf,image/*',
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
    attachmentError.value = "Impossible de charger les pièces jointes"
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
  } catch (e) {
    attachmentError.value = 'Téléchargement impossible'
  }
}

const removeAttachment = async (att) => {
  if (!confirm('Supprimer cette pièce jointe ?')) return
  try {
    await SnkVenteServices.deleteAttachment(form.value.id, att.id)
    attachments.value = attachments.value.filter((a) => a.id !== att.id)
  } catch (e) {
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
  overflow-y: auto;
  scrollbar-width: none;
}
.modal-card::-webkit-scrollbar {
  display: none;
}
</style>
