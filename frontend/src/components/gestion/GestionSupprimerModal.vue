<template>
  <div
    class="fixed inset-0 z-40 flex items-center justify-center bg-black/60 p-4"
    @click.self="close"
  >
    <div class="w-full max-w-md rounded-lg bg-white p-5 shadow-lg">
      <div class="flex items-start justify-between gap-3 mb-3">
        <div>
          <h3 class="text-xl font-semibold text-gray-900">Supprimer</h3>
          <p class="text-xs text-gray-500">
            {{ modeLabel }}
          </p>
        </div>
        <div class="[&_button:hover]:bg-gray-300">
          <button
            class="rounded px-2 py-1 text-sm text-gray-600"
            @click="close"
            :disabled="loading"
          >
            X
          </button>
        </div>
      </div>

      <!-- Switch mode -->
      <div class="mb-4 flex gap-2">
        <button
          type="button"
          class="px-3 py-1.5 text-xs rounded border"
          :class="
            mode === 'name'
              ? 'bg-gray-900 text-white border-gray-900'
              : 'bg-white text-gray-700 border-gray-300 '
          "
          @click="setMode('name')"
          :disabled="loading"
        >
          Par nom
        </button>

        <button
          type="button"
          class="px-3 py-1.5 text-xs rounded border"
          :class="
            mode === 'bulk'
              ? 'bg-gray-900 text-white border-gray-900'
              : 'bg-white text-gray-700 border-gray-300 '
          "
          @click="setMode('bulk')"
          :disabled="loading"
        >
          Selection ({{ selectedIds.length }})
        </button>
      </div>

      <!-- Success / Error -->
      <div
        v-if="success"
        class="mb-3 rounded border border-green-200 bg-green-50 px-3 py-2 text-sm text-green-800"
      >
        OK. Suppression effectuee.
      </div>

      <div
        v-if="error"
        class="mb-3 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800"
      >
        {{ error }}
      </div>

      <!-- MODE: PAR NOM -->
      <div v-if="mode === 'name'" class="space-y-3">
        <div>
          <label class="block text-sm font-medium text-gray-900 mb-1">Nom de l'item</label>
          <input
            v-model.trim="query"
            type="text"
            class="w-full rounded border border-gray-300 px-2 py-1.5 text-sm"
            placeholder="Tape un nom (Jordan, dunk...)"
            autocomplete="off"
            @focus="openList = true"
          />

          <div
            v-if="openList && suggestions.length"
            class="mt-1 max-h-56 overflow-auto rounded border border-gray-200 bg-white shadow"
          >
            <button
              v-for="v in suggestions"
              :key="v.id"
              type="button"
              class="w-full text-left px-3 py-2 text-sm"
              @click="selectVente(v)"
            >
              <div class="font-medium text-gray-900">{{ v.nomItem || v.nom_item }}</div>
              <div class="text-xs text-gray-500">ID: {{ v.id }} - {{ v.categorie || '-' }}</div>
            </button>
          </div>

          <p v-if="selected" class="mt-2 text-xs text-gray-600">
            Selection : <b>{{ selected.nomItem || selected.nom_item }}</b> (ID {{ selected.id }})
          </p>
        </div>

        <div class="flex justify-end gap-2 pt-1">
          <div class="[&_button:hover]:bg-gray-300">
            <button
              type="button"
              class="px-4 py-1.5 text-sm rounded border border-gray-300 text-gray-700"
              @click="close"
              :disabled="loading"
            >
              Annuler
            </button>
          </div>
          <div class="[&_button:hover]:bg-red-800">
            <button
              type="button"
              class="px-4 py-1.5 text-sm rounded bg-red-600 text-white disabled:opacity-60"
              :disabled="loading || !selected"
              @click="deleteOne"
            >
              {{ loading ? 'Suppression...' : 'Supprimer' }}
            </button>
          </div>
        </div>
      </div>

      <!-- MODE: BULK -->
      <div v-else class="space-y-3">
        <p class="text-sm text-gray-700">
          Tu vas supprimer <b>{{ selectedIds.length }}</b> item(s). Action definitive.
        </p>

        <div class="flex justify-end gap-2 pt-1">
          <div class="[&_button:hover]:bg-gray-300">
            <button
              type="button"
              class="px-4 py-1.5 text-sm rounded border border-gray-300 text-gray-700"
              @click="close"
              :disabled="loading"
            >
              Annuler
            </button>
          </div>
          <div class="[&_button:hover]:bg-red-800">
            <button
              type="button"
              class="px-4 py-1.5 text-sm rounded bg-red-600 text-white"
              :disabled="loading || !selectedIds.length"
              @click="deleteBulk"
            >
              {{ loading ? 'Suppression...' : 'Supprimer la selection' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import SnkVenteServices from '@/services/SnkVenteServices.js'

const props = defineProps({
  snkVentes: { type: Array, default: () => [] },
  selectedIds: { type: Array, default: () => [] },
  defaultMode: { type: String, default: 'name' }, // 'name' | 'bulk'
})

const emit = defineEmits(['close', 'deleted'])

const mode = ref(props.defaultMode)
const loading = ref(false)
const success = ref(false)
const error = ref('')

// name mode
const query = ref('')
const selected = ref(null)
const openList = ref(false)

const normalize = (s) => (s || '').toString().toLowerCase()
const suggestions = computed(() => {
  const q = normalize(query.value)
  const list = props.snkVentes || []
  if (!q) return list.slice(0, 8)
  return list.filter((v) => normalize(v.nomItem || v.nom_item).includes(q)).slice(0, 8)
})

watch(query, () => {
  selected.value = null
  success.value = false
  error.value = ''
  openList.value = true
})

const modeLabel = computed(() =>
  mode.value === 'name' ? 'Suppression par nom (autocomplete)' : 'Suppression multiple',
)

const setMode = (m) => {
  mode.value = m
  success.value = false
  error.value = ''
}

const selectVente = (v) => {
  selected.value = v
  query.value = v.nomItem || v.nom_item
  openList.value = false
}

const close = () => {
  loading.value = false
  success.value = false
  error.value = ''
  query.value = ''
  selected.value = null
  openList.value = false
  emit('close')
}

const deleteOne = async () => {
  if (!selected.value) return
  loading.value = true
  success.value = false
  error.value = ''

  try {
    await SnkVenteServices.supprimer(selected.value.id)
    emit('deleted', [selected.value.id]) // tableau d'IDs pour uniformiser
    success.value = true
    setTimeout(() => close(), 400)
  } catch (e) {
    console.error(e)
    error.value = 'Erreur lors de la suppression'
  } finally {
    loading.value = false
  }
}

const deleteBulk = async () => {
  if (!props.selectedIds.length) return
  loading.value = true
  success.value = false
  error.value = ''

  try {
    // plus robuste que Promise.all (gere les erreurs item par item)
    const results = await Promise.allSettled(
      props.selectedIds.map((id) => SnkVenteServices.supprimer(id)),
    )
    const ok = results
      .map((r, idx) => (r.status === 'fulfilled' ? props.selectedIds[idx] : null))
      .filter(Boolean)

    const failedCount = results.filter((r) => r.status === 'rejected').length

    if (!ok.length) {
      error.value = "Aucune suppression n'a reussi."
      return
    }

    emit('deleted', ok)
    success.value = true

    if (failedCount) {
      error.value = `${failedCount} suppression(s) ont echoue, mais le reste a ete supprime.`
    } else {
      setTimeout(() => close(), 400)
    }
  } catch (e) {
    console.error(e)
    error.value = 'Erreur lors de la suppression de la selection'
  } finally {
    loading.value = false
  }
}
</script>
