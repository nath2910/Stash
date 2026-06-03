# Design system frontend

## Direction commune
L'application privilégie une interface métier claire, sobre et lisible au quotidien. La référence visuelle actuelle est le shell clair des pages `homePage.vue` et `gestionPage.vue` : surfaces blanches légèrement teintées, bordures cyan/teal discrètes, ombres douces, textes slate, actions primaires teal.

À éviter pour les nouvelles pages :
- palettes très sombres hors canvas stats ou auth ;
- variantes de boutons recréées localement sans besoin ;
- cartes imbriquées ;
- statuts avec une couleur différente selon la page.

## Tokens globaux
Les tokens applicatifs sont dans `frontend/src/assets/main.css` :
- couleurs : `--app-color-page`, `--app-color-surface`, `--app-color-border`, `--app-color-text`, `--app-color-primary`, `--app-color-success`, `--app-color-warning`, `--app-color-danger` ;
- rayons : `--radius-sm`, `--radius-md`, `--radius-lg`, `--radius-xl` ;
- ombres : `--app-shadow-card`, `--app-shadow-soft` ;
- espacements : `--space-xs`, `--space-sm`, `--space-md`, `--space-lg`.

## Classes communes
Classes globales disponibles :
- `app-card` pour une surface métier standard ;
- `app-panel-heading` et `app-eyebrow` pour les headers de panneaux ;
- `app-button app-button--primary|secondary|ghost|danger` pour les actions ;
- `app-status-badge app-status-badge--success|warning|danger|info|neutral` pour les statuts.

Les composants existants peuvent garder leurs styles scoped si le rendu est très spécifique, mais les nouvelles pages doivent partir de ces classes ou des composants existants.

## Statuts
La source de vérité des labels/couleurs est `frontend/src/constants/statuses.js`.

Utiliser `getStatusConfig`, `getStatusClass`, `getStatusLabel` ou `normalizeItemStatus` au lieu de réinventer les labels `Vendu`, `En stock`, `À compléter`, `À vérifier`, etc.
