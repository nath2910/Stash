# Frontend - deep dive `StatsCanvas.vue`

## 1) Pourquoi ce fichier est critique
`StatsCanvas.vue` est le moteur principal du dashboard stats.
Il combine:
1. etat metier (widgets, profils, dates),
2. interaction UI basse couche (drag/resize/group selection),
3. camera pan/zoom,
4. persistence locale + backend,
5. orchestration de sous-composants (dock, palette, modal settings, widget frame).

Le fichier depasse 3600 lignes: c'est une zone de complexite majeure.

---

## 2) Lecture pedagogique de la structure

### 2.1 Types et modeles (debut script)
Types principaux:
- `Widget`.
- `ResizeDir`.
- `ProfileRange`.
- `LayoutBundle`.

Pedagogie JS/TS:
- un `type` TypeScript ne genere pas de code runtime; il sert a verifier la forme des objets a la compilation.

### 2.2 Etat global reactive
Le composant declare beaucoup de `ref(...)` et `computed(...)`.
Exemples:
- liste widgets,
- selection active,
- camera scale,
- profil courant,
- panneaux ouverts/fermes.

Pedagogie Vue:
- `ref([])` cree un tableau reactive.
- toute mutation entraine rerender des zones qui lisent cette valeur.

---

## 3) Gestion mode edition et theme
Fonctions:
- `loadEditMode()`.
- `persistEditMode()`.
- `toggleEditMode()`.
- `toggleThemeMode()`.

Logique:
1. Cle storage `snk_stats_canvas_edit_v1_<userId>`.
2. L'etat edit reste specifique par utilisateur.
3. Theme passe par composable `useTheme`.

---

## 4) Gestion plage de dates et profils

### Fonctions
- `setFrom`, `setTo`, `clampDate`.
- `normalizeProfileRange`, `loadRangeForProfile`, `saveRangeForProfile`.
- `preset('month'|'ytd'|'year')`.
- `switchProfile`, `openProfileEditor`, `saveProfileEditor`.

### Logique
1. Chaque profil (P1/P2/P3) garde sa plage date.
2. Les plages sont stockees en localStorage par user+profil.
3. Changement profil recharge la plage correspondante.
4. Edition profil permet renommer/couleur.

---

## 5) Tailles widgets texte et fitting automatique

### Fonctions cle
- `isTextWidget`.
- `textWidgetFontFamily`.
- `textWidgetResolvedFontSize`.
- `estimateTextWidgetSize`.
- `fitTextWidgetToRenderedContent`.
- `fitTextWidgetAfterRender`.

### Pourquoi
Les widgets texte ont une taille qui doit suivre le contenu (pas juste min/max fixe).

### Comment
1. Estimation mathematique initiale.
2. Mesure DOM reelle apres rendu (et apres chargement polices `document.fonts.ready`).
3. Ajustement hauteur/largeur selon contraintes min/max.

Pedagogie JS:
- Le DOM n'est fiable qu'apres rendu. D'ou l'usage de callbacks differees et raf.

---

## 6) Chargement et migration layout

### Fonctions
- `loadLayout(key)`.
- `defaultLayout()`.
- `normalizeLayout(raw)`.
- `normalizeBundle(raw)`.
- `applyProfileLayout(bundle, profileId)`.
- `loadLayoutForUser()`.

### Logique
1. Lit localStorage layout user.
2. Supporte anciens formats (array legacy) via normalisation.
3. Construit un `LayoutBundle` versionne avec profils.
4. Si absent/corrompu: fallback `defaultLayout()`.

---

## 7) Persistence locale et backend

### Fonctions
- `serializeWidgets()`.
- `saveBundleNow()`.
- `saveLayoutNow()`.
- `scheduleSave()`.
- `scheduleRemoteSave()`.
- `clearPendingSaves()`.
- `showSavedToast()`.

### Strategie
1. Save local rapide et frequent.
2. Save backend debounce pour reduire bruit reseau.
3. Toast discret de confirmation save.

### Endpoint
- `StatsServices.saveLayout(layout)` -> `PUT /stats/layout`.

---

## 8) Selection simple, multiple et marquee

### Fonctions
- `isWidgetSelected`.
- `setSelectedWidgets`.
- `clearSelection`.
- `selectSingleWidget`.
- `toggleWidgetSelection`.
- `startMarqueeSelection`.
- `onMarqueePointerMove`.
- `onMarqueePointerUp`.
- `finishMarqueeSelection`.

### Logique
1. Click widget:
- selection simple ou additive (ctrl/cmd).
2. Drag sur zone vide:
- cree rectangle marquee,
- selectionne widgets intersectes.
3. L'etat selection alimente actions group (delete/resize/duplicate).

---

## 9) Geometrie groupe et group resize

### Fonctions
- `selectedGroupBounds()`.
- `startGroupResize(...)`.
- `applyGroupResizeStep(...)`.
- `finishGroupResize()`.

### Comment
1. Calcule bounding box du groupe selectionne.
2. Au resize, calcule ratio de transformation.
3. Applique transformation a chaque widget membre.
4. Clamp aux limites board + tailles min/max.

---

## 10) Drag, resize, text scale (interactions bas niveau)

### Types d'etat
- `DragState`.
- `ResizeState`.
- `GroupResizeState`.
- `TextScaleState`.

### Fonctions drag
- `startDrag`.
- `onGlobalPointerMove`.
- `finishDrag`.
- `finishDragGroup`.

### Fonctions resize
- `startResize`.
- `onResizePointerMove`.
- `finishResize`.

### Fonctions text scale
- `startTextScale`.
- `applyTextScaleStep`.
- `onTextScalePointerMove`.
- `finishTextScale`.

### Pattern technique
- Utilise `requestAnimationFrame` via `schedule*Apply` pour lisser rendu.
- Evite update DOM a chaque micro event pointer.

Pedagogie JS:
- Les `PointerEvent` unifient souris/stylet/touch.
- `raf` limite la frequence au rythme d'affichage ecran.

---

## 11) Smart snap et placement

### Fonctions
- `collectAxisTargets`.
- `snapOriginToTargets`.
- `snapEdgeToTargets`.
- `setSnapGuides`.
- `placeWidget`.

### Logique
1. Calcule lignes candidates (centres/bords widgets voisins).
2. Si proche seuil, aligne widget automatiquement.
3. Affiche guides visuels de snap.
4. `placeWidget` evite superpositions trop fortes lors ajout.

---

## 12) Camera et virtualisation

### Fonctions
- `updateVisibleRectNow`.
- `scheduleVisibleRectUpdate`.
- `widgetIntersectsVisibleRect`.
- `fitToWidgets`.
- `centerView`.
- `zoomIn`, `zoomOut`, `resetZoom`, `zoomToFitContent`.

### Couplage
Appels vers composable `useCanvaCamera.ts`.

### Pourquoi
1. Maintient perf en ne calculant que widgets visibles.
2. Offre UX fluide sur grands canvas.

---

## 13) Reglages widget et edition texte

### Fonctions
- `openSettings`, `closeSettings`.
- `simplifySettingsFields`.
- `applySettings`.
- `updateTextWidgetProps`.
- `textLayoutChanged`.

### Logique
1. Ouvre modal settings selon schema registry.
2. Applique patch props widget.
3. Si patch touche layout texte, déclenche re-fit dimension.

---

## 14) Lifecycle et listeners globaux

### Fonctions
- `onCanvasPointerDown`.
- `onCanvasContextMenu`.
- `onSelectionKeyDown`.
- `onGlobalPointerUp`.
- `detachAllInteract`.

### Role
- Gerer capture pointer proprement.
- Eviter listeners orphelins.
- Assurer cleanup on unmount/changement contexte.

---

## 15) Creation/suppression/duplication widgets

### Fonctions
- `createWidgetId`.
- `addWidget(payload)`.
- `duplicateWidget(id)`.
- `removeWidget(id)`.
- `removeSelectedWidgets()`.
- `resetLayout()`.

### Logique
1. Creation via registry (`type`, `view`, props defaut).
2. Duplication clone settings/position offset.
3. Suppression simple ou groupe.
4. Reset revient au `defaultLayout()`.

---

## 16) Couplages externes de `StatsCanvas.vue`
1. `WidgetFrame.vue` pour interaction widget unitaire.
2. `CanvasDock.vue` pour commandes.
3. `WidgetPalette.vue` pour ajout.
4. `WidgetSettingsModal.vue` pour edition.
5. `useCanvaCamera.ts` + `useCanvasShortcuts.ts`.
6. `widgetRegistry.js`.
7. `StatsServices.js` pour layout/categories/date bounds.

---

## 17) Zones risquées
1. Taille du fichier et responsabilites multiples.
2. Beaucoup d'etat mutable couplé (risque regressions).
3. Difficulté de test unitaire complet (forte dependance DOM/events).
4. Effets de bord entre autosave, remote save, et interactions pointer.

---

## 18) Refactor progressif propose
1. Extraire module `layoutPersistence`.
2. Extraire module `selectionEngine`.
3. Extraire module `dragResizeEngine`.
4. Extraire module `textWidgetSizing`.
5. Ajouter tests unitaires purs sur fonctions geometriques (snap, bounds).
6. Ajouter tests e2e focus sur multi-selection/group resize.
