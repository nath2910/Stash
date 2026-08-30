<template>
  <teleport to="body">
    <Transition name="modal-smooth">
      <div v-if="modelValue" class="fixed inset-0 z-[9999]">
        <div class="absolute inset-0 bg-slate-950/48 backdrop-blur-[2px]" @click.self="close"></div>

        <div
          class="relative z-10 flex min-h-full items-end justify-center p-0 sm:items-center sm:p-4"
          @click.self="close"
        >
          <section
            class="modal-card w-full max-w-4xl max-h-[100dvh] rounded-t-2xl rounded-b-none border bg-white shadow-2xl sm:max-h-[92vh] sm:rounded-2xl"
            role="dialog"
            aria-modal="true"
            aria-labelledby="edit-item-title"
          >
            <header class="modal-card-header flex items-start justify-between border-b p-4 sm:p-5">
              <div class="min-w-0">
                <button
                  v-if="showBackToGroup"
                  type="button"
                  class="modal-back-button"
                  @click="backToGroup"
                >
                  <ArrowLeft class="h-4 w-4" aria-hidden="true" />
                  <span>Retour au groupe</span>
                </button>
                <h3 id="edit-item-title">{{ headerTitle }}</h3>
                <p>{{ headerSubtitle }}</p>
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
              {{ successMessage }}
            </div>

            <div class="modal-form">
              <section v-if="showGroupOverview" class="group-overview">
                <article class="group-summary-card">
                  <div>
                    <p class="group-summary-eyebrow">Ligne regroupee</p>
                    <h4>{{ groupState?.nomItem || groupState?.nom_item }}</h4>
                    <span>
                      {{ groupSummaryLabel }}
                    </span>
                  </div>
                  <button type="button" class="group-edit-button" @click="openGroupEditor">
                    <Pencil class="h-4 w-4" aria-hidden="true" />
                    <span>Modifier le parent</span>
                  </button>
                </article>

                <section class="group-children-panel">
                  <div class="group-children-header">
                  <div>
                    <p>Sous-items</p>
                      <span>Choisis une ligne pour la modifier individuellement.</span>
                    </div>
                  </div>

                  <div class="group-children-list">
                    <article
                      v-for="child in groupChildren"
                      :key="child.id"
                      class="group-child-row"
                    >
                      <div class="min-w-0">
                        <p>{{ childLabel(child) }}</p>
                        <span>
                          {{ childStatusLabel(child) }}
                        </span>
                      </div>
                      <button type="button" class="group-edit-button" @click="openChildEditor(child)">
                        <Pencil class="h-4 w-4" aria-hidden="true" />
                        <span>Modifier</span>
                      </button>
                    </article>
                  </div>
                </section>
              </section>

              <ItemFormFields
                v-else
                mode="edit"
                surface="modal"
                :item="editorItem"
                :items="items"
                :saving="loading"
                details-default-open
                :show-details-toggle="false"
                submit-label="Enregistrer"
                @cancel="groupState ? backToGroup() : close()"
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
                          :disabled="uploading || !editorItem?.id"
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
import { ArrowLeft, Pencil, X } from 'lucide-vue-next'
import SnkVenteServices from '@/services/SnkVenteServices.js'
import ItemFormFields from '@/components/gestion/ItemFormFields.vue'
import { formatEUR } from '@/utils/formatters'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  vente: { type: Object, default: null },
  items: { type: Array, default: () => [] },
})

const emit = defineEmits(['update:modelValue', 'saved'])

const loading = ref(false)
const success = ref(false)
const error = ref(null)
const successMessage = ref('Modifications enregistrees.')
const attachments = ref([])
const uploading = ref(false)
const attachmentError = ref(null)
const fileInput = ref(null)
const draftType = ref('OTHER')
const groupState = ref(null)
const editorItem = ref(null)
const viewMode = ref('form')
let successTimer = null

const fileAccept = computed(() =>
  draftType.value === 'TICKET' ? 'application/pdf,image/*' : 'application/pdf,image/*',
)

const groupChildren = computed(() => (Array.isArray(groupState.value?.children) ? groupState.value.children : []))
const showGroupOverview = computed(
  () => Boolean(groupState.value?.groupParent) && groupChildren.value.length > 0 && viewMode.value === 'group',
)
const showBackToGroup = computed(() => Boolean(groupState.value?.groupParent) && !showGroupOverview.value)
const headerTitle = computed(() => {
  if (showGroupOverview.value) return 'Modifier un groupe'
  if (showBackToGroup.value && editorItem.value?.id === groupState.value?.id) return 'Modifier le parent'
  if (showBackToGroup.value) return 'Modifier un sous-item'
  return 'Modifier un item'
})
const headerSubtitle = computed(() => {
  if (showGroupOverview.value) {
    return 'Ouvre la ligne principale ou une ligne precise sans surcharger le reste.'
  }
  if (showBackToGroup.value && editorItem.value?.id === groupState.value?.id) {
    return 'Les dates et montants du parent sont repercutes sur les sous-items au moment de l enregistrement.'
  }
  if (showBackToGroup.value) {
    return 'Mets a jour cette ligne puis reviens a la liste du groupe.'
  }
  return 'Mets a jour les informations, le statut et les pieces jointes.'
})
const groupSummaryLabel = computed(() => {
  const quantity = groupChildren.value.length
  const sold = groupChildren.value.filter((child) => child?.dateVente || child?.date_vente).length
  if (!sold) return quantity > 1 ? `${quantity} lignes` : '1 ligne'
  if (sold >= quantity) return `${quantity} lignes - tout vendu`
  return `${quantity} lignes - ${sold} vendue${sold > 1 ? 's' : ''}`
})

watch(
  () => props.vente,
  (vente) => {
    success.value = false
    error.value = null
    attachmentError.value = null
    successMessage.value = 'Modifications enregistrees.'

    if (isGroupViewCandidate(vente)) {
      groupState.value = cloneVente(vente)
      editorItem.value = null
      viewMode.value = 'group'
      draftType.value = vente?.type || 'OTHER'
      attachments.value = []
      return
    }

    groupState.value = null
    editorItem.value = cloneVente(vente)
    viewMode.value = 'form'
    draftType.value = vente?.type || 'OTHER'
    if (editorItem.value?.id) {
      loadAttachments(editorItem.value.id)
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

function cloneVente(vente) {
  if (!vente) return null
  try {
    return structuredClone(vente)
  } catch {
    return JSON.parse(JSON.stringify(vente))
  }
}

function isGroupViewCandidate(vente) {
  return Boolean(vente?.groupParent) && Array.isArray(vente?.children) && vente.children.length > 0
}

function handleValidationError(message) {
  error.value = message
  success.value = false
}

function handleFormChange(form) {
  draftType.value = form?.type || 'OTHER'
}

function backToGroup() {
  error.value = null
  attachmentError.value = null
  attachments.value = []
  editorItem.value = null
  viewMode.value = 'group'
}

function openGroupEditor() {
  editorItem.value = cloneVente(groupState.value)
  draftType.value = editorItem.value?.type || 'OTHER'
  viewMode.value = 'form'
  if (editorItem.value?.id) {
    loadAttachments(editorItem.value.id)
  }
}

function openChildEditor(child) {
  editorItem.value = cloneVente(child)
  draftType.value = editorItem.value?.type || 'OTHER'
  viewMode.value = 'form'
  if (editorItem.value?.id) {
    loadAttachments(editorItem.value.id)
  }
}

function childLabel(child) {
  return child?.nomItem || child?.nom_item || 'Sous-item'
}

function childStatusLabel(child) {
  const unitIndex = Number(child?.unitIndex ?? 0)
  const lineLabel = unitIndex > 0 ? `Ligne ${unitIndex}` : 'Ligne'
  const retail = formatEUR(Number(child?.prixRetail ?? child?.prix_retail ?? 0), { digits: 0 })
  const resellRaw = Number(child?.prixResell ?? child?.prix_resell ?? 0)
  const sold = Boolean(child?.dateVente || child?.date_vente)
  if (!sold) return `${lineLabel} - ${retail} - non vendu`
  return `${lineLabel} - ${retail} - vendu ${formatEUR(resellRaw, { digits: 0 })}`
}

function applySavedItem(saved) {
  if (!groupState.value || !saved?.id) return
  if (Number(saved.id) === Number(groupState.value.id)) {
    groupState.value = { ...groupState.value, ...saved }
    return
  }

  groupState.value = {
    ...groupState.value,
    children: groupChildren.value.map((child) =>
      Number(child.id) === Number(saved.id) ? { ...child, ...saved } : child,
    ),
  }
}

async function refreshGroupState(groupId, fallbackSaved = null) {
  if (!groupId) {
    if (fallbackSaved) applySavedItem(fallbackSaved)
    return
  }

  try {
    const { data } = await SnkVenteServices.getGroupedSnkVente()
    const nextGroup = Array.isArray(data)
      ? data.find((item) => Number(item?.id) === Number(groupId))
      : null
    if (nextGroup) {
      groupState.value = cloneVente(nextGroup)
      return
    }
  } catch (err) {
    console.error('Refresh grouped item failed', err)
  }

  if (fallbackSaved) applySavedItem(fallbackSaved)
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
    const currentGroupId = groupState.value?.id
    const editingParent = Boolean(groupState.value) && Number(id) === Number(currentGroupId)
    const { data } = await SnkVenteServices.update(id, payload)
    success.value = true
    successMessage.value = editingParent
      ? 'Parent enregistre et sous-items mis a jour.'
      : 'Modifications enregistrees.'
    emit('saved', data)

    if (groupState.value) {
      await refreshGroupState(currentGroupId, data || { id, ...payload })
      backToGroup()
      return
    }

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
  if (!id) {
    attachments.value = []
    return
  }
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
  const targetId = editorItem.value?.id
  if (!file || !targetId) return
  attachmentError.value = null
  uploading.value = true
  try {
    await SnkVenteServices.uploadAttachment(targetId, file)
    await loadAttachments(targetId)
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
    const { data } = await SnkVenteServices.downloadAttachment(editorItem.value.id, att.id)
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
    await SnkVenteServices.deleteAttachment(editorItem.value.id, att.id)
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
  position: relative;
  isolation: isolate;
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
  z-index: 90;
  display: block;
  height: 4px;
  background: linear-gradient(90deg, #0ea5e9, #14b8a6, #f59e0b);
}

.modal-card-header {
  position: sticky;
  top: 4px;
  z-index: 80;
  border-color: rgba(125, 211, 252, 0.26);
  background:
    linear-gradient(135deg, rgba(236, 253, 245, 0.985), rgba(224, 242, 254, 0.965)),
    rgba(255, 255, 255, 0.992);
  backdrop-filter: blur(18px);
  box-shadow:
    0 1px 0 rgba(125, 211, 252, 0.22),
    0 18px 26px -26px rgba(15, 23, 42, 0.46);
}

.modal-card-header::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -18px;
  height: 18px;
  pointer-events: none;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.96), rgba(248, 250, 252, 0));
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

.modal-back-button {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  margin-bottom: 0.55rem;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.84);
  color: #0f766e;
  padding: 0.35rem 0.7rem;
  font-size: 0.76rem;
  font-weight: 850;
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
  position: relative;
  z-index: 1;
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

.group-overview {
  display: grid;
  gap: 1rem;
}

.group-summary-card,
.group-children-panel {
  display: grid;
  gap: 0.9rem;
  border: 1px solid rgba(125, 211, 252, 0.32);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(248, 250, 252, 0.96)),
    #ffffff;
  padding: 1rem;
}

.group-summary-card {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
}

.group-summary-eyebrow,
.group-children-header p {
  color: #0f766e;
  font-size: 0.74rem;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.group-summary-card h4 {
  margin-top: 0.18rem;
  color: #0f172a;
  font-size: 1.05rem;
  font-weight: 900;
}

.group-summary-card span,
.group-children-header span,
.group-child-row span {
  color: #64748b;
  font-size: 0.8rem;
  font-weight: 700;
  line-height: 1.35;
}

.group-children-list {
  display: grid;
  gap: 0.6rem;
}

.group-child-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.8rem;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.82);
  padding: 0.8rem;
}

.group-child-row p {
  color: #0f172a;
  font-size: 0.88rem;
  font-weight: 850;
}

.group-edit-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  min-height: 2.3rem;
  border: 1px solid rgba(20, 184, 166, 0.28);
  border-radius: 999px;
  background: #ecfdf5;
  color: #0f766e;
  padding: 0 0.85rem;
  font-size: 0.78rem;
  font-weight: 850;
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

  .group-summary-card,
  .group-child-row {
    grid-template-columns: 1fr;
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
