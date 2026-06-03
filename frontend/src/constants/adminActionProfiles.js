export const ADMIN_ACTION_STORAGE_KEY = 'snk_admin_action_page_v1'

export const DEFAULT_ADMIN_ACTION_SETTINGS = Object.freeze({
  profileType: 'light_reseller',
  tradeName: '',
  legalName: '',
  siren: '',
  siret: '',
  address: '',
  email: '',
  declarationFrequency: 'monthly',
  vatMode: 'franchise',
  usesPlatforms: true,
  sellsToProfessionals: false,
  buysFromIndividuals: false,
  hasCashSoftware: false,
})

export const ADMIN_ACTION_PROFILES = Object.freeze([
  {
    key: 'light_reseller',
    label: 'Petit revendeur debutant',
    level: 'low',
    levelLabel: 'Debutant',
    description: 'Peu de ventes, peu de CA, objectif simple : garder les preuves et suivre le volume.',
    requiredFields: [],
    traits: ['Preuves', 'CA annuel', 'Plateformes'],
  },
  {
    key: 'regular_reseller',
    label: 'Micro-revendeur simple',
    level: 'medium',
    levelLabel: 'Micro',
    description: 'Achat-revente declare : suivre le CA, declarer, garder registres et preuves.',
    requiredFields: ['legalName', 'siren', 'siret', 'declarationFrequency', 'vatMode'],
    traits: ['Declaration CA', 'Livre recettes', 'Registre achats'],
  },
  {
    key: 'strict_reseller',
    label: 'Gros revendeur structure',
    level: 'high',
    levelLabel: 'Structure',
    description: 'Volume important avec registres, justificatifs, rappels et exports comptables.',
    requiredFields: ['legalName', 'siren', 'address', 'email', 'declarationFrequency', 'vatMode'],
    traits: ['Registres complets', 'Seuils TVA', 'Exports comptables'],
  },
  {
    key: 'independent_shop',
    label: 'Boutique independante',
    level: 'shop',
    levelLabel: 'Boutique',
    description: 'Commerce plus professionnel avec caisse, factures, registres et comptabilite.',
    requiredFields: ['tradeName', 'legalName', 'siren', 'siret', 'address', 'email', 'vatMode'],
    traits: ['Factures', 'Caisse', 'Dossier boutique'],
  },
])

export function normalizeAdminActionProfileKey(profileKey) {
  const aliases = {
    occasional_private: 'light_reseller',
    regularization_needed: 'regular_reseller',
    micro_reseller: 'regular_reseller',
    micro_vat_watch: 'strict_reseller',
    online_shop: 'independent_shop',
    physical_shop: 'independent_shop',
    company_real: 'strict_reseller',
    employer: 'independent_shop',
  }
  const normalized = aliases[profileKey] || profileKey
  return ADMIN_ACTION_PROFILES.some((profile) => profile.key === normalized)
    ? normalized
    : 'light_reseller'
}

export function getAdminActionProfile(profileKey) {
  const normalized = normalizeAdminActionProfileKey(profileKey)
  return ADMIN_ACTION_PROFILES.find((profile) => profile.key === normalized) || ADMIN_ACTION_PROFILES[0]
}

export function resolveAdminActionProfileKey(payload = {}) {
  const source = payload?.adminActionPage || payload || {}
  const sourceSettings = source.settings || payload?.settings || {}
  return normalizeAdminActionProfileKey(source.selectedProfile || sourceSettings.profileType)
}

export function buildAdminActionProfilePayload(payload = {}, profileKey) {
  const source = payload?.adminActionPage || payload || {}
  const sourceSettings = source.settings || payload?.settings || {}
  const normalizedProfile = normalizeAdminActionProfileKey(profileKey)
  const settings = {
    ...DEFAULT_ADMIN_ACTION_SETTINGS,
    ...sourceSettings,
    profileType: normalizedProfile,
  }

  const adminActionPage = {
    ...source,
    version: source.version || 1,
    selectedProfile: normalizedProfile,
    settings,
    updatedAt: new Date().toISOString(),
  }

  return {
    ...payload,
    adminActionPage,
    settings: {
      ...(payload?.settings || {}),
      ...settings,
      profileType: normalizedProfile,
    },
  }
}
