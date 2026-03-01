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
import { ref, watch } from 'vue'
import SnkVenteServices from '@/services/SnkVenteServices.js'
import CompactDateInput from '@/components/ui/CompactDateInput.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false }, // v-model
  vente: { type: Object, default: null },
})

const emit = defineEmits(['update:modelValue', 'saved'])

const loading = ref(false)
const success = ref(false)
const error = ref(null)
const requireDateVente = ref(false)

const form = ref({
  id: null,
  nomItem: '',
  prixRetail: null,
  prixResell: null,
  dateAchat: '',
  dateVente: null,
  description: '',
  categorie: '',
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
    }
    success.value = false
    error.value = null
    requireDateVente.value =
      form.value.prixResell !== null &&
      form.value.prixResell !== '' &&
      form.value.prixResell !== undefined
  },
  { immediate: true },
)

const close = () => {
  emit('update:modelValue', false)
}

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
