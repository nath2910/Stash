<template>
  <teleport to="body">
    <div v-if="open" class="modal-root" @keydown.esc.prevent="$emit('close')">
      <div class="modal-backdrop" @click="$emit('close')"></div>
      <div class="modal-position">
        <section class="modal-card" role="dialog" aria-modal="true" aria-labelledby="quick-item-title">
          <header class="modal-header">
            <div>
              <p class="modal-eyebrow">{{ statusLabel }}</p>
              <h2 id="quick-item-title">{{ itemName }}</h2>
            </div>
            <button type="button" class="icon-button" aria-label="Fermer" @click="$emit('close')">
              <X class="h-5 w-5" aria-hidden="true" />
            </button>
          </header>

          <div v-if="localError || error" class="modal-alert modal-alert--error">
            {{ localError || error }}
          </div>
          <div v-if="successText" class="modal-alert modal-alert--success">
            {{ successText }}
          </div>

          <div class="modal-body">
            <ItemFormFields
              mode="edit"
              surface="modal"
              :item="item"
              :items="items"
              :saving="saving"
              details-default-open
              submit-label="Enregistrer"
              @cancel="$emit('close')"
              @error="handleValidationError"
              @submit="submit"
            />
          </div>
        </section>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { X } from 'lucide-vue-next'
import ItemFormFields from '@/components/gestion/ItemFormFields.vue'
import { getField } from '@/utils/snkVente'
import { normalizeItemStatus } from '@/constants/statuses'

const props = defineProps({
  open: { type: Boolean, default: false },
  item: { type: Object, default: null },
  items: { type: Array, default: () => [] },
  saving: { type: Boolean, default: false },
  error: { type: String, default: '' },
  successKey: { type: Number, default: 0 },
})

const emit = defineEmits(['close', 'save'])

const localError = ref('')
const successText = ref('')
let successTimer = null

const statusLabel = computed(() =>
  props.item ? normalizeItemStatus(props.item).label : 'En stock',
)
const itemName = computed(() => getField(props.item || {}, 'nomItem', '') || 'Modifier un item')

watch(
  () => props.item,
  () => {
    localError.value = ''
    successText.value = ''
  },
)

watch(
  () => props.successKey,
  () => {
    if (!props.successKey) return
    if (successTimer) window.clearTimeout(successTimer)
    successText.value = 'Modifications enregistrees.'
    successTimer = window.setTimeout(() => {
      successText.value = ''
      successTimer = null
    }, 1500)
  },
)

onBeforeUnmount(() => {
  if (successTimer) window.clearTimeout(successTimer)
})

function handleValidationError(message) {
  localError.value = message
  successText.value = ''
}

function submit(event) {
  localError.value = ''
  successText.value = ''
  emit('save', event)
}
</script>

<style scoped>
.modal-root {
  position: fixed;
  inset: 0;
  z-index: 9999;
}

.modal-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.52);
  backdrop-filter: blur(4px);
}

.modal-position {
  position: relative;
  z-index: 1;
  display: flex;
  min-height: 100%;
  align-items: center;
  justify-content: center;
  padding: clamp(0.75rem, 2vw, 1.25rem);
}

.modal-card {
  width: min(100%, 940px);
  max-height: calc(100dvh - 2rem);
  overflow: auto;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.24);
}

.modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  border-bottom: 1px solid rgba(226, 232, 240, 0.96);
  padding: 0.95rem 1rem;
}

.modal-eyebrow {
  color: #0f766e;
  font-size: 0.7rem;
  font-weight: 800;
  text-transform: uppercase;
}

h2 {
  margin-top: 0.1rem;
  color: #0f172a;
  font-size: clamp(1.05rem, 2.2vw, 1.4rem);
  font-weight: 800;
}

.icon-button {
  display: inline-flex;
  width: 2.4rem;
  height: 2.4rem;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 12px;
  background: #f8fafc;
  color: #334155;
}

.icon-button:hover {
  border-color: rgba(15, 118, 110, 0.28);
  background: #ecfdf5;
  color: #0f766e;
}

.modal-body {
  padding: 1rem;
}

.modal-alert {
  margin: 0.85rem 1rem 0;
  border-radius: 12px;
  padding: 0.7rem 0.85rem;
  font-size: 0.84rem;
  font-weight: 700;
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

@media (max-width: 760px) {
  .modal-position {
    align-items: flex-end;
    padding: 0;
  }

  .modal-card {
    max-height: 100dvh;
    border-radius: 20px 20px 0 0;
  }
}
</style>
