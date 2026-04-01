# Repository map commente

Ce document cartographie les fichiers utiles du repository (hors `node_modules`, `dist`, `target`).

## 1) Racine
- `README.md`: notes de lancement rapide + resume fonctionnel historique.
- `docker-compose.yml`: compose principal (db + backend + frontend).
- `package.json`: dependances CSS/tooling racine.
- `package-lock.json`: lock racine.
- `patch_debug.txt`: note de patch hors code.
- `Features a ajouter stash.txt`: backlog informel.
- `frontend.zip`: archive hors build pipeline.
- `dump16012026.dump`, `snkProjet_docker.dump`: dumps base.
- `uploads/...`: stockage local des pieces jointes uploades.

## 2) Dossier `.github`
- `.github/appmod/appcat`: repertoire present mais non structure CI exploitable constatee.

## 3) Backend (`backend/`)

### 3.1 Config/build
- `backend/pom.xml`: dependances Maven + plugins build.
- `backend/Dockerfile`: image backend multi-stage.
- `backend/mvnw`, `backend/mvnw.cmd`: wrappers Maven.
- `backend/.env`: variables locales sensibles (non versionnees en theorie).
- `backend/.gitignore`, `.gitattributes`: hygiene git backend.
- `backend/HELP.md`: template Spring genere.
- `backend/.mvn/wrapper/maven-wrapper.properties`: version Maven wrapper.

### 3.2 Source Java principal
- `backend/src/main/java/backend/BackendApplication.java`

#### `config/`
- `AppConfig.java`
- `CacheConfig.java`
- `FlywayConfig.java`
- `StripeProperties.java`

#### `controller/`
- `AuthController.java`
- `BillingController.java`
- `DiscordController.java`
- `GlobalExceptionHandler.java`
- `HealthController.java`
- `snkVenteController.java`
- `StatsController.java`

#### `dto/`
- `AttachmentDto.java`
- `BillingStatusResponse.java`
- `ChangePasswordRequest.java`
- `CheckoutRequest.java`
- `CheckoutResponse.java`
- `DiscordEligibilityResponse.java`
- `DiscordLinkRequest.java`
- `EmailVerificationRequest.java`
- `ForgotPasswordRequest.java`
- `LoginRequest.java`
- `LoginResponse.java`
- `RegisterRequest.java`
- `ResetPasswordRequest.java`
- `SnkVenteCreateDto.java`
- `SnkVenteImportDto.java`
- `StatsBreakdownResponse.java`
- `StatsDateBoundsResponse.java`
- `StatsKpiResponse.java`
- `StatsLabelValueResponse.java`
- `StatsLayoutRequest.java`
- `StatsLayoutResponse.java`
- `StatsPointResponse.java`
- `StatsSeriesPointResponse.java`
- `StatsSummaryResponse.java`
- `TopVenteProjection.java`
- `UserMapper.java`
- `UserMeResponse.java`

#### `entity/`
- `Attachment.java`
- `DiscordAllowedGuild.java`
- `EmailVerificationToken.java`
- `ItemType.java`
- `PasswordResetToken.java`
- `SnkVente.java`
- `User.java`
- `UserStatsLayout.java`

#### `repository/`
- `AttachmentRepository.java`
- `DiscordAllowedGuildRepository.java`
- `EmailVerificationTokenRepository.java`
- `PasswordResetTokenRepository.java`
- `SnkVenteRepository.java`
- `UserRepository.java`
- `UserStatsLayoutRepository.java`

#### `security/`
- `JwtAuthFilter.java`
- `JwtService.java`
- `OAuth2SuccessHandler.java`
- `RetryingTokenResponseClient.java`
- `SecurityConfig.java`

#### `service/`
- `AttachmentService.java`
- `BillingService.java`
- `DiscordAccessService.java`
- `EmailVerificationService.java`
- `FileStorageService.java`
- `PasswordResetService.java`
- `snkVenteService.java`
- `StatsCacheKeys.java`
- `StatsLayoutService.java`
- `StatsService.java`
- `UserService.java`

### 3.3 Resources backend
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/compose.yaml`
- `backend/src/main/resources/ss.sql` (vide)

#### Flyway migrations
- `V1__init.sql`
- `V2__smoke_test.sql`
- `V3__add_phone_to_users.sql`
- `V4__drop_flyway_smoke_test.sql`
- `V5__add_indexes_tableauventes.sql`
- `V6__fixImportCsV.sql`
- `V7__password_reset_tokens.sql`
- `V8__email_verification_tokens.sql`
- `V9__user_stats_layouts.sql`
- `V10__billing_columns.sql`
- `V11__discord_allowed_guild.sql`
- `V12__seed_discord_allowed_guild.sql`
- `V13__update_discord_role.sql`
- `V14__typed_items_and_attachments.sql`

### 3.4 Tests backend
- `backend/src/test/java/backend/BackendApplicationTests.java`
- `backend/src/test/java/backend/controller/BillingControllerTest.java`
- `backend/src/test/java/backend/controller/HealthControllerTest.java`
- `backend/src/test/java/backend/controller/StatsControllerTest.java`
- `backend/src/test/java/backend/service/SnkVenteServiceImportTest.java`

## 4) Frontend (`frontend/`)

### 4.1 Config/build/outillage
- `frontend/package.json`
- `frontend/package-lock.json`
- `frontend/README.md`
- `frontend/vite.config.ts`
- `frontend/eslint.config.ts`
- `frontend/postcss.config.js`
- `frontend/tailwind.config.js`
- `frontend/tsconfig.json`
- `frontend/tsconfig.app.json`
- `frontend/tsconfig.node.json`
- `frontend/jsconfig.json`
- `frontend/index.html`
- `frontend/env.d.ts`
- `frontend/.env`
- `frontend/.env.local`
- `frontend/.env.production`
- `frontend/.editorconfig`
- `frontend/.prettierrc.json`
- `frontend/.gitignore`
- `frontend/.gitattributes`
- `frontend/.vscode/settings.json`
- `frontend/.vscode/extensions.json`
- `frontend/public/logo.png`
- `frontend/jsco` (fichier vide)

### 4.2 Source frontend (`frontend/src`)
- `App.vue`
- `main.js`

#### assets
- `assets/base.css`
- `assets/main.css`

#### components (racine)
- `AcceuilDernierItem.vue`
- `AcceuilWidgetLateral.vue`
- `AuthForm.vue`
- `DateRangeBar.vue`
- `HeaderDePage.vue`
- `StatBadge.vue`
- `StatBase.vue`

#### components/ui
- `ui/CompactDateInput.vue`

#### components/gestion
- `CsvImportExportWidget.vue`
- `GestionAfficherTout.vue`
- `GestionAjoutPaire.vue`
- `GestionBlocBoutonAddDelete.vue`
- `GestionBoutonOnAdd.vue`
- `GestionModifierItem.vue`
- `GestionRésumeStock.vue`
- `GestionSearchBarre.vue`
- `GestionSupprimerModal.vue`

#### components/stats
- `StatsCanvas.vue`
- `StatsCanvas.css`
- `WidgetPalette.vue`
- `widgetRegistry.js`
- `WidgetSettingsModal.vue`

##### components/stats/canvas
- `CanvasDock.vue`
- `useCanvaCamera.ts`
- `useCanvasShortcuts.ts`
- `WidgetFrame.vue`

##### components/stats/palette
- `paletteUtils.ts`
- `types.ts`
- `widgetPaletteMeta.ts`
- `WidgetPaletteCard.vue`
- `WidgetPaletteFilterChips.vue`
- `WidgetPaletteSearchBar.vue`
- `WidgetPreview.vue`

##### components/stats/widgets
- `ActiveListingsWidget.vue`
- `AspWidget.vue`
- `AvgDaysToSellWidget.vue`
- `AvgMarginWidget.vue`
- `BrandsWidget.vue`
- `CashFlowWidget.vue`
- `DeathPileWidget.vue`
- `GrossRevenueWidget.vue`
- `InventoryValueWidget.vue`
- `NetProfitWidget.vue`
- `OpexWidget.vue`
- `PlatformSplitWidget.vue`
- `ReturnRateWidget.vue`
- `RoiWidget.vue`
- `SellThroughWidget.vue`
- `TextBlockWidget.vue`
- `TextSectionWidget.vue`
- `TextTitleWidget.vue`
- `TopProfitDriversWidget.vue`
- `TopSalesWidget.vue`
- `TypeMixWidget.vue`

##### components/stats/widgets/_parts
- `ChartCard.vue`
- `KpiCard.vue`
- `Sparkline.vue`
- `WidgetCard.vue`

#### composables
- `useStatsDashboard.js`
- `useStatsRange.js`
- `useTheme.js`

#### constants
- `itemTypes.js`

#### layout
- `layoutPages.vue`

#### lib
- `echarts.js`

#### pages
- `AuthPage.vue`
- `ForgotPasswordPage.vue`
- `ResetPasswordPage.vue`
- `VerifyEmailPage.vue`
- `authCallbackPage.vue`
- `homePage.vue`
- `gestionPage.vue`
- `statsPage.vue`
- `accountPage.vue`
- `aboPage.vue`
- `aboViewPage.vue`

#### router
- `router/index.js`

#### services
- `api.js`
- `AuthService.js`
- `BillingService.js`
- `SnkVenteServices.js`
- `statsAdapters.js`
- `StatsServices.js`

#### store
- `authStore.js`
- `billingStore.js`

#### utils
- `formatters.js`
- `snkVente.js`

### 4.3 Tests frontend
- `frontend/tests/statsServices.test.js`
- `frontend/tests/widgetPalette.utils.test.ts`

## 5) Fichiers hors execution applicative mais importants en reprise
1. `uploads/...`: fichiers utilisateurs reels, impact RGPD / sauvegarde.
2. `backend/.env`: secrets locaux sensibles.
3. `docker-compose.yml` et `backend/src/main/resources/compose.yaml`: credentials en clair.
4. `frontend.zip`, dumps SQL: artefacts a classifier (backup? archive? obsolete?).

## 6) Note de confiance (completude)
- Cartographie realisee par scan complet du repository hors dependances generees (`node_modules`, `dist`, `target`).
- Si de nouveaux fichiers apparaissent apres cette date, ce document doit etre regenére.
