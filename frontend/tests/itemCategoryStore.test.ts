import { describe, expect, it } from 'vitest'
import {
  addItemCategory,
  buildItemCategoryAliases,
  canRemoveItemCategory,
  itemCategoryStorageKey,
  itemTypeLabel,
  normalizeItemType,
  readStoredItemCategories,
  removeItemCategory,
  renameItemCategory,
  resetItemCategory,
  resolveItemTypeOptions,
  writeStoredItemCategories,
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

  it('removes and restores default main categories', () => {
    const storage = memoryStorage()
    let labels = readStoredItemCategories('u1', storage)

    expect(canRemoveItemCategory('SNEAKER')).toBe(true)
    labels = removeItemCategory(labels, 'SNEAKER')
    expect(resolveItemTypeOptions(labels).some((option) => option.value === 'SNEAKER')).toBe(false)

    writeStoredItemCategories('u1', labels, storage)
    const reloaded = readStoredItemCategories('u1', storage)
    const reloadedOptions = resolveItemTypeOptions(reloaded)
    expect(reloadedOptions.some((option) => option.value === 'SNEAKER')).toBe(false)
    expect(reloadedOptions.some((option) => option.value === '__removedItemTypes')).toBe(false)

    labels = resetItemCategory(reloaded, 'SNEAKER')
    expect(resolveItemTypeOptions(labels)).toContainEqual(
      expect.objectContaining({
        value: 'SNEAKER',
        label: 'Sneakers',
      }),
    )
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

  it('keeps persisted labels isolated per account', () => {
    const storage = memoryStorage()
    const u1 = renameItemCategory(readStoredItemCategories('u1', storage), 'SNEAKER', 'Chaussures U1')
    const u2 = renameItemCategory(readStoredItemCategories('u2', storage), 'SNEAKER', 'Chaussures U2')

    writeStoredItemCategories('u1', u1, storage)
    writeStoredItemCategories('u2', u2, storage)

    expect(readStoredItemCategories('u1', storage).SNEAKER).toBe('Chaussures U1')
    expect(readStoredItemCategories('u2', storage).SNEAKER).toBe('Chaussures U2')
  })

  it('migrates legacy storage keys to the scoped current key', () => {
    const storage = memoryStorage({
      snk_item_categories_v1_u1: JSON.stringify({
        SNEAKER: 'Chaussures archivees',
      }),
    })

    const labels = readStoredItemCategories('u1', storage)

    expect(labels.SNEAKER).toBe('Chaussures archivees')
    expect(storage.getItem(itemCategoryStorageKey('u1'))).toContain('Chaussures archivees')
    expect(storage.getItem('snk_item_categories_v1_u1')).toBe(null)
  })

  it('repairs corrupted persisted payloads with sane defaults', () => {
    const storage = memoryStorage({
      [itemCategoryStorageKey('u1')]: '{bad json',
    })

    const labels = readStoredItemCategories('u1', storage)

    expect(labels.SNEAKER).toBe('Sneakers')
    expect(storage.getItem(itemCategoryStorageKey('u1'))).toContain('"SNEAKER":"Sneakers"')
  })

  it('recovers from a corrupted current key when a legacy key is still valid', () => {
    const storage = memoryStorage({
      [itemCategoryStorageKey('u1')]: '{bad json',
      snk_item_categories_v1_u1: JSON.stringify({
        SNEAKER: 'Chaussures recuperees',
      }),
    })

    const labels = readStoredItemCategories('u1', storage)

    expect(labels.SNEAKER).toBe('Chaussures recuperees')
    expect(storage.getItem(itemCategoryStorageKey('u1'))).toContain('Chaussures recuperees')
    expect(storage.getItem('snk_item_categories_v1_u1')).toBe(null)
  })
})
