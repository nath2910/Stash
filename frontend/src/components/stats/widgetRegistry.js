// src/components/stats/widgets/widgetRegistry.js
// contient tout les reglages de chaque widget, leurs comportement leur taille etc
// aussi les types de graph dispo (number, pie ...)
import {
  TrendingUp,
  GitCompareArrows,
  LineChart,
  Gauge,
  Boxes,
  BarChart3,
  Activity,
  Trophy,
  StickyNote,
  Target,
  Radar,
  Bell,
  ListChecks,
} from 'lucide-vue-next'

// src/components/stats/widgets/widgetRegistry.js
import NetProfitWidget from './widgets/NetProfitWidget.vue'

import RoiWidget from './widgets/RoiWidget.vue'
import GrossRevenueWidget from './widgets/ChiffreDaffaireWidget.vue'
import AvgMarginWidget from './widgets/AvgMarginWidget.vue'

import InventoryValueWidget from './widgets/InventoryValueWidget.vue'
import SellThroughWidget from './widgets/SellThroughWidget.vue'
import AvgDaysToSellWidget from './widgets/AvgDaysToSellWidget.vue'
import DeathPileWidget from './widgets/DeathPileWidget.vue'
import ActiveListingsWidget from './widgets/ActiveListingsWidget.vue'

import TopProfitDriversWidget from './widgets/TopProfitDriversWidget.vue'
import AspWidget from './widgets/AspWidget.vue'
import CashFlowWidget from './widgets/CashFlowWidget.vue'
import GoalProgressWidget from './widgets/GoalProgressWidget.vue'
import VarianceToTargetWidget from './widgets/VarianceToTargetWidget.vue'
import PeriodComparisonWidget from './widgets/PeriodComparisonWidget.vue'
import ProfitBridgeWidget from './widgets/ProfitBridgeWidget.vue'
import AlertFeedWidget from './widgets/AlertFeedWidget.vue'
import ActionChecklistWidget from './widgets/ActionChecklistWidget.vue'
import MomentumWidget from './widgets/MomentumWidget.vue'
import RiskHeatWidget from './widgets/RiskHeatWidget.vue'

import BrandsWidget from './widgets/BrandsWidget.vue'
import TopSalesWidget from './widgets/TopSalesWidget.vue'
import TypeMixWidget from './widgets/TypeMixWidget.vue'
import TextTitleWidget from './widgets/TextTitleWidget.vue'
import TextBlockWidget from './widgets/TextBlockWidget.vue'

export const CATEGORY_COLORS = {
  Texte: { color: '#a855f7', glow: 'rgba(168, 85, 247, 0.2)' },
  Finance: { color: '#22c55e', glow: 'rgba(34, 197, 94, 0.2)' },
  Stock: { color: '#38bdf8', glow: 'rgba(56, 189, 248, 0.2)' },
  Performance: { color: '#f59e0b', glow: 'rgba(245, 158, 11, 0.22)' },
  Decision: { color: '#14b8a6', glow: 'rgba(20, 184, 166, 0.22)' },
  Bonus: { color: '#ec4899', glow: 'rgba(236, 72, 153, 0.22)' },
  Autres: { color: '#94a3b8', glow: 'rgba(148, 163, 184, 0.2)' },
}

export function getCategoryColor(category) {
  return CATEGORY_COLORS[category] ?? CATEGORY_COLORS.Autres
}

export const WIDGET_DEFS = [
  // ?? Structure / notes
  {
    type: 'textTitle',
    title: 'Titre',
    category: 'Texte',
    help: 'Titre de section',
    icon: StickyNote,
    component: TextTitleWidget,
    formPicker: false,
    forms: [],
    defaultSize: { w: 720, h: 150 },
    minSize: { w: 140, h: 84 },
    defaultProps: {
      content: 'Titre',
      align: 'left',
      valign: 'center',
      fontFamily: 'open-sans',
      fontSize: 52,
      size: 'lg',
      weight: 'bold',
      italic: false,
      underline: false,
      color: '#ffffff',
      padding: 'md',
      tight: true,
      autoHeight: true,
    },
    hideGlobalRange: true,
    settings: [
      { key: 'content', label: 'Texte', type: 'text' },
      { key: 'fontSize', label: 'Taille', type: 'number', min: 16, max: 620, step: 2, unit: 'px' },
      {
        key: 'align',
        label: 'Alignement',
        type: 'select',
        options: [
          { label: 'Gauche', value: 'left' },
          { label: 'Centre', value: 'center' },
          { label: 'Droite', value: 'right' },
        ],
      },
      { key: 'color', label: 'Couleur', type: 'color', help: 'Couleur du texte' },
    ],
  },
  {
    type: 'textBlock',
    title: 'Texte',
    category: 'Texte',
    help: 'Bloc de texte type document',
    icon: StickyNote,
    component: TextBlockWidget,
    formPicker: false,
    forms: [],
    defaultSize: { w: 680, h: 260 },
    minSize: { w: 220, h: 84 },
    defaultProps: {
      content: 'Ton texte ici...',
      align: 'left',
      valign: 'start',
      fontFamily: 'open-sans',
      fontSize: 17,
      size: 'md',
      weight: 'regular',
      italic: false,
      underline: false,
      color: '#e2e8f0',
      padding: 'md',
      autoHeight: true,
    },
    hideGlobalRange: true,
    settings: [
      { key: 'content', label: 'Texte', type: 'textarea' },
      { key: 'fontSize', label: 'Taille', type: 'number', min: 12, max: 620, step: 1, unit: 'px' },
      {
        key: 'align',
        label: 'Alignement',
        type: 'select',
        options: [
          { label: 'Gauche', value: 'left' },
          { label: 'Centre', value: 'center' },
          { label: 'Droite', value: 'right' },
        ],
      },
      { key: 'color', label: 'Couleur', type: 'color', help: 'Couleur du texte' },
    ],
  },
  // 💰 Finance
  {
    type: 'netProfit',
    title: 'Bénéfice net',
    category: 'Finance',
    help: 'Vue KPI ou courbe du bénéfice net',
    icon: TrendingUp,
    component: NetProfitWidget,
    forms: ['number', 'line'],
    defaultSize: { w: 660, h: 380 },
    minSize: { w: 380, h: 300 },
    defaultProps: {
      view: 'line',
      bucket: 'week',
      showComparison: true,
      showArea: true,
      smoothLine: true,
      showSalesKpi: true,
      showAvgProfitPerSale: true,
      showNetMargin: true,
      showBestPeriod: true,
      secondaryLimit: 4,
      autoHeight: false,
      categories: [],
      types: [],
    },
    settings: [
      {
        key: 'view',
        label: 'Vue',
        type: 'select',
        options: [
          { label: 'KPI', value: 'number' },
          { label: 'Courbe', value: 'line' },
        ],
      },
      {
        key: 'bucket',
        label: 'Granularite',
        type: 'select',
        options: [
          { label: 'Jour', value: 'day' },
          { label: 'Semaine', value: 'week' },
          { label: 'Mois', value: 'month' },
        ],
      },
      { key: 'showComparison', label: 'Afficher comparaison', type: 'toggle' },
      { key: 'showArea', label: 'Afficher zone sous courbe', type: 'toggle' },
      { key: 'smoothLine', label: 'Courbe lissée', type: 'toggle' },
      { key: 'showSalesKpi', label: 'Afficher KPI ventes', type: 'toggle' },
      { key: 'showAvgProfitPerSale', label: 'Afficher bénéfice moyen / vente', type: 'toggle' },
      { key: 'showNetMargin', label: 'Afficher marge nette', type: 'toggle' },
      { key: 'showBestPeriod', label: 'Afficher meilleure période', type: 'toggle' },
      {
        key: 'secondaryLimit',
        label: 'Nombre max de sous-KPI',
        type: 'number',
        min: 1,
        max: 4,
        step: 1,
      },
    ],
    categoryFilter: true,
  },
  {
    type: 'roi',
    title: 'ROI moyen',
    category: 'Finance',
    help: 'Retour sur investissement',
    icon: GitCompareArrows,
    component: RoiWidget,
    forms: ['number'],
    defaultSize: { w: 620, h: 460 },
    minSize: { w: 420, h: 220 },
    defaultProps: { autoHeight: true, categories: [], types: [] },
    settings: [],
    categoryFilter: true,
  },
  {
    type: 'grossRevenue',
    title: 'Chiffre d’affaires',
    category: 'Finance',
    help: 'Vue KPI ou graphique du CA',
    icon: LineChart,
    component: GrossRevenueWidget,
    forms: ['number', 'line'],
    defaultSize: { w: 920, h: 560 },
    minSize: { w: 700, h: 420 },
    defaultProps: { bucket: 'day', view: 'number', autoHeight: true, categories: [], types: [] },
    settings: [],
    categoryFilter: true,
  },
  {
    type: 'avgMargin',
    title: 'Marge moyenne',
    category: 'Finance',
    help: 'Marge moyenne par article',
    icon: Gauge,
    component: AvgMarginWidget,
    forms: ['number'],
    defaultSize: { w: 600, h: 420 },
    minSize: { w: 460, h: 240 },
    maxSize: { w: 980, h: 760 },
    defaultProps: { bucket: 'week', autoHeight: false, categories: [], types: [] },
    settings: [],
    categoryFilter: true,
  },
  // 📦 Stock & vélocité
  {
    type: 'inventoryValue',
    title: 'Valeur du stock',
    category: 'Stock',
    help: 'Capital immobilisé',
    icon: Boxes,
    component: InventoryValueWidget,
    forms: ['number'],
    defaultSize: { w: 520, h: 240 },
    minSize: { w: 380, h: 210 },
    defaultProps: { autoHeight: false, useGlobalRange: false, asOf: '', categories: [], types: [] },
    settings: [{ key: 'asOf', label: 'Date', type: 'date' }],
    dateMode: 'asOf',
    categoryFilter: true,
  },
  {
    type: 'sellThrough',
    title: "Taux d'ecoulement",
    category: 'Stock',
    help: 'Part du stock vendu',
    icon: BarChart3,
    component: SellThroughWidget,
    forms: ['bars'],
    defaultSize: { w: 520, h: 260 },
    minSize: { w: 420, h: 240 },
    defaultProps: { bucket: 'week', autoHeight: true, categories: [], types: [] },
    settings: [],
    categoryFilter: true,
  },
  {
    type: 'avgDaysToSell',
    title: 'Délai moyen',
    category: 'Stock',
    help: 'Days to sell',
    icon: Activity,
    component: AvgDaysToSellWidget,
    forms: ['number'],
    defaultSize: { w: 520, h: 240 },
    minSize: { w: 420, h: 220 },
    defaultProps: { bucket: 'week', autoHeight: false, categories: [], types: [] },
    settings: [],
    categoryFilter: true,
  },
  {
    type: 'deathPile',
    title: 'Stock dormant',
    category: 'Stock',
    help: 'Age des paires en stock',
    icon: Boxes,
    component: DeathPileWidget,
    forms: ['bars'],
    defaultSize: { w: 620, h: 800 },
    minSize: { w: 520, h: 360 },
    defaultProps: { autoHeight: false, categories: [], types: [] },
    settings: [],
    categoryFilter: true,
  },
  {
    type: 'activeListings',
    title: 'Annonces actives',
    category: 'Stock',
    help: 'Nb annonces actives',
    icon: Activity,
    component: ActiveListingsWidget,
    forms: ['number'],
    defaultSize: { w: 520, h: 240 },
    minSize: { w: 380, h: 210 },
    defaultProps: { bucket: 'week', autoHeight: false, categories: [], types: [] },
    settings: [],
    categoryFilter: true,
  },

  // 🚀 Performance
  {
    type: 'topProfitDrivers',
    title: 'Top profit',
    category: 'Performance',
    help: 'Ce qui te rapporte le plus',
    icon: Trophy,
    component: TopProfitDriversWidget,
    forms: ['bars', 'pie', 'treemap', 'heatmap'],
    defaultSize: { w: 1020, h: 660 },
    minSize: { w: 520, h: 340 },
    defaultProps: { top: 8, autoHeight: true, view: 'bars', categories: [], types: [] },
    settings: [
      {
        key: 'top',
        label: "Nombre d'éléments",
        type: 'number',
        min: 1,
        max: 15,
        step: 1,
        help: 'Entre 1 et 15',
      },
    ],
    categoryFilter: true,
  },
  {
    type: 'asp',
    title: 'Prix moyen (ASP)',
    category: 'Performance',
    help: 'Average selling price',
    icon: Gauge,
    component: AspWidget,
    forms: ['number'],
    defaultSize: { w: 520, h: 240 },
    minSize: { w: 380, h: 210 },
    defaultProps: { bucket: 'week', autoHeight: false, categories: [], types: [] },
    settings: [],
    categoryFilter: true,
  },
  {
    type: 'cashFlow',
    title: 'Cash disponible',
    category: 'Performance',
    help: 'Cash pour racheter du stock',
    icon: TrendingUp,
    component: CashFlowWidget,
    forms: ['number'],
    defaultSize: { w: 520, h: 240 },
    minSize: { w: 380, h: 210 },
    defaultProps: { bucket: 'week', autoHeight: false, categories: [], types: [] },
    settings: [],
    categoryFilter: true,
  },

  // 🧭 Decision
  {
    type: 'goalProgress',
    title: 'Goal progress',
    category: 'Decision',
    help: 'Progression vers un objectif',
    icon: Target,
    component: GoalProgressWidget,
    forms: ['target'],
    defaultSize: { w: 760, h: 340 },
    minSize: { w: 460, h: 260 },
    defaultProps: {
      metric: 'netProfit',
      target: 12000,
      unit: 'EUR',
      bucket: 'week',
      categories: [],
      types: [],
    },
    settings: [
      {
        key: 'metric',
        label: 'Metric',
        type: 'select',
        options: [
          { label: 'Profit', value: 'netProfit' },
          { label: 'CA', value: 'grossRevenue' },
          { label: 'Cash', value: 'cashAvailable' },
          { label: 'Marge', value: 'avgMargin' },
          { label: 'ROI', value: 'roi' },
          { label: 'Ecoulement', value: 'sellThrough' },
        ],
      },
      { key: 'target', label: 'Objectif', type: 'number', min: 0, max: 9_999_999, step: 1 },
      {
        key: 'unit',
        label: 'Unite',
        type: 'select',
        options: [
          { label: 'EUR', value: 'EUR' },
          { label: '%', value: '%' },
        ],
      },
    ],
    categoryFilter: true,
  },
  {
    type: 'varianceToTarget',
    title: 'Variance to target',
    category: 'Decision',
    help: 'Ecart par rapport a la cible',
    icon: Radar,
    component: VarianceToTargetWidget,
    forms: ['number'],
    defaultSize: { w: 760, h: 340 },
    minSize: { w: 460, h: 260 },
    defaultProps: {
      metric: 'grossRevenue',
      target: 42000,
      unit: 'EUR',
      bucket: 'week',
      categories: [],
      types: [],
    },
    settings: [
      {
        key: 'metric',
        label: 'Metric',
        type: 'select',
        options: [
          { label: 'Profit', value: 'netProfit' },
          { label: 'CA', value: 'grossRevenue' },
          { label: 'Cash', value: 'cashAvailable' },
          { label: 'Marge', value: 'avgMargin' },
          { label: 'ROI', value: 'roi' },
          { label: 'Ecoulement', value: 'sellThrough' },
        ],
      },
      { key: 'target', label: 'Objectif', type: 'number', min: 0, max: 9_999_999, step: 1 },
      {
        key: 'unit',
        label: 'Unite',
        type: 'select',
        options: [
          { label: 'EUR', value: 'EUR' },
          { label: '%', value: '%' },
        ],
      },
    ],
    categoryFilter: true,
  },
  {
    type: 'periodComparison',
    title: 'Period comparison',
    category: 'Decision',
    help: 'Comparer deux periodes',
    icon: GitCompareArrows,
    component: PeriodComparisonWidget,
    forms: ['bars'],
    defaultSize: { w: 980, h: 420 },
    minSize: { w: 620, h: 300 },
    defaultProps: {
      metric: 'netProfit',
      bucket: 'week',
      categories: [],
      types: [],
    },
    settings: [
      {
        key: 'metric',
        label: 'Metric',
        type: 'select',
        options: [
          { label: 'Profit', value: 'netProfit' },
          { label: 'CA', value: 'grossRevenue' },
          { label: 'Marge', value: 'avgMargin' },
          { label: 'ROI', value: 'roi' },
          { label: 'Ecoulement', value: 'sellThrough' },
        ],
      },
      {
        key: 'bucket',
        label: 'Granularite',
        type: 'select',
        options: [
          { label: 'Jour', value: 'day' },
          { label: 'Semaine', value: 'week' },
          { label: 'Mois', value: 'month' },
        ],
      },
    ],
    categoryFilter: true,
  },
  {
    type: 'profitBridge',
    title: 'Profit bridge',
    category: 'Decision',
    help: 'Bridge de contribution au profit',
    icon: LineChart,
    component: ProfitBridgeWidget,
    forms: ['bars'],
    defaultSize: { w: 1080, h: 470 },
    minSize: { w: 700, h: 320 },
    defaultProps: { categories: [], types: [] },
    settings: [],
    categoryFilter: true,
  },
  {
    type: 'alertFeed',
    title: 'Alert feed',
    category: 'Decision',
    help: 'Flux d alertes prioritaires',
    icon: Bell,
    component: AlertFeedWidget,
    forms: ['number'],
    defaultSize: { w: 1080, h: 390 },
    minSize: { w: 620, h: 300 },
    defaultProps: { maxItems: 8, categories: [], types: [] },
    settings: [{ key: 'maxItems', label: 'Max alertes', type: 'number', min: 3, max: 15, step: 1 }],
    categoryFilter: true,
  },
  {
    type: 'actionChecklist',
    title: 'Action checklist',
    category: 'Decision',
    help: 'Plan d execution actionnable',
    icon: ListChecks,
    component: ActionChecklistWidget,
    forms: ['number'],
    defaultSize: { w: 1180, h: 390 },
    minSize: { w: 760, h: 300 },
    defaultProps: { categories: [], types: [] },
    settings: [],
    categoryFilter: true,
  },
  {
    type: 'momentum',
    title: 'Momentum',
    category: 'Decision',
    help: 'Indice d acceleration',
    icon: TrendingUp,
    component: MomentumWidget,
    forms: ['line'],
    defaultSize: { w: 780, h: 340 },
    minSize: { w: 500, h: 280 },
    defaultProps: { metric: 'netProfit', bucket: 'week', categories: [], types: [] },
    settings: [
      {
        key: 'metric',
        label: 'Metric',
        type: 'select',
        options: [
          { label: 'Profit', value: 'netProfit' },
          { label: 'CA', value: 'grossRevenue' },
          { label: 'ROI', value: 'roi' },
          { label: 'Marge', value: 'avgMargin' },
          { label: 'Ecoulement', value: 'sellThrough' },
        ],
      },
    ],
    categoryFilter: true,
  },
  {
    type: 'riskHeat',
    title: 'Risk heat',
    category: 'Decision',
    help: 'Carte de concentration des risques',
    icon: BarChart3,
    component: RiskHeatWidget,
    forms: ['heatmap'],
    defaultSize: { w: 720, h: 360 },
    minSize: { w: 520, h: 300 },
    defaultProps: { categories: [], types: [] },
    settings: [],
    categoryFilter: true,
  },

  // Bonus (si tu veux garder)
  {
    type: 'brands',
    title: 'Top marques (volume)',
    category: 'Bonus',
    help: 'Barres marques',
    icon: BarChart3,
    component: BrandsWidget,
    forms: ['bars', 'treemap', 'heatmap'],
    defaultSize: { w: 720, h: 420 },
    minSize: { w: 500, h: 220 },
    defaultProps: { top: 8, autoHeight: true, view: 'bars', categories: [], types: [] },
    settings: [
      {
        key: 'top',
        label: "Nombre d'éléments",
        type: 'number',
        min: 1,
        max: 15,
        step: 1,
        help: 'Entre 1 et 15',
      },
      { key: 'autoHeight', label: 'Hauteur automatique', type: 'toggle' },
    ],
    categoryFilter: true,
  },
  {
    type: 'typeMix',
    title: 'Mix par type',
    category: 'Performance',
    help: 'Repartition ventes / stock / profit par type d item',
    icon: BarChart3,
    component: TypeMixWidget,
    forms: ['pie', 'bars', 'treemap'],
    defaultSize: { w: 720, h: 420 },
    minSize: { w: 520, h: 320 },
    defaultProps: { metric: 'profit', view: 'pie', autoHeight: false, categories: [], types: [] },
    settings: [
      {
        key: 'metric',
        label: 'Mesure',
        type: 'select',
        options: [
          { label: 'Profit', value: 'profit' },
          { label: 'Ventes', value: 'sold' },
          { label: 'Stock', value: 'stock' },
        ],
      },
    ],
    categoryFilter: true,
  },
  {
    type: 'topSales',
    title: 'Top ventes',
    category: 'Bonus',
    help: 'Top par bénéfice',
    icon: Trophy,
    component: TopSalesWidget,
    forms: ['bars'],
    defaultSize: { w: 620, h: 420 },
    minSize: { w: 420, h: 260 },
    defaultProps: { limit: 5, autoHeight: true, categories: [], types: [] },
    settings: [
      {
        key: 'limit',
        label: "Nombre d'éléments",
        type: 'number',
        min: 1,
        max: 15,
        step: 1,
        help: 'Entre 1 et 15',
      },
    ],
    categoryFilter: true,
  },
]

export function getWidgetDef(type) {
  return WIDGET_DEFS.find((d) => d.type === type)
}

export function cloneWidgetProps(value) {
  if (value == null || typeof value !== 'object') return {}
  if (typeof globalThis.structuredClone === 'function') {
    try {
      return globalThis.structuredClone(value)
    } catch {
      // fallback below
    }
  }
  try {
    return JSON.parse(JSON.stringify(value))
  } catch {
    return { ...value }
  }
}

export function newWidget(type, x, y) {
  const uid =
    globalThis.crypto?.randomUUID?.() ?? `${Date.now()}_${Math.random().toString(16).slice(2)}`
  const def = getWidgetDef(type)
  if (!def) throw new Error(`Unknown widget type: ${type}`)

  return {
    id: `${type}_${uid}`,
    type,
    title: def.title,
    x,
    y,
    w: def.defaultSize.w,
    h: def.defaultSize.h,
    props: cloneWidgetProps(def.defaultProps),
  }
}
