# Dossier frontend/

## 1) Mission du frontend
Application Vue 3 SPA qui gere:
1. auth utilisateur,
2. pages metier (home, gestion, stats, account, abonnement),
3. canvas stats avancé,
4. interactions riches (drag/resize/zoom/import/export).

## 2) Sous-ensembles

### Config/build
- `package.json`, Vite, ESLint, Tailwind, TS configs.

### Runtime app
- `src/main.js`, `src/App.vue`, `src/router/index.js`, `src/layout/layoutPages.vue`.

### Metier
- `src/pages/*`.
- `src/components/gestion/*`.
- `src/components/stats/*`.

### Donnees
- `src/services/*`.
- `src/store/*`.
- `src/composables/*`.

### Utilitaires
- `src/utils/*`, `src/constants/*`, `src/lib/*`.

### Tests
- `tests/*`.

## 3) Fichiers de configuration importants

### `package.json`
- Scripts: `dev`, `build`, `lint`, `test`, `test:unit`.
- Dependances chart, panzoom, parsing CSV/XLSX.

### `.env*`
- URL API selon environnement.
- Variables OAuth frontend.

### `vite.config.ts`
- Alias `@ -> src`.
- Chunk splitting manuel (`vue`, `charts`).

### `eslint.config.ts`
- ESLint flat config avec plugin Vue + TS.

## 4) Zones les plus complexes
1. `src/components/stats/StatsCanvas.vue`.
2. `src/components/stats/canvas/WidgetFrame.vue`.
3. `src/components/stats/WidgetPalette.vue`.
4. `src/components/stats/WidgetSettingsModal.vue`.
5. `src/components/gestion/CsvImportExportWidget.vue`.

## 5) Conseils reprise frontend
1. Lire d'abord routing + stores + api.
2. Lire ensuite pages auth/account/abo.
3. Lire module gestion.
4. Garder module stats pour une lecture dediee (beaucoup de logique UI bas niveau).
