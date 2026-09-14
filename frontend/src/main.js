import './assets/main.css'
import { createApp, defineAsyncComponent } from 'vue'
import App from './App.vue'
import router from './router'

if (typeof window !== 'undefined') {
  window.addEventListener('vite:preloadError', (event) => {
    event.preventDefault()
    const reloadKey = 'snk_vite_preload_reload_attempted'
    try {
      if (window.sessionStorage.getItem(reloadKey) === '1') {
        window.sessionStorage.removeItem(reloadKey)
        return
      }
      window.sessionStorage.setItem(reloadKey, '1')
    } catch {
      // Reload anyway; a stale asset reference leaves the user on a blank page.
    }
    const url = new URL(window.location.href)
    url.searchParams.set('_v', String(Date.now()))
    window.location.replace(url.toString())
  })
}

const AsyncVChart = defineAsyncComponent(async () => {
  await import('./lib/echarts')
  const module = await import('vue-echarts')
  return module.default
})

createApp(App).use(router).component('VChart', AsyncVChart).mount('#app')
