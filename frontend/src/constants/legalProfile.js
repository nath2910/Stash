export const LEGAL_PROFILE_TYPE = Object.freeze({
  individual: 'INDIVIDUAL_UNDER_5K_MONTH',
  micro: 'MICRO_ENTREPRISE_UNDER_200K_YEAR',
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
    description: "Je fais moins de 5 000 € de chiffre d'affaires par mois.",
    traits: ['Aucun SIRET requis', 'Suivi administratif'],
  },
  {
    type: LEGAL_PROFILE_TYPE.micro,
    title: 'Micro-entreprise achat-revente',
    description:
      "Je suis en micro-entreprise achat-revente, avec moins de 200 000 € de chiffre d'affaires par an.",
    detail: 'Soit environ 16 666 € par mois.',
    traits: ['SIRET obligatoire', 'BIC', 'Achat-revente'],
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

export function normalizeLegalProfile(source = {}) {
  const raw = source?.legalProfile && typeof source.legalProfile === 'object' ? source.legalProfile : source
  const legalProfileType = raw?.legalProfileType || null
  const isMicro = legalProfileType === LEGAL_PROFILE_TYPE.micro

  return {
    legalProfileType,
    siret: isMicro ? raw?.siret || '' : '',
    taxCategory: isMicro ? raw?.taxCategory || MICRO_LEGAL_PROFILE_DEFAULTS.taxCategory : null,
    businessRegime: isMicro ? raw?.businessRegime || MICRO_LEGAL_PROFILE_DEFAULTS.businessRegime : null,
    businessActivityType:
      isMicro ? raw?.businessActivityType || MICRO_LEGAL_PROFILE_DEFAULTS.businessActivityType : null,
    declaredRevenueThreshold:
      isMicro ? raw?.declaredRevenueThreshold || MICRO_LEGAL_PROFILE_DEFAULTS.declaredRevenueThreshold : null,
    vatNumber: isMicro ? raw?.vatNumber || '' : '',
    vatStatus: isMicro ? raw?.vatStatus || 'UNKNOWN' : null,
    declarationFrequency: isMicro ? raw?.declarationFrequency || 'UNKNOWN' : null,
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
  return (
    LEGAL_PROFILE_OPTIONS.find((option) => option.type === type) || {
      type: null,
      title: 'Profil non renseigné',
      description: 'Profil administratif à compléter.',
      traits: ['À compléter'],
    }
  )
}

export function getLegalProfileLabel(profile = {}) {
  return getLegalProfileOption(normalizeLegalProfile(profile).legalProfileType).title
}

export function getLegalProfileDetails(profile = {}) {
  const normalized = normalizeLegalProfile(profile)

  if (!normalized.completed) return ['Profil à compléter']

  if (normalized.legalProfileType === LEGAL_PROFILE_TYPE.micro) {
    return [
      normalized.siret ? `SIRET ${normalized.siret}` : 'SIRET manquant',
      'BIC',
      'Achat-revente',
    ]
  }

  return ['Moins de 5 000 € de CA par mois']
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
  if (type === LEGAL_PROFILE_TYPE.individual) {
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
  }
}
