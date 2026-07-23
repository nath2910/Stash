import { beforeEach, describe, expect, it, vi } from 'vitest'

const { apiGet, apiPost } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  apiPost: vi.fn(),
}))

vi.mock('@/services/api', () => ({
  default: {
    get: apiGet,
    post: apiPost,
  },
}))

import BillingService from '@/services/BillingService'
import { describeBillingError } from '@/utils/billingErrors'

describe('BillingService', () => {
  beforeEach(() => {
    apiGet.mockReset()
    apiPost.mockReset()
  })

  it('uses a longer timeout for checkout', async () => {
    apiPost.mockResolvedValue({ data: { url: 'https://stripe.test/checkout' } })

    await BillingService.checkout(undefined, undefined)

    expect(apiPost).toHaveBeenCalledWith(
      '/billing/checkout',
      { promoCode: undefined, discord: undefined },
      { timeout: 45000 },
    )
  })

  it('uses a longer timeout for portal or forced status refreshes', async () => {
    apiGet.mockResolvedValue({ data: { status: 'active', portalUrl: '' } })

    await BillingService.status(true, false)
    await BillingService.status(false, true)

    expect(apiGet).toHaveBeenNthCalledWith(1, '/billing/status', {
      params: { includePortal: true, forceRefresh: false },
      timeout: 30000,
    })
    expect(apiGet).toHaveBeenNthCalledWith(2, '/billing/status', {
      params: { includePortal: false, forceRefresh: true },
      timeout: 30000,
    })
  })
})

describe('describeBillingError', () => {
  it('maps timeout errors to a user-facing payment message', () => {
    expect(
      describeBillingError(
        { code: 'ECONNABORTED', message: 'timeout of 15000ms exceeded' },
        'fallback',
      ),
    ).toBe('Le service de paiement met trop de temps a repondre. Reessaie dans quelques instants.')
  })

  it('prefers backend response messages when present', () => {
    expect(
      describeBillingError(
        { response: { data: { message: 'Stripe non configure' } } },
        'fallback',
      ),
    ).toBe('Stripe non configure')
  })
})
