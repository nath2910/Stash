<template>
  <div class="gestion-search-root w-full min-w-0">
    <div
      class="gestion-search-field group flex h-10 items-center gap-2.5 rounded-[14px] border border-sky-200/80 bg-white/90 px-3.5 text-slate-950 shadow-sm transition focus-within:border-teal-400/70 focus-within:bg-white focus-within:ring-2 focus-within:ring-teal-400/15"
    >
      <Search
        class="gestion-search-icon h-4 w-4 shrink-0 text-sky-600 transition group-focus-within:text-teal-600"
        aria-hidden="true"
      />

      <input
        :value="modelValue"
        @input="emit('update:modelValue', $event.target.value)"
        type="search"
        autocomplete="off"
        autocapitalize="none"
        spellcheck="false"
        placeholder="Rechercher nom, categorie, tag, SKU..."
        class="gestion-search-input min-w-0 flex-1 border-0 bg-transparent text-[14px] font-semibold leading-none text-slate-950 placeholder:text-slate-400 focus:outline-none focus:ring-0"
      />

    </div>
  </div>
</template>

<script setup>
import { Search } from 'lucide-vue-next'

defineProps({
  modelValue: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue'])
</script>

<style scoped>
.gestion-search-field {
  cursor: text;
  transition:
    border-color 170ms ease,
    background-color 170ms ease,
    box-shadow 170ms ease,
    transform 170ms ease;
}

.gestion-search-field:hover {
  border-color: rgba(20, 184, 166, 0.46);
  box-shadow:
    0 16px 30px rgba(14, 116, 144, 0.09),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.gestion-search-field:focus-within {
  transform: translateY(-1px);
}

</style>
