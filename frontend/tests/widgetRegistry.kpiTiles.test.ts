import { describe, expect, it } from 'vitest'
import { KPI_TILE_WIDGET_TYPES, WIDGET_DEFS } from '../src/components/stats/widgetRegistry'

describe('KPI tile registry', () => {
  it('keeps every single-value KPI as a square tile by default', () => {
    const defsByType = new Map(WIDGET_DEFS.map((def) => [def.type, def]))

    for (const type of KPI_TILE_WIDGET_TYPES) {
      const def = defsByType.get(type)

      expect(def, `${type} should exist in WIDGET_DEFS`).toBeTruthy()
      expect(def?.defaultSize, `${type} default size`).toEqual({ w: 180, h: 180 })
      expect(def?.minSize, `${type} min size`).toEqual({ w: 160, h: 160 })
      expect(def?.maxSize, `${type} max size`).toEqual({ w: 260, h: 260 })
    }
  })
})
