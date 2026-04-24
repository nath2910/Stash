import { ref } from 'vue'
import { applyTemplateLayout } from '@/components/stats/templates/statsTemplateUtils'
import type { StatsTemplateDensity, StatsTemplateRecord } from '@/components/stats/templates/templateTypes'

type CanvasWidget = {
  id: string
  type: string
  title: string
  x: number
  y: number
  w: number
  h: number
  props?: Record<string, unknown>
  z?: number
}

export type TemplateApplyPhase = 'idle' | 'preparing' | 'rendering' | 'finalizing' | 'done'

export type TemplateApplyState = {
  active: boolean
  phase: TemplateApplyPhase
  templateId: string | null
  progress: number
  inserted: number
  total: number
  label: string
}

type PipelineScheduler = (delayMs?: number) => Promise<void>

export type ApplyTemplatePipelineArgs = {
  template: StatsTemplateRecord
  existingWidgets: CanvasWidget[]
  density?: StatsTemplateDensity
  createWidgetId: (type: string) => string
  confirmReplace?: (template: StatsTemplateRecord, existingCount: number) => boolean
  onPhase?: (state: TemplateApplyState) => void
  onPrepare: () => void
  onSetWidgets: (widgets: CanvasWidget[]) => void
  onBatchInserted?: (batch: CanvasWidget[]) => void
  onSceneAndRange: (template: StatsTemplateRecord) => void
  onFinalize: (widgets: CanvasWidget[]) => void
  batchSize?: number
  batchDelayMs?: number
  scheduler?: PipelineScheduler
}

function defaultScheduler(delayMs = 0) {
  return new Promise<void>((resolve) => {
    const run = () => {
      if (typeof window !== 'undefined' && typeof window.requestAnimationFrame === 'function') {
        window.requestAnimationFrame(() => resolve())
        return
      }
      setTimeout(resolve, 0)
    }
    if (delayMs > 0) {
      setTimeout(run, delayMs)
      return
    }
    run()
  })
}

function emitPhase(
  onPhase: ApplyTemplatePipelineArgs['onPhase'],
  partial: Partial<TemplateApplyState>,
  current: TemplateApplyState,
) {
  const next = { ...current, ...partial }
  onPhase?.(next)
  return next
}

export async function applyTemplatePipeline(args: ApplyTemplatePipelineArgs) {
  const scheduler = args.scheduler ?? defaultScheduler
  let state: TemplateApplyState = {
    active: true,
    phase: 'preparing',
    templateId: args.template.id,
    progress: 0,
    inserted: 0,
    total: 0,
    label: 'Preparation',
  }
  args.onPhase?.(state)

  const nextWidgets = applyTemplateLayout(args.template, args.createWidgetId, {
    density: args.density ?? args.template.density,
  }) as CanvasWidget[]
  state = emitPhase(args.onPhase, { total: nextWidgets.length }, state)
  if (!nextWidgets.length) {
    state = emitPhase(
      args.onPhase,
      {
        active: false,
        phase: 'done',
        progress: 1,
        label: 'Termine',
      },
      state,
    )
    return { applied: false, widgets: [] as CanvasWidget[] }
  }

  const existingCount = args.existingWidgets.length
  if (existingCount > 0) {
    const confirmReplace =
      args.confirmReplace?.(args.template, existingCount) ??
      (typeof window === 'undefined'
        ? true
        : window.confirm(
            `Le template "${args.template.name}" va remplacer les widgets du profil actif. Continuer ?`,
          ))
    if (!confirmReplace) {
      state = emitPhase(
        args.onPhase,
        {
          active: false,
          phase: 'idle',
          templateId: null,
          label: 'Annule',
        },
        state,
      )
      return { applied: false, widgets: [] as CanvasWidget[] }
    }
  }

  args.onPrepare()
  args.onSetWidgets([])
  state = emitPhase(args.onPhase, { phase: 'rendering', label: 'Application visuelle' }, state)
  args.onSceneAndRange(args.template)

  const inferredBatchSize =
    nextWidgets.length >= 40 ? 10 : nextWidgets.length >= 24 ? 8 : nextWidgets.length >= 12 ? 6 : 4
  const batchSize = Math.max(1, Number(args.batchSize ?? inferredBatchSize))
  const batchDelayMs = Math.max(
    0,
    Number(args.batchDelayMs ?? (nextWidgets.length >= 24 ? 8 : 12)),
  )
  const mounted: CanvasWidget[] = []
  for (let cursor = 0; cursor < nextWidgets.length; cursor += batchSize) {
    const batch = nextWidgets.slice(cursor, cursor + batchSize)
    mounted.push(...batch)
    args.onSetWidgets([...mounted])
    args.onBatchInserted?.(batch)
    state = emitPhase(
      args.onPhase,
      {
        inserted: mounted.length,
        progress: mounted.length / nextWidgets.length,
      },
      state,
    )
    await scheduler(batchDelayMs)
  }

  state = emitPhase(
    args.onPhase,
    {
      phase: 'finalizing',
      label: 'Finalisation',
    },
    state,
  )
  args.onFinalize(nextWidgets)
  await scheduler()

  state = emitPhase(
    args.onPhase,
    {
      active: false,
      phase: 'done',
      progress: 1,
      inserted: nextWidgets.length,
      label: 'Termine',
    },
    state,
  )

  return { applied: true, widgets: nextWidgets }
}

export function useTemplateEngine() {
  const applyState = ref<TemplateApplyState>({
    active: false,
    phase: 'idle',
    templateId: null,
    progress: 0,
    inserted: 0,
    total: 0,
    label: '',
  })

  function updateApplyState(next: TemplateApplyState) {
    applyState.value = next
  }

  async function runPipeline(args: Omit<ApplyTemplatePipelineArgs, 'onPhase'>) {
    return applyTemplatePipeline({
      ...args,
      onPhase: updateApplyState,
    })
  }

  return {
    applyState,
    runPipeline,
  }
}
