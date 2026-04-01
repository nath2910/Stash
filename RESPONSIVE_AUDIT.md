# Responsive Audit - SNK V5

Date: 2026-04-01

## Scope audite
- Layout global et gestion full-bleed (`src/layout/layoutPages.vue`, `src/router/index.js`, `src/App.vue`)
- Pages principales: `home`, `gestion`, `account`, `abo`, `abo-view`, `auth-callback`, `forgot-password`, `reset-password`, `verify-email`
- Composants critiques UX: table/listing gestion, modales CRUD, import/export, blocs home, palette stats, modal settings stats, date inputs

## Problemes detectes
- Strategie `fullBleed` incoherente: overflow force en mode cache sur des pages qui ont besoin de scroll mobile.
- Double systeme de conteneur/padding entre layout et pages (densite visuelle irréguliere, marges incoherentes).
- Table de gestion uniquement desktop: lisibilite faible mobile.
- Modales CRUD avec structure desktop-first (actions serrees, hauteur fixe, scroll interne limite).
- Quelques contraintes rigides (`min-width`, `h-screen`, `overflow-hidden`) provoquant des cassures sur petits ecrans.
- Palette stats avec largeur minimale trop agressive sur tablette/petits laptops.

## Decisions structurantes appliquees
- Scroll `fullBleed` par defaut autorise (sauf routes explicites: stats + auth-callback).
- Harmonisation des wrappers et rythmes de page via utilitaires globaux.
- Normalisation des hauteurs viewport: `min-h-screen` + `min-h-[100dvh]`.
- Pattern table responsive: desktop en table, mobile en cartes denses et exploitables.
- Pattern modal responsive: ancrage bas mobile, centre desktop, `max-h` dynamique + scroll interne.
- Relaxation des `min-width` critiques sur palette stats et datepicker.

## Refactors responsives effectues
- Global CSS utilitaires: `src/assets/main.css`
- Layout/route/view:
  - `src/layout/layoutPages.vue`
  - `src/router/index.js`
  - `src/App.vue`
- Pages:
  - `src/pages/homePage.vue`
  - `src/pages/gestionPage.vue`
  - `src/pages/accountPage.vue`
  - `src/pages/aboPage.vue`
  - `src/pages/aboViewPage.vue`
  - `src/pages/authCallbackPage.vue`
  - `src/pages/ForgotPasswordPage.vue`
  - `src/pages/ResetPasswordPage.vue`
  - `src/pages/VerifyEmailPage.vue`
- Composants metier/UI:
  - `src/components/gestion/GestionAfficherTout.vue`
  - `src/components/gestion/GestionAjoutPaire.vue`
  - `src/components/gestion/GestionModifierItem.vue`
  - `src/components/gestion/GestionSupprimerModal.vue`
  - `src/components/gestion/CsvImportExportWidget.vue`
  - `src/components/gestion/GestionRésumeStock.vue`
  - `src/components/AcceuilDernierItem.vue`
  - `src/components/AcceuilWidgetLateral.vue`
  - `src/components/AuthForm.vue`
  - `src/components/DateRangeBar.vue`
  - `src/components/ui/CompactDateInput.vue`
- Stats overlays/modales:
  - `src/components/stats/WidgetPalette.vue`
  - `src/components/stats/WidgetSettingsModal.vue`

## Conventions a suivre pour la suite
- Eviter `h-screen` sur pages applicatives; preferer `min-h-[100dvh]`.
- Eviter les largeurs/hauteurs fixes sans fallback responsive.
- Pour toute table metier: prevoir une variante mobile (cartes ou colonnes prioritaires).
- Pour toute modal: `max-h` viewport + scroll interne + actions empilables mobile.
- Utiliser les utilitaires globaux (`app-page`, `app-stack`, `app-table-scroll`) avant d’ajouter des styles ad-hoc.
- Sur routes `fullBleed`, expliciter `allowScroll: false` uniquement quand necessaire (canvas/immersif).

## Zones a surveiller
- Le canvas stats est deja fortement specialise (pan/zoom, widgets), donc les futurs ajustements doivent rester compatbiles avec ses interactions.
- Quelques textes historiques presentent des accents non homogenes; c'est cosmetique mais a normaliser lors d'un pass contenu.

## Verification
- Build production valide:
  - `npm run build` (frontend)
