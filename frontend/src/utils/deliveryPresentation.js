const ACTIVE_STATUSES = new Set([
  'PENDING',
  'REGISTERED',
  'IN_TRANSIT',
  'OUT_FOR_DELIVERY',
  'UNKNOWN',
  'EXCEPTION',
])

const STATUS_META = {
  INCOMPLETE: {
    label: 'A completer',
    shortLabel: 'A completer',
    badgeClass: 'border-amber-400/50 bg-amber-500/10 text-amber-100',
    lightBadgeClass: 'border-amber-300/50 bg-amber-500/10 text-amber-800',
    accentClass: 'text-amber-700',
    stage: 'pending',
  },
  DELIVERED: {
    label: 'Livre',
    shortLabel: 'Livre',
    badgeClass: 'border-emerald-400/50 bg-emerald-500/10 text-emerald-200',
    lightBadgeClass: 'border-emerald-300/50 bg-emerald-500/10 text-emerald-800',
    accentClass: 'text-emerald-700',
    stage: 'delivered',
  },
  OUT_FOR_DELIVERY: {
    label: 'En transit',
    shortLabel: 'Transit',
    badgeClass: 'border-sky-400/50 bg-sky-500/10 text-sky-200',
    lightBadgeClass: 'border-sky-300/50 bg-sky-500/10 text-sky-800',
    accentClass: 'text-sky-700',
    stage: 'transit',
  },
  IN_TRANSIT: {
    label: 'En transit',
    shortLabel: 'Transit',
    badgeClass: 'border-violet-400/50 bg-violet-500/10 text-violet-100',
    lightBadgeClass: 'border-violet-300/50 bg-violet-500/10 text-violet-800',
    accentClass: 'text-violet-700',
    stage: 'transit',
  },
  EXCEPTION: {
    label: 'Incident',
    shortLabel: 'Incident',
    badgeClass: 'border-red-400/50 bg-red-500/10 text-red-200',
    lightBadgeClass: 'border-red-300/50 bg-red-500/10 text-red-700',
    accentClass: 'text-red-700',
    stage: 'attention',
  },
  REGISTERED: {
    label: 'Bordereau cree',
    shortLabel: 'Debut',
    badgeClass: 'border-amber-400/50 bg-amber-500/10 text-amber-100',
    lightBadgeClass: 'border-amber-300/50 bg-amber-500/10 text-amber-800',
    accentClass: 'text-amber-700',
    stage: 'pending',
  },
  PENDING: {
    label: 'En attente',
    shortLabel: 'Attente',
    badgeClass: 'border-amber-400/50 bg-amber-500/10 text-amber-100',
    lightBadgeClass: 'border-amber-300/50 bg-amber-500/10 text-amber-800',
    accentClass: 'text-amber-700',
    stage: 'pending',
  },
  UNKNOWN: {
    label: 'A confirmer',
    shortLabel: 'A confirmer',
    badgeClass: 'border-slate-600 bg-slate-900 text-slate-300',
    lightBadgeClass: 'border-slate-300 bg-slate-200/70 text-slate-700',
    accentClass: 'text-slate-700',
    stage: 'pending',
  },
}

const STAGE_META = {
  attention: {
    label: 'A traiter',
    description: 'Incidents ou suivis a verifier',
    accentClass: 'border-red-200/80 bg-red-50/80 text-red-950',
  },
  pending: {
    label: 'Attente',
    description: 'Bordereaux crees ou attente de prise en charge',
    accentClass: 'border-amber-200/80 bg-amber-50/80 text-amber-950',
  },
  transit: {
    label: 'En transit',
    description: 'Le colis avance sur le reseau transporteur',
    accentClass: 'border-violet-200/80 bg-violet-50/80 text-violet-950',
  },
  today: {
    label: 'En transit',
    description: 'Colis en phase finale de transport',
    accentClass: 'border-sky-200/80 bg-sky-50/80 text-sky-950',
  },
  delivered: {
    label: 'Livres',
    description: 'Colis termines recemment',
    accentClass: 'border-emerald-200/80 bg-emerald-50/80 text-emerald-950',
  },
}

const defaultStatusMeta = {
  label: 'Inconnu',
  shortLabel: 'Inconnu',
  badgeClass: 'border-slate-600 bg-slate-900 text-slate-300',
  lightBadgeClass: 'border-slate-300 bg-slate-200/70 text-slate-700',
  accentClass: 'text-slate-700',
  stage: 'pending',
}

const RELAY_READY_PHRASES = [
  'disponible en relais',
  'disponible au relais',
  'point relais',
  'point de retrait',
  'relais pickup',
  'pickup pass',
  'code de retrait',
  'a retirer',
  'vous attend dans votre point de retrait',
]

const PICKED_UP_PHRASES = ['colis retire', 'retire par le destinataire']

const normalizeStatusLabel = (value) =>
  String(value || '')
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()

const hasAnyPhrase = (value, phrases) => phrases.some((phrase) => value.includes(phrase))

const resolveStatusInput = (input, statusLabelOverride = '') => {
  if (input && typeof input === 'object' && !Array.isArray(input)) {
    return {
      status: input.status,
      statusLabel: String(input.statusLabel || statusLabelOverride || ''),
    }
  }
  return {
    status: input,
    statusLabel: String(statusLabelOverride || ''),
  }
}

export const getDeliveryStatusMeta = (input, statusLabelOverride = '') => {
  const { status, statusLabel } = resolveStatusInput(input, statusLabelOverride)
  const base = STATUS_META[status] || defaultStatusMeta
  const normalizedLabel = normalizeStatusLabel(statusLabel)

  if (status === 'DELIVERED' && normalizedLabel.includes('boite aux lettres')) {
    return { ...base, label: 'Livre boite', shortLabel: 'Livre' }
  }

  if (status === 'OUT_FOR_DELIVERY') {
    return { ...base, label: 'En livraison', shortLabel: 'Livraison', stage: 'today' }
  }

  if (status === 'IN_TRANSIT') {
    if (hasAnyPhrase(normalizedLabel, RELAY_READY_PHRASES)) {
      return { ...base, label: 'Disponible relais', shortLabel: 'Relais', stage: 'transit' }
    }
    if (normalizedLabel.includes('pris en charge')) {
      return { ...base, label: 'Pris en charge', shortLabel: 'Pris', stage: 'pending' }
    }
  }

  if (status === 'REGISTERED' && normalizedLabel.includes('pris en charge')) {
    return { ...base, label: 'Pris en charge', shortLabel: 'Pris', stage: 'pending' }
  }

  if (status === 'DELIVERED' && hasAnyPhrase(normalizedLabel, PICKED_UP_PHRASES)) {
    return { ...base, label: 'Retire', shortLabel: 'Retire' }
  }

  return base
}

export const getDeliveryStageMeta = (stage) =>
  STAGE_META[stage] || {
    label: 'En attente',
    description: 'Suivi sans categorie',
    accentClass: 'border-slate-200/80 bg-slate-50/80 text-slate-950',
  }

export const getDeliveryStageKey = (input, statusLabelOverride = '') =>
  getDeliveryStatusMeta(input, statusLabelOverride).stage

export const isActiveParcelStatus = (status) => ACTIVE_STATUSES.has(status)

export const carrierLabel = (carrier) => {
  if (!carrier || carrier === 'unknown') return 'Transporteur'
  return String(carrier)
    .split('-')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

export const formatDeliveryDateTime = (value) => {
  if (!value) return 'Date inconnue'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Date inconnue'
  return new Intl.DateTimeFormat('fr-FR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(date)
}

export const formatDeliveryDate = (value) => {
  if (!value) return 'Date inconnue'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Date inconnue'
  return new Intl.DateTimeFormat('fr-FR', {
    dateStyle: 'medium',
  }).format(date)
}

export const formatRelativeDeliveryDate = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const now = new Date()
  const dayMs = 24 * 60 * 60 * 1000
  const startOfTarget = new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const diffDays = Math.round((startOfTarget - startOfToday) / dayMs)

  if (diffDays === 0) return 'Aujourd hui'
  if (diffDays === 1) return 'Demain'
  if (diffDays === -1) return 'Hier'
  if (diffDays > 1) return `Dans ${diffDays} jours`
  return `Il y a ${Math.abs(diffDays)} jours`
}

export const getDeliveryTrackingHealth = (parcel) => {
  if (!parcel) return null

  if (parcel.completionRequired || parcel.status === 'INCOMPLETE') {
    return {
      tone: 'warning',
      title: 'Suivi Mondial Relay incomplet',
      message:
        parcel.completionHint ||
        'Le code postal du destinataire manque encore. Renseigne-le pour relancer le suivi transporteur.',
    }
  }

  const aggregator = String(parcel.aggregator || '').toUpperCase()
  const statusLabel = String(parcel.statusLabel || '').toLowerCase()

  if (aggregator === 'DIRECT_CARRIER') {
    if (
      statusLabel.includes('api officielle la poste non configuree')
      || statusLabel.includes('no supported browser executable found')
      || statusLabel.includes('browser tracking script failed')
    ) {
      return {
        tone: 'warning',
        title: 'Source La Poste indisponible',
        message:
          "Le suivi Colissimo n'a pas pu etre relu automatiquement. Verifie la cle API La Poste ou le navigateur local utilise par le scraper.",
      }
    }
    if (statusLabel.includes('non trouve')) {
      return {
        tone: 'info',
        title: 'Suivi non trouve',
        message:
          "Le colis a ete enregistre, mais le transporteur n'a renvoye aucune donnee exploitable pour ce numero.",
      }
    }
    if (statusLabel.includes('indisponible')) {
      return {
        tone: 'info',
        title: 'Transporteur temporairement indisponible',
        message:
          "La source de suivi n'a pas repondu correctement au dernier rafraichissement. Le colis reste stocke et pourra etre retente plus tard.",
      }
    }
    if (statusLabel.includes('cloudflare') || statusLabel.includes('bloque la requete') || statusLabel.includes('bloque la page')) {
      return {
        tone: 'info',
        title: 'Chronopost bloque le scraping',
        message:
          "La page Chronopost repond avec une protection anti-bot au lieu du vrai suivi. Le refresh part bien, mais le serveur ne recoit pas encore les donnees de statut.",
      }
    }
  }

  if (aggregator === 'AFTERSHIP' && !parcel.lastEventAt && parcel.status === 'REGISTERED') {
    return {
      tone: 'info',
      title: 'Suivi en attente de retour agregateur',
      message:
        "Le colis est enregistre chez l'agregateur, mais aucun checkpoint detaille n'est encore revenu.",
    }
  }

  return null
}
