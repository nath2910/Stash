import { beforeEach, describe, expect, it, vi } from 'vitest'

const adminServiceMocks = vi.hoisted(() => ({
  default: {
    settings: vi.fn(),
    saveSettings: vi.fn(),
  },
}))

vi.mock('../src/services/AdminService.js', () => adminServiceMocks)

import {
  INVENTORY_ITEM_CATEGORIES_SETTINGS_KEY,
  resetInventoryPreferencesServiceCache,
  saveInventoryPreference,
  syncInventoryPreference,
} from '../src/services/inventoryPreferencesService'

describe('inventoryPreferencesService', () => {
  beforeEach(() => {
    resetInventoryPreferencesServiceCache()
    adminServiceMocks.default.settings.mockReset()
    adminServiceMocks.default.saveSettings.mockReset()
  })

  it('hydrates a remote inventory preference without mutating the payload', async () => {
    adminServiceMocks.default.settings.mockResolvedValue({
      [INVENTORY_ITEM_CATEGORIES_SETTINGS_KEY]: { SNEAKER: 'One Piece' },
    })

    const applied = vi.fn()
    const remote = await syncInventoryPreference(
      'u1',
      INVENTORY_ITEM_CATEGORIES_SETTINGS_KEY,
      { SNEAKER: 'Maison' },
      { applyRemote: applied },
    )

    expect(remote).toEqual({ SNEAKER: 'One Piece' })
    expect(applied).toHaveBeenCalledWith({ SNEAKER: 'One Piece' })
    expect(adminServiceMocks.default.settings).toHaveBeenCalledTimes(1)
    expect(adminServiceMocks.default.saveSettings).not.toHaveBeenCalled()
  })

  it('seeds the server when no remote preference exists yet', async () => {
    adminServiceMocks.default.settings.mockResolvedValue({})
    adminServiceMocks.default.saveSettings.mockImplementation(async (payload) => payload)

    const local = { SNEAKER: 'Maison', HOME: 'One Piece' }
    const saved = await syncInventoryPreference(
      'u1',
      INVENTORY_ITEM_CATEGORIES_SETTINGS_KEY,
      local,
      { shouldSeed: () => true },
    )

    expect(saved).toEqual(local)
    expect(adminServiceMocks.default.saveSettings).toHaveBeenCalledWith({
      [INVENTORY_ITEM_CATEGORIES_SETTINGS_KEY]: local,
    })
  })

  it('merges a saved inventory preference with existing server settings', async () => {
    adminServiceMocks.default.settings.mockResolvedValue({
      dashboardDensity: 'compact',
    })
    adminServiceMocks.default.saveSettings.mockImplementation(async (payload) => payload)

    const saved = await saveInventoryPreference(
      'u1',
      INVENTORY_ITEM_CATEGORIES_SETTINGS_KEY,
      { HOME: 'One Piece' },
    )

    expect(saved).toEqual({ HOME: 'One Piece' })
    expect(adminServiceMocks.default.saveSettings).toHaveBeenCalledWith({
      dashboardDensity: 'compact',
      [INVENTORY_ITEM_CATEGORIES_SETTINGS_KEY]: { HOME: 'One Piece' },
    })
  })
})
