import { describe, expect, it, vi } from 'vitest'
import { applyTemplatePipeline } from '../src/composables/useTemplateEngine'
import type { StatsTemplateRecord } from '../src/components/stats/templates/templateTypes'

const template: StatsTemplateRecord = {
  id: 'sys_pipeline',
  source: 'system',
  name: 'Pipeline',
  description: 'Template test pipeline',
  category: 'Test',
  tags: ['pipeline'],
  accent: '#0ea5e9',
  density: 'dense',
  recommendedRange: { kind: 'month' },
  layout: [
    { type: 'grossRevenue', x: 0, y: 0, w: 700, h: 320 },
    { type: 'netProfit', x: 720, y: 0, w: 700, h: 320 },
    { type: 'goalProgress', x: 0, y: 360, w: 700, h: 320 },
    { type: 'alertFeed', x: 720, y: 360, w: 700, h: 320 },
  ],
}

describe('applyTemplatePipeline', () => {
  it('applies template in batches and emits progress phases', async () => {
    const phaseSpy = vi.fn()
    const setWidgetsSpy = vi.fn()
    const batchSpy = vi.fn()
    const finalizeSpy = vi.fn()

    const result = await applyTemplatePipeline({
      template,
      existingWidgets: [],
      createWidgetId: (type) => `${type}_id`,
      onPrepare: vi.fn(),
      onSetWidgets: setWidgetsSpy,
      onBatchInserted: batchSpy,
      onSceneAndRange: vi.fn(),
      onFinalize: finalizeSpy,
      onPhase: phaseSpy,
      batchSize: 2,
      scheduler: async () => {},
    })

    expect(result.applied).toBe(true)
    expect(result.widgets).toHaveLength(4)
    expect(setWidgetsSpy).toHaveBeenCalledTimes(3) // clear + 2 batches
    expect(batchSpy).toHaveBeenCalledTimes(2)
    expect(finalizeSpy).toHaveBeenCalledTimes(1)
    const phases = phaseSpy.mock.calls.map((call) => call[0].phase)
    expect(phases).toContain('preparing')
    expect(phases).toContain('rendering')
    expect(phases).toContain('finalizing')
    expect(phases).toContain('done')
  })

  it('stops when replacement confirmation is rejected', async () => {
    const result = await applyTemplatePipeline({
      template,
      existingWidgets: [{ id: 'w1', type: 'grossRevenue', title: 'x', x: 0, y: 0, w: 1, h: 1 }],
      createWidgetId: (type) => `${type}_id`,
      confirmReplace: () => false,
      onPrepare: vi.fn(),
      onSetWidgets: vi.fn(),
      onSceneAndRange: vi.fn(),
      onFinalize: vi.fn(),
      scheduler: async () => {},
    })

    expect(result.applied).toBe(false)
    expect(result.widgets).toHaveLength(0)
  })
})

