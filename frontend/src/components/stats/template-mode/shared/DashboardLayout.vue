<template>
  <section
    ref="layoutRef"
    class="dashboard-layout"
    :class="{
      'dashboard-layout--fit-screen': fitScreen,
      'dashboard-layout--fit-compact': fitScreen && fitDensity === 'compact',
      'dashboard-layout--fit-tight': fitScreen && fitDensity === 'tight',
    }"
    :style="layoutStyle"
    :aria-label="title"
  >
    <div class="dashboard-layout__fit" :style="fitWrapperStyle">
    <div ref="innerRef" class="dashboard-layout__inner">
      <header ref="headerRef" class="dashboard-layout__header">
        <div class="dashboard-layout__copy">
          <div class="dashboard-layout__meta">
            <p class="dashboard-layout__kicker">{{ kicker }}</p>
            <span v-if="periodLabel" class="dashboard-layout__period">{{ periodLabel }}</span>
          </div>
          <h1>{{ title }}</h1>
          <p class="dashboard-layout__description">{{ description }}</p>
        </div>

        <div v-if="$slots.selector" class="dashboard-layout__selector">
          <slot name="selector" />
        </div>
      </header>

      <div v-if="$slots.navigation" ref="navigationRef" class="dashboard-layout__navigation">
        <slot name="navigation" />
      </div>

      <template v-if="showUnifiedContent">
        <section
          ref="kpiSurfaceRef"
          class="dashboard-layout__surface dashboard-layout__surface--kpis"
          aria-label="KPI principaux"
        >
          <div ref="kpiGridRef" class="dashboard-layout__kpi-grid">
            <article
              v-for="card in kpis"
              :key="card.label"
              class="dashboard-layout__kpi-card"
              :class="`is-${card.tone || 'neutral'}`"
            >
              <div class="dashboard-layout__kpi-head">
                <span class="dashboard-layout__kpi-label">{{ card.label }}</span>
                <component
                  :is="card.icon"
                  v-if="card.icon"
                  class="dashboard-layout__kpi-icon"
                  aria-hidden="true"
                />
              </div>
              <strong class="dashboard-layout__kpi-value">{{ card.value }}</strong>
              <span v-if="card.detail" class="dashboard-layout__kpi-detail">{{ card.detail }}</span>
            </article>
          </div>
        </section>

        <section
          ref="moduleSurfaceRef"
          class="dashboard-layout__surface dashboard-layout__surface--modules"
          aria-label="Plan d'action"
        >
          <div ref="moduleHeadRef" class="dashboard-layout__section-head">
            <div>
              <p class="dashboard-layout__section-kicker">{{ analyticsKicker }}</p>
              <h2>{{ analyticsTitle }}</h2>
            </div>
            <span v-if="analyticsMeta" class="dashboard-layout__section-meta">{{ analyticsMeta }}</span>
          </div>

          <div v-if="modules.length" ref="moduleGridRef" class="dashboard-layout__module-grid">
            <article
              v-for="module in modules"
              :key="`${module.title}-${module.badge || module.value}`"
              class="dashboard-layout__module-card"
              :class="`is-${module.tone || 'neutral'}`"
            >
              <span v-if="module.badge" class="dashboard-layout__module-badge">{{ module.badge }}</span>
              <h3>{{ module.title }}</h3>
              <strong class="dashboard-layout__module-value">{{ module.value }}</strong>
              <p v-if="module.detail" class="dashboard-layout__module-detail">{{ module.detail }}</p>
            </article>
          </div>

          <div v-else class="dashboard-layout__module-empty">
            Aucun module disponible pour cette periode.
          </div>
        </section>
      </template>

      <slot v-else />
    </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, onUpdated, ref, watch } from 'vue'

type DashboardKpiTone = 'primary' | 'profit' | 'warning' | 'neutral'
type DashboardModuleTone = 'positive' | 'warning' | 'neutral' | 'profit' | 'primary'

type DashboardKpi = {
  label: string
  value: string
  detail?: string
  tone?: DashboardKpiTone
  icon?: unknown | null
}

type DashboardModule = {
  badge?: string
  title: string
  value: string
  detail?: string
  tone?: DashboardModuleTone
}

const props = withDefaults(
  defineProps<{
    kicker?: string
    title: string
    description: string
    periodLabel?: string
    analyticsKicker?: string
    analyticsTitle: string
    analyticsMeta?: string
    kpis?: DashboardKpi[]
    modules?: DashboardModule[]
    showUnifiedContent?: boolean
    fitScreen?: boolean
  }>(),
  {
    kicker: 'Template',
    periodLabel: '',
    analyticsKicker: 'Pilotage',
    analyticsMeta: '',
    kpis: () => [],
    modules: () => [],
    showUnifiedContent: true,
    fitScreen: false,
  },
)

const layoutRef = ref<HTMLElement | null>(null)
const innerRef = ref<HTMLElement | null>(null)
const headerRef = ref<HTMLElement | null>(null)
const navigationRef = ref<HTMLElement | null>(null)
const kpiSurfaceRef = ref<HTMLElement | null>(null)
const kpiGridRef = ref<HTMLElement | null>(null)
const moduleSurfaceRef = ref<HTMLElement | null>(null)
const moduleHeadRef = ref<HTMLElement | null>(null)
const moduleGridRef = ref<HTMLElement | null>(null)
const fitStyles = ref<Record<string, string> | undefined>(undefined)
const fitDensity = ref<'normal' | 'compact' | 'tight'>('normal')
const fitAvailableHeight = ref<number | null>(null)

let resizeFrame = 0
let resizeObserver: ResizeObserver | null = null

const layoutStyle = computed(() => {
  if (!props.fitScreen || !fitStyles.value) {
    return undefined
  }

  return {
    ...fitStyles.value,
    height: fitAvailableHeight.value != null ? `${Math.round(fitAvailableHeight.value)}px` : undefined,
    minHeight: fitAvailableHeight.value != null ? `${Math.round(fitAvailableHeight.value)}px` : undefined,
  }
})

const fitWrapperStyle = computed(() => undefined)

const readPx = (styles: CSSStyleDeclaration, property: string, fallback = 0) => {
  const raw = styles.getPropertyValue(property)
  const value = Number.parseFloat(raw)
  return Number.isFinite(value) ? value : fallback
}

const clamp = (value: number, min: number, max: number) => Math.min(max, Math.max(min, value))

const resolveColumns = (width: number, minWidth: number, gap: number, itemCount: number) => {
  if (itemCount <= 1) {
    return 1
  }

  const safeWidth = Math.max(width, minWidth)
  const safeGap = Math.max(gap, 0)
  const raw = Math.floor((safeWidth + safeGap) / (minWidth + safeGap))
  return clamp(raw || 1, 1, itemCount)
}

const measureFit = () => {
  if (!props.fitScreen) {
    fitStyles.value = undefined
    fitDensity.value = 'normal'
    fitAvailableHeight.value = null
    return
  }

  const layout = layoutRef.value
  const inner = innerRef.value
  if (!layout || !inner || !props.showUnifiedContent) {
    fitStyles.value = undefined
    fitDensity.value = 'normal'
    fitAvailableHeight.value = null
    return
  }

  const header = headerRef.value
  const navigation = navigationRef.value
  const kpiSurface = kpiSurfaceRef.value
  const kpiGrid = kpiGridRef.value
  const moduleSurface = moduleSurfaceRef.value
  const moduleHead = moduleHeadRef.value
  const moduleGrid = moduleGridRef.value

  if (!header || !kpiSurface || !kpiGrid || !moduleSurface || !moduleHead || !moduleGrid) {
    fitStyles.value = undefined
    fitDensity.value = 'normal'
    fitAvailableHeight.value = null
    return
  }

  const viewportHeight = window.visualViewport?.height ?? window.innerHeight
  const layoutRect = layout.getBoundingClientRect()
  const availableHeight = Math.max(520, viewportHeight - Math.max(layoutRect.top, 0) - 10)
  fitAvailableHeight.value = availableHeight
  const innerStyles = window.getComputedStyle(inner)
  const innerPaddingTop = readPx(innerStyles, 'padding-top')
  const innerPaddingBottom = readPx(innerStyles, 'padding-bottom')
  const innerGap = readPx(innerStyles, 'row-gap', readPx(innerStyles, 'gap'))
  const navigationHeight = navigation?.offsetHeight ?? 0
  const sectionGapCount = navigationHeight > 0 ? 3 : 2
  const staticHeight =
    innerPaddingTop +
    innerPaddingBottom +
    header.offsetHeight +
    navigationHeight +
    innerGap * sectionGapCount

  const contentBudget = Math.max(260, availableHeight - staticHeight)

  const rootStyles = window.getComputedStyle(layout)
  const kpiMinWidth = readPx(rootStyles, '--dashboard-kpi-min-width', 220)
  const moduleMinWidth = readPx(rootStyles, '--dashboard-module-min-width', 210)
  const kpiTargetHeight = readPx(rootStyles, '--dashboard-kpi-min-height', 148)

  const kpiGridStyles = window.getComputedStyle(kpiGrid)
  const moduleGridStyles = window.getComputedStyle(moduleGrid)
  const kpiRowGap = readPx(kpiGridStyles, 'row-gap', readPx(kpiGridStyles, 'gap'))
  const kpiColGap = readPx(kpiGridStyles, 'column-gap', readPx(kpiGridStyles, 'gap'))
  const moduleRowGap = readPx(moduleGridStyles, 'row-gap', readPx(moduleGridStyles, 'gap'))
  const moduleColGap = readPx(moduleGridStyles, 'column-gap', readPx(moduleGridStyles, 'gap'))

  const kpiColumns = resolveColumns(kpiGrid.clientWidth, kpiMinWidth, kpiColGap, Math.max(props.kpis.length, 1))
  const moduleColumns = resolveColumns(
    moduleGrid.clientWidth,
    moduleMinWidth,
    moduleColGap,
    Math.max(props.modules.length, 1),
  )
  const kpiRows = Math.max(1, Math.ceil(Math.max(props.kpis.length, 1) / kpiColumns))
  const moduleRows = Math.max(1, Math.ceil(Math.max(props.modules.length, 1) / moduleColumns))

  const kpiSurfaceStyles = window.getComputedStyle(kpiSurface)
  const moduleSurfaceStyles = window.getComputedStyle(moduleSurface)
  const kpiSurfacePadding =
    readPx(kpiSurfaceStyles, 'padding-top') + readPx(kpiSurfaceStyles, 'padding-bottom')
  const moduleSurfacePadding =
    readPx(moduleSurfaceStyles, 'padding-top') + readPx(moduleSurfaceStyles, 'padding-bottom')
  const moduleHeadHeight = moduleHead.offsetHeight

  const availableCardsHeight =
    contentBudget -
    kpiSurfacePadding -
    moduleSurfacePadding -
    moduleHeadHeight -
    kpiRowGap * Math.max(kpiRows - 1, 0) -
    moduleRowGap * Math.max(moduleRows - 1, 0)

  const preferredModuleHeight = 154
  const naturalCardHeight = kpiRows * kpiTargetHeight + moduleRows * preferredModuleHeight
  const fitRatio = clamp(availableCardsHeight / Math.max(naturalCardHeight, 1), 0.72, 1)

  let nextKpiHeight = clamp(kpiTargetHeight * fitRatio, 88, kpiTargetHeight)
  let nextModuleHeight = clamp(preferredModuleHeight * fitRatio, 104, preferredModuleHeight)

  const totalHeightFor = (kpiHeight: number, moduleHeight: number) =>
    kpiSurfacePadding +
    kpiRows * kpiHeight +
    kpiRowGap * Math.max(kpiRows - 1, 0) +
    moduleSurfacePadding +
    moduleHeadHeight +
    moduleRows * moduleHeight +
    moduleRowGap * Math.max(moduleRows - 1, 0)

  let totalHeight = totalHeightFor(nextKpiHeight, nextModuleHeight)
  if (totalHeight > contentBudget) {
    const overflow = totalHeight - contentBudget
    const rowWeight = kpiRows + moduleRows * 1.15
    nextKpiHeight = Math.max(84, nextKpiHeight - overflow / Math.max(rowWeight, 1))
    nextModuleHeight = Math.max(96, nextModuleHeight - (overflow * 1.15) / Math.max(rowWeight, 1))
    totalHeight = totalHeightFor(nextKpiHeight, nextModuleHeight)
  }

  const kpiSurfaceHeight =
    kpiSurfacePadding + nextKpiHeight * kpiRows + kpiRowGap * Math.max(kpiRows - 1, 0)
  const moduleSurfaceHeight =
    moduleSurfacePadding +
    moduleHeadHeight +
    nextModuleHeight * moduleRows +
    moduleRowGap * Math.max(moduleRows - 1, 0)

  const densityScale = clamp(
    Math.min(nextKpiHeight / Math.max(kpiTargetHeight, 1), nextModuleHeight / preferredModuleHeight),
    0.72,
    1,
  )

  fitDensity.value = densityScale <= 0.8 ? 'tight' : densityScale <= 0.92 ? 'compact' : 'normal'
  fitStyles.value = {
    '--dashboard-fit-scale': densityScale.toFixed(3),
    '--dashboard-fit-kpi-card-height': `${Math.round(nextKpiHeight)}px`,
    '--dashboard-fit-module-card-height': `${Math.round(nextModuleHeight)}px`,
    '--dashboard-fit-kpi-surface-height': `${Math.round(kpiSurfaceHeight)}px`,
    '--dashboard-fit-module-surface-height': `${Math.round(moduleSurfaceHeight)}px`,
    '--dashboard-fit-available-height': `${Math.round(availableHeight)}px`,
  }
}

const scheduleMeasure = () => {
  if (resizeFrame) {
    cancelAnimationFrame(resizeFrame)
  }

  resizeFrame = requestAnimationFrame(() => {
    resizeFrame = 0
    void nextTick(measureFit)
  })
}

watch(
  () => [props.fitScreen, props.showUnifiedContent, props.kpis.length, props.modules.length],
  () => {
    scheduleMeasure()
  },
)

onMounted(() => {
  scheduleMeasure()

  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      scheduleMeasure()
    })

    if (layoutRef.value) {
      resizeObserver.observe(layoutRef.value)
    }

    if (innerRef.value) {
      resizeObserver.observe(innerRef.value)
    }
  }

  window.addEventListener('resize', scheduleMeasure)
  window.visualViewport?.addEventListener('resize', scheduleMeasure)
})

onUpdated(() => {
  scheduleMeasure()
})

onBeforeUnmount(() => {
  if (resizeFrame) {
    cancelAnimationFrame(resizeFrame)
  }

  resizeObserver?.disconnect()
  window.removeEventListener('resize', scheduleMeasure)
  window.visualViewport?.removeEventListener('resize', scheduleMeasure)
})
</script>

<style scoped>
.dashboard-layout {
  --dashboard-gap: clamp(1rem, 1.4vw, 1.4rem);
  --dashboard-surface-padding: clamp(1rem, 1.4vw, 1.5rem);
  --dashboard-kpi-min-width: 220px;
  --dashboard-kpi-min-height: clamp(148px, 14vw, 176px);
  --dashboard-kpi-padding: clamp(1rem, 1.25vw, 1.2rem);
  --dashboard-kpi-value-size: clamp(2rem, 2.4vw, 2.8rem);
  --dashboard-kpi-detail-size: 0.92rem;
  --dashboard-module-min-width: 210px;
  --dashboard-module-padding: clamp(0.95rem, 1.2vw, 1.1rem);
  --dashboard-module-value-size: clamp(1.3rem, 1.8vw, 2rem);
  --dashboard-module-detail-size: 0.86rem;
  width: 100%;
}

.dashboard-layout__fit {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.dashboard-layout__inner {
  width: min(100%, 1840px);
  margin: 0 auto;
  padding: clamp(1rem, 1.8vw, 2rem);
  display: grid;
  gap: var(--dashboard-gap);
  box-sizing: border-box;
}

.dashboard-layout__header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 420px);
  gap: 1.25rem;
  align-items: start;
}

.dashboard-layout__copy,
.dashboard-layout__navigation,
.dashboard-layout__selector {
  min-width: 0;
}

.dashboard-layout__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.55rem;
}

.dashboard-layout__kicker {
  color: #64748b;
  font-size: 0.78rem;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.dashboard-layout__period {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  border: 1px solid rgba(99, 102, 241, 0.18);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.84);
  padding: 0.25rem 0.75rem;
  color: #4338ca;
  font-size: 0.78rem;
  font-weight: 800;
  white-space: nowrap;
}

.dashboard-layout__copy h1 {
  margin: 0;
  color: #111827;
  font-size: clamp(2rem, 3.1vw, 3.2rem);
  line-height: 0.96;
  font-weight: 900;
  letter-spacing: -0.04em;
}

.dashboard-layout__description {
  margin: 0.7rem 0 0;
  max-width: 62rem;
  color: #64748b;
  font-size: clamp(0.96rem, 1.15vw, 1.08rem);
  line-height: 1.55;
}

.dashboard-layout__selector {
  justify-self: end;
  width: min(100%, 420px);
}

.dashboard-layout__surface {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 10px;
  background: #fbfaf7;
  box-shadow: 0 6px 16px rgba(31, 41, 55, 0.045);
  padding: var(--dashboard-surface-padding);
  overflow: hidden;
}

.dashboard-layout__kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, var(--dashboard-kpi-min-width)), 1fr));
  gap: clamp(0.85rem, 1vw, 1rem);
}

.dashboard-layout__kpi-card {
  min-width: 0;
  min-height: var(--dashboard-kpi-min-height);
  display: grid;
  align-content: start;
  gap: 0.8rem;
  border: 1px solid rgba(203, 213, 225, 0.85);
  border-radius: 8px;
  background: #fffdf9;
  padding: var(--dashboard-kpi-padding);
  box-shadow: none;
  overflow: hidden;
  container-type: inline-size;
}

.dashboard-layout__kpi-head {
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.dashboard-layout__kpi-label {
  min-width: 0;
  color: #64748b;
  font-size: 0.76rem;
  font-weight: 850;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  line-height: 1.16;
  overflow-wrap: anywhere;
}

.dashboard-layout__kpi-icon {
  width: 1rem;
  height: 1rem;
  flex: 0 0 auto;
  color: #4f46e5;
}

.dashboard-layout__kpi-value {
  min-width: 0;
  color: #111827;
  font-size: var(--dashboard-kpi-value-size);
  line-height: 1;
  font-weight: 900;
  letter-spacing: -0.03em;
  overflow-wrap: anywhere;
}

.dashboard-layout__kpi-detail {
  min-width: 0;
  color: #64748b;
  font-size: var(--dashboard-kpi-detail-size);
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.dashboard-layout__kpi-card.is-profit .dashboard-layout__kpi-value,
.dashboard-layout__kpi-card.is-profit .dashboard-layout__kpi-icon {
  color: #047857;
}

.dashboard-layout__kpi-card.is-warning .dashboard-layout__kpi-value,
.dashboard-layout__kpi-card.is-warning .dashboard-layout__kpi-icon {
  color: #c2410c;
}

.dashboard-layout__kpi-card.is-neutral .dashboard-layout__kpi-icon {
  color: #64748b;
}

.dashboard-layout__section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}

.dashboard-layout__section-kicker {
  margin: 0 0 0.35rem;
  color: #64748b;
  font-size: 0.75rem;
  font-weight: 850;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.dashboard-layout__section-head h2 {
  margin: 0;
  color: #111827;
  font-size: clamp(1.3rem, 1.8vw, 1.75rem);
  line-height: 1.05;
  font-weight: 900;
  letter-spacing: -0.03em;
}

.dashboard-layout__section-meta {
  color: #4338ca;
  font-size: 0.82rem;
  font-weight: 800;
  white-space: normal;
  text-align: right;
}

.dashboard-layout__module-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, var(--dashboard-module-min-width)), 1fr));
  gap: clamp(0.8rem, 1vw, 1rem);
}

.dashboard-layout__module-card {
  min-width: 0;
  display: grid;
  align-content: start;
  gap: 0.55rem;
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 8px;
  background: #fffdf9;
  padding: var(--dashboard-module-padding);
  overflow: hidden;
  container-type: inline-size;
}

.dashboard-layout__module-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  max-width: 100%;
  min-height: 1.6rem;
  border-radius: 999px;
  background: rgba(226, 232, 240, 0.72);
  padding: 0.15rem 0.55rem;
  color: #475569;
  font-size: 0.68rem;
  font-weight: 850;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.dashboard-layout__module-card h3 {
  margin: 0;
  color: #334155;
  font-size: 0.96rem;
  line-height: 1.3;
  font-weight: 800;
  overflow-wrap: anywhere;
}

.dashboard-layout__module-value {
  min-width: 0;
  color: #111827;
  font-size: var(--dashboard-module-value-size);
  line-height: 1.08;
  font-weight: 900;
  letter-spacing: -0.03em;
  overflow-wrap: anywhere;
}

.dashboard-layout__module-detail {
  margin: 0;
  color: #64748b;
  font-size: var(--dashboard-module-detail-size);
  line-height: 1.45;
  overflow-wrap: anywhere;
}

@container (max-width: 240px) {
  .dashboard-layout__kpi-card {
    min-height: 104px;
    gap: 0.6rem;
  }

  .dashboard-layout__kpi-label {
    font-size: 0.68rem;
  }

  .dashboard-layout__kpi-icon {
    width: 0.92rem;
    height: 0.92rem;
  }

  .dashboard-layout__kpi-value {
    font-size: clamp(1.42rem, 11cqi, 1.86rem);
  }

  .dashboard-layout__kpi-detail {
    font-size: 0.78rem;
    line-height: 1.3;
  }
}

@container (max-width: 220px) {
  .dashboard-layout__module-card {
    gap: 0.42rem;
  }

  .dashboard-layout__module-badge {
    min-height: 1.3rem;
    padding-inline: 0.42rem;
    font-size: 0.6rem;
  }

  .dashboard-layout__module-card h3 {
    font-size: 0.84rem;
    line-height: 1.18;
  }

  .dashboard-layout__module-value {
    font-size: clamp(1rem, 9.6cqi, 1.34rem);
  }

  .dashboard-layout__module-detail {
    font-size: 0.76rem;
    line-height: 1.28;
  }
}

.dashboard-layout__module-card.is-positive .dashboard-layout__module-value,
.dashboard-layout__module-card.is-positive .dashboard-layout__module-badge,
.dashboard-layout__module-card.is-profit .dashboard-layout__module-value,
.dashboard-layout__module-card.is-profit .dashboard-layout__module-badge {
  color: #047857;
}

.dashboard-layout__module-card.is-positive .dashboard-layout__module-badge,
.dashboard-layout__module-card.is-profit .dashboard-layout__module-badge {
  background: rgba(209, 250, 229, 0.88);
}

.dashboard-layout__module-card.is-warning .dashboard-layout__module-value,
.dashboard-layout__module-card.is-warning .dashboard-layout__module-badge {
  color: #c2410c;
}

.dashboard-layout__module-card.is-warning .dashboard-layout__module-badge {
  background: rgba(254, 215, 170, 0.72);
}

.dashboard-layout__module-card.is-primary .dashboard-layout__module-value,
.dashboard-layout__module-card.is-primary .dashboard-layout__module-badge {
  color: #4338ca;
}

.dashboard-layout__module-card.is-primary .dashboard-layout__module-badge {
  background: rgba(224, 231, 255, 0.9);
}

.dashboard-layout__module-empty {
  border: 1px dashed rgba(148, 163, 184, 0.55);
  border-radius: 8px;
  padding: 1.1rem;
  color: #64748b;
  font-size: 0.92rem;
  font-weight: 700;
  text-align: center;
}

.dashboard-layout--fit-screen {
  --dashboard-gap: calc(clamp(0.82rem, 1vw, 1.02rem) * var(--dashboard-fit-scale, 1));
  --dashboard-surface-padding: calc(clamp(0.88rem, 1vw, 1.05rem) * var(--dashboard-fit-scale, 1));
  --dashboard-kpi-padding: calc(clamp(0.84rem, 0.96vw, 0.98rem) * var(--dashboard-fit-scale, 1));
  --dashboard-kpi-value-size: clamp(1.45rem, calc(2.05rem * var(--dashboard-fit-scale, 1)), 2.28rem);
  --dashboard-kpi-detail-size: clamp(0.72rem, calc(0.84rem * var(--dashboard-fit-scale, 1)), 0.84rem);
  --dashboard-module-padding: calc(clamp(0.82rem, 0.94vw, 0.98rem) * var(--dashboard-fit-scale, 1));
  --dashboard-module-value-size: clamp(0.98rem, calc(1.34rem * var(--dashboard-fit-scale, 1)), 1.52rem);
  --dashboard-module-detail-size: clamp(0.72rem, calc(0.82rem * var(--dashboard-fit-scale, 1)), 0.82rem);
  overflow: hidden;
}

.dashboard-layout--fit-screen .dashboard-layout__inner {
  min-height: min(100%, var(--dashboard-fit-available-height, calc(100dvh - 3.6rem)));
  padding-top: calc(0.92rem * var(--dashboard-fit-scale, 1));
  padding-bottom: calc(0.96rem * var(--dashboard-fit-scale, 1));
}

.dashboard-layout--fit-screen .dashboard-layout__header {
  gap: calc(1rem * var(--dashboard-fit-scale, 1));
}

.dashboard-layout--fit-screen .dashboard-layout__meta {
  margin-bottom: calc(0.35rem * var(--dashboard-fit-scale, 1));
}

.dashboard-layout--fit-screen .dashboard-layout__copy h1 {
  font-size: clamp(1.82rem, calc(2.8rem * var(--dashboard-fit-scale, 1)), 3rem);
}

.dashboard-layout--fit-screen .dashboard-layout__description {
  margin-top: calc(0.45rem * var(--dashboard-fit-scale, 1));
  font-size: clamp(0.82rem, calc(0.96rem * var(--dashboard-fit-scale, 1)), 1rem);
  line-height: 1.34;
}

.dashboard-layout--fit-screen .dashboard-layout__section-head {
  margin-bottom: calc(0.78rem * var(--dashboard-fit-scale, 1));
}

.dashboard-layout--fit-screen .dashboard-layout__section-head h2 {
  font-size: clamp(1rem, calc(1.26rem * var(--dashboard-fit-scale, 1)), 1.4rem);
}

.dashboard-layout--fit-screen .dashboard-layout__surface--kpis {
  min-height: var(--dashboard-fit-kpi-surface-height, auto);
}

.dashboard-layout--fit-screen .dashboard-layout__surface--modules {
  min-height: var(--dashboard-fit-module-surface-height, auto);
}

.dashboard-layout--fit-screen .dashboard-layout__kpi-card {
  min-height: var(--dashboard-fit-kpi-card-height, var(--dashboard-kpi-min-height));
}

.dashboard-layout--fit-screen .dashboard-layout__module-card {
  min-height: var(--dashboard-fit-module-card-height, 0);
}

.dashboard-layout--fit-screen .dashboard-layout__kpi-card,
.dashboard-layout--fit-screen .dashboard-layout__module-card {
  gap: 0.45rem;
}

.dashboard-layout--fit-screen .dashboard-layout__module-card h3 {
  font-size: clamp(0.84rem, 0.9vw, 0.92rem);
  line-height: 1.22;
}

.dashboard-layout--fit-screen .dashboard-layout__module-badge {
  min-height: 1.4rem;
  padding-inline: 0.48rem;
  font-size: 0.62rem;
}

.dashboard-layout--fit-screen .dashboard-layout__kpi-detail,
.dashboard-layout--fit-screen .dashboard-layout__module-detail {
  display: -webkit-box;
  overflow: hidden;
  text-overflow: ellipsis;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.dashboard-layout--fit-compact .dashboard-layout__description {
  max-width: 54rem;
}

.dashboard-layout--fit-tight .dashboard-layout__description {
  display: none;
}

.dashboard-layout--fit-tight .dashboard-layout__module-card h3 {
  font-size: 0.84rem;
  line-height: 1.18;
}

.dashboard-layout--fit-tight .dashboard-layout__module-detail {
  -webkit-line-clamp: 1;
}

@media (min-width: 1360px) {
  .dashboard-layout--fit-screen .dashboard-layout__kpi-grid {
    grid-template-columns: repeat(auto-fit, minmax(min(100%, 176px), 1fr));
  }

  .dashboard-layout--fit-screen .dashboard-layout__module-grid {
    grid-template-columns: repeat(auto-fit, minmax(min(100%, 220px), 1fr));
  }
}

@media (min-width: 1200px) and (max-height: 980px) {
  .dashboard-layout--fit-screen {
    --dashboard-gap: 0.68rem;
    --dashboard-surface-padding: 0.78rem;
    --dashboard-kpi-min-height: clamp(104px, 11dvh, 134px);
    --dashboard-kpi-padding: 0.74rem;
    --dashboard-kpi-value-size: clamp(1.48rem, 1.75vw, 2.05rem);
    --dashboard-module-padding: 0.76rem;
    --dashboard-module-value-size: clamp(1rem, 1.28vw, 1.36rem);
    --dashboard-module-detail-size: 0.74rem;
  }

  .dashboard-layout--fit-screen .dashboard-layout__inner {
    padding-top: 0.68rem;
    padding-bottom: 0.8rem;
  }

  .dashboard-layout--fit-screen .dashboard-layout__copy h1 {
    font-size: clamp(1.8rem, 2.18vw, 2.45rem);
  }

  .dashboard-layout--fit-screen .dashboard-layout__description {
    font-size: 0.9rem;
  }
}

@media (max-width: 1279px) {
  .dashboard-layout__header {
    grid-template-columns: minmax(0, 1fr);
  }

  .dashboard-layout__selector {
    justify-self: stretch;
    width: 100%;
    max-width: none;
  }
}

@media (max-width: 1024px) {
  .dashboard-layout__kpi-grid,
  .dashboard-layout__module-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .dashboard-layout__inner {
    padding: 0.9rem;
    gap: 0.8rem;
  }

  .dashboard-layout__section-head {
    display: grid;
  }

  .dashboard-layout__copy h1 {
    font-size: clamp(1.72rem, 8.8vw, 2.38rem);
    line-height: 1;
  }

  .dashboard-layout__description {
    margin-top: 0.55rem;
    font-size: 0.86rem;
    line-height: 1.42;
  }

  .dashboard-layout__surface {
    padding: 0.85rem;
  }

  .dashboard-layout__kpi-grid,
  .dashboard-layout__module-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-layout__kpi-card {
    min-height: 0;
  }
}

@media (max-width: 480px) {
  .dashboard-layout__inner {
    padding: 0.72rem;
    gap: 0.72rem;
  }

  .dashboard-layout__meta {
    gap: 0.5rem;
    margin-bottom: 0.42rem;
  }

  .dashboard-layout__kicker,
  .dashboard-layout__period {
    font-size: 0.72rem;
  }

  .dashboard-layout__period {
    min-height: 1.8rem;
    padding-inline: 0.62rem;
  }

  .dashboard-layout__copy h1 {
    font-size: clamp(1.58rem, 8.2vw, 2.08rem);
  }

  .dashboard-layout__description {
    font-size: 0.82rem;
    line-height: 1.38;
  }

  .dashboard-layout__surface {
    padding: 0.78rem;
  }
}

@media (min-width: 1600px) {
  .dashboard-layout__inner {
    width: min(100%, 1920px);
  }
}

@media (min-width: 1920px) {
  .dashboard-layout__inner {
    width: min(100%, 2040px);
  }

  .dashboard-layout__header {
    grid-template-columns: minmax(0, 1fr) minmax(340px, 460px);
  }

  .dashboard-layout__copy h1 {
    font-size: clamp(2.18rem, 2.7vw, 3.45rem);
  }

  .dashboard-layout__description {
    max-width: 68rem;
  }

  .dashboard-layout__selector {
    width: min(100%, 460px);
  }
}
</style>
