import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  apiGet: vi.fn(),
  readStoredUser: vi.fn(),
}))

vi.mock('../src/services/api.js', () => ({
  default: {
    get: mocks.apiGet,
  },
}))

vi.mock('../src/utils/authStorage.js', () => ({
  readStoredUser: mocks.readStoredUser,
}))

describe('user-scoped runtime caches', () => {
  beforeEach(() => {
    vi.resetModules()
    mocks.apiGet.mockReset()
    mocks.readStoredUser.mockReset()
  })

  it('does not reuse inventory list cache across users', async () => {
    mocks.apiGet
      .mockResolvedValueOnce({ data: [{ id: 1, nomItem: 'Compte 1' }] })
      .mockResolvedValueOnce({ data: [{ id: 2, nomItem: 'Compte 2' }] })

    const { default: SnkVenteServices } = await import('../src/services/SnkVenteServices.js')

    mocks.readStoredUser.mockReturnValue({ id: 1 })
    await SnkVenteServices.getSnkVente()

    mocks.readStoredUser.mockReturnValue({ id: 2 })
    await SnkVenteServices.getSnkVente()

    expect(mocks.apiGet).toHaveBeenCalledTimes(2)
  })

  it('does not reuse stats cache across users', async () => {
    mocks.apiGet
      .mockResolvedValueOnce({ data: { ca: 100 } })
      .mockResolvedValueOnce({ data: { ca: 200 } })

    const { default: StatsServices } = await import('../src/services/StatsServices.js')

    mocks.readStoredUser.mockReturnValue({ id: 1 })
    await StatsServices.summary('2026-01-01', '2026-01-31')

    mocks.readStoredUser.mockReturnValue({ id: 2 })
    await StatsServices.summary('2026-01-01', '2026-01-31')

    expect(mocks.apiGet).toHaveBeenCalledTimes(2)
  })
})
