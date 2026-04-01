# Backend - deep dive `SnkVenteRepository.java`

## 1) Pourquoi ce fichier est critique
Ce repository concentre la quasi-totalite des requetes analytiques SQL.
Il sert de fondation a `StatsService`.

Impact:
- bug SQL ici = bug sur plusieurs widgets.
- changement schema ici = risque global stats.

---

## 2) Structure generale

### 2.1 Projections internes
Le fichier declare plusieurs interfaces projection:
1. `BrandCount` (`marque`, `nb`).
2. `TimePointRow` (`date`, `ca`, `profit`).
3. `TimePointFullRow` (`date`, `ca`, `profit`, `cost`, `nb`).
4. `AvgDaysRow` (`date`, `avgDays`).
5. `LabelValueRow` (`label`, `value`).
6. `LabelLongRow` (`label`, `value` long).
7. `LabelCount` (`label`, `nb`).

### 2.2 Types de methodes
1. JPA derives (`findByUser...`).
2. JPQL simples (`deleteByUserAndIds`, `totalBenef`).
3. SQL natif complexes (timeseries/breakdowns/ranks).

---

## 3) Groupe CRUD/listing

### Methodes
- `findByUser_IdOrderByDateAchatDesc(...)`.
- `findByUser_IdOrderByCreatedAtDesc(..., Pageable)`.
- `deleteByIdAndUser_Id`.
- `deleteByUserAndIds`.
- `deleteByUser_Id`.

### Utilisation
`snkVenteService` pour gestion standard.

---

## 4) Groupe agrégats globaux

### Methodes
1. `totalBenef(userId)`.
2. `totalBenefBetween(userId, start, end)`.
3. `sumPrixResell(userId)`.
4. `caBetween(...)`.
5. `profitBetween(...)`.
6. `costBetween(...)`.
7. `countSoldBetween(...)`.
8. `countInStock(...)`.
9. `countInStockAt(asOf, ...)`.
10. `stockValue(...)`.
11. `stockValueAt(asOf, ...)`.

### Utilisation
`StatsService.summary`, KPIs home, page gestion resume.

---

## 5) Groupe series temporelles

### Methodes
1. `timeseriesDay(...)`.
2. `timeseriesWeek(...)`.
3. `timeseriesMonth(...)`.
4. `timeseriesDayFull(...)`.
5. `timeseriesWeekFull(...)`.
6. `timeseriesMonthFull(...)`.

### Notes
- Les versions `Full` exposent `cost` et `nb` en plus.
- Utilisees pour derive metrics (ROI, ASP, avgMargin, etc.).

---

## 6) Groupe age/moyennes

### Methodes
1. `avgDaysToSellDay(...)`.
2. `avgDaysToSellWeek(...)`.
3. `avgDaysToSellMonth(...)`.
4. `avgDaysToSellBetween(...)`.

### Utilisation
`StatsService.kpi` et `StatsService.series` metric `avgDaysToSell`.

---

## 7) Groupe breakdown par type/categorie/marque

### Methodes
1. `soldCountByType(...)`.
2. `stockCountByTypeAt(...)`.
3. `profitByType(...)`.
4. `deathPileAge(...)`.
5. `brandBreakdownSales(...)`.
6. `topBrandsProfit(...)`.
7. `topCategoriesProfit(...)`.

### Utilisation
Widgets TypeMix, DeathPile, Brands, TopProfitDrivers, ROI (rank categorie).

---

## 8) Groupe top ventes

### Methodes
1. `topVentesBetween(...)`.
2. `topVentesYear(...)` (default method wrapper).

### Utilisation
`StatsService.topSales`, `snkVenteService.getTop3VentesAnneeCourante`.

---

## 9) Groupe categories distinctes

### Methodes
1. `distinctCategories(userId)`.
2. `distinctCategoriesBetween(userId, from, to)`.

### Utilisation
Filtres stats UI (`/stats/categories`).

---

## 10) Pattern filtre categories/types
Beaucoup de requetes natives prennent:
- `categories` (array),
- `categoriesAll` (bool),
- `types` (array),
- `typesAll` (bool).

Raison:
- eviter concat SQL dynamique,
- garder query unique en activant/desactivant filtres via booleens.

---

## 11) Zones SQL fragiles
1. Detection marque basee sur `LIKE` nom item (heuristique, non normalisee).
2. Beaucoup de SQL natif multiline difficile a relire.
3. Duplications entre requetes day/week/month.
4. Pas de couche abstraite query builder.

---

## 12) Conseils maintenance
1. Isoler groupes de requetes en repositories specialises (`StatsReadRepository` etc.).
2. Ajouter tests integration SQL sur dataset fixe pour chaque requete critique.
3. Introduire vue SQL/materialized views si croissance volumetrie.
4. Normaliser la marque en colonne dediee pour eviter heuristiques `LIKE`.
