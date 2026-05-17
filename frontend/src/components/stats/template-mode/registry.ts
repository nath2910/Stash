import { defineAsyncComponent, type Component } from 'vue'

export const ANNUAL_DASHBOARD_TEMPLATE_ID = 'annual-dashboard'
export const MONTHLY_DASHBOARD_TEMPLATE_ID = 'monthly-dashboard'

export type TemplateId =
  | typeof ANNUAL_DASHBOARD_TEMPLATE_ID
  | typeof MONTHLY_DASHBOARD_TEMPLATE_ID

export type TemplateDefinition = {
  id: TemplateId
  badge: string
  title: string
  description: string
  component: Component
}

export const TEMPLATE_DEFINITIONS = [
  {
    id: ANNUAL_DASHBOARD_TEMPLATE_ID,
    badge: 'Vue annuelle',
    title: 'Dashboard annuel',
    description: 'CA, profit, achats, stock et meilleurs produits sur une annee complete.',
    component: defineAsyncComponent(
      () => import('./annual-dashboard/AnnualDashboardTemplate.vue'),
    ),
  },
  {
    id: MONTHLY_DASHBOARD_TEMPLATE_ID,
    badge: 'Vue mensuelle',
    title: 'Dashboard mensuel',
    description: 'Pilotage mois par mois avec KPI, tendances quotidiennes, achats et top ventes.',
    component: defineAsyncComponent(
      () => import('./monthly-dashboard/MonthlyDashboardTemplate.vue'),
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
