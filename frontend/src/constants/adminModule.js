export const ADMIN_STORAGE_KEYS = {
  settings: 'snk_admin_settings_v5',
  documents: 'snk_admin_documents_v5',
  manualEntries: 'snk_admin_manual_entries_v5',
  declaredPeriods: 'snk_admin_declared_periods_v5',
  proofOverrides: 'snk_admin_proof_overrides_v5',
  checklists: 'snk_admin_checklists_v5',
}

export const ADMIN_PROFILES = {
  occasional_private: {
    key: 'occasional_private',
    label: 'Particulier occasionnel',
    shortLabel: 'Occasionnel',
    level: 'low',
    description: 'Revente ponctuelle de biens personnels, sans achat organisé pour revendre.',
    tabs: ['home', 'proofs', 'settings'],
    requiredFields: [],
    primaryObjective: 'Suivre les ventes, conserver les preuves et surveiller les volumes.',
  },

  regularization_needed: {
    key: 'regularization_needed',
    label: 'Achat-revente à régulariser',
    shortLabel: 'À régulariser',
    level: 'medium',
    description: 'Activité régulière achat-revente non encore déclarée ou incomplète.',
    tabs: ['home', 'regularization', 'declare', 'registers', 'proofs', 'settings'],
    requiredFields: ['legalName', 'siren', 'address', 'declarationFrequency'],
    primaryObjective: "Mettre l'activité en conformité avant d'émettre des documents.",
  },

  micro_reseller: {
    key: 'micro_reseller',
    label: 'Micro-entreprise achat/revente',
    shortLabel: 'Micro',
    level: 'standard',
    description: 'Revendeur déclaré en micro-entreprise, principalement vente de marchandises.',
    tabs: ['home', 'declare', 'documents', 'registers', 'proofs', 'settings'],
    requiredFields: ['legalName', 'siren', 'address', 'vatMode', 'declarationFrequency'],
    primaryObjective: 'Préparer déclarations, documents, registres et justificatifs.',
  },

  micro_vat_watch: {
    key: 'micro_vat_watch',
    label: 'Micro-entreprise avec TVA à surveiller',
    shortLabel: 'Micro + TVA',
    level: 'warning',
    description: 'Micro-entreprise avec volume élevé ou seuil TVA proche / dépassé.',
    tabs: ['home', 'declare', 'vat', 'documents', 'registers', 'proofs', 'settings'],
    requiredFields: ['legalName', 'siren', 'address', 'vatMode', 'declarationFrequency'],
    primaryObjective: 'Sécuriser le régime TVA avant émission de documents.',
  },

  online_shop: {
    key: 'online_shop',
    label: 'Boutique en ligne indépendante',
    shortLabel: 'En ligne',
    level: 'standard',
    description: 'Vente via site, réseaux sociaux, plateformes ou marketplace.',
    tabs: ['home', 'declare', 'documents', 'registers', 'proofs', 'shop', 'settings'],
    requiredFields: ['legalName', 'siren', 'address', 'vatMode'],
    primaryObjective: 'Suivre commandes, frais, documents clients et exports.',
  },

  physical_shop: {
    key: 'physical_shop',
    label: 'Boutique physique',
    shortLabel: 'Physique',
    level: 'warning',
    description: 'Vente comptoir, paiements physiques, retours et journal de caisse.',
    tabs: ['home', 'declare', 'documents', 'registers', 'cash', 'proofs', 'shop', 'settings'],
    requiredFields: ['legalName', 'siren', 'address', 'vatMode'],
    primaryObjective: 'Suivre les ventes comptoir, la caisse et les justificatifs.',
  },

  company_real: {
    key: 'company_real',
    label: 'Entreprise au réel / société',
    shortLabel: 'Entreprise',
    level: 'high',
    description: 'Structure commerciale avec comptabilité plus complète, TVA, stock et exports.',
    tabs: ['home', 'declare', 'vat', 'documents', 'registers', 'proofs', 'shop', 'settings'],
    requiredFields: ['legalName', 'siren', 'siret', 'address', 'vatMode'],
    primaryObjective: 'Préparer des exports propres pour comptable ou logiciel dédié.',
  },

  employer: {
    key: 'employer',
    label: 'Structure avec employés',
    shortLabel: 'Employeur',
    level: 'high',
    description: 'Activité avec un ou plusieurs salariés, nécessitant un suivi employeur.',
    tabs: ['home', 'declare', 'documents', 'registers', 'proofs', 'shop', 'staff', 'settings'],
    requiredFields: ['legalName', 'siren', 'siret', 'address', 'vatMode'],
    primaryObjective: "Suivre l'administratif entreprise et le dossier employeur.",
  },
}

export const ADMIN_TABS = [
  { key: 'home', label: 'Accueil' },
  { key: 'regularization', label: 'Régularisation' },
  { key: 'declare', label: 'Déclarations' },
  { key: 'vat', label: 'TVA' },
  { key: 'documents', label: 'Documents' },
  { key: 'registers', label: 'Registres' },
  { key: 'cash', label: 'Caisse' },
  { key: 'proofs', label: 'Justificatifs' },
  { key: 'shop', label: 'Boutique' },
  { key: 'staff', label: 'Personnel' },
  { key: 'settings', label: 'Paramètres' },
]

export function getAdminProfile(profileType) {
  return ADMIN_PROFILES[profileType] || ADMIN_PROFILES.occasional_private
}

export function getAdminProfileLabel(profileType) {
  return getAdminProfile(profileType).label
}

export function isProfessionalAdminProfile(profileType) {
  return getAdminProfile(profileType).key !== 'occasional_private'
}

export function getAdminRequiredFields(profileType) {
  return getAdminProfile(profileType).requiredFields
}

export function getAdminTabsForProfile(settings = {}) {
  const profile = getAdminProfile(settings.profileType)
  const profileTabs = new Set(profile.tabs)

  if (settings.hasPhysicalShop) profileTabs.add('cash')
  if (settings.hasEmployees) profileTabs.add('staff')
  if (
    settings.sellsOnline ||
    settings.sellsOnPlatforms ||
    settings.hasPhysicalShop ||
    settings.buysFromIndividuals
  ) {
    profileTabs.add('shop')
  }
  if (settings.sellsTickets) {
    profileTabs.add('proofs')
    profileTabs.add('shop')
  }

  return ADMIN_TABS.filter((tab) => profileTabs.has(tab.key))
}

export const ADMIN_DOCUMENT_TYPES = [
  {
    value: 'invoice',
    label: 'Facture',
    help: 'Client professionnel ou vente nécessitant une facture.',
  },
  { value: 'receipt', label: 'Reçu', help: 'Client particulier, preuve de vente simple.' },
  { value: 'credit_note', label: 'Avoir', help: 'Correction, retour ou remboursement.' },
]

export const ADMIN_PERIOD_OPTIONS = [
  { value: 'month', label: 'Mois' },
  { value: 'quarter', label: 'Trimestre' },
  { value: 'year', label: 'Année' },
  { value: 'custom', label: 'Libre' },
]
