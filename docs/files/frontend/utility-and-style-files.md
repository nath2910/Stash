# Frontend - utility and style files

## 1) `src/utils/formatters.js`

### Fonctions
1. `toNumber`.
2. `formatEUR`.
3. `formatNumber`.
4. `formatPct`.
5. `signFmt`.
6. `formatDateFR`.

### Pourquoi
Evite duplication de logique de formatage dans tous les widgets/pages.

---

## 2) `src/utils/snkVente.js`

### Fonctions
1. `getField(obj, key, fallback)`:
- lit camelCase et fallback snake_case.
2. `isVendue`.
3. `prixRetailOf`.
4. `prixResellOf`.
5. `profitOf`.
6. `hasResell`.
7. `typeOf`.

### Pourquoi
Bridge utile entre payloads heterogenes backend/import.

---

## 3) `src/constants/itemTypes.js`

### Contenu
1. `ITEM_TYPES`: types autorises.
2. `METADATA_FIELDS`: champs metadata par type.
3. `typeLabel(type)`: label utilisateur.

### Utilisation
- formulaires ajout/modification,
- import/export,
- affichage badges type.

---

## 4) `src/lib/echarts.js`

### Role
Centralise enregistrement modules ECharts utilises pour eviter import global excessif.

### Utilisation
Importe au bootstrap pour config chart.

---

## 5) `src/components/DateRangeBar.vue`

### Role
Selecteur date range (composant historique hors canvas).

### Logique
- maintient `[from,to]` local,
- emet updates vers parent,
- presets month/ytd/year.

---

## 6) `src/components/ui/CompactDateInput.vue`

### Role
Input date compact custom (integration datepicker).

### Logique
1. Convertit display FR <-> format ISO `yyyy-mm-dd`.
2. Gere min/max date.
3. Theme light/dark support.

---

## 7) `src/components/StatBadge.vue`
- Petit composant de badge KPI avec ton dynamique.

## 8) `src/components/HeaderDePage.vue`
- Header simple reutilisable (titre/sous-titre/description).

## 9) `src/components/StatBase.vue` (legacy)
- Ancienne base stats.
- Contient reset de layout legacy key v1.
- Non coeur du systeme canvas actuel.

---

## 10) CSS globaux

### `src/assets/base.css`
- reset/base styles + variables globales.

### `src/assets/main.css`
- styles applicatifs globaux.

### `src/components/stats/StatsCanvas.css`
- styles specialises canvas stats.

---

## 11) Config frontend autour styles

### `tailwind.config.js`
- content paths + font family extension.

### `postcss.config.js`
- plugins tailwind + autoprefixer.

### `index.html`
- charge font Google Inter + monte `#app`.

---

## 12) Note reprise
Le couple `formatters.js` + `snkVente.js` est tres utilise; toute modification peut impacter home, gestion, stats simultanement.
