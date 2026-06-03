# Frontend architecture

## Vue d'ensemble
Le frontend est une SPA Vue 3 situee dans `frontend/`. Elle utilise Vue Router pour la navigation, Axios pour les appels API, ECharts/vue-echarts pour les graphiques, Panzoom pour le canvas stats, PapaParse et XLSX pour l'import/export.

Le backend, l'authentification, Stripe, Flyway et les contrats API ne doivent pas etre modifies depuis cette couche. Les services frontend conservent les endpoints existants et adaptent seulement les payloads pour l'UI.

## Structure principale
- `src/main.js`: bootstrap Vue, router, MotionPlugin et chargement asynchrone global de `VChart`.
- `src/App.vue`: shell applicatif, layout global et `router-view`.
- `src/router/index.js`: routes publiques/protegees, garde JWT, controle abonnement via `billingStore`.
- `src/pages/`: pages de haut niveau (`home`, `gestion`, `stats`, auth, compte, abonnement).
- `src/layout/`: layout principal de navigation.
- `src/components/`: composants partages et composants metier.
- `src/components/gestion/`: stock, modales CRUD, import/export.
- `src/components/stats/`: dashboard stats, canvas, widgets, palette, settings, templates.
- `src/constants/`: constantes partagees pour statuts, profils admin et onglets dynamiques.
- `src/services/`: clients API Axios par domaine.
- `src/store/`: stores globaux simples bases sur `ref`.
- `src/composables/`: logique Vue reutilisable (`useTheme`, `useStatsRange`).
- `src/utils/`: helpers purs (`formatters`, `snkVente`, `stockImportExport`).
- `src/assets/`: styles globaux.

## Routing
Les pages sont chargees en lazy import dans `src/router/index.js`. Les routes publiques sont limitees a l'auth, la decouverte, le reset/verif email et le callback OAuth. Les autres routes demandent un JWT valide et, sauf exception `allowInactive`, un abonnement actif.

Le guard ne doit pas modifier les contrats auth. Il appelle `AuthService.me()` pour restaurer l'utilisateur et `billingStore.fetchStatus()` pour l'etat abonnement.

## Services API
- `services/api.js`: instance Axios centrale, base URL issue de `VITE_API_URL`/`VITE_API_BASE_URL`, token `Bearer`, redirection login sur 401/403 hors auth.
- `AuthService.js`: register/login/me/password/email/delete.
- `BillingService.js`: status et checkout Stripe.
- `SnkVenteServices.js`: stock, import bulk, pieces jointes.
- `StatsServices.js`: endpoints stats, layout dashboard, cache TTL court et deduplication des requetes en vol.
- `NotificationService.js`: notifications.
- `statsAdapters.js`: normalisation defensive des reponses stats.

## Dashboard stats et canvas
`components/stats/StatsCanvas.vue` reste l'orchestrateur principal. Il gere profils, plage de dates, mode template, selection, drag/resize, zoom, persistence locale/backend et chargement categories/date bounds.

Les modules associes sont:
- `canvas/WidgetFrame.vue`: coque interactive des widgets, edition texte, fullscreen, handles.
- `canvas/useCanvaCamera.ts`: camera, zoom, pan et projection.
- `canvas/useCanvasShortcuts.ts`: raccourcis clavier.
- `WidgetPalette.vue`: ajout de widgets, recherche et choix de variante.
- `WidgetSettingsModal.vue`: edition dynamique des props selon le registry.
- `widgetRegistry.js`: contrat central des widgets, tailles, props par defaut, settings et composants.

## Widgets
Les widgets actifs sont declares dans `widgetRegistry.js` et charges en `defineAsyncComponent`. Les widgets partagent les pieces UI de `widgets/_parts/` (`WidgetCard`, `KpiCard`, `Sparkline`) et appellent `StatsServices` avec les filtres `from/to/categories/types`.

Les anciens widgets non references et non presents dans le registry ont ete retires pour eviter la confusion: `OpexWidget`, `PlatformSplitWidget`, `ReturnRateWidget`, `TextSectionWidget`.

## Import/export
`components/gestion/CsvImportExportWidget.vue` contient l'UI, le drag/drop, le chargement lazy de PapaParse/XLSX et l'appel `SnkVenteServices.importBulk`.

La logique pure est centralisee dans `utils/stockImportExport.ts`:
- generation CSV compatible Excel;
- detection/mapping de colonnes;
- parsing JSON/tableaux/key-value;
- normalisation nombres/dates;
- validation preview;
- detection doublons avec les lignes existantes.

## Styles
Les styles globaux sont dans `src/assets/base.css` et `src/assets/main.css`. Les styles tres specifiques au canvas sont dans `components/stats/StatsCanvas.css`. Les composants conservent leurs styles scopes quand le rendu depend fortement du composant.

La base commune legere est documentee dans `docs/design-system.md`. Les nouveaux tokens applicatifs et classes `app-card`, `app-button-*`, `app-status-badge-*` vivent dans `src/assets/main.css`.

## Module administratif
La page admin est montee dans `pages/gestionPage.vue` via l'onglet `Administratif`. Sa configuration frontend est centralisee dans `src/constants/adminModule.js` : profils, onglets, options et cles de fallback local.

La logique de statuts commune est dans `src/constants/statuses.js`.

## Conventions
- Garder les endpoints et payloads API inchanges.
- Garder les pages dans `src/pages/`, les appels API dans `src/services/`, les helpers purs dans `src/utils/`.
- Garder les profils admin et statuts dans `src/constants/`, pas dans une page.
- Declarer les widgets uniquement via `widgetRegistry.js`.
- Ne pas ajouter d'abstraction si elle ne simplifie pas clairement un composant ou un service.
- Tester `npm test`, `npm run test:unit` et `npm run build` apres modification du frontend.
