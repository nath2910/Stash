import { beforeEach, describe, expect, it, vi } from 'vitest'

const { billingStatus } = vi.hoisted(() => ({
  billingStatus: vi.fn(),
}))

vi.mock('@/services/BillingService', () => ({
  default: {
    status: billingStatus,
  },
}))

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

async function loadBillingStoreForUser(user: { id: number; subscriptionStatus?: string; hasAccess?: boolean }) {
  vi.resetModules()
  sessionStorage.setItem('snk_user', JSON.stringify(user))
  return import('../src/store/billingStore')
}

describe('billingStore', () => {
  beforeEach(() => {
    vi.unstubAllGlobals()
    vi.stubGlobal('localStorage', createStorage())
    vi.stubGlobal('sessionStorage', createStorage())
    billingStatus.mockReset()
  })

  it('keeps cached subscription status scoped to the current user', async () => {
    let module = await loadBillingStoreForUser({ id: 1 })
    let billing = module.useBillingStore()

    billing.seedFromUser({ id: 1, subscriptionStatus: 'active', hasAccess: true })

    module = await loadBillingStoreForUser({ id: 2 })
    billing = module.useBillingStore()

    expect(billing.status.value).toBe('unknown')
    expect(billing.hasAccess.value).toBe(false)

    billing.seedFromUser({ id: 2, subscriptionStatus: 'inactive', hasAccess: false })

    module = await loadBillingStoreForUser({ id: 1 })
    billing = module.useBillingStore()

    expect(billing.status.value).toBe('active')
    expect(billing.hasAccess.value).toBe(true)
  })

  it('removes legacy unscoped billing cache on load', async () => {
    localStorage.setItem(
      'snk_billing_status_cache',
      JSON.stringify({ status: 'active', hasAccess: true, fetchedAt: Date.now() }),
    )

    const module = await loadBillingStoreForUser({ id: 7 })
    const billing = module.useBillingStore()

    expect(localStorage.removeItem).toHaveBeenCalledWith('snk_billing_status_cache')
    expect(billing.status.value).toBe('unknown')
  })

  it('clears stale active access when a forced status refresh fails', async () => {
    const module = await loadBillingStoreForUser({ id: 12 })
    const billing = module.useBillingStore()

    billing.seedFromUser({ id: 12, subscriptionStatus: 'active', hasAccess: true })
    billingStatus.mockRejectedValueOnce(new Error('request rejected'))

    await expect(billing.fetchStatus(true)).resolves.toBe('inactive')

    expect(billing.status.value).toBe('inactive')
    expect(billing.hasAccess.value).toBe(false)
  })

  it('marks billing access as required immediately after a protected API 402', async () => {
    const module = await loadBillingStoreForUser({ id: 18 })
    const billing = module.useBillingStore()

    billing.seedFromUser({ id: 18, subscriptionStatus: 'active', hasAccess: true })

    expect(billing.markAccessRequired()).toBe('inactive')
    expect(billing.status.value).toBe('inactive')
    expect(billing.hasAccess.value).toBe(false)
  })
})
