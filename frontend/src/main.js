import './assets/main.css'
import { createApp, defineAsyncComponent } from 'vue'
import App from './App.vue'
import router from './router'
import { MotionPlugin } from '@vueuse/motion'

const AsyncVChart = defineAsyncComponent(async () => {
  await import('./lib/echarts')
  const module = await import('vue-echarts')
  return module.default
})

createApp(App).use(router).use(MotionPlugin).component('VChart', AsyncVChart).mount('#app')
