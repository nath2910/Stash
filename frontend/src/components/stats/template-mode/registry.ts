import { defineAsyncComponent, type Component } from 'vue'

export const ANNUAL_DASHBOARD_TEMPLATE_ID = 'annual-dashboard'
export const MONTHLY_DASHBOARD_TEMPLATE_ID = 'monthly-dashboard'
export const QUARTERLY_DASHBOARD_TEMPLATE_ID = 'quarterly-dashboard'
export const CATEGORY_DASHBOARD_TEMPLATE_ID = 'category-dashboard'

export type TemplateId =
  | typeof ANNUAL_DASHBOARD_TEMPLATE_ID
  | typeof MONTHLY_DASHBOARD_TEMPLATE_ID
  | typeof QUARTERLY_DASHBOARD_TEMPLATE_ID
  | typeof CATEGORY_DASHBOARD_TEMPLATE_ID

export type TemplateDefinition = {
  id: TemplateId
  badge: string
  title: string
  headline: string
  description: string
  highlights: string[]
  accent: 'emerald' | 'sky' | 'violet' | 'amber'
  component: Component
}

export const TEMPLATE_DEFINITIONS = [
  {
    id: ANNUAL_DASHBOARD_TEMPLATE_ID,
    badge: 'Executive',
    title: 'Pilotage annuel',
    headline: 'Vision macro, lecture dirigeant',
    description: 'Vue dirigeant avec CA, profit, achats, stock et meilleurs produits sur une annee complete.',
    highlights: ['CA et profit', 'Stock annuel', 'Top produits'],
    accent: 'emerald',
    component: defineAsyncComponent(
      () => import('./annual-dashboard/AnnualDashboardTemplate.vue'),
    ),
  },
  {
    id: MONTHLY_DASHBOARD_TEMPLATE_ID,
    badge: 'Sales',
    title: 'Performance mensuelle',
    headline: 'Rythme mensuel et execution',
    description: 'Lecture mois par mois avec KPI, tendance quotidienne, achats et top ventes.',
    highlights: ['KPI mensuels', 'Tendance jour par jour', 'Achats et ventes'],
    accent: 'sky',
    component: defineAsyncComponent(
      () => import('./monthly-dashboard/MonthlyDashboardTemplate.vue'),
    ),
  },
  {
    id: QUARTERLY_DASHBOARD_TEMPLATE_ID,
    badge: 'Quarterly',
    title: 'Pilotage trimestriel',
    headline: 'Lecture quarter et tempo mensuel',
    description: 'Vue trimestre avec KPI agreges, suivi des 3 mois, categories profitables et contexte annuel.',
    highlights: ['KPI trimestriels', 'Lecture 3 mois', 'Contexte annuel'],
    accent: 'violet',
    component: defineAsyncComponent(
      () => import('./quarterly-dashboard/QuarterlyDashboardTemplate.vue'),
    ),
  },
  {
    id: CATEGORY_DASHBOARD_TEMPLATE_ID,
    badge: 'Inventory',
    title: 'Analyse par univers',
    headline: 'Univers puis sous-categories',
    description: 'Vue simple par grande categorie avec filtre de sous-categories, stock visible et top ventes.',
    highlights: ['Univers', 'Sous-categories', 'Top ventes'],
    accent: 'amber',
    component: defineAsyncComponent(
      () => import('./category-dashboard/CategoryDashboardTemplate.vue'),
    ),
  },
] satisfies TemplateDefinition[]

export const DEFAULT_TEMPLATE_ID = ANNUAL_DASHBOARD_TEMPLATE_ID

export function isTemplateId(value: unknown): value is TemplateId {
  return (
    typeof value === 'string' &&
    TEMPLATE_DEFINITIONS.some((template) => template.id === value)
  )
}

export function sanitizeTemplateId(value: unknown): TemplateId | '' {
  return isTemplateId(value) ? value : ''
}

export function getTemplateDefinition(value: unknown): TemplateDefinition | null {
  const templateId = sanitizeTemplateId(value)
  if (!templateId) return null
  return TEMPLATE_DEFINITIONS.find((template) => template.id === templateId) ?? null
}
