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
  localStorage.setItem('snk_user', JSON.stringify(user))
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
})
