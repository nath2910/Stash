<!-- src/components/AjoutPaire.vue -->
<template>
  <teleport to="body">
    <div class="fixed inset-0 z-[9999]">
      <!-- overlay -->
      <div class="absolute inset-0 bg-slate-950/55 backdrop-blur-sm" @click.self="handleClose"></div>

      <!-- modal -->
      <div
        class="relative z-10 flex min-h-full items-end justify-center p-0 sm:items-center sm:p-4"
      >
        <div
          class="modal-card w-full max-w-4xl max-h-[100dvh] rounded-t-2xl rounded-b-none border border-gray-700 bg-gray-800 shadow-2xl sm:max-h-[92vh] sm:rounded-2xl"
          @click.stop
        >
          <!-- Header -->
          <div class="modal-card-header flex items-start justify-between border-b border-gray-700 p-4 sm:p-5">
            <div>
              <h3 class="text-xl font-semibold text-gray-100">Ajouter un item</h3>
              <p class="text-sm text-gray-400 mt-1">
                Categorie, nom et prix suffisent pour l'ajouter rapidement.
              </p>
            </div>

            <!-- Close -->
            <button
              type="button"
              @click="handleClose"
              class="rounded-lg p-2 text-gray-300 transition hover:bg-gray-700/60 hover:text-white focus:outline-none focus:ring-2 focus:ring-purple-500/30"
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

          <!-- Alerts -->
          <div
            v-if="error"
            class="mx-4 mt-4 rounded-lg border border-red-500/40 bg-red-500/10 p-3 text-sm text-red-200 sm:mx-6"
          >
            {{ error }}
          </div>

          <div
            v-if="success"
            class="mx-4 mt-4 rounded-lg border border-emerald-500/40 bg-emerald-500/10 p-3 text-sm text-emerald-200 sm:mx-6"
          >
            Item{{ copies > 1 ? 's' : '' }} ajoute{{ copies > 1 ? 's' : '' }} avec succes.
          </div>

          <!-- Form -->
          <form class="modal-form space-y-5 p-4 sm:p-6" @submit.prevent="createSales">
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <!-- Categorie principale -->
              <div class="sm:col-span-2">
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
                <label for="nomItem" class="block text-sm font-medium text-gray-200 mb-2">
                  Nom de l'item
                </label>
                <input
                  id="nomItem"
                  type="text"
                  v-model.trim="form.nomItem"
                  placeholder="Dunk low, Bundle 151, Ruinart ..."
                  required
                  class="w-full rounded-lg border border-gray-600 bg-gray-900 text-gray-100 px-3 py-2.5 text-sm placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500/50 focus:border-purple-500"
                />
              </div>

              <!-- Sous-categorie -->
              <div>
                <ItemSubcategorySelect
                  v-model="form.categorie"
                  :type="form.type"
                  :user-id="currentUserId || 'guest'"
                  :discovered="discoveredSubcategories"
                  :category-labels="categoryLabels"
                />
              </div>

              <!-- Nombre d'exemplaires -->
              <div>
                <label for="copies" class="block text-sm font-medium text-gray-200 mb-2">
                  Nombre d'exemplaires
                </label>
                <input
                  id="copies"
                  type="number"
                  v-model.number="copies"
                  min="1"
                  max="50"
                  step="1"
                  required
                  class="w-full rounded-lg border border-gray-600 bg-gray-900 text-gray-100 px-3 py-2.5 text-sm placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500/50 focus:border-purple-500"
                />
                <p class="mt-1 text-xs text-gray-500">Determine la quantite voulue.</p>
              </div>

              <!-- Prix retail -->
              <div>
                <label for="prixRetail" class="block text-sm font-medium text-gray-200 mb-2">
                  Prix d'achat (EUR)
                </label>
                <input
                  id="prixRetail"
                  type="number"
                  v-model.number="form.prixRetail"
                  placeholder="110"
                  min="0"
                  step="0.01"
                  required
                  class="w-full rounded-lg border border-gray-600 bg-gray-900 text-gray-100 px-3 py-2.5 text-sm placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500/50 focus:border-purple-500"
                />
              </div>

              <!-- Prix revente -->
              <div>
                <label for="prixResell" class="block text-sm font-medium text-gray-200 mb-2">
                  Prix de vente (EUR)
                </label>
                <input
                  id="prixResell"
                  type="number"
                  v-model.number="form.prixResell"
                  placeholder="180"
                  min="0"
                  step="0.01"
                  class="w-full rounded-lg border border-gray-600 bg-gray-900 text-gray-100 px-3 py-2.5 text-sm placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500/50 focus:border-purple-500"
                />
              </div>

              <!-- Date achat -->
              <div>
                <label for="dateAchat" class="block text-sm font-medium text-gray-200 mb-2">
                  Date d'achat
                </label>
                <CompactDateInput id="dateAchat" v-model="form.dateAchat" class="w-full" />
              </div>

              <!-- Date vente -->
              <div>
                <label for="dateVente" class="block text-sm font-medium text-gray-200 mb-2">
                  Date de vente
                </label>
                <CompactDateInput id="dateVente" v-model="form.dateVente" class="w-full" />
                <p class="mt-1 text-xs text-gray-500">Laisse vide si pas encore vendue.</p>
              </div>

              <!-- Description -->
              <div class="sm:col-span-2">
                <label for="description" class="block text-sm font-medium text-gray-200 mb-2">
                  Description
                </label>
                <textarea
                  id="description"
                  v-model.trim="form.description"
                  rows="3"
                  placeholder="Etat, taille, notes perso..."
                  class="w-full rounded-lg border border-gray-600 bg-gray-900 text-gray-100 px-3 py-2.5 text-sm placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500/50 focus:border-purple-500"
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
                  Tu pourras ensuite ajouter un PDF ou une image du ticket dans la fiche (onglet
                  pièces jointes).
                </p>
              </div>
            </div>

            <!-- Footer -->
            <div
              class="modal-footer-sticky flex flex-col-reverse gap-2 border-t border-gray-700 pt-4 sm:flex-row sm:items-center sm:justify-end"
            >
              <button
                type="button"
                @click="handleClose"
                class="w-full rounded-lg border border-gray-600 px-4 py-2 text-sm text-gray-200 transition hover:bg-gray-700/50 disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
                :disabled="loading"
              >
                Annuler
              </button>

              <button
                type="submit"
                :disabled="loading"
                class="inline-flex w-full items-center justify-center gap-2 whitespace-nowrap rounded-lg bg-purple-600 px-5 py-2 text-sm text-white transition hover:bg-purple-700 focus:outline-none focus:ring-2 focus:ring-emerald-500/40 disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
              >
                <span>
                  {{
                    loading
                      ? 'Enregistrement...'
                      : copies > 1
                        ? `Ajouter ${copies} items`
                        : "Ajouter l'item"
                  }}
                </span>
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import SnkVenteServices from '@/services/SnkVenteServices.js'
import { useAuthStore } from '@/store/authStore'
import CompactDateInput from '@/components/ui/CompactDateInput.vue'
import ItemCategorySelect from '@/components/gestion/ItemCategorySelect.vue'
import ItemSubcategorySelect from '@/components/gestion/ItemSubcategorySelect.vue'
import { METADATA_FIELDS } from '@/RegleItem/CategorieItem'
import { itemTypeLabel, readStoredItemCategories } from '@/RegleItem/itemCategoryStore'
import { extractSubcategoriesByType } from '@/RegleItem/subcategoryStore'

const emit = defineEmits(['close', 'added'])

const authStore = useAuthStore()
const currentUserId = computed(() => authStore.user?.value?.id ?? authStore.user?.id ?? null)

const loading = ref(false)
const success = ref(false)
const error = ref(null)

const copies = ref(1)
const keepCommonFields = ref(true)
const discoveredSubcategories = ref({})
const categoryLabels = ref(readStoredItemCategories(currentUserId.value || 'guest'))

const getToday = () => new Date().toISOString().split('T')[0]

const emptyForm = (prefill = {}) => ({
  nomItem: '',
  prixRetail: null,
  prixResell: null,
  dateAchat: getToday(),
  dateVente: null,
  description: '',
  categorie: '',
  type: 'SNEAKER',
  metadata: {},
  ...prefill,
})

const form = ref(emptyForm())
const requiresDateVente = computed(
  () => form.value.prixResell !== null && form.value.prixResell !== '',
)

const setType = (type) => {
  if (form.value.type === type) return
  form.value.type = type
  form.value.categorie = ''
  form.value.metadata = {}
}

const setCategoryLabels = (labels) => {
  categoryLabels.value = labels
}

const resetState = () => {
  success.value = false
  error.value = null
  loading.value = false
}

const handleClose = () => {
  form.value = emptyForm()
  copies.value = 1
  keepCommonFields.value = true
  resetState()
  emit('close')
}

const buildPayload = () => ({
  nomItem: form.value.nomItem,
  prixRetail: form.value.prixRetail,
  prixResell: form.value.prixResell,
  dateAchat: form.value.dateAchat,
  dateVente: form.value.dateVente,
  description: form.value.description,
  categorie: form.value.categorie,
  type: form.value.type,
  metadata: cleanedMetadata.value,
  user: currentUserId.value ? { id: currentUserId.value } : null,
})

const validateResellAndDate = () => {
  const hasResell = requiresDateVente.value
  const hasDateVente = !!form.value.dateVente

  if (hasResell && !hasDateVente) {
    error.value = 'Ajoute une date de vente si tu saisis un prix de revente.'
    return false
  }

  if (hasDateVente && !hasResell) {
    form.value.prixResell = 0
  }
  return true
}

// si date saisie sans prix, forcer prixResell = 0
watch(
  () => form.value.dateVente,
  (val) => {
    if (val && (form.value.prixResell === null || form.value.prixResell === '')) {
      form.value.prixResell = 0
    }
  },
)

// si on remplit prixResell et qu'une date existe deja, nettoyer erreur eventuelle
watch(
  () => form.value.prixResell,
  () => {
    if (requiresDateVente.value && form.value.dateVente) error.value = null
  },
)

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

watch(
  () => currentUserId.value,
  (userId) => {
    categoryLabels.value = readStoredItemCategories(userId || 'guest')
  },
)

const loadExistingSubcategories = async () => {
  if (!currentUserId.value) {
    discoveredSubcategories.value = {}
    return
  }
  try {
    const { data } = await SnkVenteServices.getSnkVente()
    discoveredSubcategories.value = extractSubcategoriesByType(
      Array.isArray(data) ? data : [],
      categoryLabels.value,
    )
  } catch {
    discoveredSubcategories.value = {}
  }
}

onMounted(loadExistingSubcategories)

const createSales = async () => {
  loading.value = true
  success.value = false
  error.value = null

  if (!validateResellAndDate()) {
    loading.value = false
    return
  }

  try {
    const n = Math.min(50, Math.max(1, Math.trunc(Number(copies.value || 1))))
    const payload = buildPayload()
    await SnkVenteServices.createMany(payload, n)

    success.value = true
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('snk:stock-items-change', { detail: { source: 'gestion' } }))
    }
    emit('added')

    const prefill = keepCommonFields.value
      ? {
          nomItem: form.value.nomItem,
          type: form.value.type,
          categorie: form.value.categorie,
          description: form.value.description,
        }
      : {}

    // Reset form (garde eventuellement certains champs)
    setTimeout(() => {
      form.value = emptyForm(prefill)
      copies.value = 1
      success.value = false
      // Si tu preferes garder le modal ouvert, commente la ligne suivante :
      handleClose()
    }, 700)
  } catch (err) {
    error.value = err?.response?.data?.message || "Erreur lors de la creation de l'item"

    console.error('Erreur:', err)
  } finally {
    loading.value = false
  }
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
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.35), rgba(248, 250, 252, 0.62));
}
.modal-footer-sticky {
  position: sticky;
  bottom: 0;
  z-index: 2;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.86), rgba(255, 255, 255, 0.98));
  backdrop-filter: blur(6px);
}

.modal-card :is(.border-gray-700, .border-gray-600) {
  border-color: rgba(125, 211, 252, 0.32);
}

.modal-card :is(.text-gray-100, .text-gray-200, .text-gray-300) {
  color: #0f172a;
}

.modal-card :is(.text-gray-400, .text-gray-500) {
  color: #64748b;
}

.modal-card :is(input, textarea) {
  border-color: rgba(148, 163, 184, 0.28);
  background: #ffffff;
  color: #0f172a;
  min-height: 2.65rem;
  border-radius: 0.9rem;
}

.modal-card :is(input, textarea)::placeholder {
  color: #94a3b8;
}

.modal-card :is(input, textarea):focus {
  border-color: rgba(20, 184, 166, 0.72);
  box-shadow: 0 0 0 3px rgba(20, 184, 166, 0.14);
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
@media (max-width: 639px) {
  .modal-card {
    padding-bottom: max(env(safe-area-inset-bottom), 0.75rem);
  }
  .modal-footer-sticky {
    padding-bottom: calc(1rem + env(safe-area-inset-bottom, 0px));
  }
}
</style>
