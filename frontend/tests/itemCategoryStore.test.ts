import { describe, expect, it } from 'vitest'
import {
  addItemCategory,
  buildItemCategoryAliases,
  canRemoveItemCategory,
  itemTypeLabel,
  normalizeItemType,
  readStoredItemCategories,
  removeItemCategory,
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

  it('adds and removes custom main categories', () => {
    let labels = readStoredItemCategories('u1', memoryStorage())
    const added = addItemCategory(labels, 'Montres luxe')
    labels = added.labels

    expect(added.type).toBe('MONTRES_LUXE')
    expect(canRemoveItemCategory(added.type)).toBe(true)
    expect(itemTypeLabel(added.type, labels)).toBe('Montres luxe')
    expect(resolveItemTypeOptions(labels)).toContainEqual(
      expect.objectContaining({
        value: 'MONTRES_LUXE',
        label: 'Montres luxe',
        custom: true,
      }),
    )

    labels = removeItemCategory(labels, added.type)
    expect(resolveItemTypeOptions(labels).some((option) => option.value === added.type)).toBe(false)
  })

  it('normalizes unknown values as custom type keys', () => {
    expect(normalizeItemType('montres luxe')).toBe('MONTRES_LUXE')
  })

  it('builds aliases from default and custom labels', () => {
    const labels = renameItemCategory(
      addItemCategory({}, 'Montres luxe').labels,
      'POKEMON_CARD',
      'Cartes Pokemon',
    )
    const aliases = buildItemCategoryAliases(labels)
    expect(aliases.has('pokemon')).toBe(true)
    expect(aliases.has('cartes pokemon')).toBe(true)
    expect(aliases.has('montres luxe')).toBe(true)
  })
})
