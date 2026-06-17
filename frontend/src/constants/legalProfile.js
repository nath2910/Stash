export const LEGAL_PROFILE_TYPE = Object.freeze({
  individual: 'PARTICULIER',
  micro: 'MICRO_ENTREPRISE',
})

const LEGAL_PROFILE_TYPE_ALIASES = Object.freeze({
  PARTICULIER: LEGAL_PROFILE_TYPE.individual,
  INDIVIDUAL: LEGAL_PROFILE_TYPE.individual,
  INDIVIDUAL_UNDER_5K_MONTH: LEGAL_PROFILE_TYPE.individual,
  MICRO: LEGAL_PROFILE_TYPE.micro,
  MICRO_ENTREPRISE: LEGAL_PROFILE_TYPE.micro,
  MICRO_ENTREPRISE_UNDER_200K_YEAR: LEGAL_PROFILE_TYPE.micro,
})

export const LEGAL_PROFILE_ADMIN_KEY = Object.freeze({
  individual: 'light_reseller',
  micro: 'regular_reseller',
})

export const LEGAL_PROFILE_ADMIN_STORAGE_KEY = 'snk_admin_action_page_v1'

export const MICRO_LEGAL_PROFILE_DEFAULTS = Object.freeze({
  taxCategory: 'BIC',
  businessRegime: 'MICRO_ENTREPRISE',
  businessActivityType: 'ACHAT_REVENTE',
  declaredRevenueThreshold: 'UNDER_200K_YEAR',
})

export const LEGAL_PROFILE_OPTIONS = Object.freeze([
  {
    type: LEGAL_PROFILE_TYPE.individual,
    title: 'Particulier',
    description: 'Je revends occasionnellement des biens personnels, sans achat organise pour revente.',
    traits: ['Aucun SIRET requis', 'Cas particuliers a verifier'],
  },
  {
    type: LEGAL_PROFILE_TYPE.micro,
    title: 'Micro-entreprise achat-revente',
    description: "Je suis micro-entrepreneur et je declare le chiffre d'affaires brut encaisse.",
    detail: 'Achat-revente generalement rattache au micro-BIC vente de marchandises.',
    traits: ['SIRET obligatoire', 'Micro-BIC', 'CA brut encaisse'],
  },
])

export const DEFAULT_LEGAL_PROFILE_ADMIN_SETTINGS = Object.freeze({
  profileType: LEGAL_PROFILE_ADMIN_KEY.individual,
  tradeName: '',
  legalName: '',
  siret: '',
  address: '',
  email: '',
  declarationFrequency: 'monthly',
  vatMode: 'franchise',
  usesPlatforms: true,
  buysFromIndividuals: false,
})

const [INDIVIDUAL_PROFILE_OPTION, MICRO_PROFILE_OPTION] = LEGAL_PROFILE_OPTIONS

export const LEGAL_PROFILE_ADMIN_DEFINITIONS = Object.freeze([
  {
    key: LEGAL_PROFILE_ADMIN_KEY.individual,
    legalProfileType: INDIVIDUAL_PROFILE_OPTION.type,
    label: INDIVIDUAL_PROFILE_OPTION.title,
    level: 'low',
    levelLabel: INDIVIDUAL_PROFILE_OPTION.title,
    description: INDIVIDUAL_PROFILE_OPTION.description,
    requiredFields: [],
    optionalFields: [],
    traits: INDIVIDUAL_PROFILE_OPTION.traits,
  },
  {
    key: LEGAL_PROFILE_ADMIN_KEY.micro,
    legalProfileType: MICRO_PROFILE_OPTION.type,
    label: MICRO_PROFILE_OPTION.title,
    level: 'medium',
    levelLabel: 'Micro',
    description: MICRO_PROFILE_OPTION.description,
    requiredFields: ['siret'],
    optionalFields: ['legalName', 'tradeName', 'address', 'email'],
    traits: MICRO_PROFILE_OPTION.traits,
  },
])

const LEGAL_PROFILE_ADMIN_ALIASES = Object.freeze({
  occasional_private: LEGAL_PROFILE_ADMIN_KEY.individual,
  regularization_needed: LEGAL_PROFILE_ADMIN_KEY.micro,
  micro_reseller: LEGAL_PROFILE_ADMIN_KEY.micro,
  micro_vat_watch: LEGAL_PROFILE_ADMIN_KEY.micro,
  online_shop: LEGAL_PROFILE_ADMIN_KEY.micro,
  physical_shop: LEGAL_PROFILE_ADMIN_KEY.micro,
  company_real: LEGAL_PROFILE_ADMIN_KEY.micro,
  employer: LEGAL_PROFILE_ADMIN_KEY.micro,
})

export function normalizeLegalProfileType(type) {
  if (!type) return null
  const key = String(type).trim().toUpperCase()
  return LEGAL_PROFILE_TYPE_ALIASES[key] || key
}

export function normalizeLegalProfile(source = {}) {
  const raw = source?.legalProfile && typeof source.legalProfile === 'object' ? source.legalProfile : source
  const legalProfileType = normalizeLegalProfileType(raw?.legalProfileType || raw?.profileType || null)
  const isMicro = legalProfileType === LEGAL_PROFILE_TYPE.micro

  return {
    legalProfileType,
    siret: isMicro ? raw?.siret || '' : '',
    siren: isMicro ? raw?.siren || (raw?.siret ? String(raw.siret).slice(0, 9) : '') : '',
    taxCategory: isMicro ? raw?.taxCategory || MICRO_LEGAL_PROFILE_DEFAULTS.taxCategory : null,
    businessRegime: isMicro ? raw?.businessRegime || MICRO_LEGAL_PROFILE_DEFAULTS.businessRegime : null,
    businessActivityType:
      isMicro ? raw?.businessActivityType || MICRO_LEGAL_PROFILE_DEFAULTS.businessActivityType : null,
    declaredRevenueThreshold:
      isMicro ? raw?.declaredRevenueThreshold || MICRO_LEGAL_PROFILE_DEFAULTS.declaredRevenueThreshold : null,
    vatNumber: isMicro ? raw?.vatNumber || '' : '',
    vatStatus: isMicro ? raw?.vatStatus || raw?.vatFranchise || 'UNKNOWN' : null,
    declarationFrequency: isMicro ? raw?.declarationFrequency || 'UNKNOWN' : null,
    withholdingTaxOption: isMicro ? raw?.withholdingTaxOption || 'UNKNOWN' : null,
    vatFranchise: isMicro ? raw?.vatFranchise || 'UNKNOWN' : null,
    activityStartDate: isMicro ? raw?.activityStartDate || '' : '',
    completed: Boolean(raw?.completed ?? raw?.legalProfileCompleted ?? false),
    updatedAt: raw?.updatedAt || raw?.legalProfileUpdatedAt || null,
  }
}

export function isLegalProfileCompleted(source = {}) {
  if (!source) return false
  if (source.legalProfile && typeof source.legalProfile === 'object') {
    return Boolean(source.legalProfile.completed)
  }
  return Boolean(source.completed ?? source.legalProfileCompleted)
}

export function getLegalProfileOption(type) {
  const normalizedType = normalizeLegalProfileType(type)
  return (
    LEGAL_PROFILE_OPTIONS.find((option) => option.type === normalizedType) || {
      type: null,
      title: 'Profil non renseigne',
      description: 'Profil administratif a completer.',
      traits: ['A completer'],
    }
  )
}

export function getLegalProfileLabel(profile = {}) {
  return getLegalProfileOption(normalizeLegalProfile(profile).legalProfileType).title
}

export function getLegalProfileDetails(profile = {}) {
  const normalized = normalizeLegalProfile(profile)

  if (!normalized.completed) return ['Profil a completer']

  if (normalized.legalProfileType === LEGAL_PROFILE_TYPE.micro) {
    return [
      normalized.siret ? `SIRET ${normalized.siret}` : 'SIRET manquant',
      normalized.declarationFrequency === 'QUARTERLY'
        ? 'Declaration trimestrielle'
        : normalized.declarationFrequency === 'MONTHLY'
          ? 'Declaration mensuelle'
          : 'Periodicite a renseigner',
      'Micro-BIC',
      'CA brut encaisse',
    ]
  }

  return ['Vente occasionnelle de biens personnels', 'Pas de seuil simplifie automatique']
}

export function normalizeLegalProfileAdminKey(profileKey) {
  const normalized = LEGAL_PROFILE_ADMIN_ALIASES[profileKey] || profileKey
  return LEGAL_PROFILE_ADMIN_DEFINITIONS.some((profile) => profile.key === normalized)
    ? normalized
    : LEGAL_PROFILE_ADMIN_KEY.individual
}

export function getLegalProfileAdminKey(profile = {}) {
  const normalized = normalizeLegalProfile(profile)
  return normalizeLegalProfileAdminKey(
    normalized.legalProfileType === LEGAL_PROFILE_TYPE.micro
      ? LEGAL_PROFILE_ADMIN_KEY.micro
      : LEGAL_PROFILE_ADMIN_KEY.individual,
  )
}

export function isValidSiret(value) {
  return /^\d{14}$/.test(String(value || '').trim())
}

export function buildLegalProfilePayload(type, form = {}) {
  const normalizedType = normalizeLegalProfileType(type)
  if (normalizedType === LEGAL_PROFILE_TYPE.individual) {
    return { legalProfileType: LEGAL_PROFILE_TYPE.individual }
  }

  return {
    legalProfileType: LEGAL_PROFILE_TYPE.micro,
    siret: String(form.siret || '').trim(),
    taxCategory: form.taxCategory || MICRO_LEGAL_PROFILE_DEFAULTS.taxCategory,
    businessRegime: form.businessRegime || MICRO_LEGAL_PROFILE_DEFAULTS.businessRegime,
    businessActivityType:
      form.businessActivityType || MICRO_LEGAL_PROFILE_DEFAULTS.businessActivityType,
    declaredRevenueThreshold:
      form.declaredRevenueThreshold || MICRO_LEGAL_PROFILE_DEFAULTS.declaredRevenueThreshold,
    vatNumber: String(form.vatNumber || '').trim() || null,
    vatStatus: form.vatStatus || 'UNKNOWN',
    declarationFrequency: form.declarationFrequency || 'UNKNOWN',
    withholdingTaxOption: form.withholdingTaxOption || 'UNKNOWN',
    vatFranchise: form.vatFranchise || 'UNKNOWN',
    activityStartDate: form.activityStartDate || null,
  }
}
