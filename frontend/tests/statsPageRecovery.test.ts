import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const source = readFileSync(resolve(__dirname, '../src/pages/statsPage.vue'), 'utf8')

describe('stats page recovery shell', () => {
  it('loads the heavy stats canvas asynchronously with an immediate fallback', () => {
    expect(source).toContain('defineAsyncComponent')
    expect(source).toContain("loader: () => import('@/components/stats/StatsCanvas.vue')")
    expect(source).toContain('loadingComponent')
    expect(source).toContain('stats-canvas-bootstrap')
  })

  it('keeps the watchdog and manual retry path active', () => {
    expect(source).toContain('CANVAS_STALLED_MS')
    expect(source).toContain('CANVAS_AUTO_RETRY_MS')
    expect(source).toContain('function retryCanvas()')
    expect(source).toContain('Relancer')
  })
})
