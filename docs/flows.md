# Flows (execution + donnees)

## 1) Convention de lecture
Dans ce document:
- "Frontend" = code Vue (navigateur).
- "Backend" = API Spring.
- "DB" = PostgreSQL.
- "Service" = couche metier backend.

Rappel pedagogique JS:
- Une fonction `async` retourne une *Promise*.
- `await` dit "attends la reponse avant de continuer".
- `ref(...)` en Vue cree une valeur reactive (`x.value`).
- `computed(...)` calcule automatiquement une valeur derivee reactive.
- `watch(...)` execute une action quand une valeur reactive change.

---

## 2) Flux Auth locale (email/mot de passe)

### 2.1 Login nominal
1. L'utilisateur soumet `AuthForm.vue` mode login.
2. `AuthService.login(payload)` appelle `POST /auth/login`.
3. `AuthController.login` delegue a `UserService.login`.
4. `UserService.login`:
- normalise email.
- charge user DB.
- verifie hash password via `PasswordEncoder.matches`.
5. `JwtService.generateToken(userId)` cree le JWT.
6. Backend repond `LoginResponse{user, token}`.
7. Frontend stocke token + user dans `localStorage` et `authStore`.
8. Router redirige vers `home`.

### 2.2 Controle permanent de session
1. `api.js` ajoute `Authorization: Bearer <token>` a chaque requete.
2. Backend `JwtAuthFilter` valide token et place `Authentication` dans `SecurityContext`.
3. `router/index.js` verifie expiration JWT cote client (decode `exp`).
4. En cas de 401/403 API, interceptor `api.js` purge storage puis redirige `/auth?reason=expired`.

---

## 3) Flux OAuth (Google / Discord)

### 3.1 Declenchement
1. Bouton OAuth dans `AuthForm.vue` ouvre URL backend (`/oauth2/authorization/google|discord`).
2. Spring Security lance redirection provider OAuth.

### 3.2 Retour provider
1. Provider retourne backend sur callback `/login/oauth2/code/...`.
2. `OAuth2SuccessHandler` mappe les donnees provider vers user local.
3. Handler genere JWT puis redirige vers frontend:
- `.../auth/callback#token=<jwt>`
4. `authCallbackPage.vue` lit hash, stocke token, appelle `/auth/me`, met a jour `authStore`, redirige `home`.

### 3.3 Cas Discord specifique
- `DiscordAccessService` verifie membership/role sur guild allowlist.
- Si non eligible: redirection avec `#error=discord_not_allowed`.

---

## 4) Flux inscription + verification email

### 4.1 Register
1. `AuthForm` mode signup -> `POST /auth/register`.
2. `UserService.register` cree user `provider=LOCAL`, `emailVerified=false`.
3. `EmailVerificationService.sendVerification` cree token + envoie email.
4. Frontend redirige vers `VerifyEmailPage`.

### 4.2 Verification
1. Lien email appelle frontend page verify avec token.
2. Front appelle `GET /auth/verify-email?token=...`.
3. Backend valide token, marque user verified, consomme token.
4. Backend renvoie `LoginResponse` (user + JWT).
5. Front stocke session et redirige `home`.

---

## 5) Flux reset mot de passe
1. `ForgotPasswordPage` -> `POST /auth/forgot-password`.
2. `PasswordResetService.requestReset` cree token + email.
3. Lien email -> page `ResetPasswordPage` avec token query.
4. Front -> `POST /auth/reset-password`.
5. Backend verifie token (existe, non expire, non utilise), change password, marque token utilise.

---

## 6) Flux billing Stripe

### 6.1 Lecture statut
1. Front `billingStore.fetchStatus()` -> `/billing/status`.
2. Backend `BillingController.status` appelle `BillingService.refreshStatus(user)`.
3. Si `includePortal=true`, backend cree URL billing portal Stripe.
4. Front met a jour store (`status`, `portalUrl`).

### 6.2 Checkout
1. `aboPage.vue` clique CTA -> `POST /billing/checkout`.
2. Backend cree checkout session Stripe.
3. Front redirige navigateur vers `session.url` Stripe.
4. Retour Stripe sur URL success/cancel frontend.
5. `aboPage` refetch status et peut rediriger vers `home`.

### 6.3 Webhook
1. Stripe envoie event sur `/billing/webhook`.
2. Backend valide signature.
3. `BillingService.handleWebhook` sync `subscriptionStatus` user.

---

## 7) Flux CRUD stock (gestion)

### 7.1 Chargement liste
1. `gestionPage.vue` `onMounted` -> `SnkVenteServices.getSnkVente()`.
2. Backend `snkVenteController.rechercher` -> `snkVenteService.rechercherParUser`.
3. Requete JPA par `user_id` + tri date achat.

### 7.2 Creation item
1. `GestionAjoutPaire.vue` construit payload.
2. `SnkVenteServices.ajouter` -> `/snkVente/add`.
3. Backend valide proprietaire + nettoie metadata selon `ItemType`.
4. Save DB.
5. Cache stats backend invalide (`@CacheEvict`).

### 7.3 Edition / suppression / bulk delete
- Edition: `PUT /snkVente/{id}`.
- Suppression unitaire: `DELETE /snkVente/{id}`.
- Suppression masse: `POST /snkVente/bulk-delete`.
- Toutes ces actions invalident cache stats.

### 7.4 Pieces jointes
1. Upload via `multipart/form-data`.
2. `AttachmentService` controle ownership + type MIME + taille.
3. `FileStorageService` stocke fichier sous `uploads/<user>/<item>/...`.
4. Metadonnees fichier en table `attachments`.
5. Download supprime le couplage direct au path disque (acces via endpoint protege).

---

## 8) Flux import CSV/XLSX

### 8.1 Parsing frontend
`CsvImportExportWidget.vue`:
1. Detecte extension fichier.
2. Parse CSV (PapaParse) ou Excel (XLSX).
3. Detecte colonnes via synonymes (`findHeader`).
4. Normalise valeurs (dates, nombres, metadata).
5. Construit payload `SnkVenteImportDto[]`.

### 8.2 Validation backend
`snkVenteService.importBulk`:
1. Refuse payload vide.
2. Refuse > `MAX_IMPORT_ITEMS` (500).
3. Ignore lignes invalides.
4. Applique sanitation metadata selon type.
5. SaveAll DB + renvoie nombre cree.

---

## 9) Flux stats (donnees)

### 9.1 Chargement global page stats
1. `statsPage.vue` monte `StatsCanvas`.
2. `StatsCanvas` charge:
- bornes dates (`/stats/date-bounds`),
- categories (`/stats/categories`),
- layout (`/stats/layout`),
- widgets visibles.

### 9.2 Chargement widget
Exemple widget KPI:
1. Widget recoit props `from/to/bucket/categories/types`.
2. Appelle `StatsServices.kpi(metric,...)` + `StatsServices.series(metric,...)`.
3. `StatsServices` gere cache frontend + dedup.
4. Backend `StatsController` -> `StatsService`.
5. `StatsService` choisit requetes SQL dans `SnkVenteRepository`.
6. Retour JSON normalise par `statsAdapters.js`.
7. Widget calcule `computed` affichage final.

### 9.3 Flux layout canvas
1. User deplace/redimensionne widgets.
2. `StatsCanvas` met a jour etat local reactif.
3. `scheduleSave`:
- ecriture localStorage immediate,
- save backend debounce via `/stats/layout`.
4. A la reconnexion user, layout est restaure.

---

## 10) Flux interaction canvas (UI)

### 10.1 Camera
- `useCanvaCamera.ts` gere pan/zoom (wheel, touch, pinch).
- Evite conflit avec zones scrollables internes.

### 10.2 Selection
- Simple click: selection widget unique.
- Ctrl/Cmd click: selection multiple.
- Marquee (drag sur zone vide): selection rectangle.
- Group resize: resize synchrone des widgets selectionnes.

### 10.3 Raccourcis clavier
`useCanvasShortcuts.ts`:
- `E`: toggle mode edit.
- `P`: ouvrir palette.
- `+/-/0`: zoom.
- `F`: fit content.
- `?`: aide shortcuts.
- `Esc`: fermeture panneaux/menus selon contexte.

---

## 11) Flux de persistence locale frontend

### 11.1 Auth store
- Clefs storage: `snk_token`, `snk_user`.
- Lecture initiale au chargement du module.

### 11.2 Theme
- Clef: `snk_theme_mode_v1`.
- Synchro localStorage + sessionStorage + event `storage`.

### 11.3 Date range stats
- Clef prefix: `snk_stats_range_v1_<userId>`.

### 11.4 Mode edit canvas
- Clef: `snk_stats_canvas_edit_v1_<userId>`.

### 11.5 Layout canvas
- Clef: `snk_stats_canvas_layout_v4_<userId>`.

---

## 12) Incertitudes et divergences fonctionnelles
1. Certains widgets frontend (`OpexWidget`, `PlatformSplitWidget`, `ReturnRateWidget`) appellent des metriques qui ne semblent pas implementees explicitement dans les switch backend actuels.
2. Le projet contient des traces de code legacy (`StatBase.vue`, cles de layout historiques `v1/v4`).
3. Plusieurs messages/commentaires sont mal encodes, ce qui peut fausser interpretation metier.

Voir details dans `docs/files/risky-zones.md`.
