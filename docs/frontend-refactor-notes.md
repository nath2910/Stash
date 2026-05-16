# Frontend refactor notes

## Probleme constates
- Plusieurs fichiers legacy etaient encore presents sans reference runtime.
- `CsvImportExportWidget.vue` melangeait UI, export CSV, mapping, validation et detection doublons.
- `StatsServices.js` contenait des appels billing dupliques avec `BillingService`.
- Les logs dev Axios/main ajoutaient du bruit sans valeur de maintenance.
- La documentation listait des fichiers stats/import historiques comme s'ils etaient encore actifs.

## Changements realises
- Extraction de la logique import/export stock dans `src/utils/stockImportExport.ts`.
- Ajout de tests unitaires `tests/stockImportExport.test.ts`.
- Simplification de `CsvImportExportWidget.vue`: UI + parsing fichier + appel API, avec validation deleguee au helper.
- Factorisation des filtres date/categories/types et de la granularite dans `StatsServices.js`.
- Suppression des helpers billing inutilises dans `StatsServices.js`; `BillingService.js` reste la source billing.
- `authCallbackPage.vue` utilise maintenant `AuthService.me()` au lieu d'appeler directement `api.get('/auth/me')`.
- Retrait des `console.log` de debug dans `main.js` et `services/api.js`.

## Fichiers supprimes
Suppressions faites apres verification par recherche de references dans `frontend/src`, `frontend/tests` et `docs`:
- `frontend/jsco`
- `src/components/DateRangeBar.vue`
- `src/components/StatBase.vue`
- `src/components/stats/canvas/CanvasDock.vue`
- `src/components/stats/palette/WidgetPaletteFilterChips.vue`
- `src/components/stats/templates/StatsTemplateLibrary.vue`
- `src/components/stats/templates/StatsTemplateLibrary.css`
- `src/components/stats/widgets/OpexWidget.vue`
- `src/components/stats/widgets/PlatformSplitWidget.vue`
- `src/components/stats/widgets/ReturnRateWidget.vue`
- `src/components/stats/widgets/TextSectionWidget.vue`

## Points volontairement non modifies
- Pas de refonte visuelle.
- Pas de changement de routes.
- Pas de changement des endpoints API.
- Pas de changement auth, Stripe, Flyway, PostgreSQL ou backend.
- Pas de decoupage lourd de `StatsCanvas.vue` ou `WidgetFrame.vue`: ces zones restent sensibles et demandent des tests e2e avant extraction plus profonde.
- Pas de suppression de CSS sans preuve runtime suffisante.

## Zones sensibles
- `StatsCanvas.vue`: drag/resize, Panzoom, persistence locale/backend, profils et templates.
- `WidgetFrame.vue`: contenteditable, fullscreen, pointer events et observers.
- `CsvImportExportWidget.vue`: compatibilite CSV/XLSX/JSON/TXT et mapping utilisateur.
- `StatsServices.js`: cache TTL et dedup de requetes en vol.

## Validations effectuees
- `npm test`
- `npm run test:unit`
- `npm run build`

## Tests manuels recommandes
- Connexion classique et callback OAuth.
- Navigation `/`, `/gestion`, `/stats`, `/compte`, `/abo`, `/mon-abonnement`.
- Import CSV, XLSX, JSON et TXT structure.
- Export CSV depuis la page gestion.
- Dashboard stats: ajout widget, deplacement, resize, zoom/pan, settings, suppression.
- Widgets ECharts principaux: CA, benefice net, top ventes, mix par type.
- Template annuel stats.
