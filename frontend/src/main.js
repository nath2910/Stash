import './assets/main.css'
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import VueECharts from 'vue-echarts'
import './lib/echarts'
import { MotionPlugin } from '@vueuse/motion'

if (import.meta.env.DEV) {
  console.log('VITE_API_URL =', import.meta.env.VITE_API_URL)
}

createApp(App).use(router).use(MotionPlugin).component('VChart', VueECharts).mount('#app')
