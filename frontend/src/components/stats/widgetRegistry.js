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
} from 'lucide-vue-next'

// src/components/stats/widgets/widgetRegistry.js
import NetProfitWidget from './widgets/NetProfitWidget.vue'

import RoiWidget from './widgets/RoiWidget.vue'
import GrossRevenueWidget from './widgets/GrossRevenueWidget.vue'
import AvgMarginWidget from './widgets/AvgMarginWidget.vue'

import InventoryValueWidget from './widgets/InventoryValueWidget.vue'
import SellThroughWidget from './widgets/SellThroughWidget.vue'
import AvgDaysToSellWidget from './widgets/AvgDaysToSellWidget.vue'
import DeathPileWidget from './widgets/DeathPileWidget.vue'
import ActiveListingsWidget from './widgets/ActiveListingsWidget.vue'

import TopProfitDriversWidget from './widgets/TopProfitDriversWidget.vue'
import AspWidget from './widgets/AspWidget.vue'
import CashFlowWidget from './widgets/CashFlowWidget.vue'

import BrandsWidget from './widgets/BrandsWidget.vue'
import TopSalesWidget from './widgets/TopSalesWidget.vue'
import TextTitleWidget from './widgets/TextTitleWidget.vue'
import TextBlockWidget from './widgets/TextBlockWidget.vue'

export const CATEGORY_COLORS = {
  Texte: { color: '#a855f7', glow: 'rgba(168, 85, 247, 0.2)' },
  Finance: { color: '#22c55e', glow: 'rgba(34, 197, 94, 0.2)' },
  Stock: { color: '#38bdf8', glow: 'rgba(56, 189, 248, 0.2)' },
  Performance: { color: '#f59e0b', glow: 'rgba(245, 158, 11, 0.22)' },
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
    defaultSize: { w: 520, h: 120 },
    minSize: { w: 320, h: 120 },
    defaultProps: { content: 'Titre', align: 'left', size: 'md', tight: true, autoHeight: true },
    hideGlobalRange: true,
    settings: [
      { key: 'content', label: 'Texte', type: 'text' },
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
      {
        key: 'size',
        label: 'Taille',
        type: 'select',
        options: [
          { label: 'Petit', value: 'sm' },
          { label: 'Normal', value: 'md' },
          { label: 'Grand', value: 'lg' },
        ],
      },
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
    defaultSize: { w: 520, h: 220 },
    defaultProps: { content: 'Ton texte ici...', align: 'left', size: 'md', autoHeight: true },
    hideGlobalRange: true,
    settings: [
      { key: 'content', label: 'Texte', type: 'textarea' },
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
      {
        key: 'size',
        label: 'Taille',
        type: 'select',
        options: [
          { label: 'Petit', value: 'sm' },
          { label: 'Normal', value: 'md' },
          { label: 'Grand', value: 'lg' },
          { label: 'Très grand', value: 'xl' },
        ],
      },
    ],
  },
  // 💰 Finance
  {
    type: 'netProfit',
    title: 'Bénéfice net',
    category: 'Finance',
    help: 'Profit total sur la période',
    icon: TrendingUp,
    component: NetProfitWidget,
    forms: ['number'],
    defaultSize: { w: 520, h: 240 },
    defaultProps: { bucket: 'week', autoHeight: false, categories: [] },
    settings: [],
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
    defaultSize: { w: 520, h: 260 },
    defaultProps: { autoHeight: true, categories: [] },
    settings: [],
    categoryFilter: true,
  },
  {
    type: 'grossRevenue',
    title: 'Chiffre d’affaires',
    category: 'Finance',
    help: 'Courbe du CA',
    icon: LineChart,
    component: GrossRevenueWidget,
    forms: ['line'],
    defaultSize: { w: 820, h: 520 },
    defaultProps: { bucket: 'day', autoHeight: true, categories: [] },
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
    defaultSize: { w: 520, h: 200 },
    minSize: { w: 320, h: 180 },
    defaultProps: { bucket: 'week', autoHeight: true, categories: [] },
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
    defaultProps: { autoHeight: false, useGlobalRange: false, asOf: '', categories: [] },
    settings: [{ key: 'asOf', label: 'Date', type: 'date' }],
    dateMode: 'asOf',
    categoryFilter: true,
  },
  {
    type: 'sellThrough',
    title: "Taux d'ecoulement",
    category: 'Stock',
    help: "Part du stock vendu",
    icon: BarChart3,
    component: SellThroughWidget,
    forms: ['bars'],
    defaultSize: { w: 520, h: 260 },
    defaultProps: { bucket: 'week', autoHeight: true, categories: [] },
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
    defaultProps: { bucket: 'week', autoHeight: false, categories: [] },
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
    defaultSize: { w: 620, h: 470 },
    minSize: { w: 520, h: 470 },
    defaultProps: { autoHeight: false, categories: [] },
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
    defaultProps: { bucket: 'week', autoHeight: false, categories: [] },
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
    defaultSize: { w: 720, h: 360 },
    defaultProps: { top: 8, autoHeight: true, view: 'bars', categories: [] },
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
    defaultProps: { bucket: 'week', autoHeight: false, categories: [] },
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
    defaultProps: { bucket: 'week', autoHeight: false, categories: [] },
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
    defaultProps: { top: 8, autoHeight: false, view: 'bars', categories: [] },
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
    type: 'topSales',
    title: 'Top ventes',
    category: 'Bonus',
    help: 'Top par bénéfice',
    icon: Trophy,
    component: TopSalesWidget,
    forms: ['bars'],
    defaultSize: { w: 620, h: 420 },
    defaultProps: { limit: 5, autoHeight: true, categories: [] },
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
    props: { ...def.defaultProps },
  }
}
