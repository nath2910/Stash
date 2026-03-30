import { ref, type Ref } from 'vue'

type UseCanvasShortcutsArgs = {
  editMode: Ref<boolean>
  fullscreenActive: Ref<boolean>
  paletteOpen: Ref<boolean>
  settingsOpen: Ref<boolean>
  profileEditorOpen: Ref<boolean>
  dragArmedId: Ref<string | null>
  closeSettings: () => void
  toggleEditMode: () => void
  centerView: () => void
  resetZoom: () => void
  zoomOut: () => void
  zoomIn: () => void
  syncPanzoomExclude: (enabled: boolean) => void
  setCanvasPanEnabled: (enabled: boolean) => void
}

export function useCanvasShortcuts(args: UseCanvasShortcutsArgs) {
  const shortcutHelpOpen = ref(false)
  const spacePanActive = ref(false)

  function isTypingTarget(target: EventTarget | null) {
    const el = target instanceof HTMLElement ? target : null
    if (!el) return false
    if (el.isContentEditable) return true
    return !!el.closest('input, textarea, select, option, [contenteditable="true"]')
  }

  function shouldUsePanzoomExclude() {
    return args.editMode.value && !spacePanActive.value
  }

  function setSpacePanState(enabled: boolean) {
    const active =
      enabled &&
      args.editMode.value &&
      !args.fullscreenActive.value &&
      !args.paletteOpen.value &&
      !args.settingsOpen.value &&
      !args.profileEditorOpen.value &&
      !shortcutHelpOpen.value

    if (spacePanActive.value === active) return
    spacePanActive.value = active
    args.syncPanzoomExclude(shouldUsePanzoomExclude())
    if (active) {
      args.dragArmedId.value = null
      args.setCanvasPanEnabled(true)
    }
  }

  function onCanvasKeyDown(event: KeyboardEvent) {
    const key = event.key

    if (key === 'Escape') {
      let consumed = false
      if (shortcutHelpOpen.value) {
        shortcutHelpOpen.value = false
        consumed = true
      }
      if (args.profileEditorOpen.value) {
        args.profileEditorOpen.value = false
        consumed = true
      }
      if (args.settingsOpen.value) {
        args.closeSettings()
        consumed = true
      }
      if (args.paletteOpen.value) {
        args.paletteOpen.value = false
        consumed = true
      }
      if (spacePanActive.value) {
        setSpacePanState(false)
        consumed = true
      }
      if (consumed) {
        event.preventDefault()
        event.stopPropagation()
      }
      return
    }

    if (isTypingTarget(event.target)) return

    if (
      args.paletteOpen.value ||
      args.settingsOpen.value ||
      args.profileEditorOpen.value ||
      shortcutHelpOpen.value
    ) {
      return
    }

    if ((key === ' ' || event.code === 'Space') && !event.repeat) {
      event.preventDefault()
      setSpacePanState(true)
      return
    }

    if (key === '?' || (key === '/' && event.shiftKey)) {
      event.preventDefault()
      shortcutHelpOpen.value = true
      return
    }

    if (key === 'p' || key === 'P') {
      if (!args.editMode.value || args.fullscreenActive.value || args.paletteOpen.value) return
      event.preventDefault()
      args.paletteOpen.value = true
      return
    }

    if (key === 'e' || key === 'E') {
      if (args.fullscreenActive.value) return
      event.preventDefault()
      args.toggleEditMode()
      return
    }

    if (key === 'f' || key === 'F') {
      event.preventDefault()
      args.centerView()
      return
    }

    if (key === '0' || event.code === 'Numpad0') {
      event.preventDefault()
      args.resetZoom()
      return
    }

    if (key === '-' || key === '_' || event.code === 'NumpadSubtract') {
      event.preventDefault()
      args.zoomOut()
      return
    }

    if (key === '+' || key === '=' || event.code === 'NumpadAdd') {
      event.preventDefault()
      args.zoomIn()
    }
  }

  function onCanvasKeyUp(event: KeyboardEvent) {
    if (event.key === ' ' || event.code === 'Space') {
      setSpacePanState(false)
    }
  }

  function onWindowBlur() {
    setSpacePanState(false)
  }

  function onVisibilityChange() {
    if (document.hidden) setSpacePanState(false)
  }

  return {
    shortcutHelpOpen,
    spacePanActive,
    isTypingTarget,
    shouldUsePanzoomExclude,
    setSpacePanState,
    onCanvasKeyDown,
    onCanvasKeyUp,
    onWindowBlur,
    onVisibilityChange,
  }
}
