import {
  TEMPLATE_DEFINITIONS,
  sanitizeTemplateId,
  type TemplateId,
} from './registry'

export type TemplateRuntimeState = Record<string, unknown>
export type TemplateStateMap = Partial<Record<TemplateId, TemplateRuntimeState>>

export type ProfileTemplateState = {
  active?: boolean
  templateId?: TemplateId
  state?: TemplateRuntimeState
  states?: TemplateStateMap
}

export function sanitizeTemplateRuntimeState(raw: unknown): TemplateRuntimeState {
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) return {}
  return { ...(raw as TemplateRuntimeState) }
}

export function sanitizeTemplateStateMap(raw: unknown): TemplateStateMap {
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) return {}
  const source = raw as Record<string, unknown>
  const map: TemplateStateMap = {}
  for (const template of TEMPLATE_DEFINITIONS) {
    if (!(template.id in source)) continue
    map[template.id] = sanitizeTemplateRuntimeState(source[template.id])
  }
  return map
}

export function setTemplateState(
  source: unknown,
  templateId: TemplateId,
  state: unknown,
): TemplateStateMap {
  return {
    ...sanitizeTemplateStateMap(source),
    [templateId]: sanitizeTemplateRuntimeState(state),
  }
}

export function normalizeProfileTemplateState(raw: unknown): ProfileTemplateState | null {
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) return null
  const source = raw as Record<string, unknown>
  const templateId = sanitizeTemplateId(source.templateId)
  if (source.active !== true || !templateId) return null

  const states = sanitizeTemplateStateMap(source.states)
  const activeState =
    'state' in source
      ? sanitizeTemplateRuntimeState(source.state)
      : (states[templateId] ?? {})
  const normalizedStates = setTemplateState(states, templateId, activeState)

  return {
    active: true,
    templateId,
    state: normalizedStates[templateId] ?? {},
    states: normalizedStates,
  }
}
