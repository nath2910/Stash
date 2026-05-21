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
    xAxis: { type: 'category', show: false, data: values.map((_, i) => i) },
    yAxis: { type: 'value', show: false },
    series: [
      {
        type: 'line',
        data: values,
        showSymbol: false,
        smooth: true,
        lineStyle: { width: 2.2, color: props.color },
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
</script>

<style scoped>
.sparkline-chart {
  width: 100%;
  height: 100%;
  min-height: 0;
}
</style>
