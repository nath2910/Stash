import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildIncompleteProfileReminder,
  buildAdministrativeReminderNotifications,
  deriveAdministrativeSummaryParams,
} from '../src/utils/adminNotificationBuilder.js'

test('deriveAdministrativeSummaryParams utilise le trimestre pour une micro trimestrielle', () => {
  const params = deriveAdministrativeSummaryParams(
    {
      legalStatus: 'micro',
      declarationFrequency: 'QUARTERLY',
    },
    new Date('2026-07-09T09:00:00'),
  )

  assert.deepEqual(params, {
    periodStart: '2026-07-01',
    periodEnd: '2026-09-30',
  })
})

test('buildAdministrativeReminderNotifications fabrique des rappels de profil, deadline et data', () => {
  const reminders = buildAdministrativeReminderNotifications(
    {
      legalStatus: 'micro',
      declarationFrequency: 'UNKNOWN',
      vatRegime: 'franchise_base',
      siret: '',
    },
    {
      periodStart: '2026-04-01',
      periodEnd: '2026-06-30',
      periodSaleCount: 4,
      missingSaleDateCount: 2,
      missingSaleAmountCount: 0,
      missingProofCount: 0,
      missingInvoiceCount: 1,
      sales: [],
    },
    {
      now: new Date('2026-07-09T09:00:00'),
      accountCreatedAt: '2026-07-08T00:00:00',
    },
  )

  assert.equal(reminders.some((item) => item.type === 'ADMIN_PROFILE'), true)
  assert.equal(reminders.some((item) => item.type === 'ADMIN_DEADLINE'), true)
  assert.equal(reminders.some((item) => item.type === 'ADMIN_INVOICES'), true)
})

test('buildIncompleteProfileReminder attend 12h avant affichage', () => {
  const beforeDelay = buildIncompleteProfileReminder(
    {
      legalStatus: 'micro',
      declarationFrequency: 'UNKNOWN',
      vatRegime: 'franchise_base',
      siret: '',
    },
    {
      now: new Date('2026-07-09T09:00:00Z'),
      accountCreatedAt: '2026-07-09T00:30:00Z',
    },
  )

  const afterDelay = buildIncompleteProfileReminder(
    {
      legalStatus: 'micro',
      declarationFrequency: 'UNKNOWN',
      vatRegime: 'franchise_base',
      siret: '',
    },
    {
      now: new Date('2026-07-09T14:00:00Z'),
      accountCreatedAt: '2026-07-09T00:30:00Z',
    },
  )

  assert.equal(beforeDelay, null)
  assert.equal(afterDelay?.type, 'ADMIN_PROFILE')
  assert.equal(afterDelay?.createdAt, '2026-07-09T12:30:00.000Z')
})
