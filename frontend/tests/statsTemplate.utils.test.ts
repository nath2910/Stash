import { describe, expect, it } from 'vitest'
import {
  applyTemplateLayout,
  densifyTemplateLayout,
  filterTemplates,
  mapTemplatePreview,
  normalizeTemplateDensity,
  normalizeTemplateRecord,
  resolveTemplateRange,
} from '../src/components/stats/templates/statsTemplateUtils'
import type { StatsTemplateRecord } from '../src/components/stats/templates/templateTypes'

const sampleTemplate = {
  id: 'custom_a',
  name: 'Template A',
  description: 'Vue test',
  category: 'Finance',
  tags: ['profit', 'kpi', 'profit'],
  accent: '#abc',
  recommendedRange: { kind: 'custom', from: '2026-03-01', to: '2026-03-15' },
  scene: {
    id: 'scene_test',
    boardBackground: 'linear-gradient(180deg, rgba(2, 6, 23, 1), rgba(10, 20, 30, 1))',
  },
  layout: [
    { type: 'netProfit', x: 10, y: 20, w: 640, h: 300, props: { view: 'line' } },
    { type: 'unknownWidget' },
    { type: 'grossRevenue' },
  ],
}

describe('normalizeTemplateRecord', () => {
  it('normalise metadata, accents and layout', () => {
    const output = normalizeTemplateRecord(sampleTemplate, { source: 'custom' })
    expect(output).not.toBeNull()
    expect(output?.id).toBe('custom_a')
    expect(output?.tags).toEqual(['profit', 'kpi'])
    expect(output?.accent).toBe('#aabbcc')
    expect(output?.scene?.id).toBe('scene_test')
    expect(output?.layout).toHaveLength(2)
    expect(output?.density).toBe('balanced')
    expect(output?.themeTokenId).toBe('executive_cobalt')
    expect(output?.recommendedRange).toEqual({
      kind: 'custom',
      from: '2026-03-01',
      to: '2026-03-15',
    })
  })

  it('drops invalid scene backgrounds with url()', () => {
    const output = normalizeTemplateRecord(
      {
        ...sampleTemplate,
        id: 'scene_bad',
        scene: { id: 'bad', boardBackground: 'url(https://x.test/bg.png)' },
      },
      { source: 'custom' },
    )
    expect(output?.scene).toBeUndefined()
  })

  it('keeps scene scaffold zones when valid', () => {
    const output = normalizeTemplateRecord(
      {
        ...sampleTemplate,
        id: 'scene_scaffold',
        scene: {
          id: 'scene_scaffold',
          boardBackground: 'linear-gradient(180deg, rgba(2, 6, 23, 1), rgba(10, 20, 30, 1))',
          scaffold: {
            layout: 'executive',
            zones: [{ id: 'hero', title: 'Pilotage', x: 200, y: 120, w: 2200, h: 320, tone: 'hero' }],
          },
        },
      },
      { source: 'custom' },
    )
    expect(output?.scene?.scaffold?.layout).toBe('executive')
    expect(output?.scene?.scaffold?.zones).toHaveLength(1)
    expect(output?.scene?.scaffold?.zones[0].title).toBe('Pilotage')
  })
})

describe('filterTemplates', () => {
  const templates: StatsTemplateRecord[] = [
    normalizeTemplateRecord(sampleTemplate, { source: 'custom' }) as StatsTemplateRecord,
    normalizeTemplateRecord(
      {
        id: 'sys_1',
        name: 'Bilan annuel',
        description: 'Systeme',
        category: 'Finance',
        tags: ['annuel'],
        accent: '#2563eb',
        recommendedRange: { kind: 'year' },
        layout: [{ type: 'grossRevenue' }],
      },
      { source: 'system' },
    ) as StatsTemplateRecord,
  ]

  it('supports query and source filters', () => {
    const output = filterTemplates(templates, {
      query: 'annuel',
      category: 'all',
      source: 'system',
    })
    expect(output).toHaveLength(1)
    expect(output[0].id).toBe('sys_1')
  })

  it('supports category filtering', () => {
    const output = filterTemplates(templates, {
      query: '',
      category: 'Finance',
      source: 'all',
    })
    expect(output).toHaveLength(2)
  })
})

describe('mapTemplatePreview', () => {
  it('maps layout widgets to palette previews', () => {
    const template = normalizeTemplateRecord(sampleTemplate, { source: 'custom' }) as StatsTemplateRecord
    const previews = mapTemplatePreview(template, 2)
    expect(previews).toHaveLength(2)
    expect(previews[0].preview).toHaveProperty('kind')
  })
})

describe('applyTemplateLayout', () => {
  it('regenerates widget ids and preserves template widget types', () => {
    const template = normalizeTemplateRecord(sampleTemplate, { source: 'custom' }) as StatsTemplateRecord
    const output = applyTemplateLayout(template, (type) => `${type}_new`)
    expect(output).toHaveLength(2)
    expect(output[0].id).toBe('netProfit_new')
    expect(output[1].id).toBe('grossRevenue_new')
  })
})

describe('densifyTemplateLayout', () => {
  it('reduces large vertical gaps in dense mode', () => {
    const dense = densifyTemplateLayout(
      [
        { type: 'grossRevenue', x: 0, y: 100, w: 600, h: 300 },
        { type: 'netProfit', x: 0, y: 780, w: 600, h: 300 },
        { type: 'textBlock', x: 0, y: 1320, w: 600, h: 180 },
      ],
      'dense',
    )
    expect((dense[1].y ?? 0) - (dense[0].y ?? 0)).toBeLessThan(680)
    expect(dense[2].h).toBeLessThan(180)
  })

  it('keeps balanced density by default', () => {
    expect(normalizeTemplateDensity(undefined)).toBe('balanced')
    expect(normalizeTemplateDensity('dense')).toBe('dense')
  })
})

describe('resolveTemplateRange', () => {
  it('resolves month/ytd/year ranges from a fixed date', () => {
    const now = new Date('2026-04-18T12:00:00Z')
    expect(resolveTemplateRange({ kind: 'month' }, now)).toEqual({
      from: '2026-03-18',
      to: '2026-04-18',
    })
    expect(resolveTemplateRange({ kind: 'ytd' }, now)).toEqual({
      from: '2026-01-01',
      to: '2026-04-18',
    })
    expect(resolveTemplateRange({ kind: 'year' }, now)).toEqual({
      from: '2025-04-18',
      to: '2026-04-18',
    })
  })
})
