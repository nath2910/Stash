<template>
  <TemplateKpiCard namespace="annual" v-bind="props" />
</template>

<script setup lang="ts">
import TemplateKpiCard from '../shared/TemplateKpiCard.vue'

const props = withDefaults(
  defineProps<{
    label: string
    value: string
    detail?: string
    tone?: 'primary' | 'profit' | 'warning' | 'neutral'
    icon?: unknown
  }>(),
  {
    detail: '',
    tone: 'neutral',
    icon: null,
  },
)
</script>
