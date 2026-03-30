<template>
  <div class="stats-page-shell" :class="{ 'stats-page-shell--light': themeMode === 'light' }">
    <StatsCanvas v-model:from="from" v-model:to="to" />
  </div>
</template>

<script setup lang="ts">
import StatsCanvas from '@/components/stats/StatsCanvas.vue'
import { useStatsRange } from '@/composables/useStatsRange'
import { computed } from 'vue'
import { useTheme } from '@/composables/useTheme'

const { from, to } = useStatsRange()
const { theme } = useTheme()
const themeMode = computed(() => (theme.value === 'light' ? 'light' : 'dark'))
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

.stats-page-shell--light {
  background: #edf3fb;
  color: #000;
}
</style>
