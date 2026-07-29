import { describe, expect, it } from 'vitest'
import {
  buildDiscordStockListings,
  buildDiscordStockMessage,
} from '../src/utils/marketplaceMessageBuilder'

describe('marketplaceMessageBuilder', () => {
  it('builds Discord listings from in-stock items only', () => {
    const listings = buildDiscordStockListings([
      {
        id: 1,
        nomItem: 'Jordan 4 Military Black',
        prixResell: 260,
        metadata: { size: '42.5', condition: 'TBE' },
      },
      {
        id: 2,
        nomItem: 'Nike Dunk Low Panda',
        prixResell: 150,
        dateVente: '2026-07-12',
        metadata: { size: '41' },
      },
    ])

    expect(listings).toHaveLength(1)
    expect(listings[0]).toMatchObject({
      id: 1,
      name: 'Jordan 4 Military Black',
      size: '42.5',
      condition: 'TBE',
      price: 260,
      priceLabel: '260 EUR',
    })
  })

  it('builds a copy-ready Discord message from available stock', () => {
    const message = buildDiscordStockMessage([
      {
        nomItem: 'Air Max 1 Patta',
        prixResell: 190,
        metadata: { size: '43', condition: 'Neuf', marketUrl: 'https://www.amazon.fr/example' },
      },
      {
        nomItem: 'New Balance 2002R',
        prixRetail: 120,
        metadata: { size: '44' },
      },
    ])

    expect(message).toContain('Bonjour a tous,')
    expect(message).toContain('Voici le stock actuellement disponible :')
    expect(message).toContain('- Air Max 1 Patta | Taille 43 | Etat Neuf | Prix 190 EUR')
    expect(message).toContain('- New Balance 2002R | Taille 44 | Prix 120 EUR')
  })

  it('returns a fallback message when there is no stock to post', () => {
    const message = buildDiscordStockMessage([
      {
        nomItem: 'Yeezy 350',
        prixResell: 210,
        dateVente: '2026-07-20',
      },
    ])

    expect(message).toContain("Il n'y a aucun item disponible pour le moment.")
  })
})
