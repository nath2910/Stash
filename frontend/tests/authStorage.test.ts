import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  AUTH_SYNC_EVENT,
  clearAuthState,
  readAuthToken,
  readStoredUser,
  safeStorageGet,
  writeAuthState,
} from '../src/utils/authStorage'

function createStorage(initialValues: Record<string, string> = {}) {
  const store = new Map(Object.entries(initialValues))
  return {
    getItem: vi.fn((key: string) => (store.has(key) ? store.get(key)! : null)),
    setItem: vi.fn((key: string, value: string) => {
      store.set(key, value)
    }),
    removeItem: vi.fn((key: string) => {
      store.delete(key)
    }),
  }
}

describe('authStorage', () => {
  let localStorageMock: ReturnType<typeof createStorage>
  let sessionStorageMock: ReturnType<typeof createStorage>
  const dispatchEvent = vi.fn()

  beforeEach(() => {
    localStorageMock = createStorage()
    sessionStorageMock = createStorage()
    dispatchEvent.mockReset()

    vi.stubGlobal('localStorage', localStorageMock)
    vi.stubGlobal('sessionStorage', sessionStorageMock)
    vi.stubGlobal('window', { dispatchEvent })
  })

  it('reads the auth token from session storage and removes a legacy local value', () => {
    localStorageMock.setItem('snk_token', 'local-token')
    sessionStorageMock.setItem('snk_token', 'session-token')

    expect(readAuthToken()).toBe('session-token')
    expect(localStorageMock.removeItem).toHaveBeenCalledWith('snk_token')

    sessionStorageMock.removeItem('snk_token')
    expect(readAuthToken()).toBe('')
    expect(safeStorageGet('snk_token')).toBeNull()
  })

  it('writes and clears token/user consistently and emits a sync event', () => {
    writeAuthState({
      token: 'abc123',
      user: { id: 7, email: 'user@example.com' },
    })

    expect(sessionStorageMock.setItem).toHaveBeenCalledWith('snk_token', 'abc123')
    expect(readStoredUser()).toEqual({ id: 7, email: 'user@example.com' })
    expect(dispatchEvent).toHaveBeenCalledTimes(1)
    expect(dispatchEvent.mock.calls[0][0].type).toBe(AUTH_SYNC_EVENT)

    clearAuthState()

    expect(readAuthToken()).toBe('')
    expect(readStoredUser()).toBeNull()
    expect(localStorageMock.removeItem).toHaveBeenCalledWith('snk_token')
    expect(localStorageMock.removeItem).toHaveBeenCalledWith('snk_user')
    expect(dispatchEvent).toHaveBeenCalledTimes(2)
  })

  it('keeps credentials scoped to the browser session', () => {
    writeAuthState({
      token: 'fallback-token',
      user: { id: 9 },
    })

    expect(sessionStorageMock.setItem).toHaveBeenCalledWith('snk_token', 'fallback-token')
    expect(readAuthToken()).toBe('fallback-token')
    expect(readStoredUser()).toEqual({ id: 9 })
  })
})
