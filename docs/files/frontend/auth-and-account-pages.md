# Frontend - auth and account pages

## 1) `components/AuthForm.vue`

### Role
Composant central login/signup.

### Bloc par bloc
1. Mode `login` vs `signup` derive de query `?mode=`.
2. Forms reactifs:
- `loginForm` (`email`, `password`, `remember`).
- `signupForm` (`firstName`, `lastName`, `email`, `password`, `confirmPassword`).
3. Score force mot de passe signup (`computed`).
4. Gestion erreurs success + messages SSO (`ssoError`).
5. `submitLogin()`:
- appelle `AuthService.login`,
- push store auth,
- redirect home.
6. `submitSignup()`:
- appelle register,
- redirige verify-email.
- traite conflit email avec fallback resend verification.
7. OAuth buttons:
- URL Google/Discord construites depuis env + fallback API base.

### Syntaxe JS a noter
`watch(...)` ici reactualise le mode si URL change sans remonter composant.

---

## 2) `pages/AuthPage.vue`
- Page wrapper tres simple de `AuthForm`.

## 3) `pages/authCallbackPage.vue`

### Role
Consomme hash OAuth `#token=...`.

### Etapes
1. lit hash URL.
2. si `error` -> redirige auth login avec query.
3. si token absent -> redirige auth.
4. set token store.
5. appelle `/auth/me` pour hydrater user.
6. redirige `home`.

---

## 4) `pages/ForgotPasswordPage.vue`
- Form email.
- Appel `AuthService.requestPasswordReset`.
- Message neutre de confirmation.

## 5) `pages/ResetPasswordPage.vue`

### Role
Reset mot de passe via token query.

### Logique
1. recupere `token` depuis route.
2. formulaire `newPassword` + `confirmPassword`.
3. meter force password.
4. `submitReset` -> `AuthService.resetPassword`.
5. succes -> redirection login.

---

## 6) `pages/VerifyEmailPage.vue`

### Role
Verification d'email post-inscription.

### Logique
1. lit token query/params.
2. `verify()` appelle `AuthService.verifyEmail`.
3. en succes:
- stocke session,
- update authStore,
- redirect home.
4. `resendEmail()` relance envoi verification.
5. ecoute event storage pour gerer login deja present ailleurs.

---

## 7) `pages/accountPage.vue`

### Role
Espace compte utilisateur.

### Fonctions
1. Affichage infos user (initiales/provider).
2. Changement mot de passe via `AuthService.changePassword`.
3. Suppression compte avec double confirmation:
- checkbox,
- texte de confirmation.
4. Navigation vers page abonnement.

---

## 8) Pages abonnement

### `pages/aboPage.vue`
- page achat abonnement.
- charge statut billing.
- CTA checkout Stripe.
- open portal si dispo.
- logique de redirection apres succes.

### `pages/aboViewPage.vue`
- vue plus simple de statut + action portail.

---

## 9) Vigilances
1. Flux auth touche router guard + api interceptor + authStore.
2. Eviter divergence entre storage direct et `authStore`.
3. Bien tester OAuth hash parsing sur navigateurs mobiles.
