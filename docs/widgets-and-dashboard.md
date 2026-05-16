# Widgets and dashboard

## Vue d'ensemble
Le dashboard stats est un canvas libre. Les widgets sont ajoutes via la palette, deplaces/redimensionnes sur le canvas et sauvegardes localement puis cote backend.

## Fichiers clefs
- `components/stats/StatsCanvas.vue`: orchestration globale.
- `components/stats/StatsCanvas.css`: styles canvas, selection, guides, rail.
- `components/stats/widgetRegistry.js`: catalogue et schema des widgets.
- `components/stats/WidgetPalette.vue`: recherche et ajout de widgets.
- `components/stats/WidgetSettingsModal.vue`: edition des props.
- `components/stats/canvas/WidgetFrame.vue`: frame interactive.
- `components/stats/canvas/useCanvaCamera.ts`: camera Panzoom.
- `components/stats/canvas/useCanvasShortcuts.ts`: raccourcis.
- `components/stats/widgets/_parts/`: composants UI communs.

## Contrat widget
Chaque widget actif doit etre declare dans `widgetRegistry.js` avec:
- `type`;
- `title`;
- `category`;
- composant async;
- `forms`/variantes;
- `defaultSize` et `minSize`;
- `defaultProps`;
- `settings` si editable.

## Donnees
Les widgets stats appellent `StatsServices` et normalisent les reponses avec `statsAdapters.js`. Les filtres standards sont `from`, `to`, `categories`, `types` et parfois `bucket`.

## Contraintes de maintenance
- Ne pas ajouter un fichier widget sans l'enregistrer dans `widgetRegistry.js`.
- Ne pas modifier les tailles par defaut sans verifier le rendu canvas.
- Toute modification drag/resize/zoom doit etre testee manuellement sur desktop et mobile.
- Les templates doivent rester compatibles avec l'etat runtime de `StatsCanvas`.
