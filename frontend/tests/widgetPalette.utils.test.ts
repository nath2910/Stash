import { describe, expect, it } from 'vitest'
import {
  coercePreviewSpec,
  filterPaletteGroups,
  moveGridIndexByKey,
} from '../src/components/stats/palette/paletteUtils'
import type { WidgetPaletteGroup } from '../src/components/stats/palette/types'

const groups: WidgetPaletteGroup[] = [
  {
    title: 'Finance',
    color: '#22c55e',
    items: [
      {
        type: 'netProfit',
        title: 'Benefice net',
        help: 'Profit total',
        forms: ['number'],
        formPicker: false,
        tags: ['profit', 'kpi'],
        dataType: 'finance',
      },
      {
        type: 'grossRevenue',
        title: "Chiffre d'affaires",
        help: 'Trend CA',
        forms: ['line'],
        tags: ['trend'],
        dataType: 'finance',
      },
    ],
  },
  {
    title: 'Texte',
    color: '#a855f7',
    items: [
      {
        type: 'textBlock',
        title: 'Texte',
        help: 'Bloc libre',
        forms: [],
        formPicker: false,
        tags: ['notes'],
        dataType: 'text',
      },
    ],
  },
]

describe('filterPaletteGroups', () => {
  it('filters by query and category', () => {
    const output = filterPaletteGroups(groups, {
      query: 'benefice',
      category: 'Finance',
      dataType: 'all',
      sort: 'relevance',
    })

    expect(output).toHaveLength(1)
    expect(output[0].items).toHaveLength(1)
    expect(output[0].items[0].type).toBe('netProfit')
  })

  it('supports data type filtering', () => {
    const output = filterPaletteGroups(groups, {
      query: '',
      category: 'all',
      dataType: 'text',
      sort: 'relevance',
    })

    expect(output).toHaveLength(1)
    expect(output[0].title).toBe('Texte')
    expect(output[0].items[0].type).toBe('textBlock')
  })

  it('sorts in quick mode with direct add first', () => {
    const output = filterPaletteGroups(groups, {
      query: '',
      category: 'Finance',
      dataType: 'all',
      sort: 'quick',
    })

    expect(output[0].items[0].type).toBe('netProfit')
  })
})

describe('moveGridIndexByKey', () => {
  it('moves down/up by columns', () => {
    expect(moveGridIndexByKey({ key: 'ArrowDown', index: 0, total: 8, columns: 3 })).toBe(3)
    expect(moveGridIndexByKey({ key: 'ArrowUp', index: 5, total: 8, columns: 3 })).toBe(2)
  })

  it('clamps navigation at edges', () => {
    expect(moveGridIndexByKey({ key: 'ArrowLeft', index: 0, total: 8, columns: 3 })).toBe(0)
    expect(moveGridIndexByKey({ key: 'ArrowRight', index: 7, total: 8, columns: 3 })).toBe(7)
  })
})

describe('coercePreviewSpec', () => {
  it('returns fallback when preview is missing', () => {
    expect(coercePreviewSpec(undefined)).toEqual({ kind: 'fallback' })
  })

  it('drops invalid numeric values', () => {
    const output = coercePreviewSpec({
      kind: 'bars',
      values: [12, Number.NaN, 20],
      secondaryValues: [Number.POSITIVE_INFINITY, 8],
    })

    expect(output.values).toEqual([12, 20])
    expect(output.secondaryValues).toEqual([8])
  })
})
