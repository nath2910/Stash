import { describe, expect, it } from 'vitest'
import { fitKpiValueSize } from '../src/components/stats/widgets/_parts/kpiTextFit'

const tileOptions = (width: number, height: number) => ({
  min: 22,
  max: 76,
  paddingX: Math.max(54, width * 0.3),
  paddingY: Math.max(18, height * 0.2),
  heightRatio: 0.48,
})

describe('kpiTextFit', () => {
  it('keeps percentage KPI values readable inside a compact square tile', () => {
    const size = fitKpiValueSize('75,7 %', 180, 140, tileOptions(180, 140))

    expect(size).toBeGreaterThanOrEqual(38)
    expect(size).toBeLessThanOrEqual(44)
  })

  it('scales values up only when the tile has enough room', () => {
    const compact = fitKpiValueSize('550 €', 180, 140, tileOptions(180, 140))
    const large = fitKpiValueSize('550 €', 260, 220, tileOptions(260, 220))

    expect(large).toBeGreaterThan(compact)
    expect(large).toBeLessThanOrEqual(76)
  })
})
