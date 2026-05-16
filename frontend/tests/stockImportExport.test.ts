import { describe, expect, it } from 'vitest'
import {
  analyzeImportRows,
  buildStockExportCsv,
  parseDateSmart,
  resolveImportMapping,
  toNumberSmart,
} from '../src/utils/stockImportExport'

describe('stockImportExport', () => {
  it('maps common import headers without using ambiguous generic columns twice', () => {
    const headers = ['Nom item', 'Prix achat', 'Prix vente', 'Date achat', 'Date vente', 'Pointure']
    const mapping = resolveImportMapping(headers)

    expect(mapping.name).toBe('Nom item')
    expect(mapping.retail).toBe('Prix achat')
    expect(mapping.resell).toBe('Prix vente')
    expect(mapping.dateAchat).toBe('Date achat')
    expect(mapping.dateVente).toBe('Date vente')
    expect(mapping.size).toBe('Pointure')
  })

  it('normalizes numbers and dates used by imports', () => {
    expect(toNumberSmart('1 234,50 EUR')).toBe(1234.5)
    expect(parseDateSmart('16/05/2026')).toBe('2026-05-16')
  })

  it('builds import payload and keeps duplicate detection against existing rows', () => {
    const headers = ['Nom item', 'Prix achat', 'Prix vente', 'Pointure']
    const rows = [{ 'Nom item': 'Jordan 1', 'Prix achat': '100', 'Prix vente': '150', Pointure: '42' }]
    const existingRows = [{ nomItem: 'Jordan 1', prixRetail: 100, metadata: { size: '42' } }]

    const preview = analyzeImportRows(rows, headers, resolveImportMapping(headers), existingRows)

    expect(preview.validItems).toBe(0)
    expect(preview.duplicateRows).toBe(1)
    expect(preview.rows[0].errors).toContain('Doublon detecte')
  })

  it('exports stock rows with Excel-friendly separator and escaped cells', () => {
    const csv = buildStockExportCsv([
      {
        nomItem: 'Nike "Sample"',
        type: 'SNEAKER',
        prixRetail: 100,
        prixResell: 125.5,
        dateAchat: '2026-05-16',
        metadata: { size: '42' },
      },
    ])

    expect(csv.startsWith('\uFEFFnom item;type;type label')).toBe(true)
    expect(csv).toContain('"Nike ""Sample"""')
    expect(csv).toContain('"125,5"')
    expect(csv).toContain('"42"')
  })
})
