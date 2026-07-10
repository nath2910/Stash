import {
  getAdministrativeProfile,
  getBlockingIssues,
  getDeclarativePeriod,
  shouldUseQuarterPeriod,
} from '../rules/administrativeRules.js'

export const ADMIN_NOTIFICATION_PREFIX = 'admin:'
export const ADMIN_NOTIFICATION_ROUTE = '/gestion?tab=admin'

export function deriveAdministrativeSummaryParams(profile = {}, todayValue = new Date()) {
  const today = todayValue instanceof Date ? todayValue : new Date()
  const year = today.getFullYear()

  if (shouldUseQuarterPeriod(profile)) {
    const quarter = Math.floor(today.getMonth() / 3)
    return {
      periodStart: dateToInputValue(new Date(year, quarter * 3, 1)),
      periodEnd: dateToInputValue(new Date(year, quarter * 3 + 3, 0)),
    }
  }

  return {
    periodStart: dateToInputValue(new Date(year, today.getMonth(), 1)),
    periodEnd: dateToInputValue(new Date(year, today.getMonth() + 1, 0)),
  }
}

export function buildAdministrativeReminderNotifications(profile = {}, summary = {}, options = {}) {
  if (!profile || typeof profile !== 'object' || !summary || typeof summary !== 'object') {
    return []
  }

  const now = options.now instanceof Date ? options.now : new Date()
  const adminProfile = getAdministrativeProfile(profile)
  const period = getDeclarativePeriod(profile, summary.periodStart, summary.periodEnd)
  const blockingIssues = getBlockingIssues(profile, summary, period).filter(
    (issue) => issue.id !== 'no-sales' && issue.severity !== 'info',
  )
  const reminders = []

  if (profileNeedsAttention(profile, adminProfile)) {
    reminders.push({
      id: `${ADMIN_NOTIFICATION_PREFIX}profile:${summary.periodStart || 'current'}`,
      type: 'ADMIN_PROFILE',
      title: 'Profil administratif a completer',
      message: profileReminderMessage(profile, adminProfile),
      severity: 'WARNING',
      ctaRoute: ADMIN_NOTIFICATION_ROUTE,
      ctaLabel: 'Completer',
      entityType: 'ADMIN_PROFILE',
      milestoneKey: `ADMIN_PROFILE_${summary.periodStart || 'CURRENT'}`,
      createdAt: stableIsoStamp(summary.periodStart || now, 8),
      isRead: false,
      dismissedAt: null,
    })
  }

  const deadlineReminder = buildDeadlineReminder(summary, adminProfile, period, now)
  if (deadlineReminder) {
    reminders.push(deadlineReminder)
  }

  const dataReminder = buildDataReminder(summary, period, blockingIssues)
  if (dataReminder) {
    reminders.push(dataReminder)
  }

  return reminders.sort((a, b) => Date.parse(b.createdAt || 0) - Date.parse(a.createdAt || 0))
}

function buildDeadlineReminder(summary, adminProfile, period, now) {
  if (adminProfile.family !== 'micro') return null

  const deadline = declarationDeadline(period)
  const diffDays = Math.ceil((startOfDay(deadline).getTime() - startOfDay(now).getTime()) / 86_400_000)
  const overdue = diffDays < 0
  const severity = overdue ? 'CRITICAL' : diffDays <= 7 ? 'WARNING' : 'INFO'
  const title = overdue
    ? `Declaration ${period.label || ''} en retard`.trim()
    : diffDays === 0
      ? 'Declaration URSSAF a faire aujourd hui'
      : `Declaration ${period.label || ''} a preparer`.trim()
  const count = Number(summary?.periodSaleCount || 0)
  const salesLabel = Number.isFinite(count) && count > 0 ? `${count} vente(s) concernee(s).` : ''

  return {
    id: `${ADMIN_NOTIFICATION_PREFIX}deadline:${period.start || 'current'}:${period.end || 'current'}`,
    type: 'ADMIN_DEADLINE',
    title,
    message: overdue
      ? `Echeance estimee depassee depuis ${Math.abs(diffDays)} jour(s). ${salesLabel}`.trim()
      : `Echeance estimee au ${formatDate(deadline)}${diffDays > 0 ? `, J-${diffDays}.` : '.'} ${salesLabel}`.trim(),
    severity,
    ctaRoute: ADMIN_NOTIFICATION_ROUTE,
    ctaLabel: 'Voir admin',
    entityType: 'ADMIN_PERIOD',
    milestoneKey: `ADMIN_DEADLINE_${period.start || 'CURRENT'}_${period.end || 'CURRENT'}`,
    createdAt: stableIsoStamp(deadline, overdue ? 10 : 7),
    isRead: false,
    dismissedAt: null,
  }
}

function buildDataReminder(summary, period, blockingIssues) {
  const hardCount = blockingIssues.filter((issue) => issue.severity === 'danger').length
  const warningCount = blockingIssues.filter((issue) => issue.severity === 'warning').length
  const issueCount = hardCount + warningCount

  if (!issueCount) return null

  const headline = blockingIssues
    .slice(0, 2)
    .map((issue) => issue.title)
    .join(' ')

  return {
    id: `${ADMIN_NOTIFICATION_PREFIX}data:${period.start || 'current'}:${period.end || 'current'}`,
    type: Number(summary?.missingInvoiceCount || 0) > 0 ? 'ADMIN_INVOICES' : 'ADMIN_DATA',
    title: hardCount
      ? `${hardCount} blocage(s) admin a corriger`
      : `${warningCount} rappel(s) admin a surveiller`,
    message: headline || 'Des controles administratifs meritent une verification.',
    severity: hardCount ? 'CRITICAL' : 'WARNING',
    ctaRoute: ADMIN_NOTIFICATION_ROUTE,
    ctaLabel: hardCount ? 'Corriger' : 'Verifier',
    entityType: 'ADMIN_CHECK',
    milestoneKey: `ADMIN_DATA_${period.start || 'CURRENT'}_${period.end || 'CURRENT'}`,
    createdAt: stableIsoStamp(period.end || new Date(), 9),
    isRead: false,
    dismissedAt: null,
  }
}

function profileNeedsAttention(profile, adminProfile) {
  const legalStatus = String(profile?.legalStatus || 'none')
  const declarationFrequency = String(profile?.declarationFrequency || 'UNKNOWN')
  const vatRegime = String(profile?.vatRegime || 'unknown')
  const hasSiret = Boolean(profile?.siret)

  if (legalStatus === 'none') return true
  if ((adminProfile.family === 'micro' || adminProfile.family === 'company') && !hasSiret) return true
  if (adminProfile.family === 'micro' && declarationFrequency === 'UNKNOWN') return true
  if (adminProfile.family === 'company' && vatRegime === 'unknown') return true
  return false
}

function profileReminderMessage(profile, adminProfile) {
  const legalStatus = String(profile?.legalStatus || 'none')
  if (legalStatus === 'none') {
    return 'Choisissez un statut pour afficher les bons rappels, documents et controles.'
  }

  if (!profile?.siret && (adminProfile.family === 'micro' || adminProfile.family === 'company')) {
    return 'Le SIRET manque encore dans le dossier administratif.'
  }

  if (adminProfile.family === 'micro' && String(profile?.declarationFrequency || 'UNKNOWN') === 'UNKNOWN') {
    return 'Confirmez la periodicite URSSAF pour fiabiliser les rappels.'
  }

  if (adminProfile.family === 'company' && String(profile?.vatRegime || 'unknown') === 'unknown') {
    return 'Le regime TVA reste a definir pour un dossier comptable propre.'
  }

  return 'Quelques informations administratives restent a verifier.'
}

function declarationDeadline(period) {
  const end = parseLocalDate(period?.end) || new Date()
  return new Date(end.getFullYear(), end.getMonth() + 2, 0)
}

function stableIsoStamp(value, hour = 8) {
  const date = parseLocalDate(value) || (value instanceof Date ? value : new Date())
  return new Date(
    Date.UTC(date.getFullYear(), date.getMonth(), date.getDate(), hour, 0, 0),
  ).toISOString()
}

function startOfDay(value) {
  const date = value instanceof Date ? value : parseLocalDate(value) || new Date()
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

function formatDate(value) {
  const date = value instanceof Date ? value : parseLocalDate(value)
  if (!date) return ''
  return new Intl.DateTimeFormat('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date)
}

function dateToInputValue(value) {
  const year = value.getFullYear()
  const month = `${value.getMonth() + 1}`.padStart(2, '0')
  const day = `${value.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

function parseLocalDate(value) {
  if (!value) return null
  if (value instanceof Date && !Number.isNaN(value.getTime())) return value

  const text = String(value).slice(0, 10)
  const [year, month, day] = text.split('-').map(Number)
  if (!year || !month || !day) return null
  return new Date(year, month - 1, day)
}
