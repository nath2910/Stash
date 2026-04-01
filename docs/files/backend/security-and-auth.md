# Backend - security and auth (fichier par fichier)

## 1) `security/SecurityConfig.java`

### Quoi
Configuration centrale de securite Spring.

### Bloc par bloc
1. Injection dependances:
- `JwtAuthFilter`.
- `OAuth2SuccessHandler`.
- `Environment`.
- `@Value app.cors.allowed-origins`.
2. `securityFilterChain(...)`:
- disable CSRF,
- active CORS,
- session `IF_REQUIRED`,
- routes publiques (auth de base, health, oauth, webhook),
- toutes autres routes authentifiees,
- configure oauth2 login success/failure,
- ajoute filtre JWT avant filtre standard username/password.
3. Bean `retryingTokenClient()`:
- utilise `RetryingTokenResponseClient` pour retry OAuth token endpoint.
4. Bean `corsConfigurationSource()`:
- lit origins depuis config,
- fallback dev `localhost:5173` si vide,
- refuse demarrage hors dev si origins absentes,
- autorise methodes GET/POST/PUT/DELETE/OPTIONS.

### Pourquoi
C'est le "gatekeeper" HTTP global.

### Ou utilise
Auto-detecte par Spring au demarrage.

---

## 2) `security/JwtService.java`

### Quoi
Service creation et verification JWT.

### Bloc par bloc
1. Constructeur:
- lit secret + expiration depuis config.
- construit `SecretKey` HMAC SHA.
2. `generateToken(userId)`:
- met `subject=userId`, `issuedAt`, `expiration`, signe.
3. `extractUserId(token)`:
- parse claims puis convertit `sub` en `Long`.
4. `isValid(token)`:
- parse token dans `try/catch`.

### Pourquoi
Encapsule la logique JWT au meme endroit.

### Ou utilise
- `AuthController` (apres login/register verify).
- `OAuth2SuccessHandler`.
- `JwtAuthFilter`.

---

## 3) `security/JwtAuthFilter.java`

### Quoi
Filtre HTTP execute une fois par requete pour lire token JWT.

### Bloc par bloc
1. Ignore method `OPTIONS` (preflight CORS).
2. Lit header `Authorization`.
3. Verifie prefix `Bearer `.
4. Valide token via `JwtService.isValid`.
5. Extrait userId + charge user DB.
6. Cree `UsernamePasswordAuthenticationToken`.
7. Injecte dans `SecurityContextHolder`.

### Pourquoi
Permet aux controllers de recuperer `@AuthenticationPrincipal User`.

### Ou utilise
Ajoute dans `SecurityConfig`.

---

## 4) `security/OAuth2SuccessHandler.java`

### Quoi
Post-traitement login OAuth Google/Discord.

### Bloc par bloc
1. `onAuthenticationSuccess(...)`:
- detecte type principal (`OidcUser` pour Google, `OAuth2User` pour Discord).
- dispatch vers handler specifique.
2. `handleGoogle(...)`:
- extrait infos profil,
- cherche user par `providerId` puis fallback email,
- cree/maj user,
- genere JWT,
- redirige frontend `successRedirect#token=...`.
3. `handleDiscord(...)`:
- extrait `discordId`, email, username,
- verifie eligibilite via `DiscordAccessService` AVANT persistance,
- cree/maj user,
- force statut subscription active,
- genere JWT + redirection frontend.

### Pourquoi
Unifie mapping provider externe -> compte local.

### Ou utilise
`SecurityConfig.oauth2Login().successHandler(...)`.

---

## 5) `security/RetryingTokenResponseClient.java`

### Quoi
Wrapper du client token OAuth pour retry quand Discord renvoie 429.

### Bloc par bloc
1. Appelle delegate default.
2. En cas `OAuth2AuthorizationException` rate limited:
- lit `Retry-After` si present,
- dort puis rejoue 1 fois.
3. Sinon relance exception.

### Pourquoi
Evite echec login OAuth sur rate-limit transitoire.

### Ou utilise
Bean `retryingTokenClient` dans `SecurityConfig`.

---

## 6) `controller/AuthController.java`

### Quoi
API auth publique/protegee.

### Endpoints et logiques
1. `POST /auth/register`:
- cree user local,
- retourne user ou message "verification envoyee" selon conflit/etat.
2. `POST /auth/login`:
- authentifie via `UserService.login`,
- retourne `LoginResponse(user, jwt)`.
3. `POST /auth/change-password`:
- protege, passe par user courant.
4. `POST /auth/forgot-password`:
- lance email reset (message neutre).
5. `POST /auth/reset-password`:
- consomme token reset.
6. `GET /auth/verify-email`:
- valide token email,
- retourne session JWT immediate.
7. `POST /auth/resend-verification`:
- relance envoi.
8. `GET /auth/me`:
- retourne profil courant.
9. `DELETE /auth/me`:
- suppression compte complet.

---

## 7) `service/UserService.java`

### Quoi
Logique metier de base pour comptes locaux.

### Bloc par bloc
1. `register`:
- valide email/password,
- refuse doublon email,
- encode password,
- set provider LOCAL,
- sauvegarde,
- declenche `EmailVerificationService.sendVerification`.
2. `login`:
- valide presence champs,
- lookup user email,
- compare hash password.
3. `changePassword`:
- verifie current password,
- impose longueur min nouveau password,
- sauvegarde hash.
4. `deleteAccount`:
- purge tokens/layout/ventes puis user.

### Pourquoi
Centralise regles metier auth locale.

---

## 8) `service/PasswordResetService.java`

### Quoi
Cycle complet reset password par email.

### Bloc par bloc
1. `requestReset(email)`:
- normalise email,
- no-op si user absent,
- refuse comptes non-local,
- supprime tokens actifs precedents,
- cree token expirant,
- envoie email avec lien frontend.
2. `resetPassword(request)`:
- valide token + complexite minimale,
- verifie token non expire et non utilise,
- update hash password,
- marque token utilise.
3. helpers:
- `ensureMailConfigured`,
- `resolveFromAddress`,
- `buildResetLink`.

---

## 9) `service/EmailVerificationService.java`

### Quoi
Verification email compte local.

### Bloc par bloc
1. `sendVerification(user)`:
- skip si user null/email absent/deja verifie/non LOCAL,
- invalide anciens tokens,
- cree nouveau token,
- email lien verification.
2. `requestVerification(email)`:
- lookup user par email,
- relance verification si pertinent.
3. `verifyToken(token)`:
- verifie token valide,
- marque user `emailVerified=true`,
- marque token `usedAt`,
- envoie email confirmation (non bloquant en cas d'echec).
4. `@Scheduled cleanupTokens()`:
- purge tokens expires ou utilises.

### Pourquoi
Evite comptes non verifies persistants.

---

## 10) Fichiers DTO auth associes
- `LoginRequest`, `RegisterRequest`, `ChangePasswordRequest`, `ForgotPasswordRequest`, `ResetPasswordRequest`, `EmailVerificationRequest`.
- `LoginResponse`, `UserMeResponse`, `UserMapper`.

### Lecture JS utile
Cote frontend, ces DTO se retrouvent dans `AuthService.js` puis `authStore.js`.
