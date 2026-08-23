export const INVENTORY_CHANGED_EVENT = 'snk:inventory-changed'

export function notifyInventoryChanged(detail = {}) {
  if (typeof window === 'undefined') return
  window.dispatchEvent(
    new CustomEvent(INVENTORY_CHANGED_EVENT, {
      detail: {
        at: Date.now(),
        ...detail,
      },
    }),
  )
}
