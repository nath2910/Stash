<template>
  <!-- On enveloppe toutes les pages dans ton layout global -->
  <LayoutPages>
    <!-- Le router rend ici la page correspondant à l'URL -->
    <RouterView v-slot="{ Component, route }">
      <Transition
        :name="route.meta.transition || 'page'"
        :mode="route.meta.transitionMode || 'out-in'"
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

const routeKey = (r) => {
  if (r?.name === 'auth') return 'auth'
  return r?.fullPath || route.fullPath
}
</script>
<style>
.page-view {
  height: 100%;
}

.page-enter-active {
  transition: opacity 180ms cubic-bezier(0.22, 1, 0.36, 1),
    transform 180ms cubic-bezier(0.22, 1, 0.36, 1);
  will-change: opacity, transform;
}

.page-leave-active {
  transition: opacity 140ms ease-in, transform 140ms ease-in;
  will-change: opacity, transform;
}

.page-enter-from,
.page-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

.page-canvas-enter-active,
.page-canvas-leave-active {
  transition: opacity 160ms ease;
  will-change: opacity;
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
