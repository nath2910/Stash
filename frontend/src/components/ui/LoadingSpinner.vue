<template>
  <span
    class="inline-block shrink-0 rounded-full border-2 border-current border-r-transparent animate-spin"
    :class="sizeClass"
    aria-hidden="true"
  ></span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  size: { type: String, default: 'sm' },
})

const sizeClass = computed(() => ({
  xs: 'h-3 w-3 border-[1.5px]',
  sm: 'h-4 w-4',
  md: 'h-5 w-5',
  lg: 'h-7 w-7 border-[3px]',
}[props.size] || 'h-4 w-4'))
</script>
