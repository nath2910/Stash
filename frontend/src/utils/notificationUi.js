const NOTIFICATION_DOMAINS = {
  resale: {
    label: 'Revente',
    description: 'Stock et ventes',
  },
  subscription: {
    label: 'Abonnement',
    description: 'Facturation et acces',
  },
  administrative: {
    label: 'Administratif',
    description: 'Rappels et conformite',
  },
}

export function normalizeNotificationSeverity(severity) {
  return String(severity || 'INFO').toUpperCase()
}

export function getNotificationDomain(notification = {}) {
  const type = String(notification?.type || '').toUpperCase()
  const ctaRoute = String(notification?.ctaRoute || '')
  const milestone = String(notification?.milestoneKey || '').toUpperCase()

  if (
    type.startsWith('ADMIN_') ||
    ctaRoute.includes('tab=admin') ||
    ctaRoute === '/admin' ||
    milestone.startsWith('ADMIN_')
  ) {
    return 'administrative'
  }

  if (type === 'SUBSCRIPTION_EXPIRING' || ctaRoute.startsWith('/abo')) {
    return 'subscription'
  }

  return 'resale'
}

export function getNotificationDomainMeta(notification = {}) {
  return NOTIFICATION_DOMAINS[getNotificationDomain(notification)] || NOTIFICATION_DOMAINS.resale
}

export function getNotificationTypeLabel(notification = {}) {
  const type = String(notification?.type || '').toUpperCase()

  if (type === 'SUBSCRIPTION_EXPIRING') return 'Echeance'
  if (type === 'STOCK_AGING') return 'Stock dormant'
  if (type === 'ADMIN_PROFILE') return 'Profil'
  if (type === 'ADMIN_DEADLINE') return 'Rappel'
  if (type === 'ADMIN_DATA') return 'Controle'
  if (type === 'ADMIN_INVOICES') return 'Factures'

  const domain = getNotificationDomain(notification)
  if (domain === 'subscription') return 'Abonnement'
  if (domain === 'administrative') return 'Administratif'
  return 'Revente'
}

export function getNotificationSeverityLabel(severity) {
  const normalized = normalizeNotificationSeverity(severity)
  if (normalized === 'WARNING') return 'Attention'
  if (normalized === 'CRITICAL') return 'Critique'
  return 'Info'
}

export function formatNotificationDate(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}
