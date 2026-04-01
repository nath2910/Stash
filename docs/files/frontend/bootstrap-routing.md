# Frontend - bootstrap, routing, layout

## 1) `src/main.js`

### Quoi
Point d'entree de la SPA Vue.

### Bloc par bloc
1. Importe `createApp` de Vue.
2. Importe `App.vue`.
3. Importe `router`.
4. Importe styles globaux.
5. Importe plugin motion.
6. Importe wrapper chart `VueECharts`.
7. Monte app:
- `.use(router)`
- `.use(MotionPlugin)`
- `.component('VChart', VueECharts)`
- `.mount('#app')`

### Pourquoi
Sans ce fichier, l'app ne se lance pas.

---

## 2) `src/App.vue`

### Quoi
Root component.

### Comment
- Utilise `useRoute()`.
- Calcule une `routeKey` pour forcer rerender controlé sur certains changements.
- Rend `layoutPages` qui encapsule la navigation + slot page.

### Pourquoi
Permet d'uniformiser la structure de toutes les pages.

---

## 3) `src/router/index.js`

### Quoi
Definit routes + guard globale.

### Bloc par bloc
1. Imports pages en lazy loading (`() => import(...)`).
2. Fonctions utilitaires JWT:
- `decodeJwtPayload(token)`.
- `isTokenExpired(token)`.
3. Definition tableau routes (`name`, `path`, `component`, `meta`).
4. Set `publicRoutes`.
5. `router.beforeEach(async (to) => {...})`:
- lit `authStore`, `billingStore`, token.
- si token expire -> logout.
- si token present sans user -> appelle `/auth/me`.
- gere redirection auth obligatoire.
- gere gating abonnement actif (`allowInactive` exceptions).
- gere redirection depuis page auth si deja connecte.

### Pourquoi
C'est la securite navigation cote frontend.

### Ou utilise
Automatiquement par Vue Router sur chaque navigation.

---

## 4) `src/layout/layoutPages.vue`

### Quoi
Layout principal avec header/navigation/menus et slot de page.

### Bloc par bloc
1. Etat UI:
- menu desktop/mobile,
- menu user,
- animation navigation.
2. Dependances:
- `useRoute`, `useRouter`, `useTheme`, `useAuthStore`.
3. Calculs:
- route active,
- classes dynamiques theme,
- user courant + initiales.
4. Idle tracking:
- `shouldTrackIdle` si user connecte hors routes auth.
- events `pointerdown`, `keydown`, `visibilitychange`.
- logout auto apres inactivite (~10 min).
5. Gestion scroll:
- montre/masque footer mobile selon position.
6. Actions:
- navigation auth/account,
- `logout()`.

### Pourquoi
Centralise logique de shell et interaction globale, hors pages metier.

---

## 5) `src/pages/statsPage.vue`

### Quoi
Page mince qui monte `StatsCanvas`.

### Comment
- lit plage date via `useStatsRange`.
- lit theme via `useTheme`.
- transmet props a `StatsCanvas`.

### Pourquoi
Decouple la page route et le gros composant canvas.

---

## 6) Points d'attention reprise
1. Le guard router cumule auth + billing + fetch user: fichier sensible.
2. `layoutPages.vue` contient beaucoup de logique UI + session timeout.
3. Toute evolution de navigation/protection doit etre testee sur:
- user non connecte,
- token expire,
- user connecte sans abo actif,
- user connecte avec abo actif.
