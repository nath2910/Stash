import './assets/main.css'
import { createApp, defineAsyncComponent } from 'vue'
import App from './App.vue'
import router from './router'
import { MotionPlugin } from '@vueuse/motion'

if (import.meta.env.DEV) {
  console.log('VITE_API_URL =', import.meta.env.VITE_API_URL)
}

const AsyncVChart = defineAsyncComponent(async () => {
  await import('./lib/echarts')
  const module = await import('vue-echarts')
  return module.default
})

createApp(App).use(router).use(MotionPlugin).component('VChart', AsyncVChart).mount('#app')
