# Dossier frontend/src/components/stats/

## 1) Vue globale
Ce module implemente un dashboard stats tres flexible avec:
1. canvas libre (drag/resize/zoom),
2. widgets KPI/charts/textes,
3. palette de widgets,
4. modal de settings dynamique,
5. persistance layout locale + backend.

## 2) Fichiers coeur

### `StatsCanvas.vue`
- Orchestrateur principal.
- Gere etat widgets, selection, camera, profils, date ranges, persistence.
- Fichier tres volumineux (plus de 3600 lignes).

### `StatsCanvas.css`
- CSS specifique canvas (grille, overlays, handles, transitions).

### `widgetRegistry.js`
- Catalogue central widgets.
- Definit metadata UI + composant + taille + props par defaut + champs settings.

### `WidgetPalette.vue`
- Dialog de selection widgets.
- Recherche/filtres/categories/types.
- Navigation clavier.

### `WidgetSettingsModal.vue`
- Edition dynamique des props widget selon schema registry.
- Gere nombres, select, multi-select, styles texte.

## 3) Sous-dossier `canvas/`

### `WidgetFrame.vue`
- Coque interactive de chaque widget:
  - drag,
  - resize,
  - actions,
  - inline edit texte,
  - fullscreen,
  - auto-resize.

### `useCanvaCamera.ts`
- Pan/zoom haut niveau (wheel, touch, pinch, center, fit).

### `useCanvasShortcuts.ts`
- Raccourcis clavier globaux canvas.

### `CanvasDock.vue`
- Dock actions (edit mode, palette, zoom, reset, theme, etc.).

## 4) Sous-dossier `palette/`

### `types.ts`
- Types TS utilises par la palette.

### `paletteUtils.ts`
- Filtrage/recherche/scoring widgets.
- Utilitaires navigation grille clavier.

### `widgetPaletteMeta.ts`
- Metadata de preview/description pour chaque type widget.

### `WidgetPaletteCard.vue`
- Carte unitaire d'un widget dans la palette.

### `WidgetPaletteFilterChips.vue`
- Chips filtres categorie/type de donnee.

### `WidgetPaletteSearchBar.vue`
- Barre de recherche palette.

### `WidgetPreview.vue`
- Mini preview visuelle (kpi/sparkline/bar/pie/heatmap).

## 5) Sous-dossier `widgets/`

### 5.1 Widgets KPI/sparkline
- `ActiveListingsWidget.vue`
- `AspWidget.vue`
- `AvgDaysToSellWidget.vue`
- `AvgMarginWidget.vue`
- `CashFlowWidget.vue`
- `InventoryValueWidget.vue`
- `NetProfitWidget.vue`
- `SellThroughWidget.vue`

Pattern commun:
1. Props `from/to/bucket/categories/types`.
2. `load()` async avec requetes stats.
3. `onMounted + watch` pour reload.
4. `computed` pour textes formates.

### 5.2 Widgets distribution/rank/charts
- `BrandsWidget.vue`
- `DeathPileWidget.vue`
- `TopProfitDriversWidget.vue`
- `TopSalesWidget.vue`
- `TypeMixWidget.vue`
- `GrossRevenueWidget.vue`
- `RoiWidget.vue`

Pattern:
1. requetes `breakdown/rank/kpi/series`.
2. option ECharts calculee.
3. adaptation responsive selon taille widget.

### 5.3 Widgets texte
- `TextBlockWidget.vue`
- `TextSectionWidget.vue`
- `TextTitleWidget.vue`

Pattern:
- rendu purement props/style.
- pas d'appel backend.

### 5.4 Widgets legacy/ambigus
- `OpexWidget.vue`
- `PlatformSplitWidget.vue`
- `ReturnRateWidget.vue`

Ces widgets appellent des metrics possiblement non supportees backend actuel.

## 6) Sous-dossier `widgets/_parts/`

### `WidgetCard.vue`
- base card widget (header, surface classes, responsive CSS vars).

### `KpiCard.vue`
- pattern KPI principal + delta + sparkline.

### `ChartCard.vue`
- wrapper chart ECharts avec tuning fonts/layout selon dimensions.

### `Sparkline.vue`
- mini graphe ligne ECharts minimal.

## 7) Couplages critiques du module stats
1. `widgetRegistry.js` est le contrat central.
2. `StatsCanvas.vue` depend de `WidgetFrame` et du registry.
3. Widgets dependent de `StatsServices` + `statsAdapters`.
4. Palette et modal settings se basent sur schema registry.

## 8) Priorite reprise module stats
1. Lire `widgetRegistry.js`.
2. Lire `StatsCanvas.vue`.
3. Lire `WidgetFrame.vue`.
4. Lire `WidgetPalette.vue` + `WidgetSettingsModal.vue`.
5. Lire widgets un par un selon metrique.
