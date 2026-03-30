<template>
  <div class="text-block-shell" :style="containerStyle">
    <div class="text-block-copy" :style="textStyle">
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
  valign: { type: String, default: 'start' }, // start | center | end
  fontFamily: { type: String, default: 'open-sans' },
  fontSize: { type: Number, default: 17 },
  color: { type: String, default: '#e2e8f0' },
  weight: { type: String, default: 'regular' }, // regular | medium | semibold | bold
  italic: { type: Boolean, default: false },
  underline: { type: Boolean, default: false },
  widgetWidth: { type: Number, default: 680 },
  widgetHeight: { type: Number, default: 220 },
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
    props.valign === 'center' ? 'center' : props.valign === 'end' ? 'flex-end' : 'flex-start'
  return { justifyContent, flexDirection: 'column' }
})

const textStyle = computed(() => {
  const sizes = {
    sm: { base: 14, min: 12, max: 280, line: 1.6 },
    md: { base: 17, min: 14, max: 360, line: 1.58 },
    lg: { base: 22, min: 17, max: 480, line: 1.5 },
    xl: { base: 28, min: 20, max: 620, line: 1.42 },
  }
  const weights = { regular: 400, medium: 500, semibold: 600, bold: 700 }

  const cfg = sizes[props.size] ?? sizes.md
  const fontSize = clamp(Math.round(Number(props.fontSize || cfg.base)), cfg.min, cfg.max)
  const isCentered = props.align === 'center'
  const isRight = props.align === 'right'

  return {
    textAlign: props.align,
    fontSize: `${fontSize}px`,
    lineHeight: cfg.line,
    fontFamily: FONT_FAMILIES[props.fontFamily] ?? FONT_FAMILIES['open-sans'],
    fontWeight: weights[props.weight] ?? weights.regular,
    fontStyle: props.italic ? 'italic' : 'normal',
    textDecoration: props.underline ? 'underline' : 'none',
    color: props.color || '#e2e8f0',
    width: '100%',
    maxWidth: '100%',
    marginLeft: isCentered ? 'auto' : isRight ? 'auto' : '0',
    marginRight: isCentered ? 'auto' : isRight ? '0' : 'auto',
  }
})
</script>

<style scoped>
.text-block-shell {
  width: 100%;
  height: auto;
  min-height: 100%;
  max-width: 100%;
  overflow: hidden;
  display: flex;
}

.text-block-copy {
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
