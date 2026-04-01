# Frontend - tests

## 1) `tests/statsServices.test.js`

### Couverture
Valide le comportement de `StatsServices`:
1. construction params,
2. appels endpoints attendus,
3. normalisation signatures.

### Interet
Protege la couche API stats contre regressions de mapping parametres.

---

## 2) `tests/widgetPalette.utils.test.ts`

### Couverture
Valide utilitaires `paletteUtils.ts`:
1. filtrage/recherche,
2. flatten,
3. navigation clavier grille.

### Interet
Protège logique pure de palette, independante du DOM.

---

## 3) Gaps de tests frontend
1. Peu de tests composants lourds (`StatsCanvas`, `WidgetFrame`).
2. Peu de tests e2e sur flows auth/billing.
3. Peu de tests import CSV/XLSX.

## 4) Priorites test recommandees
1. Tests e2e auth/router guard/billing gating.
2. Tests interaction canvas (drag/resize/group).
3. Tests parse import multi-formats (CSV FR/US, XLSX, dates serial Excel).
