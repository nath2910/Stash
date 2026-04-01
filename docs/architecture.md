# Architecture technique

## 1) Vue d'ensemble
Le systeme est compose de 3 briques principales:
1. Frontend SPA Vue (`frontend/`).
2. Backend API Spring Boot (`backend/`).
3. PostgreSQL (schema gere par Flyway).

Services externes:
- Stripe (billing checkout/portal/webhook).
- OAuth Google + OAuth Discord.
- SMTP (emails de reset password et verification email).
- API Discord (verification d'eligibilite abonnement via guild/role).

---

## 2) Architecture runtime

### 2.1 Frontend
Responsabilites:
1. Afficher l'UI.
2. Gerer session locale (token JWT + user snapshot).
3. Appliquer navigation protegee (router guard + statut billing).
4. Appeler API backend via Axios.
5. Orchestrer le dashboard stats (canvas libre + widgets).

Points d'entree frontend:
- `src/main.js`: bootstrap Vue + router + composants globaux chart.
- `src/App.vue`: shell principal + `router-view`.
- `src/router/index.js`: logique de navigation securisee.

### 2.2 Backend
Responsabilites:
1. Authentification (register/login/JWT/OAuth).
2. API metier stock/gestion.
3. API statistiques.
4. Billing Stripe.
5. Pieces jointes (upload/download/controle type).

Points d'entree backend:
- `backend/src/main/java/backend/BackendApplication.java`.
- Controllers REST sous `backend/controller/*`.

### 2.3 Base de donnees
- Tables principales: `users`, `tableauventes`, `attachments`.
- Tables secondaires: tokens reset/verification email, layout stats, Discord guild allowlist.
- Migrations versionnees sous `backend/src/main/resources/db/migration`.

---

## 3) Architecture backend par couches

### 3.1 Controllers (HTTP)
Role: convertir requetes HTTP en appels service, traduire HTTP -> objets Java et Gérer le contrat API.

Controllers principaux:
1. `AuthController`: register/login/password/email verification/me.
2. `snkVenteController`: CRUD stock + import + attachments.
3. `StatsController`: endpoints stats (summary, timeseries, kpi, series, rank, layout).
4. `BillingController`: status/checkout/webhook Stripe.
5. `DiscordController`: link/check Discord.
6. `HealthController`: `/health`, `/ping`.

### 3.2 Services (metier)
Role: logique metier, validation, orchestration repository/API externe. Coordonner DB + services externes.

Services structurants:
1. `UserService`.
2. `snkVenteService`.
3. `StatsService`.
4. `BillingService`.
5. `PasswordResetService`.
6. `EmailVerificationService`.
7. `AttachmentService` + `FileStorageService`.
8. `StatsLayoutService`.
9. `DiscordAccessService`.

### 3.3 Repositories (acces donnees)
- `SnkVenteRepository` concentre une grande partie des requetes SQL analytiques.
- Repositories simples JPA pour user/tokens/layout/attachments.

### 3.4 Security
- `SecurityConfig`: routes publiques/protegees, CORS, OAuth2, filtre JWT.
- `JwtAuthFilter`: lit `Authorization: Bearer ...`.
- `JwtService`: creation/validation/extraction token.
- `OAuth2SuccessHandler`: mapping OAuth vers utilisateur local + redirection frontend.

---

## 4) Architecture frontend par zones

### 4.1 Shell global
- `layout/layoutPages.vue`: layout principal, menu desktop/mobile, idle logout.
- `router/index.js`: garde d'auth + controle statut abonnement.
- `store/authStore.js` et `store/billingStore.js`: etat global simple via `ref` singleton.

### 4.2 Services frontend
- `services/api.js`: client Axios central + interceptors.
- `services/AuthService.js`, `BillingService.js`, `SnkVenteServices.js`, `StatsServices.js`.
- `services/statsAdapters.js`: normalisation defensive des payloads backend.

### 4.3 Pages metier
- `pages/homePage.vue`: KPIs rapides.
- `pages/gestionPage.vue`: inventory management.
- `pages/statsPage.vue`: canvas stats avancé.
- `pages/aboPage.vue` / `aboViewPage.vue`: subscription.
- `pages/AuthPage.vue` + pages reset/verify/callback OAuth.

### 4.4 Module stats canvas
Fichiers structurants:
1. `components/stats/StatsCanvas.vue` (orchestrateur principal).
2. `components/stats/canvas/WidgetFrame.vue` (conteneur widget interactif).
3. `components/stats/widgetRegistry.js` (catalogue widgets + schema settings).
4. `components/stats/WidgetPalette.vue` (selection widget).
5. `components/stats/WidgetSettingsModal.vue` (edition props widget).
6. `components/stats/widgets/*` (impl metrique par widget).
7. `components/stats/canvas/useCanvaCamera.ts` (pan/zoom).
8. `components/stats/canvas/useCanvasShortcuts.ts` (raccourcis clavier).

---

## 5) Flux de donnees principal

### 5.1 Auth locale
Frontend `AuthForm` -> `AuthService.login` -> `POST /auth/login` -> `UserService.login` -> JWT retour.

### 5.2 Auth OAuth
Bouton OAuth frontend -> `/oauth2/authorization/google|discord` backend -> provider externe -> `OAuth2SuccessHandler` -> redirect `.../auth/callback#token=...` -> frontend stocke token + appelle `/auth/me`.

### 5.3 CRUD stock
UI gestion -> `SnkVenteServices` -> `/snkVente/*` -> `snkVenteService` -> `SnkVenteRepository`.

### 5.4 Stats
Widgets -> `StatsServices` -> `/stats/*` -> `StatsService` -> `SnkVenteRepository` SQL -> JSON normalise via `statsAdapters` -> render UI.

### 5.5 Layout dashboard
`StatsCanvas` persiste:
1. localStorage immediate.
2. backend `/stats/layout` en differe (debounce).

---

## 6) Points d'entree execution

### 6.1 Frontend
- `src/main.js` monte l'application.
- Vue Router charge pages en lazy import.
- Les pages instancient composants metier.

### 6.2 Backend
- Spring Boot initialise beans.
- SecurityFilterChain applique auth/CORS.
- Controllers exposes endpoints.

---

## 7) Dependances techniques critiques

### 7.1 Backend
- Spring Boot Web, Security, OAuth2 client, Data JPA, Mail.
- jjwt (signature/parse JWT).
- Stripe SDK Java.
- Flyway.
- Caffeine cache.

### 7.2 Frontend
- Vue 3 + Vue Router.
- Axios.
- ECharts + vue-echarts.
- Panzoom.
- PapaParse + XLSX (import/export).

---

## 8) Mecanismes de cache

### 8.1 Backend
- `@Cacheable(cacheNames="statsQueries")` sur `StatsService`.
- Cle composee via `StatsCacheKeys` (userId + periode + filtres + metric).
- Invalidation complete via `@CacheEvict(allEntries=true)` sur operations ecriture stock.

### 8.2 Frontend
- `StatsServices.js`: petit cache memo TTL + dedup de requetes en vol.
- Objectif: limiter spam API lors de re-render widgets.

---

## 9) Logique de securite

### 9.1 Route publique vs protegee backend
Publiques: auth register/login/reset/verify, health/ping, oauth2 login, webhook stripe.

Protegees: tout le reste (`.anyRequest().authenticated()`).

### 9.2 Session
- JWT stateless pour API.
- Session IF_REQUIRED pour flux OAuth login.

### 9.3 CORS
- Origines pilotees par `app.cors.allowed-origins`.
- En profil `dev`, fallback `http://localhost:5173` si absent.

---

## 10) Donnees metier structurantes

### 10.1 Entite `SnkVente`
Champs pivots:
- identite item, prix retail/resell, dates achat/vente, categorie, type, metadata JSON.
- lien user obligatoire.

### 10.2 Entite `User`
Champs pivots:
- email/password/provider/providerId/emailVerified.
- billing Stripe (`stripeCustomerId`, `subscriptionStatus`).
- `discordId`.

### 10.3 Entite `UserStatsLayout`
- persistance JSON layout dashboard par utilisateur.

---

## 11) Couplages importants (a connaitre)
1. `StatsCanvas` <-> `widgetRegistry`: schema props/settings, taille widget, composant rendu.
2. `StatsService` <-> `SnkVenteRepository`: chaque metrique depend de SQL natif centralise.
3. `router/index.js` <-> `AuthService`/`billingStore`: gating navigation.
4. `BillingService` backend <-> webhook Stripe: etat abonnement.
5. `snkVenteService` <-> cache stats: toute ecriture invalide stats.

---

## 12) Dette technique structurelle
1. Tres gros fichiers frontend (StatsCanvas, WidgetFrame, WidgetSettingsModal) difficiles a maintenir.
2. `SnkVenteRepository` monolithique (beaucoup de SQL dans une interface unique).
3. Encodage de certains fichiers (accents/corruption) non homogène.
4. Credentials sensibles presents dans configs locales/compose (a assainir).

Voir details et impacts dans [files/risky-zones.md](files/risky-zones.md).
