# Frontend - deep dive `WidgetFrame.vue`

## 1) Role du fichier
`WidgetFrame.vue` est la coque interactive d'un widget dans le canvas.

Responsabilites:
1. barre d'actions widget,
2. drag/resize handles,
3. edition texte inline pour widgets texte,
4. fullscreen,
5. auto-mesure/auto-resize,
6. emissions d'evenements vers `StatsCanvas`.

---

## 2) Props et emits

### Props typiques
- `widget` (model complet).
- `selected`, `editMode`, `themeMode`.
- dimensions base/current.
- flags UI divers.

### Emits majeurs
- `dragStart`.
- `resizeStart`.
- `duplicate`.
- `openSettings`.
- `remove`.
- `updateText` / `updateTextProp`.
- `fullscreenChange`.
- `autoResize`.

Pedagogie Vue:
- Le composant ne modifie pas directement l'etat global canvas.
- Il emet des events; le parent decide quoi faire.

---

## 3) Calculs de style texte
Fonctions:
- `resolveTextFontSize`.
- `resolveTextLineHeight`.
- `resolveTextWeight`.

But:
- convertir props texte en valeurs CSS stables.
- garder rendu coherent selon dimensions widget.

---

## 4) Actions de barre widget
Fonctions:
- `toggleActionMenu`.
- `closeActionMenu`.
- `emitDuplicateWidget`.
- `emitOpenSettings`.
- `emitRemoveWidget`.

Note:
- Menu actions se ferme si clic hors composant (`onWindowPointerDown`).

---

## 5) Edition texte inline (zone la plus subtile)

### Fonctions
- `startInlineTextEdit`.
- `syncInlineEditorContent`.
- `readInlineEditorContent`.
- `onInlineEditorInput`.
- `onInlineEditorPaste`.
- `onInlineEditorBlur`.
- `onInlineEditorKeydown`.
- `stopInlineTextEdit`.

### Logique
1. Passe en mode edition `contenteditable`.
2. Synchronise texte existant dans l'editor DOM.
3. Sur input/paste, nettoie contenu.
4. Sur blur ou Enter/Escape, finalise edition et emet update.

Pedagogie JS:
- `contenteditable` ne fonctionne pas comme `<input>`.
- Il faut lire/normaliser `textContent` manuellement.

---

## 6) Fullscreen widget
Fonctions:
- `enterFullscreen`.
- `exitFullscreen`.
- `toggleFullscreen`.

Logique:
1. Ajoute classes/etat pour afficher widget en superposition plein ecran.
2. Informe parent via emit pour ajuster interactions globales.

---

## 7) Auto-mesure et auto-resize

### Fonctions
- `measureWidgetSize`.
- `scheduleWidgetSizeMeasure`.
- `ensureWidgetSizeObserver`.
- `clearWidgetSizeObserver`.
- `ensureAutoResizeObservers`.
- `scheduleAutoResize`.
- `clearAutoResizeObservers`.

### Mecanismes
- `ResizeObserver` pour variation taille.
- `MutationObserver` pour variation contenu.
- `requestAnimationFrame` pour throttling.

### Pourquoi
Les widgets texte et widgets dynamiques changent de contenu sans resize manuel.

---

## 8) Gestion clavier locale
Fonctions:
- `onKeydown`.
- `onRootKeydown`.

Raccourcis locaux:
- suppression,
- duplication,
- fermeture menus,
- edition texte.

---

## 9) Couplage avec `StatsCanvas`
`WidgetFrame` est un "agent local" du canvas:
1. il capte les evenements bas niveau (pointer/keyboard DOM),
2. il transmet intention au parent,
3. le parent met a jour l'etat global widgets/layout.

Ce decouplage limite les effets de bord mais reste complexe a suivre vu le volume.

---

## 10) Risques
1. Beaucoup de chemins evenementiels (pointer + clavier + observer).
2. `contenteditable` est fragile cross-browser.
3. Fullscreen + drag/resize peuvent entrer en conflit si nettoyage listeners incomplet.

---

## 11) Reco reprise
1. Ajouter tests e2e specifiques edition texte inline.
2. Verifier cleanup observers/listeners sur unmount.
3. Eventuellement extraire logiques:
- `useInlineTextEditor`,
- `useWidgetAutoResize`,
- `useWidgetActions`.
