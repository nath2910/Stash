<template>
  <section class="quick-add-panel">
    <div class="panel-heading">
      <div>
        <p class="panel-eyebrow">Ajout rapide</p>
        <h2>Ajouter un item</h2>
      </div>
    </div>

    <ItemFormFields
      ref="formRef"
      mode="create"
      surface="quick"
      :items="items"
      :saving="saving"
      :reset-key="successKey"
      quantity-enabled
      :show-cancel="false"
      auto-infer-from-name
      submit-label="Ajouter"
      @submit="$emit('submit', $event)"
      @error="$emit('error', $event)"
    />
  </section>
</template>

<script setup>
import { ref } from 'vue'
import ItemFormFields from '@/components/gestion/ItemFormFields.vue'

defineProps({
  saving: { type: Boolean, default: false },
  successKey: { type: Number, default: 0 },
  items: { type: Array, default: () => [] },
})

defineEmits(['submit', 'error'])

const formRef = ref(null)

defineExpose({
  focusFirstField() {
    formRef.value?.focusFirstField?.()
  },
})
</script>

<style scoped>
.quick-add-panel {
  position: relative;
  display: grid;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  gap: 0.8rem;
  border: 1px solid rgba(203, 213, 225, 0.72);
  border-radius: 20px;
  background: #ffffff;
  padding: clamp(0.9rem, 1.6vw, 1.15rem);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.07);
}

.quick-add-panel::before {
  content: '';
  position: absolute;
  inset: 0 1rem auto;
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, #14b8a6, #0ea5e9, #f59e0b);
}

.panel-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.panel-eyebrow {
  color: #0f766e;
  font-size: 0.7rem;
  font-weight: 800;
  text-transform: uppercase;
}

h2 {
  margin-top: 0.1rem;
  color: #0f172a;
  font-size: clamp(1.05rem, 2vw, 1.3rem);
  font-weight: 800;
}

@media (max-width: 640px) {
  .panel-heading {
    display: grid;
  }
}

@media (max-width: 420px) {
  .quick-add-panel {
    gap: 0.7rem;
    border-radius: 16px;
    padding: 0.85rem;
  }
}
</style>
