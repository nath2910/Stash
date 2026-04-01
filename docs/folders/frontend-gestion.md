# Dossier frontend/src/components/gestion/

## 1) Mission
Ce dossier implemente l'UI de gestion du stock:
1. affichage liste,
2. ajout/modification/suppression,
3. import/export CSV/XLSX,
4. pieces jointes par item,
5. filtres et selection multiple.

## 2) Fichiers

### `GestionAfficherTout.vue`
- Table principale des items filtres.
- Selection individuelle / selection globale visible.
- Affiche champs metier (prix, dates, type, profit).
- Emet `edit` et `update:modelValue` (ids selectionnes).

### `GestionAjoutPaire.vue`
- Modal d'ajout.
- Gere nombre de copies (creation multiple).
- Gere type d'item + metadata associee.
- Validation metier (si revendu => date vente requise).
- Appelle `SnkVenteServices.ajouter`.

### `GestionModifierItem.vue`
- Modal edition item existant.
- Gere metadata selon type.
- Gere pieces jointes (liste/upload/download/delete).
- Appelle `SnkVenteServices.update` et APIs attachments.

### `GestionSupprimerModal.vue`
- Modal suppression:
  - mode unitaire par recherche,
  - mode bulk via ids selectionnes.
- Appelle `SnkVenteServices.supprimer` ou `supprimerEnMasse`.

### `GestionBlocBoutonAddDelete.vue`
- Wrapper UI boutons ajout/suppression.

### `GestionBoutonOnAdd.vue`
- Bouton qui ouvre `GestionAjoutPaire`.

### `GestionRésumeStock.vue`
- Resume KPI stock (total, vendues, en stock, valeur stock).

### `GestionSearchBarre.vue`
- Barre recherche simple `v-model` style component.

### `CsvImportExportWidget.vue`
- Composant le plus complexe du dossier.
- Export CSV:
  - colonnes standards + metadata,
  - format excel-friendly (BOM UTF-8).
- Import CSV/XLSX:
  - parse fichier,
  - detection entetes par synonymes,
  - conversion nombre/date robuste,
  - extraction metadata,
  - payload final backend `importBulk`.

## 3) Couplages
1. Tous ces composants sont pilotes par `pages/gestionPage.vue`.
2. La couche API unique est `SnkVenteServices.js`.
3. Helpers metier: `utils/snkVente.js`, `constants/itemTypes.js`, `utils/formatters.js`.

## 4) Zones sensibles
1. Import parse heuristique (beaucoup de cas limites).
2. Mapping snake_case/camelCase selon source de donnees.
3. Attachments: verification MIME/taille faite aussi backend.
