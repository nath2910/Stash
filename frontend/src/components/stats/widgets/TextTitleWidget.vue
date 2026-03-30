<template>
  <div class="text-title-shell" :style="containerStyle">
    <div class="text-title-copy" :style="titleStyle">
      {{ content }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  content: { type: String, default: 'Titre' },
  size: { type: String, default: 'lg' }, // sm | md | lg | xl
  align: { type: String, default: 'left' }, // left | center | right
  valign: { type: String, default: 'center' }, // start | center | end
  fontFamily: { type: String, default: 'open-sans' },
  fontSize: { type: Number, default: 52 },
  color: { type: String, default: '#ffffff' },
  weight: { type: String, default: 'bold' }, // regular | medium | semibold | bold | heavy
  italic: { type: Boolean, default: false },
  underline: { type: Boolean, default: false },
  widgetWidth: { type: Number, default: 720 },
  widgetHeight: { type: Number, default: 110 },
})

const clamp = (value, min, max) => Math.max(min, Math.min(max, value))
const FONT_FAMILIES = {
  'open-sans': '"Open Sans", Arial, sans-serif',
  'pt-sans': '"PT Sans", Arial, sans-serif',
  'pt-serif': '"PT Serif", Georgia, serif',
  roboto: 'Roboto, Arial, sans-serif',
  'roboto-slab': '"Roboto Slab", Georgia, serif',
  'ibm-plex-sans': '"IBM Plex Sans", Arial, sans-serif',
  'ibm-plex-mono': '"IBM Plex Mono", monospace',
  georgia: 'Georgia, serif',
  arial: 'Arial, sans-serif',
  'permanent-marker': '"Permanent Marker", "Comic Sans MS", cursive',
  caveat: 'Caveat, "Comic Sans MS", cursive',
}

const containerStyle = computed(() => {
  const justifyContent =
    props.valign === 'start' ? 'flex-start' : props.valign === 'end' ? 'flex-end' : 'center'
  const alignItems =
    props.align === 'center' ? 'center' : props.align === 'right' ? 'flex-end' : 'flex-start'
  return { justifyContent, alignItems, flexDirection: 'column' }
})

const titleStyle = computed(() => {
  const sizes = {
    sm: { base: 28, min: 16, max: 280, line: 0.98 },
    md: { base: 38, min: 20, max: 360, line: 0.96 },
    lg: { base: 52, min: 24, max: 480, line: 0.94 },
    xl: { base: 68, min: 30, max: 620, line: 0.9 },
  }
  const weights = { regular: 400, medium: 500, semibold: 600, bold: 700, heavy: 800 }
  const cfg = sizes[props.size] ?? sizes.lg
  const fontSize = clamp(Math.round(Number(props.fontSize || cfg.base)), cfg.min, cfg.max)
  const isCentered = props.align === 'center'
  const isRight = props.align === 'right'

  return {
    textAlign: props.align,
    fontSize: `${fontSize}px`,
    lineHeight: cfg.line,
    fontFamily: FONT_FAMILIES[props.fontFamily] ?? FONT_FAMILIES['open-sans'],
    fontWeight: weights[props.weight] ?? weights.bold,
    fontStyle: props.italic ? 'italic' : 'normal',
    textDecoration: props.underline ? 'underline' : 'none',
    color: props.color || '#ffffff',
    letterSpacing: '-0.032em',
    width: '100%',
    maxWidth: '100%',
    marginLeft: isCentered ? 'auto' : isRight ? 'auto' : '0',
    marginRight: isCentered ? 'auto' : isRight ? '0' : 'auto',
  }
})
</script>

<style scoped>
.text-title-shell {
  width: 100%;
  height: auto;
  min-height: 100%;
  max-width: 100%;
  overflow: hidden;
  display: flex;
}

.text-title-copy {
  display: block;
  width: 100%;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
  white-space: pre-wrap;
  word-break: normal;
  overflow-wrap: break-word;
  hyphens: none;
  user-select: none;
  -webkit-user-select: none;
}
</style>
