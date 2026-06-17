<template>
  <div class="stats-page-shell" :class="{ 'stats-page-shell--light': themeMode === 'light' }">
    <StatsCanvas v-model:from="from" v-model:to="to" />
  </div>
</template>

<script setup lang="ts">
import { useStatsRange } from '@/composables/useStatsRange'
import { computed, defineAsyncComponent } from 'vue'
import { useTheme } from '@/composables/useTheme'

const StatsCanvas = defineAsyncComponent(() => import('@/components/stats/StatsCanvas.vue'))
const { from, to } = useStatsRange()
const { theme } = useTheme()
const themeMode = computed(() => (theme.value === 'light' ? 'light' : 'dark'))
</script>

<style scoped>
.stats-page-shell {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: #060a12;
}

.stats-page-shell--light {
  background: #f7f4ee;
  color: #000;
}
</style>
