import { describe, expect, it } from 'vitest'
import { searchInventoryItems } from '../src/utils/inventorySearch'

describe('inventorySearch', () => {
  const items = [
    {
      id: 1,
      nomItem: 'Jordan 1 Chicago',
      categorie: 'Jordan',
      type: 'SNEAKER',
      metadata: { size: '42', sku: 'DZ5485-612' },
      prixRetail: 180,
    },
    {
      id: 2,
      nomItem: 'One Piece TCG OP-01',
      categorie: 'Booster',
      type: 'COLLECTIBLE',
      metadata: { reference: 'OP-01' },
      prixRetail: 120,
    },
    {
      id: 3,
      nomItem: 'Jordan Hoodie PSG',
      categorie: 'Hoodie',
      type: 'CLOTHING',
      metadata: { size: 'L' },
      prixRetail: 90,
    },
  ]

  it('returns the most relevant results first', () => {
    const results = searchInventoryItems(items, 'jordan')
    expect(results.map((item) => item.id)).toEqual([1, 3])
  })

  it('matches compact and typo queries like the gestion search', () => {
    const compactResults = searchInventoryItems(items, 'op01').map((item) => item.id)
    const typoResults = searchInventoryItems(items, 'jodan chicago').map((item) => item.id)

    expect(compactResults[0]).toBe(2)
    expect(typoResults[0]).toBe(1)
  })

  it('searches across metadata and item type labels', () => {
    const skuResults = searchInventoryItems(items, 'dz5485').map((item) => item.id)
    const typeResults = searchInventoryItems(items, 'collection').map((item) => item.id)

    expect(skuResults[0]).toBe(1)
    expect(typeResults[0]).toBe(2)
  })
})
