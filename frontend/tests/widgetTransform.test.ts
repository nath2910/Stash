import { describe, expect, it } from 'vitest'
import {
  buildWidgetTransform,
  normalizeWidgetRotation,
  resolveWidgetRotation,
} from '../src/components/stats/canvas/widgetTransform'

describe('widgetTransform', () => {
  it('normalizes rotation to a stable signed angle', () => {
    expect(normalizeWidgetRotation(450)).toBe(90)
    expect(normalizeWidgetRotation(270)).toBe(-90)
    expect(normalizeWidgetRotation('bad')).toBe(0)
  })

  it('resolves persisted text widget rotation from current and legacy props', () => {
    expect(resolveWidgetRotation({ props: { rotation: -90 } })).toBe(-90)
    expect(resolveWidgetRotation({ props: { orientation: 'vertical-right' } })).toBe(90)
    expect(resolveWidgetRotation({ rotation: 180 })).toBe(180)
  })

  it('keeps rotation in the DOM transform string', () => {
    expect(buildWidgetTransform(12, 24, -90)).toBe('translate3d(12px, 24px, 0) rotate(-90deg)')
    expect(buildWidgetTransform(12, 24, 0)).toBe('translate3d(12px, 24px, 0)')
  })
})
