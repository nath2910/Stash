export const ADMIN_LEGAL_STATUS = Object.freeze({
  none: 'none',
  personal: 'personal',
  micro: 'micro',
  eiReal: 'ei_real',
  eurl: 'eurl',
  sarl: 'sarl',
  sasu: 'sasu',
  sas: 'sas',
  other: 'other',
})

export const ADMIN_LEGAL_STATUS_OPTIONS = Object.freeze([
  { value: ADMIN_LEGAL_STATUS.none, label: 'Aucun statut' },
  { value: ADMIN_LEGAL_STATUS.personal, label: 'Particulier' },
  { value: ADMIN_LEGAL_STATUS.micro, label: 'Micro-entrepreneur' },
  { value: ADMIN_LEGAL_STATUS.eiReal, label: 'EI reel' },
  { value: ADMIN_LEGAL_STATUS.eurl, label: 'EURL' },
  { value: ADMIN_LEGAL_STATUS.sarl, label: 'SARL' },
  { value: ADMIN_LEGAL_STATUS.sasu, label: 'SASU' },
  { value: ADMIN_LEGAL_STATUS.sas, label: 'SAS' },
  { value: ADMIN_LEGAL_STATUS.other, label: 'Autre' },
])

export const ADMIN_ACTIVITY_OPTIONS = Object.freeze([
  { value: 'goods_resale', label: 'Achat-revente' },
  { value: 'services', label: 'Services' },
  { value: 'mixed', label: 'Mixte' },
  { value: 'second_hand', label: 'Occasion' },
  { value: 'consignment', label: 'Depot-vente' },
  { value: 'ecommerce', label: 'E-commerce' },
  { value: 'physical_shop', label: 'Boutique' },
  { value: 'marketplaces', label: 'Marketplaces' },
  { value: 'events', label: 'Evenements' },
])

export const ADMIN_VAT_OPTIONS = Object.freeze([
  { value: 'unknown', label: 'A verifier' },
  { value: 'franchise_base', label: 'Franchise en base' },
  { value: 'real_simplified', label: 'Regime reel simplifie' },
  { value: 'real_normal', label: 'Regime reel normal' },
  { value: 'margin_scheme_possible', label: 'Regime sur marge possible' },
])

export const ADMIN_DECLARATION_OPTIONS = Object.freeze([
  { value: 'urssaf_monthly', label: 'URSSAF mois' },
  { value: 'urssaf_quarterly', label: 'URSSAF trim.' },
  { value: 'vat_monthly', label: 'TVA mois' },
  { value: 'vat_quarterly', label: 'TVA trim.' },
  { value: 'vat_annual', label: 'TVA an' },
  { value: 'annual_closing', label: 'Cloture' },
  { value: 'cfe', label: 'CFE' },
  { value: 'accountant_export', label: 'Export compta' },
])

const LEGACY_STATUS_ALIASES = Object.freeze({
  PARTICULIER: ADMIN_LEGAL_STATUS.personal,
  PERSONAL: ADMIN_LEGAL_STATUS.personal,
  INDIVIDUAL: ADMIN_LEGAL_STATUS.personal,
  INDIVIDUAL_UNDER_5K_MONTH: ADMIN_LEGAL_STATUS.personal,
  MICRO: ADMIN_LEGAL_STATUS.micro,
  MICRO_ENTREPRISE: ADMIN_LEGAL_STATUS.micro,
  MICRO_ENTREPRISE_UNDER_200K_YEAR: ADMIN_LEGAL_STATUS.micro,
  AUTO_ENTREPRENEUR: ADMIN_LEGAL_STATUS.micro,
  EI_REAL: ADMIN_LEGAL_STATUS.eiReal,
  EURL: ADMIN_LEGAL_STATUS.eurl,
  SARL: ADMIN_LEGAL_STATUS.sarl,
  SASU: ADMIN_LEGAL_STATUS.sasu,
  SAS: ADMIN_LEGAL_STATUS.sas,
  OTHER: ADMIN_LEGAL_STATUS.other,
  NONE: ADMIN_LEGAL_STATUS.none,
})

export function normalizeAdminLegalStatus(value, fallback = ADMIN_LEGAL_STATUS.none) {
  if (!value) return fallback
  const upper = String(value).trim().toUpperCase()
  if (LEGACY_STATUS_ALIASES[upper]) return LEGACY_STATUS_ALIASES[upper]
  const lower = upper.toLowerCase()
  return ADMIN_LEGAL_STATUS_OPTIONS.some((option) => option.value === lower) ? lower : fallback
}

export function getAdminOptionLabel(options, value, fallback = 'A verifier') {
  return options.find((option) => option.value === value)?.label || fallback
}

export function getLegalStatusLabel(value) {
  return getAdminOptionLabel(ADMIN_LEGAL_STATUS_OPTIONS, normalizeAdminLegalStatus(value))
}

export function getVatRegimeLabel(value) {
  return getAdminOptionLabel(ADMIN_VAT_OPTIONS, value || 'unknown')
}

export function getDeclarationLabel(value) {
  return getAdminOptionLabel(ADMIN_DECLARATION_OPTIONS, value)
}

export function defaultActivitiesForStatus(legalStatus) {
  const status = normalizeAdminLegalStatus(legalStatus)
  if (status === ADMIN_LEGAL_STATUS.micro) return ['goods_resale']
  if (status === ADMIN_LEGAL_STATUS.none || status === ADMIN_LEGAL_STATUS.personal) return ['second_hand']
  return ['goods_resale', 'ecommerce']
}

export function defaultDeclarationsForStatus(legalStatus, frequency = 'UNKNOWN') {
  const status = normalizeAdminLegalStatus(legalStatus)
  if (status === ADMIN_LEGAL_STATUS.micro) {
    return [frequency === 'MONTHLY' ? 'urssaf_monthly' : 'urssaf_quarterly', 'cfe']
  }
  if (status === ADMIN_LEGAL_STATUS.eiReal || isCompanyStatus(status)) {
    return ['vat_annual', 'annual_closing', 'accountant_export']
  }
  return []
}

export function isCompanyStatus(legalStatus) {
  return ['eurl', 'sarl', 'sasu', 'sas', 'other'].includes(normalizeAdminLegalStatus(legalStatus))
}

export function isMicroStatus(legalStatus) {
  return normalizeAdminLegalStatus(legalStatus) === ADMIN_LEGAL_STATUS.micro
}

export function normalizeAdministrativeProfile(profile = {}) {
  const legalStatus = normalizeAdminLegalStatus(profile.legalStatus || profile.profileType)
  const declarationFrequency = profile.declarationFrequency || 'UNKNOWN'
  const activities = Array.isArray(profile.activities) && profile.activities.length
    ? profile.activities
    : defaultActivitiesForStatus(legalStatus)
  const declarations = Array.isArray(profile.declarations) && profile.declarations.length
    ? profile.declarations
    : defaultDeclarationsForStatus(legalStatus, declarationFrequency)

  return {
    ...profile,
    legalStatus,
    activities,
    declarations,
    vatRegime: profile.vatRegime || (profile.vatFranchise === 'YES' ? 'franchise_base' : 'unknown'),
    businessName: profile.businessName || profile.tradeName || profile.displayName || '',
    ownerName: profile.ownerName || profile.displayName || '',
    siret: profile.siret || '',
    siren: profile.siren || (profile.siret ? String(profile.siret).slice(0, 9) : ''),
    legalForm: profile.legalForm || getLegalStatusLabel(legalStatus),
    fiscalYearEndMonth: Number(profile.fiscalYearEndMonth || 12),
    fiscalYearEndDay: Number(profile.fiscalYearEndDay || 31),
    declarationPeriodicity: profile.declarationPeriodicity || declarations[0] || '',
    urssafCategory: profile.urssafCategory || 'Vente de marchandises',
    defaultVatRate: profile.defaultVatRate ?? '',
    notes: profile.notes || '',
    usesOnlinePlatforms: Boolean(profile.usesOnlinePlatforms),
    buysForResale: Boolean(profile.buysForResale),
  }
}
