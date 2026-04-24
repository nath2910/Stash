<template>
  <section class="template-page">
    <section class="template-month-strip" aria-label="Mois">
      <div class="template-year-box">
        <label class="template-year-box__label" for="template-year-select">Annee</label>
        <select
          id="template-year-select"
          class="template-year-box__select"
          :value="props.selectedYear"
          @change="emit('year-change', $event)"
        >
          <option v-for="year in props.yearOptions" :key="`template-year-${year}`" :value="year">
            {{ year }}
          </option>
        </select>
      </div>
      <div class="template-month-strip__main">
        <div class="template-month-strip__label">Mois</div>
        <div class="template-month-strip__grid">
          <button
            v-for="month in props.monthChips"
            :key="`month-${month.index}`"
            type="button"
            class="template-month-chip"
            :class="{
              'is-active': month.active,
              'is-available': month.available,
              'is-unavailable': !month.available,
            }"
            :disabled="!month.available"
            title="Clic: 1 mois | Ctrl/Cmd+clic: plusieurs mois"
            @click="month.available && emit('month-select', month.index, $event)"
          >
            {{ month.label }}
          </button>
        </div>
      </div>
    </section>

    <section class="template-dashboard-grid">
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
              preserveAspectRatio="xMidYMid meet"
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
                v-for="lineY in props.chartGridLines"
                :key="`grid-${lineY}`"
                class="template-chart-grid"
                :x1="TEMPLATE_CHART_PAD_X"
                :y1="lineY"
                :x2="TEMPLATE_CHART_W - TEMPLATE_CHART_PAD_X"
                :y2="lineY"
              />
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
              preserveAspectRatio="xMidYMid meet"
              role="img"
              aria-label="Mini courbe de tendance"
              @mousemove="emit('chart-hover', $event, 'mini')"
              @mouseleave="emit('chart-clear', 'mini')"
            >
              <line
                v-for="lineY in props.chartGridLines"
                :key="`kpi-trend-grid-${lineY}`"
                class="template-kpi-trend__grid"
                :x1="TEMPLATE_CHART_PAD_X"
                :y1="lineY"
                :x2="TEMPLATE_CHART_W - TEMPLATE_CHART_PAD_X"
                :y2="lineY"
              />
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
import { formatEUR, formatNumber } from '@/utils/formatters'
import { TEMPLATE_CHART_H, TEMPLATE_CHART_PAD_X, TEMPLATE_CHART_W } from '../../templateModeEngine'
import type { TemplateChartTarget, TemplateSharedViewProps } from '../templateViewTypes'

const props = defineProps<TemplateSharedViewProps>()

const emit = defineEmits<{
  (e: 'year-change', event: Event): void
  (e: 'month-select', monthIndex: number, event: MouseEvent): void
  (e: 'chart-hover', event: MouseEvent, target: TemplateChartTarget): void
  (e: 'chart-clear', target: TemplateChartTarget): void
}>()
</script>
