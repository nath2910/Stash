import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildStoredStatsRange,
  getMonthToDateRange,
  resolveStoredStatsRange,
} from '../src/utils/statsRangeStorage.js'

test('buildStoredStatsRange marque la plage du mois courant comme dynamique', () => {
  const baseDate = new Date('2026-07-09T09:00:00')
  const range = getMonthToDateRange(baseDate)
  const record = buildStoredStatsRange(range.from, range.to, { baseDate })

  assert.equal(record?.preset, 'month_to_date')
  assert.equal(record?.anchor, '2026-07')
})

test('resolveStoredStatsRange recalcule automatiquement une plage dynamique quand le mois change', () => {
  const record = resolveStoredStatsRange(
    {
      from: '2026-06-01',
      to: '2026-06-30',
      preset: 'month_to_date',
      anchor: '2026-06',
    },
    { baseDate: new Date('2026-07-09T09:00:00') },
  )

  assert.deepEqual(
    { from: record.from, to: record.to, preset: record.preset, anchor: record.anchor },
    {
      from: '2026-07-01',
      to: '2026-07-09',
      preset: 'month_to_date',
      anchor: '2026-07',
    },
  )
})
