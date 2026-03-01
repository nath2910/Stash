<template>
  <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
    <DashboardHeader
      subtitle="Analytics • Stash"
      title="Statistiques"
      description="Dashboard modulable : zoom, déplacement, widgets."
    />

    <!-- Barre période (simple, propre, pas de sidebar) -->
    <div class="flex flex-col sm:flex-row gap-3 sm:items-center">
      <div class="flex gap-2 items-center">
        <input v-model="from" type="date" class="input" />
        <span class="text-gray-500 text-sm">→</span>
        <input v-model="to" type="date" class="input" />
      </div>

      <div class="sm:ml-auto flex gap-2">
        <button class="btn" @click="resetMonth">Mois en cours</button>
        <button class="btn btn-ghost" @click="resetLayout">Reset layout</button>
      </div>
    </div>

    <!-- CANVAS Lucidchart-like -->
    <StatsCanvas :from="from" :to="to" />
  </div>
</template>

<script setup>
import DashboardHeader from '@/components/HeaderDePage.vue'
import StatsCanvas from '@/components/stats/canvas/StatsCanvas.vue'
import { useStatsRange } from '@/composables/useStatsRange'

const { from, to } = useStatsRange()

function resetMonth() {
  const now = new Date()
  from.value = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10)
  to.value = new Date(now.getFullYear(), now.getMonth() + 1, 0).toISOString().slice(0, 10)
}

function resetLayout() {
  localStorage.removeItem('snk_stats_canvas_layout_v1')
  window.location.reload()
}
</script>

<style scoped>
.input {
  background: rgba(17, 24, 39, 0.85);
  border: 1px solid rgba(55, 65, 81, 0.9);
  border-radius: 14px;
  padding: 10px 12px;
  color: #e5e7eb;
}

.btn {
  padding: 10px 14px;
  border-radius: 14px;
  border: 1px solid rgba(55, 65, 81, 0.9);
  background: rgba(17, 24, 39, 0.75);
  color: #e5e7eb;
}
.btn:hover {
  background: rgba(31, 41, 55, 0.95);
}

.btn-ghost {
  background: transparent;
}
.btn-ghost:hover {
  background: rgba(31, 41, 55, 0.35);
}
</style>
