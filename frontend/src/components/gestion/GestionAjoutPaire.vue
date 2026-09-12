<template>
  <teleport to="body">
    <Transition name="modal-smooth">
      <div v-if="visible" class="fixed inset-0 z-[9999]">
        <div class="absolute inset-0 bg-slate-950/48 backdrop-blur-[2px]" @click.self="handleClose"></div>

        <div
          class="relative z-10 flex min-h-full items-center justify-center p-4"
          @click.self="handleClose"
        >
          <section
            class="modal-card w-full max-w-4xl rounded-2xl border bg-white shadow-2xl"
            role="dialog"
            aria-modal="true"
            aria-labelledby="add-item-title"
            @click.stop
          >
            <header class="modal-card-header flex items-start justify-between border-b p-4 sm:p-5">
              <div>
                <h3 id="add-item-title">Ajouter un item</h3>
                <p>Catégorie, nom, prix et dates suivent la même logique que la modification.</p>
              </div>

              <button
                type="button"
                class="modal-close-button"
                aria-label="Fermer"
                @click="handleClose"
              >
                <X class="h-5 w-5" aria-hidden="true" />
              </button>
            </header>

            <div v-if="error" class="modal-alert modal-alert--error">
              {{ error }}
            </div>

            <div v-if="success" class="modal-alert modal-alert--success">
              Item ajouté.
            </div>

            <div class="modal-form">
              <form class="native-add-form" @submit.prevent="submitNativeForm">
                <div class="native-form-grid">
                  <label class="native-field native-field--category">
                    <span>Categorie</span>
                    <select v-model="form.type" required>
                      <option v-for="option in itemTypeOptions" :key="option.value" :value="option.value">
                        {{ option.label }}
                      </option>
                    </select>
                  </label>

                  <label class="native-field native-field--name">
                    <span>Nom de l'item</span>
                    <input
                      v-model.trim="form.nomItem"
                      type="text"
                      autocomplete="off"
                      placeholder="Nom, modele, reference..."
                      required
                    />
                  </label>

                  <label class="native-field native-field--subcategory">
                    <span>Sous-categorie</span>
                    <input
                      v-model.trim="form.categorie"
                      type="text"
                      autocomplete="off"
                      placeholder="Marque, famille..."
                    />
                  </label>

                  <label class="native-field">
                    <span>Prix achat</span>
                    <input
                      v-model="form.prixRetail"
                      type="text"
                      inputmode="decimal"
                      placeholder="110"
                      required
                    />
                  </label>

                  <label class="native-field">
                    <span>Date d'achat</span>
                    <input v-model="form.dateAchat" type="date" required />
                  </label>

                  <label class="native-field native-field--quantity">
                    <span>Quantite</span>
                    <input v-model.number="form.quantity" type="number" min="1" max="50" step="1" required />
                  </label>

                  <label class="native-field">
                    <span>Prix de vente</span>
                    <input v-model="form.prixResell" type="text" inputmode="decimal" placeholder="180" />
                  </label>

                  <label class="native-field">
                    <span>Date de vente</span>
                    <input v-model="form.dateVente" type="date" />
                  </label>

                  <label class="native-field native-field--grouped">
                    <span>Regroupement</span>
                    <span class="native-switch-row">
                      <input v-model="form.grouped" type="checkbox" :disabled="Number(form.quantity) <= 1" />
                      <span>Creer une seule ligne pour cette quantite</span>
                    </span>
                  </label>

                  <label class="native-field native-field--notes">
                    <span>Description</span>
                    <textarea
                      v-model.trim="form.description"
                      rows="4"
                      placeholder="Etat, lieu d'achat, accessoires inclus..."
                    ></textarea>
                  </label>
                </div>

                <footer class="native-form-actions">
                  <button type="button" class="native-secondary-button" :disabled="loading" @click="handleClose">
                    Annuler
                  </button>
                  <button type="submit" class="native-primary-button" :disabled="loading">
                    <Plus class="h-4 w-4" aria-hidden="true" />
                    <span>{{ loading ? 'Ajout...' : 'Ajouter' }}</span>
                  </button>
                </footer>
              </form>
            </div>
          </section>
        </div>
      </div>
    </Transition>
  </teleport>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { Plus, X } from 'lucide-vue-next'
import SnkVenteServices from '@/services/SnkVenteServices.js'
import { useAuthStore } from '@/store/authStore'
import {
  normalizeItemType,
  readStoredItemCategories,
  resolveItemTypeOptions,
} from '@/RegleItem/itemCategoryStore'
import { toYmdLocal } from '@/utils/homeDashboard'

const props = defineProps({
  items: { type: Array, default: () => [] },
})

const emit = defineEmits(['close', 'added'])

const loading = ref(false)
const success = ref(false)
const error = ref(null)
const visible = ref(true)
const closeTimer = ref(null)
const auth = useAuthStore()
const currentUserId = computed(() => auth.user?.value?.id ?? auth.user?.id ?? 'guest')
const categoryLabels = computed(() => readStoredItemCategories(currentUserId.value))
const itemTypeOptions = computed(() => {
  const options = resolveItemTypeOptions(categoryLabels.value)
  return options.length ? options : [{ value: 'OTHER', label: 'Autre' }]
})

const form = ref(emptyForm())

function emptyForm() {
  return {
    type: itemTypeOptions.value?.[0]?.value || 'OTHER',
    nomItem: '',
    categorie: '',
    prixRetail: '',
    prixResell: '',
    dateAchat: toYmdLocal(new Date()),
    dateVente: '',
    quantity: 1,
    grouped: false,
    description: '',
  }
}

function resetState() {
  success.value = false
  error.value = null
  loading.value = false
}

function handleClose() {
  resetState()
  visible.value = false
  if (closeTimer.value) window.clearTimeout(closeTimer.value)
  closeTimer.value = window.setTimeout(() => {
    emit('close')
  }, 100)
}

async function createSales({ payload, quantity }) {
  loading.value = true
  success.value = false
  error.value = null

  try {
    const n = Math.min(50, Math.max(1, Math.trunc(Number(quantity || 1))))
    await SnkVenteServices.createMany(payload, n)
    success.value = true
    form.value = emptyForm()
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('snk:stock-items-change', { detail: { source: 'gestion' } }))
    }
    emit('added')
    window.setTimeout(() => {
      handleClose()
    }, 140)
  } catch (err) {
    error.value = err?.response?.data?.message || "Erreur lors de la creation de l'item"
    console.error('Erreur:', err)
  } finally {
    loading.value = false
  }
}

function parseAmount(value) {
  const normalized = String(value ?? '')
    .replace(/\s/g, '')
    .replace(/[€]/g, '')
    .replace(',', '.')
    .trim()
  if (!normalized) return null
  const parsed = Number(normalized)
  return Number.isFinite(parsed) ? parsed : null
}

function clampQuantity(value) {
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) return 1
  return Math.min(50, Math.max(1, Math.trunc(numberValue)))
}

function validateNativeForm() {
  const today = toYmdLocal(new Date())
  const retail = parseAmount(form.value.prixRetail)
  const resell = parseAmount(form.value.prixResell)
  if (!form.value.nomItem.trim()) return "Le nom de l'item est obligatoire."
  if (!form.value.type) return 'Choisis une categorie.'
  if (retail === null) return "Le prix d'achat est obligatoire."
  if (retail < 0) return "Le prix d'achat doit etre positif."
  if (resell !== null && resell < 0) return 'Le prix de vente doit etre positif.'
  if (resell !== null && !form.value.dateVente) return 'Ajoute une date de vente avec le prix de vente.'
  if (form.value.dateAchat && form.value.dateAchat > today) return "La date d'achat ne peut pas etre apres aujourd'hui."
  if (form.value.dateVente && form.value.dateVente > today) return "La date de vente ne peut pas etre apres aujourd'hui."
  if (form.value.dateAchat && form.value.dateVente && form.value.dateVente < form.value.dateAchat) {
    return "La date de vente doit etre apres la date d'achat."
  }
  return ''
}

function buildNativePayload() {
  const quantity = clampQuantity(form.value.quantity)
  const resell = parseAmount(form.value.prixResell)
  return {
    nomItem: form.value.nomItem.trim(),
    prixRetail: parseAmount(form.value.prixRetail),
    prixResell: resell,
    dateAchat: form.value.dateAchat || null,
    dateVente: form.value.dateVente || null,
    description: form.value.description.trim(),
    categorie: form.value.categorie.trim() || null,
    type: normalizeItemType(form.value.type),
    metadata: {},
    grouped: quantity > 1 ? Boolean(form.value.grouped) : false,
  }
}

function submitNativeForm() {
  const validationError = validateNativeForm()
  if (validationError) {
    error.value = validationError
    success.value = false
    return
  }
  createSales({ payload: buildNativePayload(), quantity: clampQuantity(form.value.quantity) })
}

watch(
  () => itemTypeOptions.value.map((option) => option.value).join('|'),
  () => {
    if (!itemTypeOptions.value.some((option) => option.value === form.value.type)) {
      form.value.type = itemTypeOptions.value[0]?.value || 'OTHER'
    }
  },
)

watch(
  () => form.value.quantity,
  (quantity) => {
    if (Number(quantity) <= 1) {
      form.value.grouped = false
    }
  },
)

onBeforeUnmount(() => {
  if (closeTimer.value) window.clearTimeout(closeTimer.value)
})
</script>

<style scoped>
.modal-card {
  position: relative;
  isolation: isolate;
  display: flex;
  flex-direction: column;
  max-height: min(760px, calc(100dvh - 2rem));
  min-height: min(620px, calc(100dvh - 2rem));
  border-color: rgba(125, 211, 252, 0.38);
  background:
    linear-gradient(135deg, rgba(14, 165, 233, 0.08), transparent 42%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.96)),
    #ffffff;
  color: #0f172a;
  overflow: hidden;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.22);
}

.modal-card::before {
  content: '';
  flex: 0 0 auto;
  z-index: 90;
  display: block;
  height: 4px;
  background: linear-gradient(90deg, #0ea5e9, #14b8a6, #f59e0b);
}

.modal-card-header {
  flex: 0 0 auto;
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
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 1rem;
}

.native-add-form {
  display: grid;
  min-width: 0;
  gap: 1rem;
}

.native-form-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 0.85rem;
  align-items: end;
}

.native-field {
  display: grid;
  grid-column: span 3;
  min-width: 0;
  gap: 0.35rem;
}

.native-field--category {
  grid-column: span 4;
}

.native-field--name {
  grid-column: span 5;
}

.native-field--subcategory {
  grid-column: span 3;
}

.native-field--notes {
  grid-column: 1 / -1;
}

.native-field--grouped {
  grid-column: span 6;
}

.native-field > span:first-child {
  color: #334155;
  font-size: 0.78rem;
  font-weight: 850;
}

.native-field input,
.native-field select,
.native-field textarea {
  width: 100%;
  min-height: 44px;
  border: 1px solid rgba(100, 116, 139, 0.26);
  border-radius: 14px;
  background: rgba(248, 250, 252, 0.95);
  color: #0f172a;
  padding: 0.58rem 0.78rem;
  font-size: 0.92rem;
  font-weight: 700;
  outline: none;
  transition:
    border-color 120ms ease,
    background 120ms ease,
    box-shadow 120ms ease;
}

.native-field textarea {
  min-height: 104px;
  resize: vertical;
}

.native-field input::placeholder,
.native-field textarea::placeholder {
  color: #94a3b8;
}

.native-field input:focus,
.native-field select:focus,
.native-field textarea:focus {
  border-color: rgba(20, 184, 166, 0.62);
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(20, 184, 166, 0.12);
}

.native-switch-row {
  display: flex;
  min-height: 44px;
  align-items: center;
  gap: 0.65rem;
  border: 1px solid rgba(100, 116, 139, 0.24);
  border-radius: 14px;
  background: rgba(248, 250, 252, 0.95);
  padding: 0.58rem 0.78rem;
  color: #334155;
  font-size: 0.86rem;
  font-weight: 750;
}

.native-switch-row input {
  width: 1rem;
  min-height: 1rem;
  accent-color: #0f766e;
}

.native-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.65rem;
  border-top: 1px solid rgba(226, 232, 240, 0.96);
  padding-top: 1rem;
}

.native-secondary-button,
.native-primary-button {
  display: inline-flex;
  min-height: 2.75rem;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  border-radius: 14px;
  padding: 0 1rem;
  font-size: 0.88rem;
  font-weight: 900;
}

.native-secondary-button {
  border: 1px solid rgba(148, 163, 184, 0.34);
  background: #f8fafc;
  color: #334155;
}

.native-primary-button {
  border: 1px solid rgba(15, 118, 110, 0.2);
  background: linear-gradient(135deg, #0f766e, #0e7490);
  color: #ffffff;
  box-shadow: 0 12px 24px rgba(15, 118, 110, 0.16);
}

.native-secondary-button:hover:not(:disabled) {
  border-color: rgba(15, 118, 110, 0.28);
  background: #ecfdf5;
  color: #0f766e;
}

.native-primary-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #0d9488, #0284c7);
}

.native-secondary-button:disabled,
.native-primary-button:disabled {
  cursor: not-allowed;
  opacity: 0.62;
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
    width: 100%;
    max-height: 100dvh;
    min-height: min(680px, 100dvh);
    border-radius: 18px 18px 0 0;
    padding-bottom: max(env(safe-area-inset-bottom), 0.75rem);
  }

  .modal-form {
    padding: 0.85rem;
  }

  .native-form-grid {
    grid-template-columns: 1fr;
  }

  .native-field,
  .native-field--category,
  .native-field--name,
  .native-field--subcategory,
  .native-field--grouped,
  .native-field--notes {
    grid-column: auto;
  }

  .native-form-actions {
    display: grid;
  }

  .native-secondary-button,
  .native-primary-button {
    width: 100%;
  }
}
</style>
