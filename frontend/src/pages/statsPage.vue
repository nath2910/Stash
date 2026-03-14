<template>
  <div class="stats-page-shell">
    <StatsCanvas v-model:from="from" v-model:to="to" />
  </div>
</template>

<script setup lang="ts">
import StatsCanvas from '@/components/stats/StatsCanvas.vue'
import { useStatsRange } from '@/composables/useStatsRange'

const { from, to } = useStatsRange()
</script>

<style scoped>
.stats-page-shell {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 100svh;
  min-height: 100dvh;
  overflow: hidden;
  background: #060a12;
}
</style>
