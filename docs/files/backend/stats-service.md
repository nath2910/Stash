# Backend - deep dive `StatsService.java`

## 1) Pourquoi ce fichier est critique
`StatsService` est le coeur analytique backend.
Il transforme des parametres API (dates, filtres, metric) en:
1. appels SQL repository,
2. calculs metriques,
3. DTO de sortie pour les widgets frontend.

Il est largement cache via `@Cacheable`.

---

## 2) Dependencies directes
- `SnkVenteRepository repo`.
- `StatsCacheKeys` (dans annotations cache).
- DTO stats (`StatsSummaryResponse`, `StatsPointResponse`, etc.).

---

## 3) Lecture methodes publiques (quoi / pourquoi / ou utilise)

### 3.1 `summary(...)`

#### Quoi
Calcule resume global sur periode:
- CA,
- profit,
- cout,
- nombre vendus,
- en stock,
- valeur stock,
- marge (%).

#### Comment
1. Normalise la plage via `normalizeRange`.
2. Normalise filtres categories/types (`normalizeCategories`, `normalizeTypes`).
3. Convertit listes en arrays SQL + flags "all".
4. Appelle repository:
- `caBetween`,
- `profitBetween`,
- `costBetween`,
- `countSoldBetween`,
- `countInStock` ou `countInStockAt` si `asOf`,
- `stockValue` ou `stockValueAt`.
5. Calcule `profitMargin` si CA > 0.
6. Retourne `StatsSummaryResponse`.

#### Ou utilise
- `GET /stats/summary`.
- widgets (ex: `InventoryValueWidget`, `NetProfitWidget` partiellement).

---

### 3.2 `timeseries(...)`

#### Quoi
Retourne liste points date avec `ca` et `profit`.

#### Comment
1. Normalise dates/filtres.
2. Choisit query par granularite (`day/week/month`).
3. Map projections SQL vers `StatsPointResponse`.

#### Ou utilise
- endpoint `/stats/timeseries`.
- anciens composants stats (hors canvas moderne majoritaire).

---

### 3.3 `brandBreakdown(...)`

#### Quoi
Retourne breakdown marques (label + nb).

#### Comment
- Appel `repo.brandBreakdownSales(...)`.
- Map vers `StatsBreakdownResponse`.

#### Ou utilise
- `/stats/brands`
- `BrandsWidget`.

---

### 3.4 `topSales(...)`

#### Quoi
Top ventes par profit sur periode.

#### Comment
- Appel `repo.topVentesBetween(..., limit, filters)`.

#### Ou utilise
- `/stats/top-sales`
- `TopSalesWidget`, `AvgMarginWidget` (top items).

---

### 3.5 `kpi(userId, from, to, metric, ...)`

#### Quoi
Calcule la valeur KPI actuelle + delta% vs periode precedente.

#### Comment
1. `currentSummary` sur plage courante.
2. `prevSummary` sur plage precedente (`prevRange`).
3. `metricFromSummary(metric, ...)` pour current/previous.
4. `deltaPct(current, previous)`.
5. Retour `StatsKpiResponse(value, deltaPct)`.

#### Ou utilise
- plupart widgets KPI (`netProfit`, `roi`, `asp`, `sellThrough`, etc.).

---

### 3.6 `series(userId, from, to, metric, granularity, ...)`

#### Quoi
Serie temporelle d'une metric specifique.

#### Comment
1. Cas special `avgDaysToSell`:
- requetes `avgDaysToSellDay|Week|Month`.
2. Sinon:
- charge `timeseriesFull` (ca/profit/cost/nb),
- derive chaque point via `metricFromTimeseries(metric, row...)`.
3. Retour `StatsSeriesPointResponse(date, value, label)`.

#### Ou utilise
- widgets sparkline/charts modernes.

---

### 3.7 `breakdown(userId, metric, ...)`

#### Quoi
Breakdown de type label/value selon metric demandee.

#### Metrics supportees code actuel
1. `deathPileAge` -> `repo.deathPileAge`.
2. `brands` -> `repo.brandBreakdownSales`.
3. `typeSoldCount` -> `repo.soldCountByType`.
4. `typeStockCount` -> `repo.stockCountByTypeAt`.
5. `typeProfit` -> `repo.profitByType`.

Sinon retourne `List.of()` vide.

#### Implication
Toute metric non listee ici retourne vide, meme si frontend l'appelle.

---

### 3.8 `rank(userId, from, to, metric, limit, ...)`

#### Quoi
Classement label/value.

#### Metrics supportees
1. `topBrandsProfit`.
2. `topCategoriesProfit`.

Sinon liste vide.

---

### 3.9 `dateBounds(userId)`

#### Quoi
Min/max dates disponibles pour bornes UI.

#### Comment
- lit `minAchatDate` et `minVenteDate`.
- prend min non null.
- max = `LocalDate.now()`.

---

### 3.10 `categories(userId, from, to)`

#### Quoi
Liste categories disponibles.

#### Comment
- si plage fournie: tente `distinctCategoriesBetween`.
- fallback `distinctCategories` globales.

---

## 4) Fonctions utilitaires internes

### `normalizeRange(from, to)`
- remplace null par `today`.
- inverse si `from > to`.

### `prevRange(from, to)`
- calcule periode precedente de meme longueur.

### `normalizeCategories`, `normalizeTypes`
- trim, remove blanks, dedupe.
- retourne `null` si vide.

### `toArray(list)`
- convert list -> `String[]` pour param SQL natif.

### `deltaPct(current, previous)`
- `(current - previous) / |previous| * 100`.
- retourne `null` si previous null/0.

---

## 5) `metricFromSummary(...)` detail metriques

Mapping principal:
1. `roi` -> `profit / cost * 100`.
2. `avgMargin` -> `profit / sold`.
3. `asp` -> `ca / sold`.
4. `activeListings` -> `stock count`.
5. `sellThrough` -> `sold / (sold + stock) * 100`.
6. `cashAvailable` -> `profit`.
7. `avgDaysToSell` -> requete avg jours vendus.
8. `grossRevenue` / `ca` -> `ca`.
9. `netProfit` / `profit` -> `profit`.
10. fallback -> `0`.

---

## 6) `metricFromTimeseries(...)` detail metriques
Mapping proche du summary mais calcule point par point:
1. `avgMargin`.
2. `asp`.
3. `roi`.
4. `sellThrough`.
5. `activeListings` (stock historique par bucket).
6. `grossRevenue` / `ca`.
7. `netProfit` / `profit` / `cashAvailable`.
8. fallback `0`.

---

## 7) Cache strategy

### Endpoints caches
- summary,
- timeseries,
- brandBreakdown,
- topSales,
- kpi,
- series,
- breakdown,
- rank,
- dateBounds,
- categories.

### Cle
Composee par `StatsCacheKeys` avec user/periode/filtres.

### Invalidation
`snkVenteService` evict `allEntries=true` sur create/update/delete/import.

---

## 8) Risques et ambiguïtes
1. Design metric en `if` successifs: ajoute une metric = edit multiple zones.
2. Certaines metrics frontend legacy ne sont pas mappees ici.
3. Retour silencieux `List.of()` peut masquer une erreur fonctionnelle.
4. Service long + logique melangee (normalisation + calcul + orchestration SQL).

---

## 9) Recommandations de refactor progressif
1. Introduire enum `Metric` type-safe backend.
2. Remplacer `if` par strategy map metric -> handler.
3. Separer couche "query access" et couche "metric formulas".
4. Ajouter tests parametrises metric x granularity x filtres.
5. Journaliser metrics inconnues au lieu de retour vide silencieux.
