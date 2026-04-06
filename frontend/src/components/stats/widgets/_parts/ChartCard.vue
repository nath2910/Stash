<template>
  <WidgetCard
    :title="title"
    :subtitle="subtitle"
    :accent="accent"
    :surface="surface"
    :loading="loading"
    :error="error"
    :widget-width="widgetWidth"
    :widget-height="widgetHeight"
    :widget-base-width="widgetBaseWidth"
    :widget-base-height="widgetBaseHeight"
  >
    <div class="chart-card h-full flex flex-col gap-2">
      <VChart class="chart-card__chart w-full flex-1" :style="chartStyle" :option="responsiveOption" autoresize />
      <slot />
    </div>
  </WidgetCard>
</template>

<script setup>
import { computed } from 'vue'
import WidgetCard from './WidgetCard.vue'

const props = defineProps({
  title: String,
  subtitle: String,
  accent: { type: String, default: '#38BDF8' },
  surface: { type: String, default: 'trend' },
  loading: Boolean,
  error: String,
  option: { type: Object, required: true },
  widgetWidth: { type: Number, default: 720 },
  widgetHeight: { type: Number, default: 360 },
  widgetBaseWidth: { type: Number, default: 0 },
  widgetBaseHeight: { type: Number, default: 0 },
})

const clamp = (value, min, max) => Math.max(min, Math.min(max, value))

function tuneAxis(axis, fontSize) {
  if (!axis) return axis
  if (Array.isArray(axis)) return axis.map((item) => tuneAxis(item, fontSize))
  return {
    ...axis,
    axisLabel: {
      ...(axis.axisLabel ?? {}),
      fontSize,
    },
    nameTextStyle: {
      ...(axis.nameTextStyle ?? {}),
      fontSize,
    },
  }
}

const responsiveOption = computed(() => {
  const base = props.option ?? {}
  const axisFont = clamp(Math.round(Math.min(props.widgetWidth * 0.016, props.widgetHeight * 0.05)), 11, 16)
  const legendFont = clamp(axisFont, 11, 15)
  const labelFont = clamp(axisFont + 1, 11, 17)

  return {
    ...base,
    legend: base.legend
      ? {
          ...base.legend,
          textStyle: {
            ...(base.legend.textStyle ?? {}),
            fontSize: legendFont,
          },
        }
      : base.legend,
    tooltip: base.tooltip
      ? {
          ...base.tooltip,
          textStyle: {
            ...(base.tooltip.textStyle ?? {}),
            fontSize: labelFont,
          },
        }
      : base.tooltip,
    xAxis: tuneAxis(base.xAxis, axisFont),
    yAxis: tuneAxis(base.yAxis, axisFont),
    series: Array.isArray(base.series)
      ? base.series.map((series) => ({
          ...series,
          label: series?.label
            ? {
                ...series.label,
                fontSize: labelFont,
              }
            : series?.label,
          emphasis: series?.emphasis
            ? {
                ...series.emphasis,
                label: series.emphasis.label
                  ? {
                      ...series.emphasis.label,
                      fontSize: labelFont,
                    }
                  : series.emphasis.label,
              }
            : series?.emphasis,
        }))
      : base.series,
  }
})

const chartStyle = computed(() => {
  const minHeight = clamp(Math.round(props.widgetHeight * 0.5), 160, 340)
  return {
    minHeight: `${minHeight}px`,
  }
})
</script>

<style scoped>
.chart-card {
  min-height: 0;
}

.chart-card__chart {
  min-height: 0;
}
</style>
