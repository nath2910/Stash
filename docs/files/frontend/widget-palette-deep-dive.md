# Frontend - deep dive `WidgetPalette.vue`

## 1) Role du composant
`WidgetPalette.vue` est la fenetre de selection des widgets stats.

Fonctions:
1. recherche textuelle,
2. filtres categorie/type,
3. affichage groupes,
4. choix variante (`forms`) d'un widget,
5. navigation clavier et accessibilite.

---

## 2) Etat principal

Variables reactives majeures:
- `query`.
- `activeCategory`.
- `activeDataType`.
- `pendingWidget` (si widget a plusieurs formes).
- `activeWidgetType` (focus clavier).
- refs DOM (`scrollEl`, `gridEl`).

Pedagogie Vue:
- Ces etats pilotent tout rendu via `computed` (pas de DOM imperative directe sauf focus/scroll).

---

## 3) Donnees derivees

### `visibleGroups`
Construit via `filterPaletteGroups(...)` (utilitaire pur).

### `flatVisibleItems`
Liste flatten pour navigation clavier.

### Compteurs
- `totalWidgets`.
- `visibleWidgetsCount`.

### `formList`
- liste variantes d'un widget quand `forms` present.

---

## 4) Watchers importants
1. Watch ouverture palette:
- reset etat,
- focus input recherche.
2. Watch `query`:
- controle category active.
3. Watch `flatVisibleItems`:
- maintient selection valide quand filtre change.

Pourquoi:
- eviter etats incoherents lors des changements rapides utilisateur.

---

## 5) Selection et ajout widget

### `onWidgetClick(widget)`
1. Si widget simple -> `queueAdd(type)` immediat.
2. Si widget avec plusieurs `forms` -> ouvre picker de forme.

### `selectForm(key)`
- emit payload `{ type, view }`.

### `queueAdd(payload)`
- emit `close`, puis emit `add` en micro-delay pour fermeture propre.

---

## 6) Filtres et navigation sections

### `onCategorySelect(value)`
- active categorie,
- scroll vers section correspondante.

### `focusCard(type)`
- place focus DOM sur carte cible.

---

## 7) Navigation clavier

### Fonctions
- `activeIndexFromDom()`.
- `onSearchKeydown(event)`.
- `onGridKeydown(event)`.
- `onDialogKeydown(event)`.

### Logique
1. Depuis search input:
- ArrowDown envoie focus au 1er widget.
2. Dans grille:
- fleches deplacement selon colonnes,
- Enter selectionne,
- Escape ferme.
3. Dans dialog:
- raccourcis globaux fallback.

Utilitaire clé:
- `moveGridIndexByKey(...)` de `paletteUtils.ts`.

---

## 8) Couplages
1. Source widgets: `widgetRegistry.js` (indirect via parent).
2. Metadata preview: `widgetPaletteMeta.ts`.
3. Composants enfants:
- `WidgetPaletteSearchBar`,
- `WidgetPaletteFilterChips`,
- `WidgetPaletteCard`.

---

## 9) Risques
1. Etat focus clavier peut desynchroniser si liste change vite.
2. Beaucoup de logique UI dans un seul composant.
3. Gestion forms picker peut etre source de bugs UX subtils.

---

## 10) Reco reprise
1. Garder logique pure dans `paletteUtils.ts`.
2. Ajouter tests composant sur navigation clavier.
3. Verifier accessibilite lecteurs ecran/ARIA sur modale palette.
