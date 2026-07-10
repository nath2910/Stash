import test from 'node:test'
import assert from 'node:assert/strict'
import {
  getAdministrativeProfile,
  getAdministrativeIndicators,
  getAvailableActions,
  getBlockingIssues,
  getDeclarationSummary,
  getDeclarativePeriod,
  getDocumentRows,
  getLegalAutopilotPlan,
} from '../src/rules/administrativeRules.js'

const baseSummary = {
  periodRevenue: 690,
  annualRevenue: 690,
  periodSaleCount: 5,
  annualSaleCount: 5,
  incompleteSaleCount: 0,
  missingSaleDateCount: 0,
  missingSaleAmountCount: 0,
  periodPurchaseCount: 0,
  periodPurchaseTotal: 0,
  sales: [{ id: 1, purchaseAmount: 120 }],
}

const documents = [
  { id: 'urssaf-summary', title: 'Fiche URSSAF', format: 'PDF' },
  { id: 'receipts-register', title: 'Registre ventes', format: 'PDF' },
  { id: 'purchases-register', title: 'Registre achats', format: 'PDF' },
  { id: 'fiscal-summary', title: 'Dossier comptable', format: 'PDF' },
]

test('EURL affiche une preparation comptable et aucune action URSSAF', () => {
  const profile = {
    legalStatus: 'eurl',
    siret: '12345678901234',
    vatRegime: 'unknown',
    activities: ['goods_resale'],
    fiscalYearEndMonth: 12,
    fiscalYearEndDay: 31,
  }
  const period = getDeclarativePeriod(profile, '2026-04-01', '2026-06-30')
  const summary = getDeclarationSummary(profile, baseSummary, period)
  const actions = getAvailableActions(profile, baseSummary, period, documents)
  const rows = getDocumentRows(profile, baseSummary, period, documents, [])
  const autopilot = getLegalAutopilotPlan(profile, baseSummary, period, documents)

  assert.equal(getAdministrativeProfile(profile).id, 'societe_reel')
  assert.equal(summary.title, 'Preparation comptable T2 2026')
  assert.equal(actions.some((action) => action.id.includes('urssaf') || action.documentType === 'urssaf-summary'), false)
  assert.equal(rows.some((row) => row.id === 'accounting-export-csv'), true)
  assert.equal(autopilot.title, 'Autopilote legal societe')
  assert.equal(autopilot.steps.some((step) => step.id === 'official' && step.owner === 'Utilisateur'), true)
})

test('micro-entreprise expose URSSAF et les registres micro', () => {
  const profile = {
    legalStatus: 'micro',
    siret: '12345678901234',
    vatRegime: 'franchise_base',
    declarationFrequency: 'QUARTERLY',
    activities: ['goods_resale'],
  }
  const period = getDeclarativePeriod(profile, '2026-04-01', '2026-06-30')
  const actions = getAvailableActions(profile, baseSummary, period, documents)
  const autopilot = getLegalAutopilotPlan(profile, baseSummary, period, documents)

  assert.equal(getAdministrativeProfile(profile).id, 'micro_entreprise_achat_revente')
  assert.equal(actions.some((action) => action.id === 'prepare-urssaf'), true)
  assert.equal(actions.some((action) => action.documentType === 'urssaf-summary'), true)
  assert.equal(autopilot.officialTarget.label, 'URSSAF')
})

test('micro-entreprise expose des indicateurs de statut, seuils et rappel URSSAF', () => {
  const profile = {
    legalStatus: 'micro',
    siret: '12345678901234',
    vatRegime: 'franchise_base',
    declarationFrequency: 'QUARTERLY',
    activities: ['goods_resale'],
  }
  const period = getDeclarativePeriod(profile, '2026-04-01', '2026-06-30')
  const indicators = getAdministrativeIndicators(profile, {
    ...baseSummary,
    annualRevenue: 72000,
    missingInvoiceCount: 2,
  }, period, [], new Date('2026-07-03T00:00:00'))

  assert.equal(indicators.some((indicator) => indicator.id === 'overall-status'), true)
  assert.equal(indicators.some((indicator) => indicator.id === 'urssaf-reminder' && indicator.value === 'J-28'), true)
  assert.equal(indicators.some((indicator) => indicator.id === 'micro-threshold'), true)
  assert.equal(indicators.some((indicator) => indicator.id === 'vat-threshold'), true)
  assert.equal(indicators.some((indicator) => indicator.id === 'invoices' && indicator.status === 'to_verify'), true)
})

test('micro-entreprise remonte justificatifs et factures manquants dans les points a corriger', () => {
  const profile = {
    legalStatus: 'micro',
    siret: '12345678901234',
    vatRegime: 'franchise_base',
    declarationFrequency: 'QUARTERLY',
    activities: ['goods_resale'],
  }
  const period = getDeclarativePeriod(profile, '2026-04-01', '2026-06-30')
  const issues = getBlockingIssues(profile, {
    ...baseSummary,
    missingProofCount: 2,
    missingInvoiceCount: 3,
  }, period)

  assert.equal(issues.some((issue) => issue.id === 'missing-proof' && issue.severity === 'warning'), true)
  assert.equal(issues.some((issue) => issue.id === 'missing-invoices' && issue.target === 'invoices'), true)
})

test('particulier occasionnel masque les actions professionnelles', () => {
  const profile = {
    legalStatus: 'personal',
    vatRegime: 'unknown',
    activities: ['second_hand'],
  }
  const period = getDeclarativePeriod(profile, '2026-04-01', '2026-06-30')
  const actions = getAvailableActions(profile, baseSummary, period, documents)
  const issues = getBlockingIssues(profile, baseSummary, period)
  const autopilot = getLegalAutopilotPlan(profile, baseSummary, period, documents)

  assert.equal(getAdministrativeProfile(profile).id, 'particulier_occasionnel')
  assert.equal(actions.some((action) => action.id === 'prepare-vat'), false)
  assert.equal(actions.some((action) => action.id === 'accounts-deposit'), false)
  assert.equal(issues.some((issue) => issue.id === 'vat-unknown'), false)
  assert.equal(autopilot.steps.find((step) => step.id === 'official').status, 'not_applicable')
})
