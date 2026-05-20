<template>
  <div class="rectangle-shape" :style="shapeStyle" aria-hidden="true"></div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  fillEnabled: { type: Boolean, default: true },
  fillColor: { type: String, default: '#ffffff' },
  borderEnabled: { type: Boolean, default: true },
  borderColor: { type: String, default: '#cbd5e1' },
  borderWidth: { type: Number, default: 1 },
  cornerRadius: { type: Number, default: 8 },
})

const clamp = (value, min, max) => Math.max(min, Math.min(max, value))

const shapeStyle = computed(() => {
  const borderWidth = props.borderEnabled ? clamp(Number(props.borderWidth || 0), 0, 64) : 0
  return {
    background: props.fillEnabled ? props.fillColor || '#ffffff' : 'transparent',
    border: borderWidth > 0 ? `${borderWidth}px solid ${props.borderColor || '#cbd5e1'}` : 'none',
    borderRadius: `${clamp(Number(props.cornerRadius || 0), 0, 240)}px`,
  }
})
</script>

<style scoped>
.rectangle-shape {
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  pointer-events: none;
}
</style>
