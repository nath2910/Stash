<template>
  <StatsCanvas v-model:from="from" v-model:to="to" />
</template>

<script setup lang="ts">
import StatsCanvas from '@/components/stats/StatsCanvas.vue'
import { useStatsRange } from '@/composables/useStatsRange'

const { from, to } = useStatsRange()
</script>
