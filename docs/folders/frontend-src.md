# Dossier frontend/src/

## 1) Boot application

### `main.js`
- Cree l'app Vue.
- Enregistre router.
- Enregistre plugin motion.
- Enregistre `VChart` global.

### `App.vue`
- Conteneur principal.
- Rend `layoutPages` selon route.
- Gere key de transition/navigation via `route.fullPath`.

## 2) Routing et layout

### `router/index.js`
- Declare routes lazy-load.
- Defini routes publiques.
- Guard globale:
  - detecte expiration token JWT,
  - hydrate user via `/auth/me` si besoin,
  - applique gating auth,
  - applique gating abonnement actif (sauf routes autorisees).

### `layout/layoutPages.vue`
- Navigation desktop/mobile.
- Theme-aware styles.
- Idle timeout auto-logout.
- Menus user + actions login/logout/account.

## 3) Stores

### `store/authStore.js`
- Singleton reactivite auth (`user`, `token`).
- hydrate depuis storage.
- `setAuth`, `setToken`, `setUser`, `logout`.

### `store/billingStore.js`
- Singleton statut abonnement.
- `fetchStatus` avec anti-double appel inflight.
- `reset`.

## 4) Services API

### `services/api.js`
- Axios instance central.
- Base URL depuis `VITE_API_URL` ou `VITE_API_BASE_URL`.
- Interceptor request: inject JWT.
- Interceptor response: purge auth + redirect login sur 401/403 (hors endpoints auth publics).

### `services/AuthService.js`
- register/login/logout.
- forgot/reset password.
- verify/resend email.
- `me` et `deleteAccount`.

### `services/BillingService.js`
- wrappers `/billing/status`, `/billing/checkout`.

### `services/SnkVenteServices.js`
- wrappers CRUD stock + import + attachments.

### `services/StatsServices.js`
- wrappers endpoints stats.
- cache TTL local + dedup requetes en vol.
- normalisation params range/categories/types.
- endpoints layout stats (GET/PUT).

### `services/statsAdapters.js`
- Transforme payload brut backend en structures frontend stables.
- Evite crash UI sur donnees partielles.

## 5) Composables

### `composables/useTheme.js`
- Theme dark/light global.
- persistence storage + listener cross-tab.

### `composables/useStatsRange.js`
- Gere plage date stats par utilisateur.
- persistance storage user-scoped.

### `composables/useStatsDashboard.js`
- Ancien orchestrateur stats "dashboard classique".
- charge summary/timeseries/brands/topSales + deltas.

## 6) Pages

### Auth
- `pages/AuthPage.vue`: shell page auth (compose `AuthForm`).
- `pages/authCallbackPage.vue`: recupere token OAuth hash.
- `pages/ForgotPasswordPage.vue`: demande reset.
- `pages/ResetPasswordPage.vue`: reset via token.
- `pages/VerifyEmailPage.vue`: verification email + renvoi.

### Metier
- `pages/homePage.vue`: apercu rapide KPIs et dernieres ventes.
- `pages/gestionPage.vue`: liste, filtres, edition, suppression, import/export.
- `pages/statsPage.vue`: monte `StatsCanvas`.
- `pages/accountPage.vue`: changement mot de passe + suppression compte.
- `pages/aboPage.vue`: souscription stripe.
- `pages/aboViewPage.vue`: vue statut abonnement.

## 7) Components globaux
- `components/AuthForm.vue`: login/signup complet + OAuth buttons.
- `components/AcceuilDernierItem.vue`: table derniers items.
- `components/AcceuilWidgetLateral.vue`: cartes KPI laterales home.
- `components/DateRangeBar.vue`: selecteur plage date classique.
- `components/StatBadge.vue`: badge KPI reusable.
- `components/StatBase.vue`: composant legacy stats ancien.
- `components/HeaderDePage.vue`: titre/sous-titre standard pages.
- `components/ui/CompactDateInput.vue`: input date compact custom.

## 8) Assets et utilitaires

### `assets/base.css`, `assets/main.css`
- styles globaux, design tokens, themes.

### `constants/itemTypes.js`
- enum types item + mapping metadata fields.

### `utils/formatters.js`
- format EUR/number/percent/date FR.

### `utils/snkVente.js`
- helpers metier lecture champs snake/camel + calcul profit.

### `lib/echarts.js`
- enregistrement modules ECharts utilises.

## 9) Suites dediees
- Module stats detaille: `docs/folders/frontend-stats.md`.
- Module gestion detaille: `docs/folders/frontend-gestion.md`.
