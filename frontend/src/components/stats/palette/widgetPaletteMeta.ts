import type { WidgetDataType, WidgetDisplayGroup, WidgetPreviewSpec } from './types'

type WidgetPaletteMeta = {
  tags: string[]
  dataType: WidgetDataType
  preview: WidgetPreviewSpec
}

const DEFAULT_META: WidgetPaletteMeta = {
  tags: ['dashboard'],
  dataType: 'mixed',
  preview: { kind: 'fallback' },
}

const META_BY_TYPE: Record<string, WidgetPaletteMeta> = {
  textTitle: {
    tags: ['structure', 'header'],
    dataType: 'text',
    preview: { kind: 'text', label: 'Titre section', legend: ['Titre principal'] },
  },
  textBlock: {
    tags: ['notes', 'context'],
    dataType: 'text',
    preview: { kind: 'text', label: 'Bloc texte', legend: ['Texte descriptif'] },
  },
  netProfit: {
    tags: ['kpi', 'trend', 'profit'],
    dataType: 'finance',
    preview: {
      kind: 'sparkline',
      values: [10, 12, 11, 15, 14, 19, 17, 22],
      label: 'Bénéfice net',
      valueText: '12 480 EUR',
      deltaText: '+8.2%',
    },
  },
  roi: {
    tags: ['kpi', 'margin'],
    dataType: 'finance',
    preview: {
      kind: 'kpi',
      ratio: 0.64,
      values: [12, 16, 14, 18, 20, 19, 22],
      label: 'ROI moyen',
      valueText: '34.5%',
      deltaText: '+2.4 pts',
    },
  },
  grossRevenue: {
    tags: ['trend', 'timeseries'],
    dataType: 'finance',
    preview: {
      kind: 'sparkline',
      values: [12, 15, 14, 20, 19, 25, 29, 32],
      label: "Chiffre d'affaires",
      valueText: '42 900 EUR',
      deltaText: '+11%',
    },
  },
  avgMargin: {
    tags: ['kpi', 'unit economics'],
    dataType: 'finance',
    preview: {
      kind: 'kpi',
      ratio: 0.71,
      values: [9, 12, 10, 14, 13, 16, 18],
      label: 'Marge moyenne',
      valueText: '27.8%',
      deltaText: '+1.3 pts',
    },
  },
  inventoryValue: {
    tags: ['stock', 'valuation'],
    dataType: 'inventory',
    preview: {
      kind: 'kpi',
      ratio: 0.62,
      values: [42, 40, 38, 35, 33, 31],
      label: 'Valeur du stock',
      valueText: '68 240 EUR',
      deltaText: '-3.2%',
    },
  },
  sellThrough: {
    tags: ['velocity', 'conversion'],
    dataType: 'inventory',
    preview: {
      kind: 'bars',
      values: [18, 24, 31, 37, 42],
      label: 'Taux d’écoulement',
      legend: ['S', 'M', 'L', 'XL', 'XXL'],
    },
  },
  avgDaysToSell: {
    tags: ['velocity', 'cycle'],
    dataType: 'inventory',
    preview: {
      kind: 'kpi',
      ratio: 0.56,
      values: [34, 30, 28, 25, 22, 21],
      label: 'Délai moyen avant vente',
      valueText: '24 jours',
      deltaText: '-2 jours',
    },
  },
  deathPile: {
    tags: ['risk', 'aging'],
    dataType: 'inventory',
    preview: {
      kind: 'bars',
      values: [41, 29, 24, 18, 12],
      label: 'Stock dormant',
      legend: ['0-30', '31-60', '61-90', '91-120', '120+'],
    },
  },
  activeListings: {
    tags: ['inventory', 'marketplace'],
    dataType: 'inventory',
    preview: {
      kind: 'kpi',
      ratio: 0.83,
      values: [24, 26, 28, 30, 34, 36],
      label: 'Annonces actives',
      valueText: '238',
      deltaText: '+16',
    },
  },
  topProfitDrivers: {
    tags: ['ranking', 'drivers'],
    dataType: 'performance',
    preview: {
      kind: 'bars',
      values: [40, 32, 27, 19, 13],
      label: 'Top profit',
      legend: ['AJ1', 'Dunk', 'NB', 'AF1', 'Yeezy'],
    },
  },
  asp: {
    tags: ['price', 'kpi'],
    dataType: 'performance',
    preview: {
      kind: 'kpi',
      ratio: 0.69,
      values: [15, 19, 18, 21, 20, 24],
      label: 'Prix moyen de vente',
      valueText: '182 EUR',
      deltaText: '+6 EUR',
    },
  },
  cashFlow: {
    tags: ['cash', 'forecast'],
    dataType: 'finance',
    preview: {
      kind: 'kpi',
      ratio: 0.74,
      values: [10, 14, 12, 18, 16, 23, 26],
      label: 'Cash disponible',
      valueText: '19 720 EUR',
      deltaText: '+9.4%',
    },
  },
  goalProgress: {
    tags: ['goal', 'target', 'execution'],
    dataType: 'performance',
    preview: {
      kind: 'kpi',
      ratio: 0.78,
      values: [45, 52, 58, 61, 67, 74, 79],
      label: 'Progression objectif',
      valueText: '78%',
      deltaText: '+9 pts',
    },
  },
  varianceToTarget: {
    tags: ['variance', 'target', 'decision'],
    dataType: 'performance',
    preview: {
      kind: 'kpi',
      ratio: 0.56,
      values: [72, 69, 65, 67, 64, 61, 58],
      label: 'Écart à l’objectif',
      valueText: '-8.2%',
      deltaText: '-3 pts',
    },
  },
  periodComparison: {
    tags: ['comparison', 'periods'],
    dataType: 'mixed',
    preview: {
      kind: 'bars',
      values: [38, 46],
      secondaryValues: [41, 43],
      label: 'Periode',
      legend: ['N-1', 'N'],
    },
  },
  profitBridge: {
    tags: ['bridge', 'profit', 'waterfall'],
    dataType: 'finance',
    preview: {
      kind: 'bars',
      values: [28, 11, 35],
      label: 'Profit bridge',
      legend: ['Start', 'Delta', 'End'],
    },
  },
  alertFeed: {
    tags: ['alerts', 'risk', 'signal'],
    dataType: 'mixed',
    preview: {
      kind: 'text',
      label: 'Alertes',
      legend: ['High', 'Medium', 'Info'],
    },
  },
  actionChecklist: {
    tags: ['actions', 'execution', 'checklist'],
    dataType: 'mixed',
    preview: {
      kind: 'text',
      label: 'Checklist',
      legend: ['Action 1', 'Action 2'],
    },
  },
  momentum: {
    tags: ['momentum', 'trend', 'speed'],
    dataType: 'performance',
    preview: {
      kind: 'sparkline',
      values: [12, 15, 14, 18, 20, 23, 27, 29],
      label: 'Momentum',
      valueText: '32 pts',
      deltaText: '+6.1%',
    },
  },
  riskHeat: {
    tags: ['risk', 'heat', 'exposure'],
    dataType: 'mixed',
    preview: {
      kind: 'heatmap',
      values: [12, 24, 45, 66, 78],
      label: 'Risk heat',
      legend: ['Low', 'Medium', 'High'],
    },
  },
  brands: {
    tags: ['mix', 'brands'],
    dataType: 'mixed',
    preview: {
      kind: 'bars',
      values: [35, 26, 21, 18, 13],
      label: 'Top marques',
      legend: ['Nike', 'Adidas', 'NB', 'Asics', 'Autres'],
    },
  },
  typeMix: {
    tags: ['distribution', 'types'],
    dataType: 'mixed',
    preview: {
      kind: 'pie',
      values: [45, 30, 15, 10],
      label: 'Mix type',
      legend: ['Sneakers', 'Cards', 'Tickets', 'Autres'],
    },
  },
  topSales: {
    tags: ['ranking', 'volume'],
    dataType: 'performance',
    preview: {
      kind: 'bars',
      values: [39, 34, 28, 22, 16],
      label: 'Top ventes',
      legend: ['AJ4', 'Dunk', 'Gazelle', '550', 'Forum'],
    },
  },
}

const METRIC_VARIANTS = {
  netProfit: { label: 'Bénéfice net', hint: 'Profit net sur la periode', unit: 'EUR', target: 12000 },
  grossRevenue: { label: "Chiffre d'affaires", hint: 'CA sur la periode', unit: 'EUR', target: 42000 },
  cashAvailable: { label: 'Cash disponible', hint: 'Cash mobilisable', unit: 'EUR', target: 12000 },
  avgMargin: { label: 'Marge moyenne', hint: 'Marge moyenne par vente', unit: '%', target: 28 },
  roi: { label: 'ROI moyen', hint: 'Retour sur investissement', unit: '%', target: 35 },
  sellThrough: { label: 'Taux d’écoulement', hint: 'Part du stock vendu', unit: '%', target: 50 },
} as const

function metricVariant(metric: keyof typeof METRIC_VARIANTS) {
  const entry = METRIC_VARIANTS[metric]
  return {
    key: metric,
    label: entry.label,
    hint: entry.hint,
    props: {
      metric,
      unit: entry.unit,
      target: entry.target,
    },
    widgetTitle: entry.label,
  }
}

const SELECTION_BY_TYPE: Record<string, WidgetDisplayGroup[]> = {
  textTitle: [
    {
      key: 'text',
      label: 'Texte',
      hint: 'Structure du canvas',
      variants: [{ key: 'title', label: 'Titre de section', hint: 'Grand titre libre' }],
    },
  ],
  textBlock: [
    {
      key: 'text',
      label: 'Texte',
      hint: 'Notes et contexte',
      variants: [{ key: 'note', label: 'Bloc texte', hint: 'Texte libre multi-ligne' }],
    },
  ],
  netProfit: [
    {
      key: 'number',
      label: 'KPI',
      hint: 'Carte compacte',
      variants: [
        {
          key: 'total',
          label: 'Bénéfice net total',
          hint: 'Total net sur la periode',
          view: 'number',
          props: { view: 'number', kpiVariant: 'total' },
          widgetTitle: 'Bénéfice net',
        },
        {
          key: 'average',
          label: 'Bénéfice moyen',
          hint: 'Bénéfice moyen par vente',
          view: 'number',
          props: { view: 'number', kpiVariant: 'avgProfit' },
          widgetTitle: 'Bénéfice moyen',
        },
        {
          key: 'sales',
          label: 'Ventes réalisées',
          hint: 'Ventes dans la periode',
          view: 'number',
          props: { view: 'number', kpiVariant: 'sales' },
          widgetTitle: 'Ventes réalisées',
        },
      ],
    },
    {
      key: 'line',
      label: 'Courbe',
      hint: 'Evolution temporelle',
      variants: [
        {
          key: 'trend',
          label: 'Évolution du bénéfice net',
          hint: 'Courbe par jour, semaine ou mois',
          view: 'line',
          props: { view: 'line' },
          widgetTitle: 'Évolution bénéfice net',
        },
      ],
    },
  ],
  grossRevenue: [
    {
      key: 'number',
      label: 'KPI',
      hint: 'Carte compacte',
      variants: [
        {
          key: 'total',
          label: "Chiffre d'affaires total",
          hint: 'Somme du CA sur la periode',
          view: 'number',
          props: { view: 'number' },
          widgetTitle: "Chiffre d'affaires",
        },
      ],
    },
    {
      key: 'line',
      label: 'Courbe',
      hint: 'Evolution temporelle',
      variants: [
        {
          key: 'trend',
          label: "Evolution du chiffre d'affaires",
          hint: 'Courbe du CA selon la periode',
          view: 'line',
          props: { view: 'line' },
          widgetTitle: 'Evolution CA',
        },
      ],
    },
  ],
  roi: [
    {
      key: 'number',
      label: 'KPI',
      hint: 'Rentabilite',
      variants: [{ key: 'avg', label: 'ROI moyen', hint: 'Retour moyen sur investissement' }],
    },
  ],
  avgMargin: [
    {
      key: 'number',
      label: 'KPI',
      hint: 'Rentabilite unitaire',
      variants: [{ key: 'avg', label: 'Marge moyenne', hint: 'Gain moyen par vente' }],
    },
  ],
  inventoryValue: [
    {
      key: 'number',
      label: 'KPI',
      hint: 'Stock',
      variants: [{ key: 'value', label: 'Valeur du stock', hint: "Capital immobilise a la date choisie" }],
    },
  ],
  sellThrough: [
    {
      key: 'number',
      label: 'KPI',
      hint: 'Vitesse de stock',
      variants: [{ key: 'rate', label: 'Taux d’écoulement', hint: 'Part du stock vendu vs objectif' }],
    },
  ],
  avgDaysToSell: [
    {
      key: 'number',
      label: 'KPI',
      hint: 'Rotation',
      variants: [{ key: 'days', label: 'Délai moyen avant vente', hint: 'Temps moyen entre achat et vente' }],
    },
  ],
  activeListings: [
    {
      key: 'number',
      label: 'KPI',
      hint: 'Stock publie',
      variants: [{ key: 'count', label: 'Annonces actives', hint: 'Nombre d annonces en ligne' }],
    },
  ],
  asp: [
    {
      key: 'number',
      label: 'KPI',
      hint: 'Prix de vente',
      variants: [{ key: 'avg', label: 'Prix moyen de vente', hint: 'Prix de vente moyen sur la periode' }],
    },
  ],
  cashFlow: [
    {
      key: 'number',
      label: 'KPI',
      hint: 'Tresorerie',
      variants: [{ key: 'available', label: 'Cash disponible', hint: 'Cash mobilisable pour le stock' }],
    },
  ],
  goalProgress: [
    {
      key: 'number',
      label: 'KPI objectif',
      hint: 'Progression vers une cible',
      variants: Object.keys(METRIC_VARIANTS).map((metric) => ({
        ...metricVariant(metric as keyof typeof METRIC_VARIANTS),
        key: `goal-${metric}`,
        label: `Objectif ${METRIC_VARIANTS[metric as keyof typeof METRIC_VARIANTS].label.toLowerCase()}`,
        widgetTitle: `Objectif ${METRIC_VARIANTS[metric as keyof typeof METRIC_VARIANTS].label}`,
      })),
    },
  ],
  varianceToTarget: [
    {
      key: 'number',
      label: 'KPI écart',
      hint: 'Actuel vs cible',
      variants: Object.keys(METRIC_VARIANTS).map((metric) => ({
        ...metricVariant(metric as keyof typeof METRIC_VARIANTS),
        key: `variance-${metric}`,
        label: `Écart ${METRIC_VARIANTS[metric as keyof typeof METRIC_VARIANTS].label.toLowerCase()}`,
        widgetTitle: `Écart ${METRIC_VARIANTS[metric as keyof typeof METRIC_VARIANTS].label}`,
      })),
    },
  ],
  periodComparison: [
    {
      key: 'bars',
      label: 'Barres',
      hint: 'Comparer deux periodes',
      variants: ['netProfit', 'grossRevenue', 'avgMargin', 'roi', 'sellThrough'].map((metric) => ({
        ...metricVariant(metric as keyof typeof METRIC_VARIANTS),
        key: `compare-${metric}`,
        label: `Comparaison ${METRIC_VARIANTS[metric as keyof typeof METRIC_VARIANTS].label.toLowerCase()}`,
        widgetTitle: `Comparaison ${METRIC_VARIANTS[metric as keyof typeof METRIC_VARIANTS].label}`,
      })),
    },
  ],
  momentum: [
    {
      key: 'line',
      label: 'Courbe',
      hint: 'Acceleration',
      variants: ['netProfit', 'grossRevenue', 'roi', 'avgMargin', 'sellThrough'].map((metric) => ({
        ...metricVariant(metric as keyof typeof METRIC_VARIANTS),
        key: `momentum-${metric}`,
        label: `Momentum ${METRIC_VARIANTS[metric as keyof typeof METRIC_VARIANTS].label.toLowerCase()}`,
        widgetTitle: `Momentum ${METRIC_VARIANTS[metric as keyof typeof METRIC_VARIANTS].label}`,
      })),
    },
  ],
  topProfitDrivers: [
    {
      key: 'bars',
      label: 'Barres',
      hint: 'Classement lisible',
      variants: [{ key: 'bars', label: 'Top profit en barres', hint: 'Top elements par contribution', view: 'bars', props: { view: 'bars' } }],
    },
    {
      key: 'pie',
      label: 'Donut',
      hint: 'Repartition',
      variants: [{ key: 'pie', label: 'Repartition du profit', hint: 'Part de chaque driver', view: 'pie', props: { view: 'pie' } }],
    },
    {
      key: 'treemap',
      label: 'Treemap',
      hint: 'Surface relative',
      variants: [{ key: 'treemap', label: 'Treemap profit', hint: 'Comparer par surface', view: 'treemap', props: { view: 'treemap' } }],
    },
    {
      key: 'heatmap',
      label: 'Heatmap',
      hint: 'Intensites',
      variants: [{ key: 'heatmap', label: 'Heatmap profit', hint: 'Concentration des gains', view: 'heatmap', props: { view: 'heatmap' } }],
    },
  ],
  brands: [
    {
      key: 'bars',
      label: 'Barres',
      hint: 'Volume par marque',
      variants: [{ key: 'bars', label: 'Top marques en barres', hint: 'Classement par volume', view: 'bars', props: { view: 'bars' } }],
    },
    {
      key: 'treemap',
      label: 'Treemap',
      hint: 'Surface relative',
      variants: [{ key: 'treemap', label: 'Treemap marques', hint: 'Comparer les marques', view: 'treemap', props: { view: 'treemap' } }],
    },
    {
      key: 'heatmap',
      label: 'Heatmap',
      hint: 'Intensites',
      variants: [{ key: 'heatmap', label: 'Heatmap marques', hint: 'Concentration de volume', view: 'heatmap', props: { view: 'heatmap' } }],
    },
  ],
  typeMix: [
    {
      key: 'pie',
      label: 'Donut',
      hint: 'Repartition',
      variants: [
        { key: 'profit-pie', label: 'Repartition du profit', hint: 'Profit par type', view: 'pie', props: { view: 'pie', metric: 'profit' } },
        { key: 'sold-pie', label: 'Repartition des ventes', hint: 'Ventes par type', view: 'pie', props: { view: 'pie', metric: 'sold' } },
        { key: 'stock-pie', label: 'Repartition du stock', hint: 'Stock par type', view: 'pie', props: { view: 'pie', metric: 'stock' } },
      ],
    },
    {
      key: 'bars',
      label: 'Barres',
      hint: 'Comparer les types',
      variants: [
        { key: 'profit-bars', label: 'Profit par type', hint: 'Barres de contribution', view: 'bars', props: { view: 'bars', metric: 'profit' } },
        { key: 'sold-bars', label: 'Ventes par type', hint: 'Barres de volume', view: 'bars', props: { view: 'bars', metric: 'sold' } },
        { key: 'stock-bars', label: 'Stock par type', hint: 'Barres de stock', view: 'bars', props: { view: 'bars', metric: 'stock' } },
      ],
    },
    {
      key: 'treemap',
      label: 'Treemap',
      hint: 'Surface relative',
      variants: [
        { key: 'profit-treemap', label: 'Treemap profit', hint: 'Contribution par type', view: 'treemap', props: { view: 'treemap', metric: 'profit' } },
        { key: 'stock-treemap', label: 'Treemap stock', hint: 'Stock par type', view: 'treemap', props: { view: 'treemap', metric: 'stock' } },
      ],
    },
  ],
  topSales: [
    {
      key: 'list',
      label: 'Liste courte',
      hint: 'Classement',
      variants: [{ key: 'profit', label: 'Top ventes par benefice', hint: 'Produits les plus rentables' }],
    },
  ],
  deathPile: [
    {
      key: 'bars',
      label: 'Barres',
      hint: 'Aging stock',
      variants: [{ key: 'aging', label: 'Stock dormant par age', hint: 'Articles immobilises par tranche' }],
    },
  ],
  profitBridge: [
    {
      key: 'bars',
      label: 'Barres',
      hint: 'Bridge',
      variants: [{ key: 'bridge', label: 'Bridge de profit', hint: 'Contribution entre deux periodes' }],
    },
  ],
  riskHeat: [
    {
      key: 'heatmap',
      label: 'Heatmap',
      hint: 'Risque',
      variants: [{ key: 'risk', label: 'Carte des risques', hint: 'Concentration stock et profit' }],
    },
  ],
  alertFeed: [
    {
      key: 'list',
      label: 'Liste courte',
      hint: 'Alertes',
      variants: [{ key: 'feed', label: 'Flux d alertes', hint: 'Signaux prioritaires' }],
    },
  ],
  actionChecklist: [
    {
      key: 'list',
      label: 'Liste courte',
      hint: 'Execution',
      variants: [{ key: 'actions', label: 'Checklist actions', hint: 'Plan d execution priorise' }],
    },
  ],
}

const FORM_SELECTION_LABELS: Record<string, { label: string; hint: string }> = {
  number: { label: 'KPI', hint: 'Carte compacte' },
  line: { label: 'Courbe', hint: 'Evolution temporelle' },
  bars: { label: 'Barres', hint: 'Comparaison' },
  pie: { label: 'Donut', hint: 'Repartition' },
  target: { label: 'Objectif', hint: 'Progression cible' },
  treemap: { label: 'Treemap', hint: 'Surface relative' },
  heatmap: { label: 'Heatmap', hint: 'Intensites' },
}

function fallbackSelection(forms: string[] = []): WidgetDisplayGroup[] {
  return forms
    .filter((form) => String(form || '').trim().length > 0)
    .map((form) => {
      const meta = FORM_SELECTION_LABELS[form] ?? { label: form, hint: 'Affichage disponible' }
      return {
        key: form,
        label: meta.label,
        hint: meta.hint,
        variants: [
          {
            key: form,
            label: meta.label,
            hint: meta.hint,
            view: form,
            props: { view: form },
          },
        ],
      }
    })
}

export function getWidgetPaletteMeta(type: string): WidgetPaletteMeta {
  const meta = META_BY_TYPE[type]
  if (!meta) return DEFAULT_META
  return {
    tags: [...meta.tags],
    dataType: meta.dataType,
    preview: {
      ...meta.preview,
      values: meta.preview.values ? [...meta.preview.values] : undefined,
      secondaryValues: meta.preview.secondaryValues ? [...meta.preview.secondaryValues] : undefined,
      legend: meta.preview.legend ? [...meta.preview.legend] : undefined,
    },
  }
}

export function getWidgetSelectionMeta(type: string, forms: string[] = []): WidgetDisplayGroup[] {
  const selection = SELECTION_BY_TYPE[type] ?? fallbackSelection(forms)
  return selection
    .map((group) => ({
      ...group,
      variants: (group.variants ?? []).map((variant) => ({
        ...variant,
        props: variant.props ? { ...variant.props } : undefined,
      })),
    }))
    .filter((group) => group.variants.length > 0)
}
