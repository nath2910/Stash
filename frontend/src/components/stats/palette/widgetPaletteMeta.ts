import type { WidgetDataType, WidgetPreviewSpec } from './types'

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
      label: 'Benefice net',
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
      label: 'Valeur stock',
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
      label: "Taux d'ecoulement",
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
      label: 'Delai moyen',
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
      label: 'ASP',
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
      label: 'Goal progress',
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
      label: 'Variance cible',
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
