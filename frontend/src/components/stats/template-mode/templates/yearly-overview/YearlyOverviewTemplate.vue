<template>
  <section class="template-page template-page--yearly-overview">
    <header class="template-overview-hero" aria-label="Synthese annuelle">
      <div class="template-overview-hero__copy">
        <p class="template-overview-hero__kicker">Dashboard annuel</p>
        <h2 class="template-overview-hero__title">Vue generale {{ props.selectedYear }}</h2>
        <p class="template-overview-hero__subtitle">
          Vision globale des performances, de la rentabilite et des signaux business clefs.
        </p>
      </div>
    </header>

    <section class="template-dashboard-grid">
      <section class="template-right-stack">
        <section class="template-kpi-band">
          <div class="template-kpi-row">
            <article v-for="kpi in props.kpiCards" :key="kpi.key" class="template-kpi-card">
              <p class="template-kpi-card__label">{{ kpi.label }}</p>
              <p class="template-kpi-card__value" :class="kpi.tone">{{ kpi.value }}</p>
            </article>
          </div>
        </section>
      </section>

      <section class="template-left-stack">
        <article class="template-panel template-panel--pie">
          <div class="template-graph-head">
            <h3 class="template-graph-title">Repartition par marque</h3>
            <p class="template-graph-subtitle">Volume cumule sur la periode</p>
          </div>
          <div class="template-pie-layout">
            <div class="template-pie-chart" :style="props.pieChartStyle"></div>
            <ul class="template-pie-legend">
              <li v-for="slice in props.pieSlices" :key="slice.label">
                <span class="template-pie-dot" :style="{ backgroundColor: slice.color }"></span>
                <span class="template-pie-label">{{ slice.label }}</span>
                <strong class="template-pie-value">{{ slice.percentText }}</strong>
              </li>
            </ul>
          </div>
        </article>
      </section>

      <section class="template-bottom-info">
        <article class="template-panel template-panel--table template-panel--brands">
          <h3 class="template-panel__title">Top marques</h3>
          <div class="template-table-wrap">
            <table class="template-table">
              <thead>
                <tr>
                  <th>Marque</th>
                  <th class="is-number">Volume</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="props.brandsRows.length">
                  <tr v-for="brand in props.brandsRows" :key="brand.label">
                    <td>{{ brand.label }}</td>
                    <td class="is-number">{{ formatNumber(brand.nb) }}</td>
                  </tr>
                </template>
                <tr v-else class="template-table__empty-row">
                  <td colspan="2">Aucune marque sur la periode.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <article class="template-panel template-panel--facts">
          <h3 class="template-panel__title">Synthese rapide</h3>
          <div class="template-facts-grid">
            <div v-for="fact in props.quickFacts" :key="fact.key" class="template-fact">
              <p class="template-fact__label">{{ fact.label }}</p>
              <p class="template-fact__value">{{ fact.value }}</p>
            </div>
          </div>
        </article>

        <article class="template-panel template-panel--table template-panel--sales">
          <h3 class="template-panel__title">Top ventes</h3>
          <div class="template-table-wrap">
            <table class="template-table">
              <thead>
                <tr>
                  <th>Produit</th>
                  <th class="is-number">Benefice</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="props.topSalesRows.length">
                  <tr v-for="item in props.topSalesRows" :key="item.nomItem">
                    <td class="truncate">{{ item.nomItem }}</td>
                    <td class="is-number">{{ formatEUR(item.benefice, { compact: true }) }}</td>
                  </tr>
                </template>
                <tr v-else class="template-table__empty-row">
                  <td colspan="2">Aucune vente sur la periode.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </section>

      <section class="template-bottom-charts">
        <article class="template-panel template-panel--trend">
          <div class="template-graph-head">
            <h3 class="template-graph-title">Tendance benefice</h3>
            <p class="template-graph-subtitle">{{ props.cadenceText }}</p>
          </div>
          <div class="template-chart-shell" :class="{ 'is-empty': props.mainDataState !== 'ready' }">
            <svg
              v-if="props.mainDataState === 'ready'"
              class="template-chart-svg"
              :viewBox="`0 0 ${TEMPLATE_CHART_W} ${TEMPLATE_CHART_H}`"
              preserveAspectRatio="none"
              role="img"
              aria-label="Courbe de benefice"
              @mousemove="emit('chart-hover', $event, 'main')"
              @mouseleave="emit('chart-clear', 'main')"
            >
              <defs>
                <linearGradient id="templateChartAreaFill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="rgba(14, 165, 233, 0.36)" />
                  <stop offset="100%" stop-color="rgba(14, 165, 233, 0.04)" />
                </linearGradient>
              </defs>
              <line
                class="template-chart-axis template-chart-axis--x"
                :x1="chartLeft"
                :y1="chartBottom"
                :x2="chartRight"
                :y2="chartBottom"
              />
              <line
                class="template-chart-axis template-chart-axis--y"
                :x1="chartLeft"
                :y1="TEMPLATE_CHART_PAD_Y"
                :x2="chartLeft"
                :y2="chartBottom"
              />
              <line
                v-for="lineY in props.chartGridLines"
                :key="`grid-${lineY}`"
                class="template-chart-grid"
                :x1="TEMPLATE_CHART_PAD_X"
                :y1="lineY"
                :x2="TEMPLATE_CHART_W - TEMPLATE_CHART_PAD_X"
                :y2="lineY"
              />
              <line
                v-for="lineY in props.chartGridLines"
                :key="`axis-main-tick-${lineY}`"
                class="template-chart-axis-tick"
                :x1="chartLeft"
                :y1="lineY"
                :x2="chartLeft - 7"
                :y2="lineY"
              />
              <line class="template-chart-axis-tick" :x1="chartLeft" :y1="chartBottom" :x2="chartLeft" :y2="chartBottom + 6" />
              <line
                class="template-chart-axis-tick"
                :x1="chartMidX"
                :y1="chartBottom"
                :x2="chartMidX"
                :y2="chartBottom + 6"
              />
              <line class="template-chart-axis-tick" :x1="chartRight" :y1="chartBottom" :x2="chartRight" :y2="chartBottom + 6" />
              <text class="template-chart-axis-label" :x="chartLeft - 10" :y="props.chartGridLines[0] + 4" text-anchor="end">
                {{ mainAxisLabels.top }}
              </text>
              <text class="template-chart-axis-label" :x="chartLeft - 10" :y="props.chartGridLines[1] + 4" text-anchor="end">
                {{ mainAxisLabels.mid }}
              </text>
              <text class="template-chart-axis-label" :x="chartLeft - 10" :y="props.chartGridLines[2] + 4" text-anchor="end">
                {{ mainAxisLabels.bottom }}
              </text>
              <text class="template-chart-axis-label template-chart-axis-label--x" :x="chartLeft" :y="chartBottom + 18" text-anchor="start">
                {{ mainXAxisStart }}
              </text>
              <text class="template-chart-axis-label template-chart-axis-label--x" :x="chartMidX" :y="chartBottom + 18" text-anchor="middle">
                {{ mainXAxisMid }}
              </text>
              <text class="template-chart-axis-label template-chart-axis-label--x" :x="chartRight" :y="chartBottom + 18" text-anchor="end">
                {{ mainXAxisEnd }}
              </text>
              <path class="template-chart-area" :d="props.mainChartAreaPath" />
              <path class="template-chart-line" :d="props.mainChartLinePath" />
              <circle
                v-if="props.mainLastPointCoord"
                class="template-chart-dot"
                :cx="props.mainLastPointCoord.x"
                :cy="props.mainLastPointCoord.y"
                r="3.8"
              />
              <circle
                v-if="props.mainHover.visible"
                class="template-chart-hover-dot"
                :cx="props.mainHover.x"
                :cy="props.mainHover.y"
                r="4.2"
              />
            </svg>
            <div v-else class="template-chart-empty template-chart-empty--compact">
              <p class="template-chart-empty__title">{{ props.emptyTitle }}</p>
            </div>
            <div
              v-if="props.mainHover.visible"
              class="template-chart-tooltip"
              :style="{ left: `${props.mainHover.xPct}%`, top: `${props.mainHover.yPct}%` }"
            >
              <strong>{{ props.mainHover.valueText }}</strong>
              <span>{{ props.mainHover.dateText }}</span>
            </div>
          </div>
          <div class="template-chart-meta">
            <div>{{ props.profitStartLabel }}</div>
            <div>{{ props.profitEndLabel }}</div>
          </div>
        </article>

        <article class="template-kpi-trend template-kpi-trend--square">
          <div class="template-kpi-trend__head">
            <h3 class="template-kpi-trend__title">Tendance CA</h3>
            <p class="template-kpi-trend__subtitle">{{ props.cadenceText }}</p>
          </div>
          <div class="template-kpi-trend__chart" :class="{ 'is-empty': props.miniDataState !== 'ready' }">
            <svg
              v-if="props.miniDataState === 'ready'"
              class="template-kpi-trend__svg"
              :viewBox="`0 0 ${TEMPLATE_CHART_W} ${TEMPLATE_CHART_H}`"
              preserveAspectRatio="none"
              role="img"
              aria-label="Mini courbe de tendance"
              @mousemove="emit('chart-hover', $event, 'mini')"
              @mouseleave="emit('chart-clear', 'mini')"
            >
              <line
                class="template-chart-axis template-chart-axis--x"
                :x1="chartLeft"
                :y1="chartBottom"
                :x2="chartRight"
                :y2="chartBottom"
              />
              <line
                class="template-chart-axis template-chart-axis--y"
                :x1="chartLeft"
                :y1="TEMPLATE_CHART_PAD_Y"
                :x2="chartLeft"
                :y2="chartBottom"
              />
              <line
                v-for="lineY in props.chartGridLines"
                :key="`kpi-trend-grid-${lineY}`"
                class="template-kpi-trend__grid"
                :x1="TEMPLATE_CHART_PAD_X"
                :y1="lineY"
                :x2="TEMPLATE_CHART_W - TEMPLATE_CHART_PAD_X"
                :y2="lineY"
              />
              <line
                v-for="lineY in props.chartGridLines"
                :key="`axis-mini-tick-${lineY}`"
                class="template-chart-axis-tick"
                :x1="chartLeft"
                :y1="lineY"
                :x2="chartLeft - 7"
                :y2="lineY"
              />
              <line class="template-chart-axis-tick" :x1="chartLeft" :y1="chartBottom" :x2="chartLeft" :y2="chartBottom + 6" />
              <line
                class="template-chart-axis-tick"
                :x1="chartMidX"
                :y1="chartBottom"
                :x2="chartMidX"
                :y2="chartBottom + 6"
              />
              <line class="template-chart-axis-tick" :x1="chartRight" :y1="chartBottom" :x2="chartRight" :y2="chartBottom + 6" />
              <text class="template-chart-axis-label" :x="chartLeft - 10" :y="props.chartGridLines[0] + 4" text-anchor="end">
                {{ miniAxisLabels.top }}
              </text>
              <text class="template-chart-axis-label" :x="chartLeft - 10" :y="props.chartGridLines[1] + 4" text-anchor="end">
                {{ miniAxisLabels.mid }}
              </text>
              <text class="template-chart-axis-label" :x="chartLeft - 10" :y="props.chartGridLines[2] + 4" text-anchor="end">
                {{ miniAxisLabels.bottom }}
              </text>
              <text class="template-chart-axis-label template-chart-axis-label--x" :x="chartLeft" :y="chartBottom + 18" text-anchor="start">
                {{ miniXAxisStart }}
              </text>
              <text class="template-chart-axis-label template-chart-axis-label--x" :x="chartMidX" :y="chartBottom + 18" text-anchor="middle">
                {{ miniXAxisMid }}
              </text>
              <text class="template-chart-axis-label template-chart-axis-label--x" :x="chartRight" :y="chartBottom + 18" text-anchor="end">
                {{ miniXAxisEnd }}
              </text>
              <path class="template-kpi-trend__area" :d="props.miniChartAreaPath" />
              <path class="template-kpi-trend__line" :d="props.miniChartLinePath" />
              <circle
                v-if="props.miniLastPointCoord"
                class="template-kpi-trend__dot"
                :cx="props.miniLastPointCoord.x"
                :cy="props.miniLastPointCoord.y"
                r="5"
              />
              <circle
                v-if="props.miniHover.visible"
                class="template-chart-hover-dot"
                :cx="props.miniHover.x"
                :cy="props.miniHover.y"
                r="5"
              />
            </svg>
            <p v-else class="template-kpi-trend__empty">{{ props.emptyTitle }}</p>
            <div
              v-if="props.miniHover.visible"
              class="template-chart-tooltip template-chart-tooltip--mini"
              :style="{ left: `${props.miniHover.xPct}%`, top: `${props.miniHover.yPct}%` }"
            >
              <strong>{{ props.miniHover.valueText }}</strong>
              <span>{{ props.miniHover.dateText }}</span>
            </div>
          </div>
        </article>

        <article class="template-panel template-panel--success template-panel--success-right">
          <h3 class="template-panel__title">Succes rate</h3>
          <div class="template-success-top">
            <div class="template-success-note">
              <p>{{ props.cadenceText }}</p>
              <strong>{{ props.totalText }}</strong>
            </div>
          </div>
          <div class="template-gauge-wrap">
            <svg class="template-gauge" viewBox="0 0 300 180" role="img" aria-label="Jauge de performance">
              <path
                class="template-gauge-track"
                d="M 30 150 A 120 120 0 0 1 270 150"
                pathLength="100"
              />
              <path
                class="template-gauge-fill"
                d="M 30 150 A 120 120 0 0 1 270 150"
                pathLength="100"
                :stroke-dasharray="props.gaugeDasharray"
              />
              <text x="150" y="132" text-anchor="middle" class="template-gauge-value">
                {{ props.successRateText }}
              </text>
            </svg>
            <div class="template-gauge-labels">
              <span>0</span>
              <span>100</span>
            </div>
          </div>
        </article>
      </section>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatEUR, formatNumber } from '@/utils/formatters'
import { TEMPLATE_CHART_H, TEMPLATE_CHART_PAD_X, TEMPLATE_CHART_PAD_Y, TEMPLATE_CHART_W } from '../../templateModeEngine'
import type { TemplateChartTarget, TemplateSharedViewProps } from '../templateViewTypes'

const props = defineProps<TemplateSharedViewProps>()

const emit = defineEmits<{
  (e: 'year-change', event: Event): void
  (e: 'month-select', monthIndex: number, event: MouseEvent): void
  (e: 'chart-hover', event: MouseEvent, target: TemplateChartTarget): void
  (e: 'chart-clear', target: TemplateChartTarget): void
}>()

const chartLeft = TEMPLATE_CHART_PAD_X
const chartRight = TEMPLATE_CHART_W - TEMPLATE_CHART_PAD_X
const chartBottom = TEMPLATE_CHART_H - TEMPLATE_CHART_PAD_Y
const chartMidX = TEMPLATE_CHART_W / 2

function formatAxisDate(value: string) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(String(value ?? ''))) return '--'
  const date = new Date(`${value}T00:00:00`)
  if (Number.isNaN(date.getTime())) return '--'
  return date.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' })
}

function buildYAxisLabels(values: number[]) {
  if (!values.length) return { top: '--', mid: '--', bottom: '--' }
  const min = Math.min(...values)
  const max = Math.max(...values)
  const mid = (min + max) / 2
  return {
    top: formatEUR(max, { compact: true }),
    mid: formatEUR(mid, { compact: true }),
    bottom: formatEUR(min, { compact: true }),
  }
}

function buildXAxisLabels(points: Array<{ date: string }>) {
  if (!points.length) return { start: '--', mid: '--', end: '--' }
  const midIndex = Math.floor((points.length - 1) / 2)
  return {
    start: formatAxisDate(points[0]?.date ?? ''),
    mid: formatAxisDate(points[midIndex]?.date ?? ''),
    end: formatAxisDate(points[points.length - 1]?.date ?? ''),
  }
}

const mainAxisLabels = computed(() => buildYAxisLabels((props.profitSeries ?? []).map((point) => Number(point.value || 0))))
const miniAxisLabels = computed(() => buildYAxisLabels((props.series ?? []).map((point) => Number(point.value || 0))))

const mainXAxisLabels = computed(() => buildXAxisLabels(props.profitSeries ?? []))
const miniXAxisLabels = computed(() => buildXAxisLabels(props.series ?? []))

const mainXAxisStart = computed(() => mainXAxisLabels.value.start)
const mainXAxisMid = computed(() => mainXAxisLabels.value.mid)
const mainXAxisEnd = computed(() => mainXAxisLabels.value.end)

const miniXAxisStart = computed(() => miniXAxisLabels.value.start)
const miniXAxisMid = computed(() => miniXAxisLabels.value.mid)
const miniXAxisEnd = computed(() => miniXAxisLabels.value.end)
</script>
