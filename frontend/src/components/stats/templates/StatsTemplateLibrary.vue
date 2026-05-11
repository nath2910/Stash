<template>
  <Teleport to="body">
    <div v-if="open" class="template-library-overlay" role="dialog" aria-modal="true">
      <button
        type="button"
        class="template-library-backdrop"
        aria-label="Fermer les templates"
        @click="emit('close')"
      ></button>

      <section class="template-library-shell" @click.stop>
        <header class="template-library-head">
          <div>
            <p class="template-library-kicker">Templates</p>
            <h2 class="template-library-title">Selection de template</h2>
          </div>
          <button type="button" class="template-library-close" @click="emit('close')">x</button>
        </header>

        <div v-if="!safeTemplates.length" class="template-library-empty" role="status">
          <h3>Aucun template disponible</h3>
          <p>La liste est vide et ne charge aucun ancien template.</p>
        </div>

        <div v-else class="template-library-list">
          <button
            v-for="template in safeTemplates"
            :key="template.id"
            type="button"
            class="template-library-card"
            @click="emit('apply', template)"
          >
            <span class="template-library-card__title">{{ template.name }}</span>
            <span v-if="template.description" class="template-library-card__desc">
              {{ template.description }}
            </span>
          </button>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type TemplateLibraryItem = {
  id: string
  name: string
  description?: string
}

const props = withDefaults(
  defineProps<{
    open: boolean
    templates?: TemplateLibraryItem[]
  }>(),
  {
    templates: () => [],
  },
)

const emit = defineEmits<{
  (event: 'close'): void
  (event: 'apply', payload: TemplateLibraryItem): void
}>()

const safeTemplates = computed(() =>
  (props.templates ?? []).filter((template) => template?.id && template?.name),
)
</script>

<style scoped src="./StatsTemplateLibrary.css"></style>
