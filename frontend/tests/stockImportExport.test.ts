import { describe, expect, it } from 'vitest'
import {
  analyzeImportRows,
  buildImportIssueReportCsv,
  buildStockExportCsv,
  detectDelimiter,
  EXPORT_HEADERS,
  normalizeMapping,
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
    expect(toNumberSmart('$1,234.50')).toBe(1234.5)
    expect(toNumberSmart('1.234,50 €')).toBe(1234.5)
    expect(parseDateSmart('16/05/2026')).toBe('2026-05-16')
    expect(parseDateSmart('05/16/2026')).toBe('2026-05-16')
    expect(parseDateSmart('2026/05/16')).toBe('2026-05-16')
    expect(parseDateSmart('22/032024')).toBe('2024-03-22')
  })

  it('builds import payload and keeps duplicate detection against existing rows', () => {
    const headers = ['Nom item', 'Prix achat', 'Prix vente', 'Pointure']
    const rows = [{ 'Nom item': 'Jordan 1', 'Prix achat': '100', 'Prix vente': '150', Pointure: '42' }]
    const existingRows = [{ nomItem: 'Jordan 1', prixRetail: 100, metadata: { size: '42' } }]

    const preview = analyzeImportRows(rows, headers, resolveImportMapping(headers), existingRows)

    expect(preview.validItems).toBe(1)
    expect(preview.duplicateRows).toBe(0)
    expect(preview.rows[0].warnings).toContain('Ressemble à un item déjà présent')
  })

  it('keeps duplicates from the imported file as valid rows', () => {
    const headers = ['Nom item', 'Prix achat', 'Pointure']
    const rows = [
      { 'Nom item': 'Jordan 1', 'Prix achat': '100', Pointure: '42' },
      { 'Nom item': 'Jordan 1', 'Prix achat': '100', Pointure: '42' },
    ]

    const preview = analyzeImportRows(rows, headers, resolveImportMapping(headers), [])

    expect(preview.validRows).toBe(2)
    expect(preview.invalidRows).toBe(0)
    expect(preview.validItems).toBe(2)
    expect(preview.duplicateRows).toBe(1)
    expect(preview.rows[1].status).toBe('valid')
    expect(preview.rows[1].errors).toEqual([])
    expect(preview.rows[1].warnings).toContain('Doublon détecté dans le fichier')
  })

  it('keeps custom item types when importing rows', () => {
    const headers = ['Nom item', 'Type', 'Prix achat']
    const rows = [{ 'Nom item': 'Rolex sample', Type: 'Montres luxe', 'Prix achat': '1000' }]

    const preview = analyzeImportRows(rows, headers, resolveImportMapping(headers), [])

    expect(preview.validItems).toBe(1)
    expect(preview.payload[0].type).toBe('MONTRES_LUXE')
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

  it('accepts rows shaped like the built-in export even when optional sneaker metadata is missing', () => {
    const csv = buildStockExportCsv([
      {
        nomItem: 'Nike P-6000',
        type: 'SNEAKER',
        prixRetail: 110,
        prixResell: 140,
        dateAchat: '2026-05-16',
      },
    ])

    expect(detectDelimiter(csv)).toBe(';')

    const headers = EXPORT_HEADERS
    const rows = [
      {
        'nom item': 'Nike P-6000',
        type: 'SNEAKER',
        'type label': 'Sneaker',
        categorie: '',
        'prix retail': '110',
        'prix resell': '140',
        profit: '30',
        'date achat': '2026-05-16',
        'date vente': '',
        description: '',
        pointure: '',
      },
    ]
    const preview = analyzeImportRows(rows, headers, resolveImportMapping(headers), [])

    expect(preview.validItems).toBe(1)
    expect(preview.rows[0].errors).toEqual([])
    expect(preview.payload[0]).toMatchObject({
      nomItem: 'Nike P-6000',
      prixRetail: 110,
      prixResell: 140,
      dateAchat: '2026-05-16',
    })
  })

  it('maps tolerant header variants with mixed casing and wording', () => {
    const headers = ['Product Title', 'Retail Price EUR', 'Selling Price', 'Date Acheté', 'Date Sold']
    const mapping = resolveImportMapping(headers)

    expect(mapping.name).toBe('Product Title')
    expect(mapping.retail).toBe('Retail Price EUR')
    expect(mapping.resell).toBe('Selling Price')
    expect(mapping.dateAchat).toBe('Date Acheté')
    expect(mapping.dateVente).toBe('Date Sold')
  })

  it('recognizes acquisition cost headers used by inventory exports', () => {
    const mapping = resolveImportMapping(['Désignation article', "Coût d'acquisition"])

    expect(mapping.name).toBe('Désignation article')
    expect(mapping.retail).toBe("Coût d'acquisition")
  })

  it('recognizes French resale spreadsheets with a “nom de la paire” column', () => {
    const headers = ['nom de la paire', 'prix retail', 'prix resell', 'date', 'benef']
    const mapping = resolveImportMapping(headers)
    const preview = analyzeImportRows(
      [
        {
          'nom de la paire': 'dunk low b&w',
          'prix retail': 100,
          'prix resell': 185,
          date: 44233,
          benef: 85,
        },
      ],
      headers,
      mapping,
    )

    expect(mapping.name).toBe('nom de la paire')
    expect(mapping.retail).toBe('prix retail')
    expect(mapping.resell).toBe('prix resell')
    expect(mapping.dateAchat).toBe('date')
    expect(preview.validRows).toBe(1)
    expect(preview.payload[0]).toMatchObject({
      nomItem: 'dunk low b&w',
      prixRetail: 100,
      prixResell: 185,
      dateAchat: '2021-02-06',
    })
  })

  it('ignores empty spreadsheet tails that only contain values in unmapped columns', () => {
    const headers = ['nom de la paire', 'prix retail', 'benef']
    const preview = analyzeImportRows(
      [
        { 'nom de la paire': 'Jordan 1', 'prix retail': 100, benef: 20 },
        { 'nom de la paire': '', 'prix retail': '', benef: 0 },
      ],
      headers,
      resolveImportMapping(headers),
    )

    expect(preview.rowsDetected).toBe(1)
    expect(preview.validItems).toBe(1)
  })

  it('understands the useful columns in common Shopify-style exports', () => {
    const headers = [
      'Title',
      'Vendor',
      'Product Type',
      'Variant SKU',
      'Variant Price',
      'Cost per item',
      'Variant Inventory Qty',
      'Body (HTML)',
    ]
    const mapping = resolveImportMapping(headers)

    expect(mapping.name).toBe('Title')
    expect(mapping.brand).toBe('Vendor')
    expect(mapping.type).toBe('Product Type')
    expect(mapping.sku).toBe('Variant SKU')
    expect(mapping.resell).toBe('Variant Price')
    expect(mapping.retail).toBe('Cost per item')
    expect(mapping.quantity).toBe('Variant Inventory Qty')
    expect(mapping.notes).toBe('Body (HTML)')
  })

  it('does not let one manually selected column populate several fields', () => {
    const mapping = normalizeMapping(['Title', 'Price'], {
      name: 'Title',
      notes: 'Title',
      retail: 'Price',
      resell: 'Price',
    })

    expect(mapping.name).toBe('Title')
    expect(mapping.notes).toBe('')
    expect(mapping.retail).toBe('Price')
    expect(mapping.resell).toBe('')
  })

  it('warns about ambiguous day/month dates instead of silently hiding the choice', () => {
    const headers = ['Title', 'Purchase date']
    const preview = analyzeImportRows(
      [{ Title: 'Jordan 1', 'Purchase date': '05/06/2026' }],
      headers,
      resolveImportMapping(headers),
    )

    expect(preview.payload[0].dateAchat).toBe('2026-06-05')
    expect(preview.rows[0].warnings).toContain("Date d'achat ambiguë : interprétée au format jour/mois/année")
  })

  it('builds a correction report that clearly separates skipped and imported rows', () => {
    const headers = ['Title', 'Quantity']
    const preview = analyzeImportRows(
      [
        { Title: '', Quantity: '1' },
        { Title: 'Jordan 1', Quantity: '1' },
      ],
      headers,
      resolveImportMapping(headers),
    )
    const report = buildImportIssueReportCsv(preview)

    expect(report).toContain('ligne;item;statut;action;erreurs;avertissements')
    expect(report).toContain('non importe')
    expect(report).toContain('Modèle ou nom manquant')
  })
})
