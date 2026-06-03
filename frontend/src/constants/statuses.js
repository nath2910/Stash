const stripDiacritics = (value) =>
  String(value ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')

const normalizeKey = (value) =>
  stripDiacritics(value)
    .toLowerCase()
    .trim()
    .replace(/['’]/g, '')
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')

export const STATUS_CONFIG = {
  complete: {
    key: 'complete',
    label: 'Complet',
    tone: 'success',
    className: 'app-status-badge--success',
  },
  incomplete: {
    key: 'incomplete',
    label: 'À compléter',
    tone: 'warning',
    className: 'app-status-badge--warning',
  },
  to_verify: {
    key: 'to_verify',
    label: 'À vérifier',
    tone: 'warning',
    className: 'app-status-badge--warning',
  },
  blocking: {
    key: 'blocking',
    label: 'Bloquant',
    tone: 'danger',
    className: 'app-status-badge--danger',
  },
  not_applicable: {
    key: 'not_applicable',
    label: 'Non applicable',
    tone: 'neutral',
    className: 'app-status-badge--neutral',
  },
  pending: {
    key: 'pending',
    label: 'En attente',
    tone: 'info',
    className: 'app-status-badge--info',
  },
  active: {
    key: 'active',
    label: 'Actif',
    tone: 'success',
    className: 'app-status-badge--success',
  },
  archived: {
    key: 'archived',
    label: 'Archivé',
    tone: 'neutral',
    className: 'app-status-badge--neutral',
  },
  sold: {
    key: 'sold',
    label: 'Vendu',
    tone: 'success',
    className: 'app-status-badge--success',
  },
  in_stock: {
    key: 'in_stock',
    label: 'En stock',
    tone: 'info',
    className: 'app-status-badge--info',
  },
  delivered: {
    key: 'delivered',
    label: 'Livré',
    tone: 'success',
    className: 'app-status-badge--success',
  },
  in_transit: {
    key: 'in_transit',
    label: 'En transit',
    tone: 'info',
    className: 'app-status-badge--info',
  },
  error: {
    key: 'error',
    label: 'Erreur',
    tone: 'danger',
    className: 'app-status-badge--danger',
  },
  draft: {
    key: 'draft',
    label: 'Brouillon',
    tone: 'neutral',
    className: 'app-status-badge--neutral',
  },
  checked: {
    key: 'checked',
    label: 'Vérifié',
    tone: 'success',
    className: 'app-status-badge--success',
  },
  issued: {
    key: 'issued',
    label: 'Émis',
    tone: 'info',
    className: 'app-status-badge--info',
  },
  paid: {
    key: 'paid',
    label: 'Payé',
    tone: 'success',
    className: 'app-status-badge--success',
  },
  cancelled: {
    key: 'cancelled',
    label: 'Annulé',
    tone: 'danger',
    className: 'app-status-badge--danger',
  },
}

const STATUS_ALIASES = {
  ok: 'complete',
  complet: 'complete',
  complete: 'complete',
  checked: 'checked',
  verifie: 'checked',
  a_completer: 'incomplete',
  a_renseigner: 'incomplete',
  missing: 'incomplete',
  incomplete: 'incomplete',
  a_verifier: 'to_verify',
  verify: 'to_verify',
  warning: 'to_verify',
  bloquant: 'blocking',
  blocking: 'blocking',
  danger: 'blocking',
  non_applicable: 'not_applicable',
  na: 'not_applicable',
  pending: 'pending',
  en_attente: 'pending',
  active: 'active',
  actif: 'active',
  archived: 'archived',
  archive: 'archived',
  sold: 'sold',
  vendu: 'sold',
  vendue: 'sold',
  in_stock: 'in_stock',
  stock: 'in_stock',
  en_stock: 'in_stock',
  delivered: 'delivered',
  livre: 'delivered',
  livree: 'delivered',
  delivered_ok: 'delivered',
  in_transit: 'in_transit',
  transit: 'in_transit',
  erreur: 'error',
  error: 'error',
  draft: 'draft',
  brouillon: 'draft',
  issued: 'issued',
  emis: 'issued',
  paid: 'paid',
  paye: 'paid',
  cancelled: 'cancelled',
  annule: 'cancelled',
  standard: 'pending',
  success: 'complete',
}

export function getStatusConfig(status, fallback = STATUS_CONFIG.pending) {
  const normalized = normalizeKey(status)
  const key = STATUS_ALIASES[normalized] || normalized
  return STATUS_CONFIG[key] || fallback
}

export function getStatusLabel(status, fallbackLabel = 'En attente') {
  return getStatusConfig(status, { label: fallbackLabel }).label
}

export function getStatusClass(status, fallbackClass = 'app-status-badge--info') {
  return getStatusConfig(status, { className: fallbackClass }).className
}

export function normalizeItemStatus(item) {
  const soldDate =
    item?.dateVente ||
    item?.date_vente ||
    item?.soldAt ||
    item?.saleDate ||
    item?.sellingDate ||
    item?.dateSold

  return soldDate ? STATUS_CONFIG.sold : STATUS_CONFIG.in_stock
}
