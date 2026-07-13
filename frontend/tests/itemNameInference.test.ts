import { describe, expect, it } from 'vitest'
import { inferItemClassificationFromName } from '../src/RegleItem/itemNameInference'

describe('itemNameInference', () => {
  it('detects a sneaker and its family from a Jordan name', () => {
    expect(inferItemClassificationFromName('Nike Air Jordan 4 Bred')).toMatchObject({
      type: 'SNEAKER',
      subcategory: 'Jordan',
    })
  })

  it('detects Pokemon sealed products', () => {
    expect(inferItemClassificationFromName('Pokemon 151 ETB Dracaufeu')).toMatchObject({
      type: 'POKEMON_CARD',
      subcategory: 'ETB',
    })
  })

  it('can infer ETB from a short TB shorthand', () => {
    expect(inferItemClassificationFromName('TB', {
      storedSubcategories: { POKEMON_CARD: ['ETB'] },
    })).toMatchObject({
      type: 'POKEMON_CARD',
      subcategory: 'ETB',
    })
  })

  it('detects electronics from common model names', () => {
    expect(inferItemClassificationFromName('iPhone 15 Pro Max 256GB')).toMatchObject({
      type: 'ELECTRONICS',
      subcategory: 'Smartphone',
    })
  })

  it('detects event tickets', () => {
    expect(inferItemClassificationFromName('Billet concert Accor Arena')).toMatchObject({
      type: 'TICKET',
      subcategory: 'Concert',
    })
  })

  it('uses custom category and subcategory labels when they match the item name', () => {
    expect(
      inferItemClassificationFromName('Jeux video Nintendo Switch Zelda', {
        categoryLabels: { JEUX_VIDEO: 'Jeux video' },
        storedSubcategories: { JEUX_VIDEO: ['Nintendo'] },
      }),
    ).toMatchObject({
      type: 'JEUX_VIDEO',
      subcategory: 'Nintendo',
    })
  })
})
