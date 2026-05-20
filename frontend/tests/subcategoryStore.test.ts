import { describe, expect, it } from 'vitest'
import {
  addSubcategory,
  dedupeSubcategories,
  extractSubcategoriesByType,
  normalizeItemType,
  readStoredSubcategories,
  removeSubcategory,
  renameSubcategory,
  resolveSubcategoryOptions,
} from '../src/RegleItem/subcategoryStore'

function memoryStorage(seed = {}) {
  const data = new Map(Object.entries(seed))
  return {
    getItem: (key: string) => data.get(key) ?? null,
    setItem: (key: string, value: unknown) => data.set(key, String(value)),
    removeItem: (key: string) => data.delete(key),
  }
}

describe('subcategoryStore', () => {
  it('normalizes supported item types and falls back to OTHER', () => {
    expect(normalizeItemType('SNEAKER')).toBe('SNEAKER')
    expect(normalizeItemType('unknown')).toBe('OTHER')
  })

  it('deduplicates subcategories while keeping display casing', () => {
    expect(dedupeSubcategories([' Jordan ', 'jordan', 'Dunk'])).toEqual(['Dunk', 'Jordan'])
  })

  it('initializes persisted lists with defaults', () => {
    const stored = readStoredSubcategories('u1', memoryStorage())
    expect(stored.SNEAKER).toContain('Jordan')
    expect(stored.TICKET).toContain('Concert')
  })

  it('merges saved and discovered values by main category', () => {
    const stored = addSubcategory({}, 'SNEAKER', 'SB Dunk')
    const discovered = extractSubcategoriesByType([
      { type: 'SNEAKER', categorie: 'Running' },
      { type: 'TICKET', categorie: 'Festival' },
    ])

    expect(resolveSubcategoryOptions('SNEAKER', { stored, discovered })).toEqual(
      expect.arrayContaining(['Jordan', 'Running', 'SB Dunk']),
    )
    expect(resolveSubcategoryOptions('TICKET', { stored, discovered })).toEqual(
      expect.arrayContaining(['Festival']),
    )
  })

  it('does not expose main category labels as subcategories', () => {
    expect(
      resolveSubcategoryOptions('POKEMON_CARD', {
        stored: { POKEMON_CARD: ['Pokemon', 'Booster'] },
        currentValue: 'Pokemon',
      }),
    ).toEqual(['Booster'])
  })

  it('supports rename and delete without touching other item types', () => {
    let map = addSubcategory({}, 'OTHER', 'Accessoire')
    map = renameSubcategory(map, 'OTHER', 'Accessoire', 'Objet rare')
    expect(map.OTHER).toContain('Objet rare')

    map = removeSubcategory(map, 'OTHER', 'Objet rare')
    expect(map.OTHER).not.toContain('Objet rare')
    expect(map.SNEAKER).toContain('Jordan')
  })
})
