<template>
  <div class="w-full">
    <div
      class="group flex h-[42px] items-center gap-2.5 rounded-[14px] border border-slate-700/90 bg-slate-950/55 px-4 shadow-sm transition focus-within:border-violet-400/60 focus-within:ring-2 focus-within:ring-violet-500/25"
    >
      <svg
        class="h-4 w-4 shrink-0 text-slate-400 transition group-focus-within:text-violet-300"
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
        placeholder="Rechercher modele, categorie, description..."
        class="min-w-0 flex-1 border-0 bg-transparent text-[14px] leading-none text-slate-100 placeholder:text-slate-500 focus:outline-none focus:ring-0"
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
