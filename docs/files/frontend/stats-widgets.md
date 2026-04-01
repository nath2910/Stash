# Frontend - stats widgets (fichier par fichier)

## 1) Pattern commun des widgets data
La plupart des widgets data suivent ce schema:
1. `props` contenant `from/to/bucket/categories/types`.
2. `loading` + `error` + state data en `ref`.
3. fonction `load()` async.
4. `onMounted(load)` + `watch([...props], load)`.
5. `computed` pour format final UI.

Pedagogie JS:
- Le `watch(() => [a,b,c], load)` relance `load` quand n'importe quelle valeur du tableau change.

---

## 2) Widgets KPI / sparkline

### `ActiveListingsWidget.vue`
- Metric backend: `kpi('activeListings')`, `series('activeListings')`.
- Affiche nb annonces actives + delta + sparkline.

### `AspWidget.vue`
- Metric backend: `asp`.
- Affiche prix moyen de vente.

### `AvgDaysToSellWidget.vue`
- Metric backend: `avgDaysToSell`.
- Compare periode courante vs precedente (`prevPeriod`).
- Affiche delta en jours.

### `AvgMarginWidget.vue`
- Metric backend: `avgMargin` + `topSales(limit=3)`.
- Affiche marge moyenne + top items contextuels.

### `CashFlowWidget.vue`
- Metric backend: `cashAvailable`.

### `InventoryValueWidget.vue`
- Utilise `summary(...)` avec logique `asOf`.
- Peut ignorer range globale selon prop `useGlobalRange`.

### `NetProfitWidget.vue`
- Widget avancé TypeScript.
- Metrics:
- `kpi('netProfit')` courant,
- `kpi('netProfit')` periode precedente,
- `series('netProfit')`,
- `summary(...)` pour marge.
- Gros travail de layout responsive interne (size modes, spark SVG custom, tons couleur).

### `SellThroughWidget.vue`
- Metric backend: `sellThrough`.
- Affiche progression vs cible.

---

## 3) Widgets distribution / ranking / chart

### `BrandsWidget.vue`
- Source: `StatsServices.brands(...)`.
- Render chart horizontal style bar.

### `DeathPileWidget.vue`
- Source: `breakdown('deathPileAge')`.
- Classe stock non vendu par anciennete.

### `GrossRevenueWidget.vue`
- Source: `kpi('grossRevenue')` + `series('grossRevenue')`.
- Ajuste granularite effective selon plage temporelle.
- UI premium avec mini analytics (high/low/variation).

### `RoiWidget.vue`
- Source:
- `kpi('roi')` courant,
- `kpi('roi')` periode precedente,
- `rank('topCategoriesProfit')`.
- Affiche progression vers cible ROI.

### `TopProfitDriversWidget.vue`
- Source: `rank('topCategoriesProfit')`.
- Affiche top categories profitables.

### `TopSalesWidget.vue`
- Source: `topSales(...)`.
- Gestion mode etendu/collapsé du nombre de lignes.

### `TypeMixWidget.vue`
- Source variable selon prop `metric`:
- `typeProfit`,
- `typeSoldCount`,
- `typeStockCount`.
- Vue pie ou bar selon `view`.

---

## 4) Widgets texte

### `TextBlockWidget.vue`
- Texte libre multi-style (align, taille, poids, famille).

### `TextSectionWidget.vue`
- Bloc section sobre (titre + sous-titre).

### `TextTitleWidget.vue`
- Titre fort avec options typographiques.

Ces widgets n'appellent pas l'API.

---

## 5) Widgets potentiellement legacy / ambigus

### `OpexWidget.vue`
- Appelle `breakdown('opex')`.
- `StatsService.breakdown` ne mappe pas explicitement `opex`.
- Risque: widget vide en prod.

### `PlatformSplitWidget.vue`
- Appelle `breakdown('platformSplit')`.
- Metric non mappee explicitement backend actuel.

### `ReturnRateWidget.vue`
- Appelle `series('returnRate')`.
- Metric non mappee explicitement backend actuel.

Action reprise: verifier si code backend manquant, code frontend legacy, ou endpoint externe attendu.

---

## 6) Composants partages `_parts/`

### `WidgetCard.vue`
- Coquille visuelle standard.
- Gere classes de surface (`kpi`, `trend`, `distribution`, etc.).
- Adapte padding/font/radius selon dimensions widget.

### `KpiCard.vue`
- Layout KPI + delta + sparkline.
- Classes delta up/down.

### `ChartCard.vue`
- Wrapper chart ECharts.
- Ajuste taille polices axes/legend selon dimension widget.

### `Sparkline.vue`
- Mini line chart simple.

---

## 7) Liens entre fichiers
1. Chaque widget concret est reference dans `widgetRegistry.js`.
2. Le rendu est encapsule dans `WidgetFrame.vue`.
3. Les options de settings de widget viennent du registry et sont editees dans `WidgetSettingsModal.vue`.
4. Toutes donnees passent via `StatsServices.js` puis `statsAdapters.js`.
