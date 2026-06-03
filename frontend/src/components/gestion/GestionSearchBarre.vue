<template>
  <div class="gestion-search-root w-full min-w-0">
    <div
      class="gestion-search-field group flex h-10 items-center gap-2.5 rounded-[14px] border border-sky-200/80 bg-white/90 px-3.5 text-slate-950 shadow-sm transition focus-within:border-teal-400/70 focus-within:bg-white focus-within:ring-2 focus-within:ring-teal-400/15"
    >
      <svg
        class="gestion-search-icon h-4 w-4 shrink-0 text-sky-600 transition group-focus-within:text-teal-600"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M21 21l-4.35-4.35M5 11a6 6 0 1112 0 6 6 0 01-12 0z"
        />
      </svg>

      <input
        :value="modelValue"
        @input="$emit('update:modelValue', $event.target.value)"
        type="text"
        placeholder="Rechercher nom, categorie, description..."
        class="gestion-search-input min-w-0 flex-1 border-0 bg-transparent text-[14px] font-semibold leading-none text-slate-950 placeholder:text-slate-400 focus:outline-none focus:ring-0"
      />
    </div>
  </div>
</template>

<script setup>
defineProps({
  modelValue: {
    type: String,
    default: '',
  },
})

defineEmits(['update:modelValue'])
</script>
