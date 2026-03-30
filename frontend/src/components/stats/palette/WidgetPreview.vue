<template>
  <div class="widget-preview" :style="previewStyle" aria-hidden="true">
    <template v-if="kind === 'kpi'">
      <div class="preview-head">
        <span class="preview-label">{{ safeModel.label ?? 'KPI' }}</span>
        <span class="preview-delta" :class="{ 'is-negative': isNegativeDelta }">{{ safeModel.deltaText ?? '+4.2%' }}</span>
      </div>
      <div class="preview-kpi-value">{{ safeModel.valueText ?? kpiValue }}</div>
      <svg class="widget-preview__spark" viewBox="0 0 100 28" preserveAspectRatio="none">
        <polyline :points="sparklinePoints" class="preview-line" />
      </svg>
    </template>

    <template v-else-if="kind === 'sparkline'">
      <div class="preview-head">
        <span class="preview-label">{{ safeModel.label ?? 'Trend' }}</span>
        <span class="preview-value-sm">{{ safeModel.valueText ?? '42.9k' }}</span>
      </div>
      <svg class="widget-preview__spark widget-preview__spark--full" viewBox="0 0 100 40" preserveAspectRatio="none">
        <polyline :points="sparklineFill" class="preview-fill" />
        <polyline :points="sparklinePoints" class="preview-line" />
        <circle :cx="sparklineLastPoint.x" :cy="sparklineLastPoint.y" r="2" class="preview-dot" />
      </svg>
      <div class="preview-footer">{{ safeModel.deltaText ?? '+9%' }}</div>
    </template>

    <template v-else-if="kind === 'bars'">
      <div class="preview-head">
        <span class="preview-label">{{ safeModel.label ?? 'Comparatif' }}</span>
      </div>
      <svg class="widget-preview__bars" viewBox="0 0 100 46" preserveAspectRatio="none">
        <line x1="2" y1="42" x2="98" y2="42" class="preview-axis" />
        <rect
          v-for="(bar, index) in barValues"
          :key="`bar-${index}`"
          :x="index * 18 + 6"
          :y="42 - bar"
          width="11"
          :height="bar"
          rx="2"
          class="preview-bar"
          :style="{ opacity: 0.58 + (index / 12) }"
        />
      </svg>
      <div class="preview-legend" v-if="safeLegend.length">
        <span v-for="(entry, idx) in safeLegend" :key="`legend-${idx}`">{{ entry }}</span>
      </div>
    </template>

    <template v-else-if="kind === 'pie'">
      <div class="preview-head">
        <span class="preview-label">{{ safeModel.label ?? 'Distribution' }}</span>
      </div>
      <div class="preview-pie-layout">
        <svg class="widget-preview__pie" viewBox="0 0 48 48">
          <circle cx="24" cy="24" r="16" class="preview-pie-track" />
          <circle cx="24" cy="24" r="16" class="preview-pie-arc" :stroke-dasharray="pieDash" stroke-dashoffset="12" />
          <circle cx="24" cy="24" r="8" class="preview-pie-center" />
        </svg>
        <div class="preview-pie-legend">
          <span v-for="(entry, idx) in pieLegend" :key="`pie-${idx}`">{{ entry }}</span>
        </div>
      </div>
    </template>

    <template v-else-if="kind === 'text'">
      <div class="preview-head">
        <span class="preview-label">{{ safeModel.label ?? 'Texte' }}</span>
      </div>
      <div class="widget-preview__text">
        <span class="line line--lg"></span>
        <span class="line line--md"></span>
        <span class="line line--sm"></span>
      </div>
    </template>

    <template v-else-if="kind === 'treemap'">
      <div class="preview-head">
        <span class="preview-label">{{ safeModel.label ?? 'Treemap' }}</span>
      </div>
      <svg class="widget-preview__blocks" viewBox="0 0 100 46" preserveAspectRatio="none">
        <rect x="0" y="0" width="54" height="28" rx="3" class="preview-block preview-block--strong" />
        <rect x="56" y="0" width="44" height="18" rx="3" class="preview-block" />
        <rect x="0" y="30" width="32" height="16" rx="3" class="preview-block" />
        <rect x="34" y="30" width="28" height="16" rx="3" class="preview-block" />
        <rect x="64" y="20" width="36" height="26" rx="3" class="preview-block" />
      </svg>
    </template>

    <template v-else-if="kind === 'heatmap'">
      <div class="preview-head">
        <span class="preview-label">{{ safeModel.label ?? 'Heatmap' }}</span>
      </div>
      <svg class="widget-preview__blocks" viewBox="0 0 100 46" preserveAspectRatio="none">
        <rect
          v-for="(cell, index) in heatCells"
          :key="`cell-${index}`"
          :x="cell.x"
          :y="cell.y"
          width="16"
          height="9"
          rx="2"
          class="preview-cell"
          :style="{ opacity: cell.opacity }"
        />
      </svg>
    </template>

    <div v-else class="widget-preview__fallback">
      <LayoutGrid :size="16" />
      <span>Apercu indisponible</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { LayoutGrid } from 'lucide-vue-next'
import { coercePreviewSpec } from './paletteUtils'
import type { WidgetPreviewSpec } from './types'

const props = defineProps<{
  model?: WidgetPreviewSpec
  accent: string
}>()

const safeModel = computed(() => coercePreviewSpec(props.model))
const kind = computed(() => safeModel.value.kind)

const previewStyle = computed(() => ({
  '--preview-accent': props.accent,
}))

const rawValues = computed(() => safeModel.value.values ?? [14, 18, 16, 20, 26, 22, 28])

const sparklinePoints = computed(() => {
  const values = rawValues.value
  const max = Math.max(...values, 1)
  const min = Math.min(...values, 0)
  const range = Math.max(max - min, 1)
  return values
    .map((value, index) => {
      const x = (index / Math.max(values.length - 1, 1)) * 100
      const y = 24 - ((value - min) / range) * 20
      return `${x},${y}`
    })
    .join(' ')
})

const sparklineLastPoint = computed(() => {
  const values = rawValues.value
  const max = Math.max(...values, 1)
  const min = Math.min(...values, 0)
  const range = Math.max(max - min, 1)
  const x = 100
  const value = values[values.length - 1] ?? 0
  const y = 24 - ((value - min) / range) * 20
  return { x, y }
})

const sparklineFill = computed(() => `0,30 ${sparklinePoints.value} 100,30`)

const barValues = computed(() => {
  const values = rawValues.value.slice(0, 5)
  const max = Math.max(...values, 1)
  return values.map((value) => 7 + Math.round((value / max) * 32))
})

const pieRatio = computed(() => {
  const values = rawValues.value
  const total = values.reduce((sum, value) => sum + Math.max(value, 0), 0)
  if (!total) return 0.64
  return Math.max(0.12, Math.min(0.9, values[0] / total))
})

const pieDash = computed(() => {
  const circumference = 2 * Math.PI * 16
  const active = Math.round(circumference * pieRatio.value)
  return `${active} ${Math.round(circumference - active)}`
})

const pieLegend = computed(() => {
  if (safeModel.value.legend?.length) {
    return safeModel.value.legend.slice(0, 3)
  }
  return ['A', 'B', 'C']
})

const safeLegend = computed(() => safeModel.value.legend?.slice(0, 4) ?? [])

const kpiValue = computed(() => `${Math.round((safeModel.value.ratio ?? 0.72) * 100)}%`)

const isNegativeDelta = computed(() => String(safeModel.value.deltaText ?? '').trim().startsWith('-'))

const heatCells = computed(() => {
  const cells = [] as Array<{ x: number; y: number; opacity: number }>
  let idx = 0
  for (let row = 0; row < 4; row += 1) {
    for (let col = 0; col < 5; col += 1) {
      cells.push({
        x: col * 19 + 2,
        y: row * 11 + 1,
        opacity: 0.28 + ((idx % 5) * 0.14),
      })
      idx += 1
    }
  }
  return cells
})
</script>

<style scoped>
.widget-preview {
  position: relative;
  overflow: hidden;
  border-radius: 10px;
  border: 1px solid rgba(100, 116, 139, 0.28);
  background:
    linear-gradient(180deg, rgba(214, 224, 237, 0.74), rgba(203, 213, 225, 0.74)),
    radial-gradient(circle at 100% 0, color-mix(in srgb, var(--preview-accent) 14%, transparent), transparent 55%);
  min-height: 88px;
  padding: 9px;
}

.preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.preview-label {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: #64748b;
}

.preview-value-sm {
  font-size: 10px;
  font-weight: 700;
  color: #1e293b;
}

.preview-delta {
  padding: 2px 6px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 700;
  color: #166534;
  background: rgba(187, 247, 208, 0.45);
}

.preview-delta.is-negative {
  color: #9a3412;
  background: rgba(254, 215, 170, 0.55);
}

.preview-kpi-value {
  margin-top: 4px;
  font-size: 18px;
  line-height: 1;
  font-weight: 750;
  color: #0f172a;
}

.preview-footer {
  margin-top: 3px;
  font-size: 10px;
  color: #64748b;
}

.widget-preview__spark {
  width: 100%;
  height: 30px;
  margin-top: 8px;
}

.widget-preview__spark--full {
  height: 52px;
  margin-top: 7px;
}

.preview-line {
  fill: none;
  stroke: var(--preview-accent);
  stroke-width: 2.2;
  stroke-linejoin: round;
  stroke-linecap: round;
}

.preview-fill {
  fill: color-mix(in srgb, var(--preview-accent) 14%, transparent);
  stroke: none;
}

.preview-dot {
  fill: color-mix(in srgb, var(--preview-accent) 82%, #0f172a);
}

.widget-preview__bars {
  width: 100%;
  height: 52px;
  margin-top: 6px;
}

.preview-axis {
  stroke: rgba(148, 163, 184, 0.36);
  stroke-width: 1;
}

.preview-bar {
  fill: color-mix(in srgb, var(--preview-accent) 58%, #94a3b8);
}

.preview-legend {
  margin-top: 4px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 4px;
}

.preview-legend span {
  font-size: 9px;
  color: #64748b;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
}

.preview-pie-layout {
  display: grid;
  grid-template-columns: 58px 1fr;
  gap: 8px;
  align-items: center;
  margin-top: 3px;
}

.widget-preview__pie {
  width: 58px;
  height: 58px;
}

.preview-pie-track {
  fill: none;
  stroke: rgba(148, 163, 184, 0.28);
  stroke-width: 8;
}

.preview-pie-arc {
  fill: none;
  stroke: var(--preview-accent);
  stroke-width: 8;
  transform-origin: center;
  transform: rotate(-90deg);
}

.preview-pie-center {
  fill: rgba(236, 242, 249, 0.96);
}

.preview-pie-legend {
  display: grid;
  gap: 4px;
}

.preview-pie-legend span {
  position: relative;
  padding-left: 10px;
  font-size: 10px;
  color: #64748b;
}

.preview-pie-legend span::before {
  content: '';
  position: absolute;
  left: 0;
  top: 5px;
  width: 5px;
  height: 5px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--preview-accent) 72%, #94a3b8);
}

.widget-preview__text {
  display: grid;
  gap: 8px;
  padding-top: 7px;
}

.line {
  display: block;
  height: 5px;
  border-radius: 99px;
  background: rgba(148, 163, 184, 0.3);
}

.line--lg {
  width: 82%;
}

.line--md {
  width: 63%;
}

.line--sm {
  width: 46%;
}

.widget-preview__blocks {
  width: 100%;
  height: 56px;
  margin-top: 5px;
}

.preview-block {
  fill: color-mix(in srgb, var(--preview-accent) 28%, rgba(148, 163, 184, 0.32));
}

.preview-block--strong {
  fill: color-mix(in srgb, var(--preview-accent) 46%, rgba(148, 163, 184, 0.38));
}

.preview-cell {
  fill: var(--preview-accent);
}

.widget-preview__fallback {
  min-height: 64px;
  display: grid;
  place-items: center;
  gap: 5px;
  color: #64748b;
  font-size: 12px;
}
</style>
