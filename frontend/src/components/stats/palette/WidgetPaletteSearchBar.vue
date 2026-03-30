<template>
  <label class="palette-search" for="widget-palette-search">
    <Search class="palette-search__icon" :size="16" aria-hidden="true" />
    <input
      id="widget-palette-search"
      ref="inputRef"
      :value="modelValue"
      type="search"
      inputmode="search"
      autocomplete="off"
      class="palette-search__input"
      placeholder="Rechercher un widget, un tag, un type..."
      aria-label="Rechercher un widget"
      @input="onInput"
      @keydown="emit('keydown', $event)"
    />
    <span class="palette-search__meta" aria-live="polite">{{ resultCount }}/{{ totalCount }}</span>
    <button
      v-if="modelValue"
      type="button"
      class="palette-search__clear"
      aria-label="Effacer la recherche"
      @click="emit('update:modelValue', '')"
    >
      <X :size="14" aria-hidden="true" />
    </button>
  </label>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Search, X } from 'lucide-vue-next'

defineProps<{
  modelValue: string
  resultCount: number
  totalCount: number
}>()

const inputRef = ref<HTMLInputElement | null>(null)

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
  (event: 'keydown', payload: KeyboardEvent): void
}>()

function onInput(event: Event) {
  const input = event.target as HTMLInputElement | null
  emit('update:modelValue', input?.value ?? '')
}

defineExpose({
  focus: () => inputRef.value?.focus(),
})
</script>

<style scoped>
.palette-search {
  display: grid;
  grid-template-columns: auto 1fr auto auto;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  padding: 0 10px;
  border-radius: 12px;
  border: 1px solid rgba(100, 116, 139, 0.44);
  background: rgba(214, 224, 237, 0.82);
  transition: border-color 140ms ease, background 140ms ease;
}

.palette-search:focus-within {
  border-color: rgba(59, 130, 246, 0.54);
  background: rgba(226, 232, 240, 0.9);
}

.palette-search__icon {
  color: #64748b;
}

.palette-search__input {
  min-width: 0;
  width: 100%;
  border: 0;
  outline: none;
  background: transparent;
  font-size: 14px;
  color: #0f172a;
}

.palette-search__input::placeholder {
  color: #94a3b8;
}

.palette-search__meta {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: #64748b;
}

.palette-search__clear {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.56);
  color: #475569;
  background: rgba(226, 232, 240, 0.86);
  transition: border-color 120ms ease, background 120ms ease;
}

.palette-search__clear:hover {
  border-color: rgba(59, 130, 246, 0.48);
  background: rgba(241, 245, 249, 0.94);
}
</style>
