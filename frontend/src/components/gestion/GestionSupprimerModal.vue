<template>
  <div class="delete-sheet-backdrop" @click.self="close">
    <section class="delete-sheet" role="dialog" aria-modal="true" aria-labelledby="delete-title">
      <header class="delete-sheet-header">
        <div>
          <p>Suppression</p>
          <h3 id="delete-title">{{ modeLabel }}</h3>
          <span>Annulable quelques secondes via le toast.</span>
        </div>
      </header>

      <div class="delete-mode-switch" role="tablist" aria-label="Mode de suppression">
        <button
          type="button"
          :class="{ 'is-active': mode === 'name' }"
          role="tab"
          :aria-selected="mode === 'name'"
          @click="setMode('name')"
        >
          Par nom
        </button>

        <button
          type="button"
          :class="{ 'is-active': mode === 'bulk' }"
          role="tab"
          :aria-selected="mode === 'bulk'"
          @click="setMode('bulk')"
        >
          Selection
          <span>{{ selectedIds.length }}</span>
        </button>
      </div>

      <div v-if="error" class="delete-feedback">
        {{ error }}
      </div>

      <div v-if="mode === 'name'" class="delete-name-mode">
        <label class="delete-search-field">
          <Search class="h-4 w-4" aria-hidden="true" />
          <input
            v-model.trim="query"
            type="text"
            placeholder="Jordan, Dunk, SKU, categorie..."
            autocomplete="off"
            @focus="openList = true"
          />
        </label>

        <div v-if="openList && suggestions.length" class="delete-suggestions">
          <button
            v-for="vente in suggestions"
            :key="vente.id"
            type="button"
            :class="{ 'is-selected': selected?.id === vente.id }"
            @click="selectVente(vente)"
          >
            <span>{{ vente.nomItem || vente.nom_item }}</span>
            <small>ID {{ vente.id }} - {{ vente.categorie || 'Sans categorie' }}</small>
          </button>
        </div>

        <p v-else-if="query" class="delete-empty">Aucun item proche trouve.</p>
      </div>

      <div v-else class="delete-bulk-mode">
        <div>
          <Trash2 class="h-4 w-4" aria-hidden="true" />
          <p>
            <strong>{{ selectedIds.length }}</strong>
            item(s) selectionne(s)
          </p>
        </div>
        <span>La liste se mettra a jour tout de suite, puis tu pourras annuler.</span>
      </div>

      <footer class="delete-sheet-actions">
        <button type="button" class="delete-secondary-button" @click="close">Annuler</button>
        <button
          type="button"
          class="delete-danger-button"
          :disabled="mode === 'name' ? !selected : !selectedIds.length"
          @click="queueDelete"
        >
          <Trash2 class="h-4 w-4" aria-hidden="true" />
          <span>{{ mode === 'name' ? 'Supprimer cet item' : 'Supprimer la selection' }}</span>
        </button>
      </footer>
    </section>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { Search, Trash2 } from 'lucide-vue-next'
import { normalizeSearchText, searchTokens } from '@/utils/homeDashboard'

const props = defineProps({
  snkVentes: { type: Array, default: () => [] },
  selectedIds: { type: Array, default: () => [] },
  defaultMode: { type: String, default: 'name' },
})

const emit = defineEmits(['close', 'delete-requested'])

const mode = ref(props.defaultMode)
const error = ref('')
const query = ref('')
const selected = ref(null)
const openList = ref(false)

const searchTextForVente = (vente) =>
  normalizeSearchText([
    vente?.id,
    vente?.nomItem ?? vente?.nom_item,
    vente?.categorie,
    vente?.description,
    vente?.sku,
    vente?.reference,
  ].join(' '))

const suggestions = computed(() => {
  const list = props.snkVentes || []
  const q = normalizeSearchText(query.value)
  if (!q) return list.slice(0, 8)

  const tokens = searchTokens(q)
  return list
    .map((vente) => ({ vente, text: searchTextForVente(vente) }))
    .filter((record) => tokens.every((token) => record.text.includes(token)))
    .slice(0, 8)
    .map((record) => record.vente)
})

const modeLabel = computed(() =>
  mode.value === 'name' ? 'Choisir un item' : 'Supprimer la selection',
)

const setMode = (nextMode) => {
  mode.value = nextMode
  error.value = ''
}

const selectVente = (vente) => {
  selected.value = vente
  query.value = vente.nomItem || vente.nom_item
  openList.value = false
  error.value = ''
}

const close = () => {
  error.value = ''
  query.value = ''
  selected.value = null
  openList.value = false
  emit('close')
}

const queueDelete = () => {
  const ids = mode.value === 'name' ? [selected.value?.id].filter(Boolean) : props.selectedIds
  if (!ids.length) {
    error.value = 'Aucun item a supprimer.'
    return
  }

  emit('delete-requested', ids)
  close()
}

watch(query, () => {
  selected.value = null
  error.value = ''
  openList.value = true
})

watch(
  () => props.defaultMode,
  (nextMode) => {
    mode.value = nextMode
  },
)
</script>

<style scoped>
.delete-sheet-backdrop {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: rgba(15, 23, 42, 0.38);
  padding: 0;
  backdrop-filter: blur(8px);
}

.delete-sheet {
  display: grid;
  width: min(100%, 30rem);
  max-height: 92dvh;
  gap: 1rem;
  overflow-y: auto;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 20px 20px 0 0;
  background: #fbfaf7;
  padding: 1rem;
  box-shadow: 0 24px 64px rgba(15, 23, 42, 0.22);
}

.delete-sheet-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.delete-sheet-header p {
  color: #b91c1c;
  font-size: 0.72rem;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.delete-sheet-header h3 {
  margin-top: 0.1rem;
  color: #0f172a;
  font-size: 1.25rem;
  font-weight: 900;
}

.delete-sheet-header span,
.delete-bulk-mode > span,
.delete-empty {
  color: #64748b;
  font-size: 0.82rem;
  font-weight: 650;
}

.delete-icon-button,
.delete-mode-switch button,
.delete-suggestions button,
.delete-secondary-button,
.delete-danger-button {
  cursor: pointer;
  transition:
    border-color 160ms ease,
    background-color 160ms ease,
    color 160ms ease,
    box-shadow 160ms ease,
    transform 140ms ease;
}

.delete-icon-button {
  display: inline-grid;
  width: 2.35rem;
  height: 2.35rem;
  place-items: center;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.86);
  color: #475569;
}

.delete-mode-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.35rem;
  border-radius: 16px;
  background: #f1f5f9;
  padding: 0.25rem;
}

.delete-mode-switch button {
  display: inline-flex;
  min-height: 2.4rem;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  border: 1px solid transparent;
  border-radius: 13px;
  color: #475569;
  font-size: 0.82rem;
  font-weight: 900;
}

.delete-mode-switch button span {
  display: inline-grid;
  min-width: 1.35rem;
  height: 1.35rem;
  place-items: center;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.18);
  color: inherit;
  font-size: 0.72rem;
}

.delete-mode-switch button.is-active {
  border-color: rgba(15, 118, 110, 0.18);
  background: #ffffff;
  color: #0f766e;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.07);
}

.delete-search-field {
  display: flex;
  min-height: 3rem;
  align-items: center;
  gap: 0.65rem;
  border: 1px solid rgba(14, 165, 233, 0.28);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.94);
  color: #0f766e;
  padding: 0 0.85rem;
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease;
}

.delete-search-field:focus-within,
.delete-search-field:hover {
  border-color: rgba(20, 184, 166, 0.46);
  box-shadow: 0 0 0 3px rgba(45, 212, 191, 0.12);
}

.delete-search-field input {
  min-width: 0;
  flex: 1;
  border: 0;
  background: transparent;
  color: #0f172a;
  font-size: 0.92rem;
  font-weight: 750;
  outline: none;
}

.delete-name-mode {
  display: grid;
  gap: 0.65rem;
}

.delete-suggestions {
  display: grid;
  max-height: 15rem;
  gap: 0.35rem;
  overflow-y: auto;
  padding-right: 0.1rem;
}

.delete-suggestions button {
  display: grid;
  gap: 0.12rem;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.82);
  padding: 0.75rem;
  text-align: left;
}

.delete-suggestions button span {
  color: #0f172a;
  font-size: 0.9rem;
  font-weight: 850;
}

.delete-suggestions button small {
  color: #64748b;
  font-size: 0.74rem;
  font-weight: 700;
}

.delete-suggestions button:hover,
.delete-suggestions button.is-selected {
  border-color: rgba(20, 184, 166, 0.44);
  background: #ecfdf5;
  transform: translateY(-1px);
}

.delete-bulk-mode {
  display: grid;
  gap: 0.55rem;
  border: 1px solid rgba(248, 113, 113, 0.2);
  border-radius: 16px;
  background: #fff7f7;
  padding: 0.9rem;
}

.delete-bulk-mode > div {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  color: #b91c1c;
  font-weight: 850;
}

.delete-feedback {
  border: 1px solid rgba(248, 113, 113, 0.3);
  border-radius: 14px;
  background: #fef2f2;
  color: #b91c1c;
  padding: 0.65rem 0.75rem;
  font-size: 0.82rem;
  font-weight: 800;
}

.delete-sheet-actions {
  display: flex;
  flex-direction: column-reverse;
  gap: 0.55rem;
}

.delete-secondary-button,
.delete-danger-button {
  display: inline-flex;
  min-height: 2.65rem;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  border-radius: 999px;
  padding: 0 1rem;
  font-size: 0.86rem;
  font-weight: 900;
}

.delete-secondary-button {
  border: 1px solid rgba(148, 163, 184, 0.3);
  background: rgba(255, 255, 255, 0.8);
  color: #334155;
}

.delete-danger-button {
  border: 1px solid rgba(239, 68, 68, 0.22);
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  color: #ffffff;
  box-shadow: 0 14px 28px rgba(185, 28, 28, 0.18);
}

.delete-danger-button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  transform: none;
}

.delete-icon-button:hover,
.delete-secondary-button:hover {
  border-color: rgba(20, 184, 166, 0.42);
  background: #ecfdf5;
  color: #0f766e;
  transform: translateY(-1px);
}

.delete-danger-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #ef4444, #b91c1c);
  box-shadow: 0 18px 34px rgba(185, 28, 28, 0.22);
  transform: translateY(-1px);
}

.delete-icon-button:active,
.delete-mode-switch button:active,
.delete-suggestions button:active,
.delete-secondary-button:active,
.delete-danger-button:active:not(:disabled) {
  transform: translateY(0) scale(0.97);
}

@media (min-width: 640px) {
  .delete-sheet-backdrop {
    align-items: center;
    padding: 1rem;
  }

  .delete-sheet {
    border-radius: 20px;
    padding: 1.1rem;
  }

  .delete-sheet-actions {
    flex-direction: row;
    justify-content: flex-end;
  }
}
</style>
