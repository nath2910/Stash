# Frontend - home and gestion (fichier par fichier)

## 1) `pages/homePage.vue`

### Role
Dashboard d'action rapide.

### Logique
1. Charge le stock via `SnkVenteServices.getSnkVente()` pour la recherche locale.
2. Charge les KPI initiaux via `StatsServices.summary(from, to)`.
3. Calcule une synthese locale via `utils/homeDashboard.js` pour garder les KPI synchronises apres ajout/modification rapide.
4. Gere la recherche predictive, l'ouverture de modale item, l'ajout rapide et l'onboarding.

### Fichiers relies
- `components/home/QuickSearchBar.vue`
- `components/home/SearchResultsDropdown.vue`
- `components/home/QuickItemModal.vue`
- `components/home/QuickAddItemForm.vue`
- `components/home/HomeMonthlyKpis.vue`
- `components/home/KpiCard.vue`
- helpers `utils/homeDashboard.js`, `utils/snkVente.js`, `utils/formatters.js`.

---

## 2) `components/home/QuickSearchBar.vue`
- Recherche locale debounced sur nom, categorie, description, type et metadata utiles.
- Navigation clavier haut/bas, entree pour ouvrir, echap pour fermer.
- Emet l'item selectionne sans changer de route.

## 3) `components/home/QuickItemModal.vue`
- Modale courte de correction item.
- Sauvegarde via `SnkVenteServices.update(id, payload)`.
- Met a jour l'etat local de l'accueil sans rechargement.

## 4) `components/home/QuickAddItemForm.vue`
- Form compact d'ajout stock.
- Sauvegarde orchestree par `homePage.vue` via `SnkVenteServices.create(payload)`.
- Reset apres succes, quantite bornee a 50.

## 5) `components/home/HomeMonthlyKpis.vue`
- Affiche C.A. du mois, benefices, items en stock et stock en euros.
- Utilise les stats API au chargement puis la synthese locale si le stock est charge.

## 6) Legacy accueil
- `AcceuilWidgetLateral.vue` et `AcceuilDernierItem.vue` restent presents mais ne sont plus utilises par la route `/`.

---

## 7) `pages/gestionPage.vue`

### Role
Page orchestration gestion stock.

### Etat principal
- `snkVentes` (liste complete).
- `searchTerm`.
- `selectedIds` (bulk).
- modales edit/delete.

### Logique
1. `chargerVentes` onMounted.
2. KPIs compute (total/vendues/en stock/valeur stock).
3. `filteredVentes` selon terme (id/nom/categorie/description/type).
4. Maintient coherence selection quand filtre change.
5. handlers ajout/edit/delete/import reloadent la liste.

---

## 8) Composants gestion

### `GestionSearchBarre.vue`
- Input `v-model` de recherche.

### `GestionRésumeStock.vue`
- Affiche resume KPI.

### `GestionBlocBoutonAddDelete.vue`
- Compose boutons d'action principales.

### `GestionBoutonOnAdd.vue`
- Ouvre modal d'ajout.

### `GestionAfficherTout.vue`
- Tableau principal items.
- Selection row/all visible.
- Bouton edit par ligne.

### `GestionAjoutPaire.vue`
- Form creation item.
- Support creation multiple copies.
- Garde `keepCommonFields`.
- Nettoie metadata selon type.

### `GestionModifierItem.vue`
- Form update item.
- Gere attachments (list/upload/download/delete).
- Mapping metadata dynamique par type.

### `GestionSupprimerModal.vue`
- suppression single (autocomplete) ou bulk (ids selectionnes).

### `CsvImportExportWidget.vue`
- Export CSV robuste (BOM, mapping colonnes, metadata).
- Import smart CSV/XLSX/JSON/TXT avec heuristiques colonne/date/nombre.
- Delegue mapping, validation et generation CSV a `utils/stockImportExport.ts`.
- Envoie payload backend import.

---

## 9) Utilitaires utilises par gestion

### `constants/itemTypes.js`
- liste types + champs metadata autorises par type.

### `utils/snkVente.js`
- helpers lecture valeurs snake/camel et calculs metier.

### `utils/formatters.js`
- format monetaire/num/date.

### `utils/stockImportExport.ts`
- parsing/mapping/validation import export stock, couvert par tests unitaires.

---

## 10) Legacy
Les anciens composants stats non references ont ete retires du frontend runtime.
