import { describe, expect, it } from 'vitest'
import {
  buildDiscordStockListings,
  buildDiscordStockMessage,
  estimateDiscordProfitPercent,
  splitDiscordMessageForClipboard,
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
        prixRetail: 120,
        prixResell: 190,
        metadata: { size: '43', condition: 'Neuf', marketUrl: 'https://www.amazon.fr/example' },
      },
      {
        nomItem: 'New Balance 2002R',
        prixRetail: 120,
        metadata: { size: '44' },
      },
    ])

    expect(message.startsWith('WTS :\n\n')).toBe(true)
    expect(message).not.toContain('Bonjour a tous,')
    expect(message).not.toContain('CA si tout part')
    expect(message).not.toContain('Benef si tout part')
    expect(message).not.toContain('Marge cible')
    expect(message).toContain('- Air Max 1 Patta | Taille 43 | Etat Neuf | Prix 190 EUR')
    expect(message).toContain('- New Balance 2002R | Taille 44 | Prix 120 EUR')
  })

  it('recalculates Discord prices from a target profit percent', () => {
    const listings = buildDiscordStockListings(
      [
        {
          nomItem: 'Jordan 1 High',
          prixRetail: 200,
          prixResell: 320,
          metadata: { size: '44' },
        },
      ],
      { targetProfitPercent: 25 },
    )

    expect(listings[0]).toMatchObject({
      price: 250,
      priceLabel: '250 EUR',
      estimatedProfit: 50,
    })
  })

  it('includes target profit and totals in the Discord message', () => {
    const message = buildDiscordStockMessage(
      [
        {
          nomItem: 'Jordan 4 SB',
          prixRetail: 220,
          metadata: { size: '45' },
        },
      ],
      { targetProfitPercent: 30 },
    )

    expect(message).toContain('- Jordan 4 SB | Taille 45 | Prix 286 EUR')
    expect(message).not.toContain('CA si tout part')
    expect(message).not.toContain('Marge cible')
  })

  it('returns a fallback message when there is no stock to post', () => {
    const message = buildDiscordStockMessage([
      {
        nomItem: 'Yeezy 350',
        prixResell: 210,
        dateVente: '2026-07-20',
      },
    ])

    expect(message.startsWith('WTS :\n\n')).toBe(true)
    expect(message).toContain("Il n'y a aucun item disponible pour le moment.")
  })

  it('estimates a default Discord profit percent from existing resale prices', () => {
    const percent = estimateDiscordProfitPercent([
      {
        nomItem: 'Air Max',
        prixRetail: 100,
        prixResell: 130,
      },
      {
        nomItem: 'Jordan 3',
        prixRetail: 200,
        prixResell: 260,
      },
    ])

    expect(percent).toBe(30)
  })

  it('splits long Discord messages into plain-text chunks under the limit', () => {
    const message = [
      'WTS :',
      '',
      ...Array.from({ length: 80 }, (_, index) => `- Item ${index + 1} | Prix ${(index + 1) * 10} EUR`),
    ].join('\n')

    const chunks = splitDiscordMessageForClipboard(message, 250)

    expect(chunks.length).toBeGreaterThan(1)
    expect(chunks.every((chunk) => chunk.length <= 250)).toBe(true)
    expect(chunks.join('\n')).toContain('WTS :')
    expect(chunks.join('\n')).toContain('- Item 80 | Prix 800 EUR')
  })
})
