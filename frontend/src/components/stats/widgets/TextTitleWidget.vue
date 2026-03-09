<template>
  <div class="h-full w-full flex items-center">
    <div :class="titleClass" class="w-full whitespace-pre-wrap">
      {{ content }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  content: { type: String, default: 'Titre' },
  size: { type: String, default: 'md' }, // sm | md | lg
  align: { type: String, default: 'left' }, // left | center | right
})

const titleClass = computed(() => {
  const align =
    props.align === 'center' ? 'text-center' : props.align === 'right' ? 'text-right' : 'text-left'
  const sizes = {
    sm: 'text-3xl sm:text-4xl',
    md: 'text-4xl sm:text-5xl',
    lg: 'text-5xl sm:text-6xl',
  }
  return `${align} ${sizes[props.size] ?? sizes.md} font-semibold text-slate-100 tracking-tight`
})
</script>
