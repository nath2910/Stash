import type {
  StatsTemplateRecord,
  StatsTemplateScene,
  TemplateThemeTokens,
  TemplateThemeVariant,
} from './templateTypes'

const DEFAULT_DARK_BOARD =
  'linear-gradient(180deg, rgba(6, 10, 20, 0.98) 0%, rgba(4, 8, 16, 0.99) 100%), radial-gradient(circle at 12% -10%, rgba(56, 189, 248, 0.14), transparent 36%), radial-gradient(circle at 88% -12%, rgba(59, 130, 246, 0.12), transparent 36%), linear-gradient(rgba(148, 163, 184, 0.05) 1px, transparent 1px) 0 0 / 34px 34px, linear-gradient(90deg, rgba(148, 163, 184, 0.05) 1px, transparent 1px) 0 0 / 34px 34px'

const DEFAULT_LIGHT_BOARD =
  'linear-gradient(180deg, rgba(243, 248, 255, 0.98) 0%, rgba(232, 240, 251, 0.98) 100%), radial-gradient(circle at 10% -10%, rgba(37, 99, 235, 0.14), transparent 38%), radial-gradient(circle at 92% -18%, rgba(14, 165, 233, 0.1), transparent 34%), linear-gradient(rgba(100, 116, 139, 0.08) 1px, transparent 1px) 0 0 / 28px 28px, linear-gradient(90deg, rgba(100, 116, 139, 0.08) 1px, transparent 1px) 0 0 / 28px 28px'

const THEME_TOKENS: Record<string, { dark: TemplateThemeTokens; light: TemplateThemeTokens }> = {
  executive_cobalt: {
    dark: {
      id: 'executive_cobalt',
      label: 'Executive Cobalt',
      density: 'balanced',
      variant: 'dark',
      palette: {
        accent: '#38bdf8',
        accentSoft: 'rgba(56, 189, 248, 0.2)',
        accentStrong: '#0ea5e9',
        text: '#f8fafc',
        textMuted: '#bfdbfe',
        success: '#22c55e',
        warning: '#f59e0b',
        danger: '#f43f5e',
      },
      surfaces: {
        board: 'rgba(4, 10, 24, 0.92)',
        card: 'linear-gradient(180deg, rgba(7, 18, 38, 0.92), rgba(6, 13, 27, 0.96))',
        cardAlt: 'linear-gradient(180deg, rgba(10, 24, 48, 0.86), rgba(8, 18, 34, 0.9))',
        glass: 'rgba(8, 18, 36, 0.64)',
        line: 'rgba(125, 211, 252, 0.22)',
      },
      typography: {
        titleFont: '"Sora", "Inter", sans-serif',
        bodyFont: '"Inter", "Open Sans", sans-serif',
        monoFont: '"IBM Plex Mono", monospace',
      },
      shadows: {
        board: '0 34px 68px rgba(2, 6, 23, 0.42)',
        card: '0 12px 28px rgba(2, 6, 23, 0.28)',
        cardInset: 'inset 0 1px 0 rgba(224, 242, 254, 0.1)',
      },
      radius: '18px',
      boardBackground: DEFAULT_DARK_BOARD,
      boardPattern:
        'radial-gradient(circle at 0 0, rgba(56, 189, 248, 0.22), transparent 44%), radial-gradient(circle at 100% 0, rgba(14, 165, 233, 0.18), transparent 44%)',
    },
    light: {
      id: 'executive_cobalt',
      label: 'Executive Cobalt',
      density: 'balanced',
      variant: 'light',
      palette: {
        accent: '#0284c7',
        accentSoft: 'rgba(2, 132, 199, 0.14)',
        accentStrong: '#0369a1',
        text: '#0f172a',
        textMuted: '#475569',
        success: '#16a34a',
        warning: '#d97706',
        danger: '#e11d48',
      },
      surfaces: {
        board: 'rgba(243, 248, 255, 0.94)',
        card: 'linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(241, 245, 249, 0.96))',
        cardAlt: 'linear-gradient(180deg, rgba(240, 249, 255, 0.9), rgba(224, 242, 254, 0.9))',
        glass: 'rgba(255, 255, 255, 0.66)',
        line: 'rgba(14, 116, 144, 0.2)',
      },
      typography: {
        titleFont: '"Sora", "Inter", sans-serif',
        bodyFont: '"Inter", "Open Sans", sans-serif',
        monoFont: '"IBM Plex Mono", monospace',
      },
      shadows: {
        board: '0 20px 44px rgba(15, 23, 42, 0.16)',
        card: '0 10px 22px rgba(15, 23, 42, 0.12)',
        cardInset: 'inset 0 1px 0 rgba(255, 255, 255, 0.8)',
      },
      radius: '18px',
      boardBackground: DEFAULT_LIGHT_BOARD,
      boardPattern:
        'radial-gradient(circle at 0 0, rgba(2, 132, 199, 0.15), transparent 44%), radial-gradient(circle at 100% 0, rgba(14, 165, 233, 0.12), transparent 44%)',
    },
  },
  board_indigo: {
    dark: {
      id: 'board_indigo',
      label: 'Board Indigo',
      density: 'balanced',
      variant: 'dark',
      palette: {
        accent: '#818cf8',
        accentSoft: 'rgba(129, 140, 248, 0.24)',
        accentStrong: '#6366f1',
        text: '#eef2ff',
        textMuted: '#c7d2fe',
        success: '#34d399',
        warning: '#fbbf24',
        danger: '#fb7185',
      },
      surfaces: {
        board: 'rgba(9, 10, 30, 0.95)',
        card: 'linear-gradient(180deg, rgba(17, 19, 45, 0.92), rgba(11, 13, 32, 0.97))',
        cardAlt: 'linear-gradient(180deg, rgba(25, 23, 58, 0.84), rgba(14, 18, 40, 0.88))',
        glass: 'rgba(20, 23, 44, 0.62)',
        line: 'rgba(165, 180, 252, 0.22)',
      },
      typography: {
        titleFont: '"Outfit", "Inter", sans-serif',
        bodyFont: '"Inter", "Open Sans", sans-serif',
        monoFont: '"IBM Plex Mono", monospace',
      },
      shadows: {
        board: '0 34px 64px rgba(2, 6, 23, 0.5)',
        card: '0 16px 30px rgba(6, 8, 24, 0.36)',
        cardInset: 'inset 0 1px 0 rgba(224, 231, 255, 0.08)',
      },
      radius: '20px',
      boardBackground:
        'linear-gradient(180deg, rgba(3, 6, 23, 0.99) 0%, rgba(9, 10, 33, 0.99) 100%), radial-gradient(circle at 18% -14%, rgba(99, 102, 241, 0.2), transparent 38%), radial-gradient(circle at 88% -10%, rgba(129, 140, 248, 0.18), transparent 35%), linear-gradient(rgba(129, 140, 248, 0.05) 1px, transparent 1px) 0 0 / 36px 36px, linear-gradient(90deg, rgba(129, 140, 248, 0.05) 1px, transparent 1px) 0 0 / 36px 36px',
      boardPattern:
        'radial-gradient(circle at 10% 0, rgba(99, 102, 241, 0.24), transparent 46%), radial-gradient(circle at 90% 0, rgba(167, 139, 250, 0.18), transparent 44%)',
    },
    light: {
      id: 'board_indigo',
      label: 'Board Indigo',
      density: 'balanced',
      variant: 'light',
      palette: {
        accent: '#4f46e5',
        accentSoft: 'rgba(79, 70, 229, 0.14)',
        accentStrong: '#4338ca',
        text: '#1f2a44',
        textMuted: '#475569',
        success: '#16a34a',
        warning: '#d97706',
        danger: '#e11d48',
      },
      surfaces: {
        board: 'rgba(241, 245, 255, 0.96)',
        card: 'linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(245, 244, 255, 0.96))',
        cardAlt: 'linear-gradient(180deg, rgba(238, 242, 255, 0.92), rgba(224, 231, 255, 0.9))',
        glass: 'rgba(255, 255, 255, 0.68)',
        line: 'rgba(99, 102, 241, 0.2)',
      },
      typography: {
        titleFont: '"Outfit", "Inter", sans-serif',
        bodyFont: '"Inter", "Open Sans", sans-serif',
        monoFont: '"IBM Plex Mono", monospace',
      },
      shadows: {
        board: '0 20px 40px rgba(30, 41, 59, 0.14)',
        card: '0 10px 24px rgba(15, 23, 42, 0.12)',
        cardInset: 'inset 0 1px 0 rgba(255, 255, 255, 0.86)',
      },
      radius: '20px',
      boardBackground:
        'linear-gradient(180deg, rgba(246, 248, 255, 0.98) 0%, rgba(236, 241, 255, 0.98) 100%), radial-gradient(circle at 14% -14%, rgba(99, 102, 241, 0.16), transparent 40%), radial-gradient(circle at 90% -12%, rgba(129, 140, 248, 0.12), transparent 36%), linear-gradient(rgba(129, 140, 248, 0.08) 1px, transparent 1px) 0 0 / 30px 30px, linear-gradient(90deg, rgba(129, 140, 248, 0.08) 1px, transparent 1px) 0 0 / 30px 30px',
      boardPattern:
        'radial-gradient(circle at 0 0, rgba(99, 102, 241, 0.16), transparent 44%), radial-gradient(circle at 100% 0, rgba(167, 139, 250, 0.12), transparent 42%)',
    },
  },
  profit_emerald: {
    dark: {
      id: 'profit_emerald',
      label: 'Profit Emerald',
      density: 'dense',
      variant: 'dark',
      palette: {
        accent: '#34d399',
        accentSoft: 'rgba(52, 211, 153, 0.22)',
        accentStrong: '#10b981',
        text: '#ecfdf5',
        textMuted: '#a7f3d0',
        success: '#4ade80',
        warning: '#fbbf24',
        danger: '#fb7185',
      },
      surfaces: {
        board: 'rgba(6, 20, 18, 0.95)',
        card: 'linear-gradient(180deg, rgba(9, 28, 24, 0.93), rgba(7, 22, 19, 0.96))',
        cardAlt: 'linear-gradient(180deg, rgba(15, 42, 35, 0.82), rgba(9, 28, 24, 0.86))',
        glass: 'rgba(8, 26, 22, 0.62)',
        line: 'rgba(110, 231, 183, 0.24)',
      },
      typography: {
        titleFont: '"Manrope", "Inter", sans-serif',
        bodyFont: '"Inter", "Open Sans", sans-serif',
        monoFont: '"IBM Plex Mono", monospace',
      },
      shadows: {
        board: '0 32px 62px rgba(2, 6, 23, 0.48)',
        card: '0 14px 30px rgba(3, 12, 10, 0.34)',
        cardInset: 'inset 0 1px 0 rgba(209, 250, 229, 0.1)',
      },
      radius: '16px',
      boardBackground:
        'linear-gradient(180deg, rgba(2, 12, 12, 0.99) 0%, rgba(4, 26, 21, 0.99) 100%), radial-gradient(circle at 16% -10%, rgba(16, 185, 129, 0.2), transparent 36%), radial-gradient(circle at 86% -14%, rgba(52, 211, 153, 0.18), transparent 36%), linear-gradient(rgba(110, 231, 183, 0.045) 1px, transparent 1px) 0 0 / 32px 32px, linear-gradient(90deg, rgba(110, 231, 183, 0.045) 1px, transparent 1px) 0 0 / 32px 32px',
      boardPattern:
        'radial-gradient(circle at 0 0, rgba(34, 197, 94, 0.2), transparent 44%), radial-gradient(circle at 100% 0, rgba(16, 185, 129, 0.15), transparent 44%)',
    },
    light: {
      id: 'profit_emerald',
      label: 'Profit Emerald',
      density: 'dense',
      variant: 'light',
      palette: {
        accent: '#059669',
        accentSoft: 'rgba(5, 150, 105, 0.14)',
        accentStrong: '#047857',
        text: '#052e2a',
        textMuted: '#3f5f56',
        success: '#16a34a',
        warning: '#d97706',
        danger: '#e11d48',
      },
      surfaces: {
        board: 'rgba(238, 252, 247, 0.96)',
        card: 'linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(236, 253, 245, 0.95))',
        cardAlt: 'linear-gradient(180deg, rgba(220, 252, 231, 0.88), rgba(209, 250, 229, 0.9))',
        glass: 'rgba(255, 255, 255, 0.68)',
        line: 'rgba(5, 150, 105, 0.2)',
      },
      typography: {
        titleFont: '"Manrope", "Inter", sans-serif',
        bodyFont: '"Inter", "Open Sans", sans-serif',
        monoFont: '"IBM Plex Mono", monospace',
      },
      shadows: {
        board: '0 18px 40px rgba(15, 23, 42, 0.14)',
        card: '0 10px 22px rgba(15, 23, 42, 0.1)',
        cardInset: 'inset 0 1px 0 rgba(255, 255, 255, 0.9)',
      },
      radius: '16px',
      boardBackground:
        'linear-gradient(180deg, rgba(239, 253, 248, 0.98) 0%, rgba(224, 247, 239, 0.98) 100%), radial-gradient(circle at 12% -10%, rgba(5, 150, 105, 0.16), transparent 38%), radial-gradient(circle at 88% -12%, rgba(16, 185, 129, 0.14), transparent 36%), linear-gradient(rgba(5, 150, 105, 0.08) 1px, transparent 1px) 0 0 / 30px 30px, linear-gradient(90deg, rgba(5, 150, 105, 0.08) 1px, transparent 1px) 0 0 / 30px 30px',
      boardPattern:
        'radial-gradient(circle at 0 0, rgba(5, 150, 105, 0.14), transparent 44%), radial-gradient(circle at 100% 0, rgba(16, 185, 129, 0.1), transparent 42%)',
    },
  },
  velocity_amber: {
    dark: {
      id: 'velocity_amber',
      label: 'Velocity Amber',
      density: 'dense',
      variant: 'dark',
      palette: {
        accent: '#fb923c',
        accentSoft: 'rgba(251, 146, 60, 0.22)',
        accentStrong: '#f97316',
        text: '#ffedd5',
        textMuted: '#fdba74',
        success: '#22c55e',
        warning: '#fbbf24',
        danger: '#fb7185',
      },
      surfaces: {
        board: 'rgba(23, 12, 7, 0.95)',
        card: 'linear-gradient(180deg, rgba(39, 18, 9, 0.93), rgba(30, 14, 8, 0.97))',
        cardAlt: 'linear-gradient(180deg, rgba(56, 24, 12, 0.82), rgba(38, 18, 10, 0.86))',
        glass: 'rgba(35, 18, 10, 0.62)',
        line: 'rgba(251, 146, 60, 0.24)',
      },
      typography: {
        titleFont: '"Space Grotesk", "Inter", sans-serif',
        bodyFont: '"Inter", "Open Sans", sans-serif',
        monoFont: '"IBM Plex Mono", monospace',
      },
      shadows: {
        board: '0 32px 68px rgba(2, 6, 23, 0.5)',
        card: '0 14px 30px rgba(20, 10, 6, 0.4)',
        cardInset: 'inset 0 1px 0 rgba(255, 237, 213, 0.08)',
      },
      radius: '16px',
      boardBackground:
        'linear-gradient(180deg, rgba(12, 8, 8, 0.99) 0%, rgba(30, 12, 6, 0.99) 100%), radial-gradient(circle at 16% -10%, rgba(249, 115, 22, 0.22), transparent 36%), radial-gradient(circle at 88% -14%, rgba(251, 146, 60, 0.18), transparent 34%), linear-gradient(rgba(251, 146, 60, 0.05) 1px, transparent 1px) 0 0 / 30px 30px, linear-gradient(90deg, rgba(251, 146, 60, 0.05) 1px, transparent 1px) 0 0 / 30px 30px',
      boardPattern:
        'radial-gradient(circle at 0 0, rgba(249, 115, 22, 0.2), transparent 44%), radial-gradient(circle at 100% 0, rgba(251, 146, 60, 0.14), transparent 44%)',
    },
    light: {
      id: 'velocity_amber',
      label: 'Velocity Amber',
      density: 'dense',
      variant: 'light',
      palette: {
        accent: '#ea580c',
        accentSoft: 'rgba(234, 88, 12, 0.16)',
        accentStrong: '#c2410c',
        text: '#431407',
        textMuted: '#7c2d12',
        success: '#16a34a',
        warning: '#d97706',
        danger: '#dc2626',
      },
      surfaces: {
        board: 'rgba(255, 247, 237, 0.96)',
        card: 'linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(255, 237, 213, 0.94))',
        cardAlt: 'linear-gradient(180deg, rgba(255, 237, 213, 0.9), rgba(254, 215, 170, 0.88))',
        glass: 'rgba(255, 255, 255, 0.68)',
        line: 'rgba(194, 65, 12, 0.2)',
      },
      typography: {
        titleFont: '"Space Grotesk", "Inter", sans-serif',
        bodyFont: '"Inter", "Open Sans", sans-serif',
        monoFont: '"IBM Plex Mono", monospace',
      },
      shadows: {
        board: '0 18px 40px rgba(15, 23, 42, 0.14)',
        card: '0 10px 22px rgba(15, 23, 42, 0.1)',
        cardInset: 'inset 0 1px 0 rgba(255, 255, 255, 0.88)',
      },
      radius: '16px',
      boardBackground:
        'linear-gradient(180deg, rgba(255, 247, 237, 0.98) 0%, rgba(255, 237, 213, 0.98) 100%), radial-gradient(circle at 14% -10%, rgba(251, 146, 60, 0.16), transparent 38%), radial-gradient(circle at 90% -14%, rgba(249, 115, 22, 0.14), transparent 34%), linear-gradient(rgba(194, 65, 12, 0.08) 1px, transparent 1px) 0 0 / 28px 28px, linear-gradient(90deg, rgba(194, 65, 12, 0.08) 1px, transparent 1px) 0 0 / 28px 28px',
      boardPattern:
        'radial-gradient(circle at 0 0, rgba(251, 146, 60, 0.16), transparent 44%), radial-gradient(circle at 100% 0, rgba(249, 115, 22, 0.12), transparent 42%)',
    },
  },
  growth_teal: {
    dark: {
      id: 'growth_teal',
      label: 'Growth Teal',
      density: 'balanced',
      variant: 'dark',
      palette: {
        accent: '#2dd4bf',
        accentSoft: 'rgba(45, 212, 191, 0.24)',
        accentStrong: '#14b8a6',
        text: '#ecfeff',
        textMuted: '#99f6e4',
        success: '#22c55e',
        warning: '#fbbf24',
        danger: '#fb7185',
      },
      surfaces: {
        board: 'rgba(4, 20, 28, 0.95)',
        card: 'linear-gradient(180deg, rgba(6, 30, 36, 0.93), rgba(5, 22, 29, 0.96))',
        cardAlt: 'linear-gradient(180deg, rgba(10, 44, 51, 0.82), rgba(7, 28, 35, 0.86))',
        glass: 'rgba(7, 30, 34, 0.62)',
        line: 'rgba(94, 234, 212, 0.22)',
      },
      typography: {
        titleFont: '"Sora", "Inter", sans-serif',
        bodyFont: '"Inter", "Open Sans", sans-serif',
        monoFont: '"IBM Plex Mono", monospace',
      },
      shadows: {
        board: '0 32px 66px rgba(2, 6, 23, 0.48)',
        card: '0 14px 28px rgba(4, 16, 20, 0.34)',
        cardInset: 'inset 0 1px 0 rgba(204, 251, 241, 0.08)',
      },
      radius: '18px',
      boardBackground:
        'linear-gradient(180deg, rgba(3, 10, 21, 0.99) 0%, rgba(3, 25, 31, 0.99) 100%), radial-gradient(circle at 16% -12%, rgba(20, 184, 166, 0.22), transparent 36%), radial-gradient(circle at 90% -12%, rgba(45, 212, 191, 0.16), transparent 32%), linear-gradient(rgba(94, 234, 212, 0.044) 1px, transparent 1px) 0 0 / 34px 34px, linear-gradient(90deg, rgba(94, 234, 212, 0.044) 1px, transparent 1px) 0 0 / 34px 34px',
      boardPattern:
        'radial-gradient(circle at 0 0, rgba(20, 184, 166, 0.22), transparent 44%), radial-gradient(circle at 100% 0, rgba(45, 212, 191, 0.14), transparent 42%)',
    },
    light: {
      id: 'growth_teal',
      label: 'Growth Teal',
      density: 'balanced',
      variant: 'light',
      palette: {
        accent: '#0f766e',
        accentSoft: 'rgba(15, 118, 110, 0.16)',
        accentStrong: '#115e59',
        text: '#042f2e',
        textMuted: '#155e75',
        success: '#16a34a',
        warning: '#d97706',
        danger: '#dc2626',
      },
      surfaces: {
        board: 'rgba(236, 253, 250, 0.96)',
        card: 'linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(236, 253, 250, 0.94))',
        cardAlt: 'linear-gradient(180deg, rgba(204, 251, 241, 0.88), rgba(153, 246, 228, 0.86))',
        glass: 'rgba(255, 255, 255, 0.68)',
        line: 'rgba(15, 118, 110, 0.2)',
      },
      typography: {
        titleFont: '"Sora", "Inter", sans-serif',
        bodyFont: '"Inter", "Open Sans", sans-serif',
        monoFont: '"IBM Plex Mono", monospace',
      },
      shadows: {
        board: '0 18px 40px rgba(15, 23, 42, 0.14)',
        card: '0 10px 22px rgba(15, 23, 42, 0.1)',
        cardInset: 'inset 0 1px 0 rgba(255, 255, 255, 0.9)',
      },
      radius: '18px',
      boardBackground:
        'linear-gradient(180deg, rgba(240, 253, 250, 0.98) 0%, rgba(222, 247, 240, 0.98) 100%), radial-gradient(circle at 16% -12%, rgba(20, 184, 166, 0.16), transparent 38%), radial-gradient(circle at 90% -12%, rgba(13, 148, 136, 0.14), transparent 34%), linear-gradient(rgba(15, 118, 110, 0.08) 1px, transparent 1px) 0 0 / 30px 30px, linear-gradient(90deg, rgba(15, 118, 110, 0.08) 1px, transparent 1px) 0 0 / 30px 30px',
      boardPattern:
        'radial-gradient(circle at 0 0, rgba(20, 184, 166, 0.14), transparent 44%), radial-gradient(circle at 100% 0, rgba(13, 148, 136, 0.1), transparent 42%)',
    },
  },
}

const DEFAULT_THEME_KEY = 'executive_cobalt'

export function resolveTemplateSceneBackground(
  scene: StatsTemplateScene | null | undefined,
  variant: TemplateThemeVariant,
) {
  if (!scene) return variant === 'light' ? DEFAULT_LIGHT_BOARD : DEFAULT_DARK_BOARD
  if (variant === 'light') {
    return scene.boardBackgroundLight || scene.boardBackground || DEFAULT_LIGHT_BOARD
  }
  return scene.boardBackgroundDark || scene.boardBackground || DEFAULT_DARK_BOARD
}

export function resolveTemplateThemeTokens(
  templateOrThemeId: StatsTemplateRecord | string | null | undefined,
  variant: TemplateThemeVariant,
): TemplateThemeTokens {
  const themeId =
    typeof templateOrThemeId === 'string'
      ? templateOrThemeId
      : templateOrThemeId?.themeTokenId ||
        templateOrThemeId?.scene?.themeTokenId ||
        DEFAULT_THEME_KEY
  const entry = THEME_TOKENS[themeId] || THEME_TOKENS[DEFAULT_THEME_KEY]
  return variant === 'light' ? entry.light : entry.dark
}

export function mapTemplateThemeTokensToCssVars(tokens: TemplateThemeTokens) {
  return {
    '--template-accent': tokens.palette.accent,
    '--template-accent-soft': tokens.palette.accentSoft,
    '--template-accent-strong': tokens.palette.accentStrong,
    '--template-text': tokens.palette.text,
    '--template-text-muted': tokens.palette.textMuted,
    '--template-success': tokens.palette.success,
    '--template-warning': tokens.palette.warning,
    '--template-danger': tokens.palette.danger,
    '--template-surface-board': tokens.surfaces.board,
    '--template-surface-card': tokens.surfaces.card,
    '--template-surface-card-alt': tokens.surfaces.cardAlt,
    '--template-surface-glass': tokens.surfaces.glass,
    '--template-surface-line': tokens.surfaces.line,
    '--template-radius': tokens.radius,
    '--template-title-font': tokens.typography.titleFont,
    '--template-body-font': tokens.typography.bodyFont,
    '--template-mono-font': tokens.typography.monoFont,
    '--template-shadow-board': tokens.shadows.board,
    '--template-shadow-card': tokens.shadows.card,
    '--template-shadow-card-inset': tokens.shadows.cardInset,
    '--template-board-pattern': tokens.boardPattern,
  } satisfies Record<string, string>
}

export function resolveTemplateBoardStyle(
  scene: StatsTemplateScene | null | undefined,
  tokens: TemplateThemeTokens,
  variant: TemplateThemeVariant,
) {
  return {
    background: resolveTemplateSceneBackground(scene, variant),
    '--template-board-pattern': scene?.overlayPattern || tokens.boardPattern,
    '--template-board-overlay': scene?.overlayGradient || 'transparent',
  } as Record<string, string>
}

