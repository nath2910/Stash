# Dossier backend/src/main/java/backend/

## 1) `BackendApplication.java`
- Point d'entree Spring Boot.
- Active `@EnableConfigurationProperties(StripeProperties.class)`.

## 2) package `config/`

### `AppConfig.java`
- Declare bean `PasswordEncoder` (`BCryptPasswordEncoder`).
- Utilise par `UserService` et `PasswordResetService`.

### `CacheConfig.java`
- Construit `CacheManager` Caffeine.
- Cache cible principal: `statsQueries`.

### `FlywayConfig.java`
- Strategie migration: `repair` puis `migrate`.
- Evite blocages en cas checksum drift mineur.

### `StripeProperties.java`
- DTO de config `stripe.*` (secret key, priceId, webhookSecret, success/cancel URL).

## 3) package `security/`

### `SecurityConfig.java`
- Configure `SecurityFilterChain`.
- Desactive CSRF (API JSON + JWT).
- Active CORS.
- Session policy `IF_REQUIRED` (utile OAuth).
- Defini routes publiques:
  - `/auth/register`, `/auth/login`, reset/verify, `/health`, `/ping`, `/oauth2/**`, `/login/**`, `/billing/webhook`, `/error`.
- Tout le reste: authentifie.
- Branche OAuth2 success handler + token client avec retry 429.
- Branche `JwtAuthFilter` avant `UsernamePasswordAuthenticationFilter`.

### `JwtService.java`
- `generateToken(userId)`.
- `extractUserId(token)`.
- `isValid(token)`.
- Le `sub` JWT stocke l'id user.

### `JwtAuthFilter.java`
- Lit header `Authorization`.
- Si token valide, charge user DB et injecte authentication.
- Ignore preflight `OPTIONS`.

### `OAuth2SuccessHandler.java`
- Cas Google:
  - mappe account provider vers user local.
  - complete nom/photo/emailVerified.
- Cas Discord:
  - verifie eligibilite guild/role via `DiscordAccessService` avant persistance.
  - mappe/cree user Discord.
  - force `subscriptionStatus=active` si login Discord eligible.
- Dans les deux cas: genere JWT et redirige frontend avec hash token.

### `RetryingTokenResponseClient.java`
- Wrap du client OAuth default.
- Retry unique apres 429 (Discord), avec `Retry-After` ou fallback 1s.

## 4) package `controller/`

### `AuthController.java`
Endpoints:
- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/change-password`
- `POST /auth/forgot-password`
- `POST /auth/reset-password`
- `GET /auth/verify-email`
- `POST /auth/resend-verification`
- `GET /auth/me`
- `DELETE /auth/me`

### `BillingController.java`
Endpoints:
- `GET /billing/status`
- `POST /billing/checkout`
- `POST /billing/webhook`

### `DiscordController.java`
Endpoints:
- `POST /discord/link`
- `GET /discord/check`

### `HealthController.java`
Endpoints:
- `GET /health`
- `GET /ping`

### `snkVenteController.java`
Endpoints:
- CRUD principal `snkVente`.
- `recent`, `total`, `ca`, `marque`, `topVentes`.
- `bulk-delete`, `import`.
- routes attachments list/upload/delete/download.

### `StatsController.java`
Endpoints:
- `summary`, `timeseries`, `brands`, `top-sales/topVentes`.
- `kpi/{metric}`, `series/{metric}`, `breakdown/{metric}`, `rank/{metric}`.
- `categories`, `date-bounds`, `layout` (GET/PUT).

### `GlobalExceptionHandler.java`
- Formate erreurs `ResponseStatusException`.
- Fallback generic 500 JSON.

## 5) package `service/`

### `UserService.java`
- Register/login/change password.
- Delete account en cascade manuelle (tokens, layout, ventes, user).

### `PasswordResetService.java`
- Genere tokens reset.
- Envoie email reset.
- Consomme token et met a jour mot de passe.

### `EmailVerificationService.java`
- Envoi email verification.
- Verification token + activation compte.
- Cleanup planifie des tokens expires/utilises.

### `snkVenteService.java`
- CRUD metier de `SnkVente`.
- Controle ownership user.
- Import bulk + sanitation metadata par type.
- Invalidation cache stats sur ecritures.

### `StatsService.java`
- Service analytique central.
- Normalise periodes.
- Compose metriques KPI/series/rank/breakdown.
- Applique cache par metrique/filtres.

### `StatsLayoutService.java`
- Lit/ecrit layout JSON par user.

### `StatsCacheKeys.java`
- Generation centralisee de cles cache deterministes.

### `BillingService.java`
- Creation customer Stripe.
- Creation checkout + portal.
- Traitement webhook.
- Rafraichit statut subscription.

### `DiscordAccessService.java`
- Interroge API Discord bot pour membership/roles.
- Determine eligibilite premium.

### `AttachmentService.java`
- CRUD metadonnees attachments.
- Delegue stockage physique a `FileStorageService`.
- Validation MIME/type selon `ItemType`.

### `FileStorageService.java`
- Sauvegarde disque locale sous root configure.
- Protection traversal path.
- Chargement `Resource` et suppression.

## 6) package `repository/`

### `SnkVenteRepository.java`
- Interface la plus critique et la plus dense.
- Melange JPQL + SQL natif pour analytics.
- Fournit projections pour:
  - summaries,
  - timeseries,
  - metriques type/brand/category,
  - top sales.

### Repositories secondaires
- `UserRepository`: lookup email/provider/stripe.
- `AttachmentRepository`: attachments par item/user.
- `PasswordResetTokenRepository`, `EmailVerificationTokenRepository`: gestion tokens.
- `UserStatsLayoutRepository`: layout stats par user.
- `DiscordAllowedGuildRepository`: allowlist Discord.

## 7) package `entity/`

### Entites coeur
- `User`.
- `SnkVente`.
- `Attachment`.

### Entites support
- `PasswordResetToken`.
- `EmailVerificationToken`.
- `UserStatsLayout`.
- `DiscordAllowedGuild`.
- `ItemType` (enum type item).

## 8) package `dto/`
- Contrats d'entree/sortie API.
- Melange `record` Java et classes POJO.
- `UserMapper` convertit entite User -> DTO exposable (`UserMeResponse`).

## 9) Conseils de reprise Java
1. Commencer par `StatsController` -> `StatsService` -> `SnkVenteRepository`.
2. Puis lire `SecurityConfig` + `JwtAuthFilter` + `OAuth2SuccessHandler`.
3. Ensuite `snkVenteService` pour logique CRUD/import.
