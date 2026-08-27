import { onBeforeUnmount, onMounted } from 'vue'

function toList(value) {
  if (Array.isArray(value)) return value
  return [value]
}

function resolveElement(entry) {
  if (!entry) return null
  if (typeof entry === 'function') return entry() ?? null
  return entry?.value ?? entry
}

export function useClickOutside(targets, handler, options = {}) {
  const { enabled = () => true, ignore = [] } = options

  const onPointerDown = (event) => {
    if (!enabled()) return

    const target = event.target
    const allTargets = toList(targets)
      .map(resolveElement)
      .filter(Boolean)

    if (!allTargets.length) return
    if (allTargets.some((element) => element.contains(target))) return

    const ignoredTargets = toList(ignore)
      .map(resolveElement)
      .filter(Boolean)

    if (ignoredTargets.some((element) => element.contains(target))) return

    handler(event)
  }

  onMounted(() => {
    document.addEventListener('pointerdown', onPointerDown, true)
  })

  onBeforeUnmount(() => {
    document.removeEventListener('pointerdown', onPointerDown, true)
  })
}
