<template>
  <!-- On enveloppe toutes les pages dans ton layout global -->
  <LayoutPages>
    <!-- Le router rend ici la page correspondant à l'URL -->
    <RouterView v-slot="{ Component, route }">
      <Transition
        :name="route.meta.transition || 'page'"
        :mode="route.meta.transitionMode"
      >
        <component :is="Component" :key="routeKey(route)" class="page-view" />
      </Transition>
    </RouterView>
  </LayoutPages>
</template>

<script setup>
import { useRoute } from 'vue-router'
import LayoutPages from '@/layout/layoutPages.vue'

const route = useRoute()

const stringifyParams = (params) => {
  try {
    return JSON.stringify(params || {})
  } catch {
    return ''
  }
}

const routeKey = (r) => {
  if (!r) return route.fullPath
  if (r.meta?.remountOnQuery) return r.fullPath || route.fullPath
  return `${String(r.name || r.path || route.name || route.path)}::${stringifyParams(r.params)}`
}
</script>
<style>
.page-view {
  height: 100%;
  min-width: 0;
  min-height: 0;
}

.page-enter-active {
  transition: opacity 90ms ease;
  will-change: opacity;
}

.page-leave-active {
  transition: opacity 70ms ease;
  will-change: opacity;
}

.page-enter-from,
.page-leave-to {
  opacity: 0;
}

.page-canvas-enter-active,
.page-canvas-leave-active {
  transition: opacity 90ms ease;
  will-change: opacity;
}

.page-canvas-leave-active {
  pointer-events: none;
}

.page-canvas-enter-from,
.page-canvas-leave-to {
  opacity: 0;
}

@media (prefers-reduced-motion: reduce) {
  .page-enter-active,
  .page-leave-active,
  .page-canvas-enter-active,
  .page-canvas-leave-active {
    transition: none;
  }
}
</style>
