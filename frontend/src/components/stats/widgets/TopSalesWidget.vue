<template>
  <WidgetCard
    class="ts-widget"
    title="Top ventes"
    subtitle="Plus gros benefices"
    :accent="accent"
    surface="ranking"
    :loading="loading"
    :error="error"
    :auto-height="props.autoHeight"
    :widget-width="props.widgetWidth"
    :widget-height="props.widgetHeight"
    :widget-base-width="props.widgetBaseWidth"
    :widget-base-height="props.widgetBaseHeight"
  >
    <div ref="rootEl" class="ts-root">
      <div ref="listEl" class="ts-list space-y-2">
        <div
          v-for="(x, idx) in topSales"
          :key="idx"
          class="flex items-center justify-between gap-3 rounded-xl border border-slate-800 bg-slate-950/30 px-3 py-2"
        >
          <div class="min-w-0">
            <p class="text-xs text-white/90 truncate">{{ x.nomItem }}</p>
            <p class="text-[11px] text-white/45">#{{ idx + 1 }}</p>
          </div>
          <div class="text-sm font-semibold text-emerald-300">
            {{ formatEUR(x.benefice, { compact: true }) }}
          </div>
        </div>
      </div>

      <div v-if="maxLimit > collapsedLimit" ref="actionsEl" class="ts-actions pt-3">
        <button
          type="button"
          class="inline-flex items-center justify-center gap-2 rounded-full border border-emerald-400/40 bg-gradient-to-r from-emerald-500/15 via-emerald-500/5 to-transparent px-4 py-2 text-xs font-semibold uppercase tracking-wide text-emerald-200 shadow-[0_0_20px_rgba(16,185,129,0.15)] transition hover:border-emerald-300/70 hover:text-emerald-100"
          :aria-label="expanded ? 'Reduire la liste des top ventes' : 'Afficher plus de ventes'"
          @click="expanded = !expanded"
        >
          <span>{{ expanded ? 'Reduire la liste' : 'Voir plus' }}</span>
          <span class="rounded-full bg-emerald-500/15 px-2 py-0.5 text-[10px] font-bold">
            {{ activeLimit }}
          </span>
        </button>
      </div>
    </div>
  </WidgetCard>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import StatsServices from '@/services/StatsServices'
import { normalizeTopSales } from '@/services/statsAdapters'
import WidgetCard from './_parts/WidgetCard.vue'
import { formatEUR } from '@/utils/formatters'

const emit = defineEmits(['auto-resize'])

const props = defineProps({
  from: String,
  to: String,
  limit: { type: Number, default: 5 },
  autoHeight: { type: Boolean, default: true },
  widgetWidth: { type: Number, default: 620 },
  widgetHeight: { type: Number, default: 420 },
  widgetBaseWidth: { type: Number, default: 0 },
  widgetBaseHeight: { type: Number, default: 0 },
  widgetRenderWidth: { type: Number, default: 0 },
  widgetRenderHeight: { type: Number, default: 0 },
  categories: { type: Array, default: () => [] },
  types: { type: Array, default: () => [] },
})
const accent = '#22C55E'

const loading = ref(false)
const error = ref('')
const topSales = ref([])
const expanded = ref(false)
const rootEl = ref(null)
const listEl = ref(null)
const actionsEl = ref(null)
const maxLimit = computed(() => Math.min(props.limit ?? 5, 15))
const collapsedLimit = computed(() => Math.min(5, maxLimit.value))
const activeLimit = computed(() => (expanded.value ? maxLimit.value : collapsedLimit.value))
const autoHeightEnabled = computed(() => props.autoHeight !== false)
let req = 0
let resizeRaf = null
let listResizeObserver = null
let lastAutoResizeHeight = 0

async function load() {
  const id = ++req
  loading.value = true
  error.value = ''
  try {
    const { data } = await StatsServices.topSales(
      props.from,
      props.to,
      activeLimit.value,
      props.categories,
      props.types,
    )
    if (id !== req) return
    topSales.value = normalizeTopSales(data)
  } catch (e) {
    if (id !== req) return
    error.value = e?.response?.data?.message ?? e?.message ?? 'Impossible de charger'
  } finally {
    if (id === req) loading.value = false
  }
}

function listContentHeight() {
  const list = listEl.value
  if (!(list instanceof HTMLElement)) return 0
  const rows = Array.from(list.children).filter((child) => child instanceof HTMLElement)
  if (!rows.length) return 0
  const first = rows[0].getBoundingClientRect()
  const last = rows[rows.length - 1].getBoundingClientRect()
  return Math.max(0, last.bottom - first.top)
}

function scheduleAutoResize() {
  if (!autoHeightEnabled.value) return
  if (resizeRaf) return
  resizeRaf = requestAnimationFrame(() => {
    resizeRaf = null
    const root = rootEl.value
    if (!(root instanceof HTMLElement)) return
    const widget = root.closest('.widget')
    if (!(widget instanceof HTMLElement)) return

    const currentRootHeight = root.getBoundingClientRect().height
    if (!Number.isFinite(currentRootHeight) || currentRootHeight <= 0) return
    const actionsHeight =
      actionsEl.value instanceof HTMLElement ? actionsEl.value.getBoundingClientRect().height : 0
    const desiredRootHeight = listContentHeight() + actionsHeight
    if (!Number.isFinite(desiredRootHeight) || desiredRootHeight <= 0) return

    const currentWidgetHeight = widget.getBoundingClientRect().height
    if (!Number.isFinite(currentWidgetHeight) || currentWidgetHeight <= 0) return
    const nextHeight = Math.round(currentWidgetHeight + (desiredRootHeight - currentRootHeight))
    if (!Number.isFinite(nextHeight) || nextHeight <= 0) return
    if (Math.abs(nextHeight - lastAutoResizeHeight) < 4) return
    lastAutoResizeHeight = nextHeight
    emit('auto-resize', nextHeight)
  })
}

function ensureListResizeObserver() {
  if (listResizeObserver) {
    listResizeObserver.disconnect()
    listResizeObserver = null
  }
  if (!autoHeightEnabled.value || !globalThis.ResizeObserver) return
  listResizeObserver = new ResizeObserver(() => scheduleAutoResize())
  if (rootEl.value instanceof HTMLElement) listResizeObserver.observe(rootEl.value)
  if (listEl.value instanceof HTMLElement) listResizeObserver.observe(listEl.value)
  if (actionsEl.value instanceof HTMLElement) listResizeObserver.observe(actionsEl.value)
}

onMounted(() => {
  load()
  nextTick(() => {
    ensureListResizeObserver()
    scheduleAutoResize()
  })
})
watch(() => [props.from, props.to, props.limit, expanded.value, props.categories, props.types], load)

watch(
  () => [
    autoHeightEnabled.value,
    topSales.value.length,
    expanded.value,
    loading.value,
    error.value,
    props.widgetRenderWidth,
    props.widgetRenderHeight,
  ],
  () => {
    nextTick(() => {
      ensureListResizeObserver()
      scheduleAutoResize()
    })
  },
)

watch(
  () => [rootEl.value, listEl.value, actionsEl.value],
  () => {
    nextTick(() => {
      ensureListResizeObserver()
      scheduleAutoResize()
    })
  },
)

onBeforeUnmount(() => {
  if (resizeRaf) {
    cancelAnimationFrame(resizeRaf)
    resizeRaf = null
  }
  if (listResizeObserver) {
    listResizeObserver.disconnect()
    listResizeObserver = null
  }
})
</script>

<style scoped>
.ts-root {
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
}

.ts-list {
  min-height: 0;
  overflow: auto;
  padding-right: 2px;
}
</style>
