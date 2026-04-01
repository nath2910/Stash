# Responsive Audit - SNK V5

Date: 2026-04-02

## Scope audite
- Pages: `home`, `gestion`, `account`, `abo`, `abo-view`, `auth`, `forgot-password`, `reset-password`, `verify-email`
- Composants critiques: table gestion, modales CRUD gestion, widgets resume, date pickers
- Contrainte prioritaire: ameliorer le responsive sans toucher aux comportements existants du layout global (header/footer, effets scroll, transitions)

## Problemes detectes
- Classes Tailwind invalides sur les pages auth secondaires (`min-h-scr...`) causant des hauteurs non fiables.
- Wrappers auth avec `overflow-hidden` vertical, risquant du clipping sur mobile.
- Page `abo-view` en `h-screen` (viewport mobile parfois tronque).
- Table gestion uniquement orientee desktop: lecture difficile sur petit ecran.
- Modales gestion trop desktop-first: actions serrees, ergonomie mobile limitee, hauteur fixe.
- Zone d actions gestion (ajout/suppression) trop compacte sur mobile.
- Widgets de resume en 2 colonnes forcees sur tres petits ecrans.
- Date picker avec largeur minimale trop rigide.

## Decisions structurelles
- Preservation stricte du layout global: aucune modification de `layoutPages.vue` et aucun changement de logique header/footer.
- Strategie mobile ciblee:
  - conserver le rendu desktop existant;
  - ajouter des adaptations uniquement sous les breakpoints mobiles/tablette.
- Pattern table responsive pour gestion:
  - desktop: table existante conservee;
  - mobile: cartes denses avec les memes actions et infos prioritaires.
- Pattern modal responsive unifie:
  - mobile: presentation en bottom sheet, scroll interne fiable, boutons empilables;
  - desktop: rendu centre inchange.

## Modifications appliquees

### Pages
- `frontend/src/pages/ForgotPasswordPage.vue`
  - `min-h-screen` corrige, `overflow-x-hidden` pour eviter le clipping vertical.
- `frontend/src/pages/ResetPasswordPage.vue`
  - meme correction que forgot password.
- `frontend/src/pages/VerifyEmailPage.vue`
  - `min-h-screen` corrige.
- `frontend/src/pages/aboViewPage.vue`
  - passage a `min-h-[100dvh]` pour fiabiliser le viewport mobile.
- `frontend/src/pages/gestionPage.vue`
  - header d actions plus souple sur mobile;
  - hauteur de zone liste adaptee (`max-h` responsive).
- `frontend/src/pages/accountPage.vue`
  - bloc action abonnement adapte en wrapping mobile.

### Composants gestion
- `frontend/src/components/gestion/GestionAfficherTout.vue`
  - ajout d une variante mobile en cartes (md hidden);
  - conservation de la table desktop (hidden md:block);
  - checkboxes, edition et infos metier conserves.
- `frontend/src/components/gestion/GestionAjoutPaire.vue`
  - modal mobile en bottom sheet, desktop conserve;
  - espacements et footer actions responsive;
  - gestion safe-area + scroll interne.
- `frontend/src/components/gestion/GestionModifierItem.vue`
  - meme refactor responsive que la modale d ajout.
- `frontend/src/components/gestion/GestionSupprimerModal.vue`
  - bottom sheet mobile, actions empilables, scroll interne.
- `frontend/src/components/gestion/GestionBlocBoutonAddDelete.vue`
  - largeur responsive de conteneur d actions.
- `frontend/src/components/gestion/GestionBoutonOnAdd.vue`
  - bouton plein largeur sur mobile, auto sur desktop.
- `frontend/src/components/gestion/GestionResumeStock.vue`
  - grille mobile assouplie (1 col tres petit ecran -> 2 -> 4).

### Composants transverses
- `frontend/src/components/AcceuilWidgetLateral.vue`
  - grille resume assouplie sur tres petits ecrans.
- `frontend/src/components/DateRangeBar.vue`
  - boutons wrap mobile;
  - resume de periode en pleine largeur sur mobile;
  - `min-width` du datepicker passe en `clamp(...)`.
- `frontend/src/components/ui/CompactDateInput.vue`
  - largeur du menu datepicker contrainte au viewport disponible.

## Non regression / preservation explicite
- Aucun changement dans:
  - `frontend/src/layout/layoutPages.vue`
  - logique d apparition/disparition header/footer
  - animations et transitions globales du layout
- Aucun redesign desktop volontaire.
- Les modifications touchent surtout mobile/tablette et restent localisees.

## Verification
- Build frontend valide:
  - `npm run build` (dans `frontend/`)

## Points a surveiller
- Les modales gestion sont maintenant plus robustes sur mobile; verifier sur appareils reels iOS Safari et Chrome Android (clavier ouvert + scroll).
- La table gestion mobile est en cartes; si des colonnes metier supplementaires sont ajoutees, garder cette priorisation (infos clés + actions visibles sans horizontal scroll).
