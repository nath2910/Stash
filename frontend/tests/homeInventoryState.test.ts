import { describe, expect, it } from 'vitest'
import {
  countHomeInventoryUnits,
  countHomeStockUnits,
  normalizeHomeApiSummary,
  resolveHomeInventoryCount,
  resolveHomeKpiError,
  shouldShowHomeImportPrompt,
} from '../src/utils/homeInventoryState'

describe('home inventory state', () => {
  it('uses the server stats count while the searchable stock list is not loaded', () => {
    expect(
      resolveHomeInventoryCount({
        stockLoaded: false,
        stockItems: [],
        apiSummary: {
          ca: 4242,
          profit: 1229.02,
          itemsEnStock: 193,
          valeurStock: 21830,
        },
      }),
    ).toBe(193)
  })

  it('keeps the server stock count after the searchable list has loaded', () => {
    expect(
      resolveHomeInventoryCount({
        stockLoaded: true,
        stockItems: [{ dateVente: null }, { dateVente: '2026-09-01' }],
        apiSummary: { itemsEnStock: 193 },
      }),
    ).toBe(193)
  })

  it('counts grouped inventory in units without counting sold units', () => {
    const items = [
      {
        groupParent: true,
        quantity: 3,
        soldCount: 1,
        children: [{ dateVente: null }, { dateVente: null }, { dateVente: '2026-09-01' }],
      },
      { dateVente: null },
      { dateVente: '2026-09-02' },
    ]

    expect(countHomeInventoryUnits(items)).toBe(5)
    expect(countHomeStockUnits(items)).toBe(3)
  })

  it('does not surface stock-list timeout errors in the KPI panel', () => {
    expect(
      resolveHomeKpiError({
        statsError: '',
        stockError: "Recherche inventaire temporairement indisponible.",
      } as unknown as { statsError: string }),
    ).toBe('')
  })

  it('only shows the import prompt after a confirmed empty stock list', () => {
    expect(
      shouldShowHomeImportPrompt({
        hasToken: true,
        stockLoaded: false,
        stockLoading: false,
        stockError: "Recherche inventaire temporairement indisponible.",
        stockItems: [],
      }),
    ).toBe(false)

    expect(
      shouldShowHomeImportPrompt({
        hasToken: true,
        stockLoaded: true,
        stockLoading: false,
        stockError: '',
        stockItems: [],
      }),
    ).toBe(true)
  })

  it('normalizes API summary values defensively', () => {
    expect(
      normalizeHomeApiSummary({
        ca: '4242',
        profit: '1229.02',
        itemsVendues: '12',
        itemsEnStock: '193',
        valeurStock: '21830',
      }),
    ).toMatchObject({
      ca: 4242,
      profit: 1229.02,
      itemsVendues: 12,
      itemsEnStock: 193,
      valeurStock: 21830,
      estimatedStockValue: 21830,
    })
  })
})
