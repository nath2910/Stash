import {
  getLegalStatusLabel,
  isCompanyStatus,
  isMicroStatus,
  normalizeAdministrativeProfile,
} from '../constants/adminModule.js'

const MICRO_GOODS_THRESHOLD_2026 = 203100
const VAT_GOODS_BASE_THRESHOLD_2026 = 85000
const VAT_GOODS_TOLERANCE_THRESHOLD_2026 = 93500
const VAT_ACTIVE_REGIMES = new Set(['real_simplified', 'real_normal', 'margin_scheme_possible'])

export function getAdministrativeProfile(profile = {}) {
  const normalized = normalizeAdministrativeProfile(profile)
  const legalStatus = normalized.legalStatus || 'none'

  if (isMicroStatus(legalStatus)) {
    return {
      id: normalized.vatRegime === 'franchise_base'
        ? 'micro_entreprise_achat_revente'
        : 'micro_entreprise_tva_a_surveiller',
      family: 'micro',
      label: normalized.vatRegime === 'franchise_base'
        ? 'Micro-entreprise achat-revente'
        : 'Micro-entreprise, TVA a surveiller',
      legalStatus,
      channels: [],
    }
  }

  if (legalStatus === 'ei_real' || isCompanyStatus(legalStatus)) {
    return {
      id: 'societe_reel',
      family: 'company',
      label: `${getLegalStatusLabel(legalStatus)} au reel`,
      legalStatus,
      channels: [],
    }
  }

  if (legalStatus === 'personal') {
    return {
      id: 'particulier_occasionnel',
      family: 'personal',
      label: 'Particulier occasionnel',
      legalStatus,
      channels: [],
    }
  }

  return {
    id: 'achat_revente_a_regulariser',
    family: 'regularization',
    label: 'Activite a qualifier',
    legalStatus,
    channels: [],
  }
}

export function getDeclarativePeriod(profile = {}, periodStart, periodEnd) {
  const normalized = normalizeAdministrativeProfile(profile)
  const start = parseLocalDate(periodStart)
  const end = parseLocalDate(periodEnd)
  const year = end?.getFullYear() || start?.getFullYear() || new Date().getFullYear()
  const month = (start?.getMonth() ?? 0) + 1
  const quarter = Math.floor((month - 1) / 3) + 1
  const kind = isMicroStatus(normalized.legalStatus) && normalized.declarationFrequency === 'MONTHLY'
    ? 'month'
    : 'quarter'
  const label = kind === 'month' ? monthLabel(month, year) : `T${quarter} ${year}`
  const fiscalEndMonth = Number(normalized.fiscalYearEndMonth || 12)
  const fiscalEndDay = Number(normalized.fiscalYearEndDay || 31)

  return {
    start: periodStart,
    end: periodEnd,
    year,
    month,
    quarter,
    kind,
    label,
    isClosingPeriod: Boolean(end)
      && end.getMonth() + 1 === fiscalEndMonth
      && end.getDate() === fiscalEndDay,
  }
}

export function shouldUseQuarterPeriod(profile = {}) {
  const normalized = normalizeAdministrativeProfile(profile)
  if (isMicroStatus(normalized.legalStatus)) {
    return normalized.declarationFrequency !== 'MONTHLY'
  }
  return true
}

export function getDeclarationSummary(profile = {}, summary = {}, period = {}) {
  const adminProfile = getAdministrativeProfile(profile)
  const revenue = amount(summary?.periodRevenue)
  const purchases = amount(summary?.periodPurchaseTotal)
  const blockedCount = count(summary?.incompleteSaleCount)
  const saleCount = count(summary?.periodSaleCount)

  return {
    profile: adminProfile,
    title: titleForProfile(adminProfile, period),
    amountLabel: adminProfile.family === 'micro' ? 'CA encaisse a declarer' : 'CA encaisse',
    amount: revenue,
    resultLabel: adminProfile.family === 'company' ? 'Resultat estime' : 'Benefice estime',
    estimatedResult: revenue - purchases,
    saleCount,
    status: blockedCount > 0 ? 'incomplete' : saleCount > 0 ? 'ready' : 'pending',
  }
}

export function getDeclarationFields(profile = {}, summary = {}, period = {}) {
  const normalized = normalizeAdministrativeProfile(profile)
  const adminProfile = getAdministrativeProfile(normalized)
  const revenue = amount(summary?.periodRevenue)
  const purchases = amount(summary?.periodPurchaseTotal)
  const fields = [
    field('period', 'Periode', period.label || '-', period.label || ''),
    field('revenue', 'CA encaisse', formatMoney(revenue), formatAmountForCopy(revenue)),
    field('included-sales', 'Ventes incluses', formatNumber(summary?.periodSaleCount), String(count(summary?.periodSaleCount))),
  ]

  if (adminProfile.family === 'micro') {
    fields.push(
      field('urssaf-category', 'Rubrique URSSAF', normalized.urssafCategory || 'Vente de marchandises', normalized.urssafCategory || 'Vente de marchandises'),
      field('siret', 'SIRET', normalized.siret || 'A completer', normalized.siret || '', !normalized.siret ? 'Information manquante pour preparer la saisie.' : ''),
    )
  } else if (adminProfile.family === 'company') {
    fields.push(
      field('purchases', 'Achats rattaches', formatMoney(purchases), formatAmountForCopy(purchases)),
      field('estimated-result', 'Resultat estime', formatMoney(revenue - purchases), formatAmountForCopy(revenue - purchases)),
      field('vat', 'TVA', vatPreparationLabel(normalized), ''),
    )
  }

  return fields
}

export function getPrefilledFields(profile = {}, summary = {}, period = {}) {
  return getDeclarationFields(profile, summary, period)
}

export function getBlockingIssues(profile = {}, summary = {}, period = {}) {
  const normalized = normalizeAdministrativeProfile(profile)
  const adminProfile = getAdministrativeProfile(normalized)
  const issues = []
  const missingDate = count(summary?.missingSaleDateCount)
  const missingAmount = count(summary?.missingSaleAmountCount)
  const missingPurchase = countMissingPurchases(summary?.sales)
  const missingProof = count(summary?.missingProofCount)
  const missingInvoice = count(summary?.missingInvoiceCount)

  if (normalized.legalStatus === 'none') {
    issues.push(issue('profile-status', 'warning', 'Profil administratif incomplet', 'Le statut administratif doit etre configure.', 'Configurer', 'profile'))
  }

  if ((adminProfile.family === 'micro' || adminProfile.family === 'company') && !normalized.siret) {
    issues.push(issue('siret', 'warning', 'SIRET manquant', 'Le SIRET manque dans les parametres administratifs.', 'Configurer', 'profile'))
  }

  if (adminProfile.family === 'micro' && (!normalized.declarationFrequency || normalized.declarationFrequency === 'UNKNOWN')) {
    issues.push(issue('urssaf-frequency', 'warning', 'Periodicite URSSAF non configuree', 'La periodicite mensuelle ou trimestrielle doit etre confirmee.', 'Configurer', 'profile'))
  }

  if (missingDate > 0) {
    issues.push(issue('missing-cash-in-date', 'danger', `${formatNumber(missingDate)} ventes sans date d'encaissement`, 'Ces ventes ne peuvent pas etre incluses dans la declaration.', 'Corriger dans Gestion', 'sales'))
  }

  if (missingAmount > 0) {
    issues.push(issue('missing-sale-amount', 'danger', `${formatNumber(missingAmount)} ventes sans prix de vente`, 'Ces ventes n ont pas de montant exploitable.', 'Corriger dans Gestion', 'sales'))
  }

  if (adminProfile.family !== 'personal' && missingPurchase > 0) {
    issues.push(issue('missing-purchase', 'warning', `${formatNumber(missingPurchase)} achats manquants`, 'Ces achats peuvent bloquer les registres ou la marge estimee.', 'Corriger dans Gestion', 'sales'))
  }

  if (adminProfile.family !== 'personal' && missingProof > 0) {
    issues.push(issue('missing-proof', 'warning', `${formatNumber(missingProof)} justificatifs manquants`, 'Ces ventes n ont pas de preuve ou piece rattachee detectable.', 'Corriger dans Gestion', 'sales'))
  }

  if ((adminProfile.family === 'micro' || adminProfile.family === 'company') && missingInvoice > 0) {
    issues.push(issue('missing-invoices', 'warning', `${formatNumber(missingInvoice)} factures manquantes`, 'Ces ventes encaissees n ont pas encore de facture rattachee.', 'Generer les factures', 'invoices'))
  }

  if (adminProfile.family === 'company' && normalized.vatRegime === 'unknown') {
    issues.push(issue('vat-unknown', 'warning', 'TVA a configurer', 'Le regime TVA de la societe est inconnu.', 'Configurer la TVA', 'profile'))
  }

  if (!count(summary?.periodSaleCount) && !issues.length) {
    issues.push(issue('no-sales', 'info', 'Periode sans vente exploitable', 'Aucune vente exploitable n est retenue sur cette periode.', 'Verifier la periode', 'period'))
  }

  if (period?.start && period?.end && period.start > period.end) {
    issues.push(issue('period-range', 'danger', 'Periode invalide', 'La periode affichee est incoherente.', 'Changer la periode', 'period'))
  }

  return issues
}

export function getDocumentRows(profile = {}, summary = {}, period = {}, documents = [], generatedRecords = []) {
  const adminProfile = getAdministrativeProfile(profile)
  const documentMap = new Map((documents || []).map((document) => [document.id, document]))
  const blockers = getBlockingIssues(profile, summary, period)
  const hasHardBlockers = blockers.some((item) => item.severity === 'danger')
  const rows = []

  if (adminProfile.family === 'micro') {
    addDocument(rows, documentMap, 'urssaf-summary', 'Fiche URSSAF', 'PDF', true)
    addDocument(rows, documentMap, 'receipts-register', 'Livre des recettes', 'PDF', true)
    addDocument(rows, documentMap, 'purchases-register', 'Registre des achats', 'PDF', false)
    rows.push(csvRow())
    addDocument(rows, documentMap, 'fiscal-summary', 'Recap annuel 2042-C-PRO', 'PDF', false, String(period.year || ''))
  } else if (adminProfile.family === 'company') {
    rows.push(csvRow())
    addDocument(rows, documentMap, 'receipts-register', 'Registre ventes', 'PDF', true)
    addDocument(rows, documentMap, 'purchases-register', 'Registre achats', 'PDF', false)
    addDocument(rows, documentMap, 'fiscal-summary', 'Recap resultat annuel', 'PDF', false, String(period.year || ''))
  } else {
    addDocument(rows, documentMap, 'receipts-register', 'Recap ventes', 'PDF', false)
  }

  return rows.map((row) => {
    const record = latestRecordFor(row.id, generatedRecords)
    const disabled = Boolean(row.requiresCleanData && hasHardBlockers)
    return {
      ...row,
      period: row.period || period.label || '-',
      lines: linesForDocument(row.id, summary),
      generatedAt: record?.generatedAt || record?.createdAt || '',
      status: disabled ? 'data_missing' : record ? 'generated' : 'regenerable',
      statusLabel: disabled ? 'Donnees manquantes' : record ? 'Document genere' : 'A generer',
      disabled,
      missingReason: disabled ? 'Corriger les ventes bloquees avant generation.' : '',
    }
  })
}

export function getAvailableActions(profile = {}, summary = {}, period = {}, documents = []) {
  const adminProfile = getAdministrativeProfile(profile)
  const documentIds = new Set((documents || []).map((document) => document.id))
  const hasHardBlockers = getBlockingIssues(profile, summary, period).some((item) => item.severity === 'danger')
  const actions = []

  if (hasHardBlockers) {
    actions.push(action('fix-sales', 'Corriger les ventes', 'route', 'incomplete', { primary: true }))
  }

  if (adminProfile.family === 'micro') {
    if (!hasHardBlockers) {
      actions.push(action('prepare-urssaf', 'Preparer URSSAF', 'tab', 'to_verify', { primary: true }))
      actions.push(action('copy-revenue', 'Copier CA encaisse', 'copy', 'to_verify', { copyKey: 'amount' }))
    }
    if (documentIds.has('urssaf-summary')) {
      actions.push(action('urssaf-summary', 'Generer fiche URSSAF', 'document', hasHardBlockers ? 'incomplete' : 'to_verify', { documentType: 'urssaf-summary', disabled: hasHardBlockers }))
    }
    actions.push(action('accounting-export', 'Generer export comptable', 'export', hasHardBlockers ? 'incomplete' : 'to_verify', { documentType: 'accounting-export-csv', disabled: hasHardBlockers }))
  } else if (adminProfile.family === 'company') {
    if (!hasHardBlockers) {
      actions.push(action('accounting-export', 'Generer export comptable', 'export', 'to_verify', { documentType: 'accounting-export-csv', primary: true }))
    }
    if (VAT_ACTIVE_REGIMES.has(normalizeAdministrativeProfile(profile).vatRegime)) {
      actions.push(action('prepare-vat', 'Preparer TVA', 'tab', 'to_verify'))
    }
  } else if (documentIds.has('receipts-register')) {
    actions.push(action('personal-sales-export', 'Export recap ventes', 'document', 'to_verify', { documentType: 'receipts-register' }))
  }

  return actions
}

export function getAdministrativeIndicators(profile = {}, summary = {}, period = {}, generatedRecords = [], todayValue = new Date()) {
  const adminProfile = getAdministrativeProfile(profile)
  const checks = getBlockingIssues(profile, summary, period).filter((item) => item.id !== 'no-sales')
  const hardCount = checks.filter((item) => item.severity === 'danger').length
  const warningCount = checks.filter((item) => item.severity === 'warning').length
  const indicators = [
    indicator(
      'overall-status',
      'Statut dossier',
      hardCount ? 'Bloque' : warningCount ? 'A surveiller' : 'OK',
      hardCount ? `${formatNumber(hardCount)} blocage(s) a corriger.` : warningCount ? `${formatNumber(warningCount)} point(s) a surveiller.` : 'Aucun blocage detecte.',
      hardCount ? 'incomplete' : warningCount ? 'to_verify' : 'complete',
    ),
  ]

  if (adminProfile.family === 'micro') {
    indicators.push(urssafReminderIndicator(period, todayValue, generatedRecords))
    indicators.push(thresholdIndicator('micro-threshold', 'Seuil micro-fiscal', amount(summary?.annualRevenue), MICRO_GOODS_THRESHOLD_2026, 'CA annuel achat-revente.'))
    indicators.push(thresholdIndicator('vat-threshold', 'Seuil TVA', amount(summary?.annualRevenue), VAT_GOODS_BASE_THRESHOLD_2026, `Tolerance ${formatMoney(VAT_GOODS_TOLERANCE_THRESHOLD_2026)}.`))
  }

  indicators.push(indicator(
    'invoices',
    'Factures',
    count(summary?.missingInvoiceCount) ? `${formatNumber(summary?.missingInvoiceCount)} manquante(s)` : 'A jour',
    count(summary?.missingInvoiceCount) ? 'Ventes encaissees sans facture rattachee.' : 'Aucune facture manquante detectee.',
    count(summary?.missingInvoiceCount) ? 'to_verify' : 'complete',
  ))

  return indicators
}

export function getLegalAutopilotPlan(profile = {}, summary = {}, period = {}, documents = []) {
  const adminProfile = getAdministrativeProfile(profile)
  const officialTarget = officialTargetForProfile(adminProfile)

  return {
    title: legalScopeTitle(adminProfile),
    subtitle: legalScopeSubtitle(adminProfile),
    promise: 'MyStash prepare les montants, controles et documents. La validation officielle reste manuelle.',
    officialTarget,
    steps: [
      autopilotStep('collect', 'Recuperer les donnees', 'Ventes, achats et pieces disponibles.', 'complete', 'MyStash'),
      autopilotStep('control', 'Controler les blocages', 'Dates, montants, profil et documents.', getBlockingIssues(profile, summary, period).some((item) => item.severity === 'danger') ? 'incomplete' : 'complete', 'MyStash'),
      autopilotStep('calculate', 'Calculer les montants', `${formatMoney(summary?.periodRevenue)} de CA encaisse.`, 'complete', 'MyStash'),
      autopilotStep('documents', 'Generer le dossier legal', `${formatNumber(documents?.length || 0)} document(s) disponible(s).`, 'to_verify', 'MyStash'),
      autopilotStep('official', 'Valider sur le portail officiel', officialTarget.action, adminProfile.family === 'personal' ? 'not_applicable' : 'pending', 'Utilisateur'),
    ],
  }
}

function titleForProfile(adminProfile, period) {
  if (adminProfile.family === 'micro') return `Declaration URSSAF ${period.label || ''}`.trim()
  if (adminProfile.family === 'company') return `Preparation comptable ${period.label || ''}`.trim()
  if (adminProfile.family === 'regularization') return `Diagnostic d'activite ${period.label || ''}`.trim()
  return `Suivi personnel ${period.label || ''}`.trim()
}

function addDocument(rows, documentMap, id, fallbackName, fallbackFormat, requiresCleanData, period = '') {
  const descriptor = documentMap.get(id)
  if (!descriptor && id !== 'receipts-register' && id !== 'purchases-register') return
  rows.push({
    id,
    name: descriptor?.title || fallbackName,
    format: descriptor?.format || fallbackFormat,
    type: descriptor?.format || fallbackFormat,
    period,
    kind: 'document',
    documentType: id,
    actionLabel: descriptor?.format === 'CSV' ? 'Telecharger CSV' : 'Generer PDF',
    requiresCleanData,
  })
}

function csvRow() {
  return {
    id: 'accounting-export-csv',
    name: 'Export comptable CSV',
    format: 'CSV',
    type: 'CSV',
    kind: 'export',
    documentType: 'accounting-export-csv',
    actionLabel: 'Telecharger CSV',
    requiresCleanData: true,
  }
}

function latestRecordFor(documentId, records = []) {
  return [...(records || [])]
    .filter((record) => (record?.documentType || record?.type) === documentId)
    .sort((a, b) => String(b?.generatedAt || b?.createdAt || '').localeCompare(String(a?.generatedAt || a?.createdAt || '')))[0]
}

function linesForDocument(id, summary) {
  if (id === 'purchases-register') return formatNumber(summary?.periodPurchaseCount)
  if (id === 'accounting-export-csv') return formatNumber(count(summary?.periodSaleCount) + count(summary?.periodPurchaseCount))
  return formatNumber(summary?.periodSaleCount)
}

function urssafReminderIndicator(period, todayValue, records) {
  const done = latestRecordFor('urssaf-declaration', records)
  const deadline = declarationDeadline(period)
  const diffDays = Math.ceil((deadline.getTime() - startOfDay(todayValue).getTime()) / 86_400_000)
  return indicator(
    'urssaf-reminder',
    'Rappel URSSAF',
    done ? 'Fait' : diffDays >= 0 ? `J-${diffDays}` : `J+${Math.abs(diffDays)}`,
    done ? 'Declaration marquee comme faite.' : `Echeance estimee au ${formatDate(deadline)}.`,
    done ? 'complete' : diffDays < 0 ? 'incomplete' : 'to_verify',
  )
}

function thresholdIndicator(id, label, value, threshold, detail) {
  const progress = threshold > 0 ? Math.min(100, Math.round((amount(value) / threshold) * 100)) : 0
  return {
    id,
    label,
    value: `${progress}%`,
    detail,
    status: progress >= 100 ? 'incomplete' : progress >= 80 ? 'to_verify' : 'complete',
    progress,
  }
}

function declarationDeadline(period) {
  const end = parseLocalDate(period?.end) || new Date()
  return new Date(end.getFullYear(), end.getMonth() + 2, 0)
}

function officialTargetForProfile(adminProfile) {
  if (adminProfile.family === 'micro') {
    return {
      label: 'URSSAF',
      action: 'Recopier et valider sur autoentrepreneur.urssaf.fr',
      limitation: 'Pas de teletransmission officielle sans mandat certifie.',
    }
  }
  if (adminProfile.family === 'company') {
    return {
      label: 'Impots / comptable',
      action: 'Transmettre le dossier au comptable ou saisir sur les portails officiels',
      limitation: 'Pas de depot fiscal automatique.',
    }
  }
  return {
    label: 'Suivi personnel',
    action: 'Conserver le recapitulatif',
    limitation: 'Aucune declaration professionnelle affichee.',
  }
}

function legalScopeTitle(adminProfile) {
  if (adminProfile.family === 'micro') return 'Autopilote legal micro-entreprise'
  if (adminProfile.family === 'company') return 'Autopilote legal societe'
  if (adminProfile.family === 'regularization') return 'Autopilote de regularisation'
  return 'Autopilote suivi personnel'
}

function legalScopeSubtitle(adminProfile) {
  if (adminProfile.family === 'micro') return 'URSSAF, livre des recettes, registre achats et recap annuel.'
  if (adminProfile.family === 'company') return 'Comptabilite, TVA, registres et export comptable.'
  return 'Suivi des ventes et recapitulatif exportable.'
}

function vatPreparationLabel(profile = {}) {
  const normalized = normalizeAdministrativeProfile(profile)
  if (normalized.vatRegime === 'franchise_base') return 'Franchise en base, seuils a surveiller'
  if (VAT_ACTIVE_REGIMES.has(normalized.vatRegime)) return 'Preparation TVA disponible'
  return 'TVA a configurer'
}

function issue(id, severity, title, message, actionLabel, target) {
  return {
    id,
    severity,
    status: severity === 'danger' ? 'incomplete' : severity === 'warning' ? 'to_verify' : 'pending',
    title,
    message,
    actionLabel,
    target,
  }
}

function action(id, label, kind, status, extra = {}) {
  return {
    id,
    label,
    kind,
    status,
    disabled: false,
    primary: false,
    ...extra,
  }
}

function indicator(id, label, value, detail, status) {
  return { id, label, value, detail, status, progress: null }
}

function autopilotStep(id, title, detail, status, owner) {
  return { id, title, detail, status, owner }
}

function field(id, label, value, copyValue, detail = '') {
  return { id, label, value, copyValue, detail, missing: Boolean(detail && !copyValue) }
}

function countMissingPurchases(sales = []) {
  return (Array.isArray(sales) ? sales : []).filter((sale) => amount(sale?.purchaseAmount ?? sale?.prixAchat ?? sale?.purchasePrice) <= 0).length
}

function amount(value) {
  const numberValue = Number(value || 0)
  return Number.isFinite(numberValue) ? numberValue : 0
}

function count(value) {
  const numberValue = Number(value || 0)
  return Number.isFinite(numberValue) ? Math.max(0, Math.trunc(numberValue)) : 0
}

function formatMoney(value) {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 2 }).format(amount(value))
}

function formatNumber(value) {
  return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(count(value))
}

function formatAmountForCopy(value) {
  return amount(value).toFixed(2).replace('.', ',')
}

function formatDate(value) {
  return new Intl.DateTimeFormat('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(value)
}

function monthLabel(month, year) {
  const date = new Date(year, Math.max(0, Number(month || 1) - 1), 1)
  return new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' }).format(date)
}

function parseLocalDate(value) {
  if (!value) return null
  const text = String(value).slice(0, 10)
  const [year, month, day] = text.split('-').map(Number)
  if (!year || !month || !day) return null
  return new Date(year, month - 1, day)
}

function startOfDay(value) {
  const date = value instanceof Date ? value : new Date(value)
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}
