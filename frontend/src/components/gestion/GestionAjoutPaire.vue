<template>
  <teleport to="body">
    <Transition name="modal-smooth">
      <div v-if="visible" class="fixed inset-0 z-[9999]">
        <div class="absolute inset-0 bg-slate-950/48 backdrop-blur-[2px]" @click.self="handleClose"></div>

        <div class="relative z-10 flex min-h-full items-end justify-center p-0 sm:items-center sm:p-4">
          <section
            class="modal-card w-full max-w-4xl max-h-[100dvh] rounded-t-2xl rounded-b-none border bg-white shadow-2xl sm:max-h-[92vh] sm:rounded-2xl"
            role="dialog"
            aria-modal="true"
            aria-labelledby="add-item-title"
            @click.stop
          >
            <header class="modal-card-header flex items-start justify-between border-b p-4 sm:p-5">
              <div>
                <h3 id="add-item-title">Ajouter un item</h3>
                <p>Categorie, nom, prix et dates suivent la meme logique que la modification.</p>
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
              Item ajoute.
            </div>

            <div class="modal-form">
              <ItemFormFields
                mode="create"
                surface="modal"
                :items="items"
                :saving="loading"
                details-default-open
                quantity-enabled
                :show-details-toggle="false"
                submit-label="Ajouter"
                @cancel="handleClose"
                @error="handleValidationError"
                @submit="createSales"
              />
            </div>
          </section>
        </div>
      </div>
    </Transition>
  </teleport>
</template>

<script setup>
import { onBeforeUnmount, ref } from 'vue'
import { X } from 'lucide-vue-next'
import SnkVenteServices from '@/services/SnkVenteServices.js'
import ItemFormFields from '@/components/gestion/ItemFormFields.vue'

defineProps({
  items: { type: Array, default: () => [] },
})

const emit = defineEmits(['close', 'added'])

const loading = ref(false)
const success = ref(false)
const error = ref(null)
const visible = ref(true)
const closeTimer = ref(null)

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
  }, 120)
}

function handleValidationError(message) {
  error.value = message
  success.value = false
}

async function createSales({ payload, quantity }) {
  loading.value = true
  success.value = false
  error.value = null

  try {
    const n = Math.min(50, Math.max(1, Math.trunc(Number(quantity || 1))))
    await SnkVenteServices.createMany(payload, n)
    success.value = true
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('snk:stock-items-change', { detail: { source: 'gestion' } }))
    }
    emit('added')
    window.setTimeout(() => {
      handleClose()
    }, 220)
  } catch (err) {
    error.value = err?.response?.data?.message || "Erreur lors de la creation de l'item"
    console.error('Erreur:', err)
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => {
  if (closeTimer.value) window.clearTimeout(closeTimer.value)
})
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

.modal-smooth-enter-active,
.modal-smooth-leave-active {
  transition: opacity 110ms ease;
}

.modal-smooth-enter-active .modal-card,
.modal-smooth-leave-active .modal-card {
  transition:
    transform 140ms cubic-bezier(0.2, 0.9, 0.2, 1),
    opacity 110ms ease;
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
}
</style>
