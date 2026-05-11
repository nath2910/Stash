<template>
  <section class="template-empty-layout" aria-live="polite">
    <div class="template-empty-layout__content">
      <p class="template-empty-layout__kicker">Templates</p>
      <h2 class="template-empty-layout__title">Aucun template disponible</h2>
      <p class="template-empty-layout__text">
        Le shell du module est conserve, mais aucun ancien template n'est charge.
      </p>
      <button type="button" class="template-empty-layout__back" @click="emit('back')">
        Retour au canvas
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
const emit = defineEmits<{
  (event: 'back'): void
}>()
</script>
