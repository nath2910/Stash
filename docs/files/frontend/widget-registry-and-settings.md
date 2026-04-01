# Frontend - widget registry, palette, settings

## 1) `components/stats/widgetRegistry.js`

### Role
Source de verite des widgets disponibles.

### Structure par entree widget
Chaque objet `WIDGET_DEFS` contient typiquement:
1. `type` (identifiant unique).
2. `title`.
3. `category`.
4. `component` (fichier Vue).
5. dimensions:
- `w`, `h` (defaut),
- `minW`, `minH`.
6. `props` par defaut.
7. `settings` schema (champs editables dans modal).
8. options additionnelles (`forms`, `categoryFilter`, `dateMode`, etc.).

### Fonctions exposees
- `getCategoryColor(category)`.
- `getWidgetDef(type)`.
- `newWidget(type, x, y)`:
- genere id unique,
- clone props defaut,
- retourne instance widget canvas.

### Pourquoi critique
Tout ajout widget passe par ce fichier.

---

## 2) `components/stats/WidgetSettingsModal.vue`

### Role
Modal generique d'edition de props widget.

### Bloc par bloc
1. Props:
- `open`,
- `title`,
- `fields` (schema),
- `model` (valeurs actuelles).
2. Etat `draft` reactive clone du model.
3. `orderedFields`:
- trie champs text widgets avec ordre dedie (`TEXT_FIELD_ORDER`).
4. Helpers UI:
- `tileGridClass`, `settingCardClass`, `optionStyle`.
5. Gestion champs numeriques:
- `clampNumberField`, `stepNumber`, `hasNumberMeta`.
6. Multi-select:
- `toggleMulti`, `selectAllMulti`, `clearMulti`, `ensureMultiList`.
7. `onSave()`:
- normalise draft,
- emit `save`.
8. watchers:
- resync draft quand modal/model change.

### Pourquoi
Permet d'ajouter des settings sans coder une modal specifique par widget.

---

## 3) `components/stats/WidgetPalette.vue`

### Role
Palette d'ajout de widgets.

### Bloc par bloc
1. Recoit liste widgets (depuis registry transforme).
2. Maintient etat recherche/filtres/categorie active.
3. Utilise `paletteUtils` pour filtrage et flatten.
4. Gere forms picker (quand widget a variantes de vue).
5. Support navigation clavier:
- focus cards,
- deplacement grille,
- Enter/Escape.
6. Emets:
- `add` (type ou `{type, view}`),
- `close`.

### Pourquoi
Point d'entree principal creation widgets.

---

## 4) `components/stats/palette/paletteUtils.ts`

### Fonctions cle
1. `normalizeText`.
2. `filterPaletteGroups(...)`:
- applique query + filtres categorie/dataType.
3. `flattenPalette(...)`.
4. `resolveGridColumns(width)`.
5. `moveGridIndexByKey(...)`.
6. `coercePreviewSpec(...)`.

### Pourquoi
Concentre logique pure (testable) de recherche/navigation palette.

---

## 5) `components/stats/palette/widgetPaletteMeta.ts`
- Table metadata description/tagline/preview selon `type`.
- `getWidgetPaletteMeta(type)` avec fallback default.

## 6) `components/stats/palette/types.ts`
- Defs TS des structures palette.

## 7) Composants palette de presentation

### `WidgetPaletteCard.vue`
- carte widget clickable.
- detecte visibilite via `IntersectionObserver` (optimisation preview).

### `WidgetPaletteFilterChips.vue`
- chips selection categorie et data type.

### `WidgetPaletteSearchBar.vue`
- input recherche + clear.

### `WidgetPreview.vue`
- mini rendus visuels (spark, bar, pie, heatmap, kpi).

---

## 8) Comment ajouter un nouveau widget
1. Creer composant `widgets/NewWidget.vue`.
2. Ajouter entree dans `WIDGET_DEFS` (`widgetRegistry.js`).
3. Definir `props` et `settings` schema.
4. Ajouter metadata preview dans `widgetPaletteMeta.ts`.
5. Verifier rendu dans palette + canvas + modal settings.
