<template>
  <VChart class="sparkline-chart" :option="option" autoresize />
</template>

<script setup>
import { computed } from 'vue'

defineOptions({ name: 'StatsSparkline' })

const props = defineProps({
  data: { type: Array, default: () => [] }, // numbers
  color: { type: String, default: '#22C55E' },
})

const option = computed(() => {
  const values = (props.data ?? []).map((n) => Number(n ?? 0))
  return {
    backgroundColor: 'transparent',
    grid: { left: 2, right: 2, top: 4, bottom: 2 },
    tooltip: {
      trigger: 'axis',
      triggerOn: 'mousemove|click',
      confine: true,
      transitionDuration: 0,
      backgroundColor: 'rgba(255,255,255,0.98)',
      borderColor: 'rgba(148,163,184,0.28)',
      borderWidth: 1,
      padding: [7, 9],
      textStyle: { color: '#0f172a', fontSize: 11, fontWeight: 650 },
      extraCssText: 'border-radius:9px;box-shadow:0 10px 22px rgba(15,23,42,0.13);',
      axisPointer: {
        type: 'line',
        snap: true,
        lineStyle: { color: `${props.color}80`, width: 1 },
      },
      formatter: (params) => {
        const entry = Array.isArray(params) ? params[0] : params
        const index = Number(entry?.dataIndex ?? -1)
        const value = Number(entry?.data ?? 0)
        const previous = Number(values[index - 1] ?? NaN)
        const detail =
          Number.isFinite(previous) && index > 0
            ? `<div style="margin-top:3px;color:#64748b;">Variation: <strong style="color:${
                value - previous >= 0 ? '#047857' : '#be123c'
              };">${escapeHtml(formatSignedCompact(value - previous))}</strong></div>`
            : ''
        return `<div style="color:#64748b;font-size:10px;text-transform:uppercase;letter-spacing:.06em;">Point ${index + 1}</div><div style="margin-top:3px;font-weight:800;color:#111827;">${escapeHtml(formatCompact(value))}</div>${detail}`
      },
    },
    xAxis: {
      type: 'category',
      show: false,
      data: values.map((_, i) => i),
      axisPointer: { show: true, snap: true },
    },
    yAxis: { type: 'value', show: false },
    series: [
      {
        type: 'line',
        data: values,
        showSymbol: false,
        smooth: true,
        lineStyle: { width: 2.2, color: props.color },
        emphasis: {
          focus: 'self',
          scale: true,
          itemStyle: {
            color: '#ffffff',
            borderColor: props.color,
            borderWidth: 2,
          },
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: `${props.color}66` },
              { offset: 1, color: `${props.color}00` },
            ],
          },
        },
      },
    ],
  }
})

function formatCompact(value) {
  const n = Number(value ?? 0)
  if (!Number.isFinite(n)) return '--'
  return new Intl.NumberFormat('fr-FR', {
    notation: Math.abs(n) >= 10_000 ? 'compact' : 'standard',
    maximumFractionDigits: Math.abs(n) >= 100 ? 0 : 1,
  }).format(n)
}

function formatSignedCompact(value) {
  const n = Number(value ?? 0)
  const sign = n > 0 ? '+' : ''
  return `${sign}${formatCompact(n)}`
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (char) => {
    const entities = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;',
    }
    return entities[char] || char
  })
}
</script>

<style scoped>
.sparkline-chart {
  width: 100%;
  height: 100%;
  min-height: 0;
}
</style>
