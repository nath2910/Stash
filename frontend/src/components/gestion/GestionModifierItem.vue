<template>
  <teleport to="body">
    <Transition name="modal-smooth">
      <div v-if="modelValue" class="fixed inset-0 z-[9999]">
        <div class="absolute inset-0 bg-slate-950/48 backdrop-blur-[2px]" @click.self="close"></div>

        <div class="relative z-10 flex min-h-full items-end justify-center p-0 sm:items-center sm:p-4">
          <section
            class="modal-card w-full max-w-4xl max-h-[100dvh] rounded-t-2xl rounded-b-none border bg-white shadow-2xl sm:max-h-[92vh] sm:rounded-2xl"
            role="dialog"
            aria-modal="true"
            aria-labelledby="edit-item-title"
          >
            <header class="modal-card-header flex items-start justify-between border-b p-4 sm:p-5">
              <div>
                <h3 id="edit-item-title">Modifier un item</h3>
                <p>Mets a jour les informations, le statut et les pieces jointes.</p>
              </div>
              <button
                type="button"
                class="modal-close-button"
                aria-label="Fermer"
                @click="close"
              >
                <X class="h-5 w-5" aria-hidden="true" />
              </button>
            </header>

            <div v-if="error" class="modal-alert modal-alert--error">
              {{ error }}
            </div>

            <div v-if="success" class="modal-alert modal-alert--success">
              Modifications enregistrees.
            </div>

            <div class="modal-form">
              <ItemFormFields
                mode="edit"
                surface="modal"
                :item="vente"
                :items="items"
                :saving="loading"
                details-default-open
                :show-details-toggle="false"
                submit-label="Enregistrer"
                @cancel="close"
                @error="handleValidationError"
                @form-change="handleFormChange"
                @submit="save"
              >
                <template #after-fields="{ form }">
                  <section class="attachments-panel">
                    <div class="attachments-header">
                      <div>
                        <p>Pieces jointes</p>
                        <span>
                          PDF ou images (max 10MB). {{ form.type === 'TICKET' ? 'PDF recommande.' : '' }}
                        </span>
                      </div>
                      <div class="attachments-actions">
                        <input
                          ref="fileInput"
                          type="file"
                          class="hidden"
                          :accept="fileAccept"
                          @change="onFileSelected"
                        />
                        <button
                          type="button"
                          class="attachment-upload-button"
                          :disabled="uploading"
                          @click="fileInput?.click()"
                        >
                          {{ uploading ? 'Upload...' : 'Ajouter un fichier' }}
                        </button>
                      </div>
                    </div>

                    <div v-if="attachmentError" class="attachment-error">
                      {{ attachmentError }}
                    </div>

                    <ul v-if="attachments.length" class="attachments-list">
                      <li v-for="att in attachments" :key="att.id" class="attachment-row">
                        <div class="min-w-0">
                          <p>{{ att.filename }}</p>
                          <span>{{ formatSize(att.sizeBytes) }} - {{ att.mimeType || 'application/octet-stream' }}</span>
                        </div>
                        <div class="attachment-row-actions">
                          <button type="button" class="attachment-action" @click="download(att)">
                            Telecharger
                          </button>
                          <button type="button" class="attachment-action is-danger" @click="removeAttachment(att)">
                            Supprimer
                          </button>
                        </div>
                      </li>
                    </ul>

                    <p v-else class="attachments-empty">Aucune piece jointe pour le moment.</p>
                  </section>
                </template>
              </ItemFormFields>
            </div>
          </section>
        </div>
      </div>
    </Transition>
  </teleport>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { X } from 'lucide-vue-next'
import SnkVenteServices from '@/services/SnkVenteServices.js'
import ItemFormFields from '@/components/gestion/ItemFormFields.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  vente: { type: Object, default: null },
  items: { type: Array, default: () => [] },
})

const emit = defineEmits(['update:modelValue', 'saved'])

const loading = ref(false)
const success = ref(false)
const error = ref(null)
const attachments = ref([])
const uploading = ref(false)
const attachmentError = ref(null)
const fileInput = ref(null)
const draftType = ref('OTHER')
let successTimer = null

const fileAccept = computed(() =>
  draftType.value === 'TICKET' ? 'application/pdf,image/*' : 'application/pdf,image/*',
)

watch(
  () => props.vente,
  (vente) => {
    success.value = false
    error.value = null
    attachmentError.value = null
    draftType.value = vente?.type || 'OTHER'
    if (vente?.id) {
      loadAttachments(vente.id)
    } else {
      attachments.value = []
    }
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  if (successTimer) window.clearTimeout(successTimer)
})

function close() {
  emit('update:modelValue', false)
}

function handleValidationError(message) {
  error.value = message
  success.value = false
}

function handleFormChange(form) {
  draftType.value = form?.type || 'OTHER'
}

async function save({ id, payload }) {
  if (!id) {
    error.value = 'Item introuvable.'
    return
  }

  loading.value = true
  success.value = false
  error.value = null

  try {
    const { data } = await SnkVenteServices.update(id, payload)
    success.value = true
    emit('saved', data)

    if (successTimer) window.clearTimeout(successTimer)
    successTimer = window.setTimeout(() => {
      close()
      success.value = false
      successTimer = null
    }, 160)
  } catch (err) {
    error.value = err.response?.data?.message || 'Erreur lors de la modification'
    console.error(err)
  } finally {
    loading.value = false
  }
}

async function loadAttachments(id) {
  try {
    const { data } = await SnkVenteServices.listAttachments(id)
    attachments.value = Array.isArray(data) ? data : []
  } catch (e) {
    console.error('Load attachments failed', e)
    attachmentError.value = 'Impossible de charger les pieces jointes'
  }
}

async function onFileSelected(event) {
  const file = event.target.files?.[0]
  if (!file || !props.vente?.id) return
  attachmentError.value = null
  uploading.value = true
  try {
    await SnkVenteServices.uploadAttachment(props.vente.id, file)
    await loadAttachments(props.vente.id)
  } catch (e) {
    attachmentError.value =
      e?.response?.data?.message || 'Erreur pendant le televersement (max 10MB, PDF ou image)'
  } finally {
    uploading.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}

async function download(att) {
  try {
    const { data } = await SnkVenteServices.downloadAttachment(props.vente.id, att.id)
    const blob = new Blob([data], { type: att.mimeType || 'application/octet-stream' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = att.filename || 'attachment'
    a.click()
    window.URL.revokeObjectURL(url)
  } catch {
    attachmentError.value = 'Telechargement impossible'
  }
}

async function removeAttachment(att) {
  if (!confirm('Supprimer cette piece jointe ?')) return
  try {
    await SnkVenteServices.deleteAttachment(props.vente.id, att.id)
    attachments.value = attachments.value.filter((a) => a.id !== att.id)
  } catch {
    attachmentError.value = 'Suppression impossible'
  }
}

function formatSize(bytes) {
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
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.96)),
    #ffffff;
  color: #0f172a;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.22);
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

.modal-card-header {
  position: sticky;
  top: 4px;
  z-index: 3;
  border-color: rgba(125, 211, 252, 0.26);
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

.modal-card-header p {
  margin-top: 0.25rem;
  color: #64748b;
  font-size: 0.86rem;
  font-weight: 650;
}

.modal-close-button {
  display: inline-grid;
  width: 2.35rem;
  height: 2.35rem;
  place-items: center;
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  color: #475569;
}

.modal-close-button:hover {
  border-color: rgba(20, 184, 166, 0.48);
  background: #ecfdf5;
  color: #0f766e;
}

.modal-form {
  padding: 1rem;
}

.modal-alert {
  margin: 0.85rem 1rem 0;
  border-radius: 12px;
  padding: 0.7rem 0.85rem;
  font-size: 0.84rem;
  font-weight: 750;
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

.attachments-panel {
  display: grid;
  grid-column: 1 / -1;
  gap: 0.85rem;
  border: 1px solid rgba(14, 165, 233, 0.34);
  border-radius: 16px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.86), rgba(248, 250, 252, 0.92)),
    #f8fafc;
  padding: 1rem;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.95),
    0 10px 24px rgba(15, 23, 42, 0.05);
}

.attachments-header,
.attachment-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.85rem;
}

.attachments-header p {
  color: #0f172a;
  font-size: 0.9rem;
  font-weight: 900;
}

.attachments-header span,
.attachment-row span,
.attachments-empty,
.attachment-error {
  color: #64748b;
  font-size: 0.78rem;
  font-weight: 700;
}

.attachment-error {
  color: #b91c1c;
}

.attachment-upload-button,
.attachment-action {
  display: inline-flex;
  min-height: 2.25rem;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(20, 184, 166, 0.32);
  border-radius: 999px;
  background: #ecfdf5;
  color: #047857;
  padding: 0 0.8rem;
  font-size: 0.78rem;
  font-weight: 850;
}

.attachment-upload-button:hover:not(:disabled),
.attachment-action:hover {
  border-color: rgba(20, 184, 166, 0.52);
  background: #ccfbf1;
}

.attachment-upload-button:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.attachments-list {
  display: grid;
  gap: 0.55rem;
}

.attachment-row {
  border: 1px solid rgba(125, 211, 252, 0.28);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.76);
  padding: 0.68rem;
}

.attachment-row p {
  overflow-wrap: anywhere;
  color: #0f172a;
  font-size: 0.86rem;
  font-weight: 800;
}

.attachment-row-actions {
  display: inline-flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.attachment-action.is-danger {
  border-color: rgba(239, 68, 68, 0.24);
  background: #fff7f7;
  color: #b91c1c;
}

.attachment-action.is-danger:hover {
  border-color: rgba(239, 68, 68, 0.42);
  background: #fef2f2;
}

.modal-smooth-enter-active,
.modal-smooth-leave-active {
  transition: opacity 90ms ease;
}

.modal-smooth-enter-active .modal-card,
.modal-smooth-leave-active .modal-card {
  transition:
    transform 110ms cubic-bezier(0.2, 0.9, 0.2, 1),
    opacity 90ms ease;
}

.modal-smooth-enter-from,
.modal-smooth-leave-to {
  opacity: 0;
}

.modal-smooth-enter-from .modal-card {
  opacity: 0.96;
  transform: translateY(10px) scale(0.99);
}

.modal-smooth-leave-to .modal-card {
  opacity: 0.98;
  transform: translateY(6px) scale(0.995);
}

@media (prefers-reduced-motion: reduce) {
  .modal-smooth-enter-active,
  .modal-smooth-leave-active,
  .modal-smooth-enter-active .modal-card,
  .modal-smooth-leave-active .modal-card {
    transition: none;
  }
}

@media (max-width: 639px) {
  .modal-card {
    padding-bottom: max(env(safe-area-inset-bottom), 0.75rem);
  }

  .attachments-header,
  .attachment-row {
    align-items: stretch;
    flex-direction: column;
  }

  .attachment-upload-button,
  .attachment-action {
    width: 100%;
  }
}
</style>
