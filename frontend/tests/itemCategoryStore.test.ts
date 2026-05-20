import { describe, expect, it } from 'vitest'
import {
  buildItemCategoryAliases,
  itemTypeLabel,
  readStoredItemCategories,
  renameItemCategory,
  resetItemCategory,
  resolveItemTypeOptions,
} from '../src/RegleItem/itemCategoryStore'

function memoryStorage(seed = {}) {
  const data = new Map(Object.entries(seed))
  return {
    getItem: (key: string) => data.get(key) ?? null,
    setItem: (key: string, value: unknown) => data.set(key, String(value)),
    removeItem: (key: string) => data.delete(key),
  }
}

describe('itemCategoryStore', () => {
  it('initializes persisted category labels with defaults', () => {
    const labels = readStoredItemCategories('u1', memoryStorage())
    expect(labels.SNEAKER).toBe('Sneakers')
    expect(labels.POKEMON_CARD).toBe('Pokemon')
  })

  it('renames and resets a fixed backend category label', () => {
    let labels = readStoredItemCategories('u1', memoryStorage())
    labels = renameItemCategory(labels, 'OTHER', 'Objets perso')
    expect(itemTypeLabel('OTHER', labels)).toBe('Objets perso')

    labels = resetItemCategory(labels, 'OTHER')
    expect(itemTypeLabel('OTHER', labels)).toBe('Autre')
  })

  it('resolves options while preserving stable backend values', () => {
    const labels = renameItemCategory({}, 'SNEAKER', 'Chaussures')
    expect(resolveItemTypeOptions(labels)).toContainEqual(
      expect.objectContaining({
        value: 'SNEAKER',
        label: 'Chaussures',
        defaultLabel: 'Sneakers',
      }),
    )
  })

  it('builds aliases from default and custom labels', () => {
    const labels = renameItemCategory({}, 'POKEMON_CARD', 'Cartes Pokemon')
    const aliases = buildItemCategoryAliases(labels)
    expect(aliases.has('pokemon')).toBe(true)
    expect(aliases.has('cartes pokemon')).toBe(true)
  })
})
