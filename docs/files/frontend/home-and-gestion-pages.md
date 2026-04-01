# Frontend - home and gestion (fichier par fichier)

## 1) `pages/homePage.vue`

### Role
Dashboard d'accueil synthetique.

### Logique
1. Charge liste ventes via `SnkVenteServices.getSnkVente()`.
2. Calcule KPIs (`computed`):
- nb en stock,
- ventes du mois,
- CA mensuel,
- benef mensuel,
- prix moyen revente mensuel.
3. Gere modal onboarding (flags `localStorage`).
4. Navigation rapide vers gestion et stats.

### Fichiers relies
- `AcceuilWidgetLateral.vue`
- `AcceuilDernierItem.vue`
- helpers `utils/snkVente.js`, `utils/formatters.js`.

---

## 2) `components/AcceuilWidgetLateral.vue`
- Carte KPI lateral (loading + ton positif/negatif).
- Emet navigation vers gestion/stats.

## 3) `components/AcceuilDernierItem.vue`
- Charge `recent(6)`.
- Affiche mini table des derniers items.

---

## 4) `pages/gestionPage.vue`

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

## 5) Composants gestion

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
- Import smart CSV/XLSX avec heuristiques colonne/date/nombre.
- Envoie payload backend import.

---

## 6) Utilitaires utilises par gestion

### `constants/itemTypes.js`
- liste types + champs metadata autorises par type.

### `utils/snkVente.js`
- helpers lecture valeurs snake/camel et calculs metier.

### `utils/formatters.js`
- format monetaire/num/date.

---

## 7) Legacy
- `components/StatBase.vue` est un ancien composant stats (non central dans flux actuel canvas).
