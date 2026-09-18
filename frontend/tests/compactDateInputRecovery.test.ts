import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const source = readFileSync(
  resolve(__dirname, '../src/components/ui/CompactDateInput.vue'),
  'utf8',
)

describe('compact date input recovery', () => {
  it('contains failures from the date-picker instead of escalating them to App.vue', () => {
    expect(source).toContain('onErrorCaptured')
    expect(source).toContain('pickerFailed.value = true')
    expect(source).toContain('return false')
  })

  it('uses the documented date-picker input slot and retains a native fallback', () => {
    expect(source).toContain('#dp-input="{ openMenu }"')
    expect(source).toContain('@click="openPicker(openMenu)"')
    expect(source).toContain('type="date"')
    expect(source).toContain('@input="onNativeInput"')
  })
})
