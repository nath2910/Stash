<template>
  <!-- On enveloppe toutes les pages dans ton layout global -->
  <LayoutPages>
    <!-- Le router rend ici la page correspondant à l'URL -->
    <section v-if="appCrashed" class="app-recovery" role="alert" aria-live="assertive">
      <div class="app-recovery__card">
        <p class="app-recovery__eyebrow">MyStash</p>
        <h1>La page n'a pas charge correctement.</h1>
        <p>
          Recharge l'application. Si une ancienne version est encore en cache, MyStash ira
          chercher la version la plus recente.
        </p>
        <button type="button" @click="reloadApp">Recharger</button>
      </div>
    </section>

    <RouterView v-else v-slot="{ Component, route }">
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
import { onErrorCaptured, ref } from 'vue'
import { useRoute } from 'vue-router'
import LayoutPages from '@/layout/layoutPages.vue'

const route = useRoute()
const appCrashed = ref(false)

onErrorCaptured((error) => {
  console.error('Application render failed', error)
  appCrashed.value = true
  return false
})

function reloadApp() {
  if (typeof window === 'undefined') return
  const url = new URL(window.location.href)
  url.searchParams.set('_v', String(Date.now()))
  window.location.replace(url.toString())
}

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
.app-recovery {
  min-height: 100%;
  display: grid;
  place-items: center;
  padding: clamp(18px, 4vw, 36px);
  background: #020617;
}

.app-recovery__card {
  width: min(100%, 440px);
  border: 1px solid rgba(45, 212, 191, 0.22);
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.92);
  padding: 24px;
  color: #e2e8f0;
  box-shadow: 0 24px 70px rgba(2, 6, 23, 0.42);
}

.app-recovery__eyebrow {
  margin: 0 0 8px;
  color: #5eead4;
  font-size: 0.78rem;
  font-weight: 900;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.app-recovery h1 {
  margin: 0;
  color: #ffffff;
  font-size: clamp(1.45rem, 4vw, 2rem);
  font-weight: 900;
}

.app-recovery p:not(.app-recovery__eyebrow) {
  margin: 12px 0 0;
  color: #cbd5e1;
  line-height: 1.55;
}

.app-recovery button {
  width: 100%;
  min-height: 46px;
  margin-top: 18px;
  border: 0;
  border-radius: 8px;
  background: linear-gradient(90deg, #0f766e, #0ea5e9);
  color: #ffffff;
  font: inherit;
  font-weight: 900;
  cursor: pointer;
}

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
