import { describe, expect, it } from 'vitest'
import {
  buildCommonDatePatch,
  buildCommonDateRangePatch,
  duplicateWidgetGroup,
} from '../src/components/stats/canvas/widgetBatchActions'

describe('duplicateWidgetGroup', () => {
  it('duplicates a selection while preserving relative layout and props', () => {
    const sources = [
      {
        id: 'a',
        type: 'netProfit',
        title: 'A',
        x: 100,
        y: 120,
        w: 200,
        h: 140,
        props: { bucket: 'week', nested: { keep: true } },
        z: 2,
      },
      {
        id: 'b',
        type: 'inventoryValue',
        title: 'B',
        x: 360,
        y: 200,
        w: 180,
        h: 180,
        props: { asOf: '2026-05-20' },
        z: 5,
      },
    ]

    const result = duplicateWidgetGroup(sources, {
      createId: (type, source) => `${type}_${source.id}_copy`,
      cloneProps: (props) => structuredClone(props),
      boardWidth: 9000,
      boardHeight: 6000,
      offset: 60,
      zStart: 10,
    })

    expect(result.duplicates.map((widget) => widget.id)).toEqual([
      'netProfit_a_copy',
      'inventoryValue_b_copy',
    ])
    expect(result.duplicates[1].x - result.duplicates[0].x).toBe(sources[1].x - sources[0].x)
    expect(result.duplicates[1].y - result.duplicates[0].y).toBe(sources[1].y - sources[0].y)
    expect(result.duplicates[0].props).toEqual(sources[0].props)
    expect(result.duplicates[0].props).not.toBe(sources[0].props)
    expect(result.duplicates.map((widget) => widget.z)).toEqual([11, 12])
  })

  it('uses a reverse offset near the board edge', () => {
    const result = duplicateWidgetGroup(
      [
        {
          id: 'edge',
          type: 'rectangleShape',
          x: 940,
          y: 940,
          w: 50,
          h: 50,
          props: {},
        },
      ],
      {
        createId: (type) => `${type}_copy`,
        cloneProps: (props) => ({ ...props }),
        boardWidth: 1000,
        boardHeight: 1000,
        offset: 80,
        zStart: 1,
      },
    )

    expect(result.duplicates[0].x).toBe(860)
    expect(result.duplicates[0].y).toBe(860)
  })
})

describe('buildCommonDatePatch', () => {
  it('builds a local range patch for date-compatible widgets', () => {
    expect(buildCommonDatePatch({}, '2026-05-20')).toEqual({
      useGlobalRange: false,
      from: '2026-05-20',
      to: '2026-05-20',
    })
  })

  it('builds an as-of patch for snapshot widgets', () => {
    expect(buildCommonDatePatch({ dateMode: 'asOf' }, '2026-05-20')).toEqual({
      useGlobalRange: false,
      asOf: '2026-05-20',
    })
  })

  it('ignores widgets without compatible date settings', () => {
    expect(buildCommonDatePatch({ hideGlobalRange: true }, '2026-05-20')).toBeNull()
    expect(buildCommonDatePatch({ dateMode: 'custom' }, '2026-05-20')).toBeNull()
  })

  it('builds a sorted local range patch for multi-selection updates', () => {
    expect(buildCommonDateRangePatch({}, '2026-05-31', '2026-05-01')).toEqual({
      useGlobalRange: false,
      from: '2026-05-01',
      to: '2026-05-31',
    })
  })

  it('uses the range end date for as-of widgets', () => {
    expect(buildCommonDateRangePatch({ dateMode: 'asOf' }, '2026-05-01', '2026-05-31')).toEqual({
      useGlobalRange: false,
      asOf: '2026-05-31',
    })
  })
})
