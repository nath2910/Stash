<template>
  <div class="h-full w-full flex items-start">
    <div :class="textClass" class="w-full whitespace-pre-wrap">
      {{ content }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  content: { type: String, default: 'Ton texte ici...' },
  size: { type: String, default: 'md' }, // sm | md | lg | xl
  align: { type: String, default: 'left' }, // left | center | right
})

const textClass = computed(() => {
  const align =
    props.align === 'center' ? 'text-center' : props.align === 'right' ? 'text-right' : 'text-left'
  const sizes = {
    sm: 'text-sm leading-relaxed',
    md: 'text-base leading-relaxed',
    lg: 'text-lg leading-relaxed',
    xl: 'text-xl leading-relaxed',
  }
  return `${align} ${sizes[props.size] ?? sizes.md} text-slate-300`
})
</script>
