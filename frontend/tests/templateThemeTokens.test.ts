import { describe, expect, it } from 'vitest'
import {
  mapTemplateThemeTokensToCssVars,
  resolveTemplateBoardStyle,
  resolveTemplateSceneBackground,
  resolveTemplateThemeTokens,
} from '../src/components/stats/templates/templateThemeTokens'

describe('templateThemeTokens', () => {
  it('resolves fallback theme tokens for unknown id', () => {
    const tokens = resolveTemplateThemeTokens('unknown_theme', 'dark')
    expect(tokens.id).toBe('executive_cobalt')
    expect(tokens.variant).toBe('dark')
    expect(tokens.palette.accent).toMatch(/^#/)
  })

  it('maps tokens to css vars', () => {
    const tokens = resolveTemplateThemeTokens('board_indigo', 'light')
    const vars = mapTemplateThemeTokensToCssVars(tokens)
    expect(vars).toHaveProperty('--template-accent')
    expect(vars).toHaveProperty('--template-title-font')
    expect(vars['--template-accent']).toBe(tokens.palette.accent)
  })

  it('prefers light scene background in light mode', () => {
    const background = resolveTemplateSceneBackground(
      {
        id: 'scene',
        boardBackground: 'dark-bg',
        boardBackgroundLight: 'light-bg',
      },
      'light',
    )
    expect(background).toBe('light-bg')
  })

  it('builds board style with overlay vars', () => {
    const tokens = resolveTemplateThemeTokens('executive_cobalt', 'dark')
    const style = resolveTemplateBoardStyle(
      {
        id: 'scene',
        boardBackground: 'board-bg',
        overlayPattern: 'pattern-bg',
      },
      tokens,
      'dark',
    )
    expect(style.background).toBe('board-bg')
    expect(style['--template-board-pattern']).toBe('pattern-bg')
  })
})

