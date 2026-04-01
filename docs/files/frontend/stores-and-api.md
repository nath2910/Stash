# Frontend - stores and API layer

## 1) `src/store/authStore.js`

### Quoi
Store auth singleton base sur `ref` module-scope (pas Pinia/Vuex).

### Variables
- `user` (ref).
- `token` (ref).

### Fonctions
1. `safeGet/safeSet/safeRemove`:
- robustesse localStorage/sessionStorage (try/catch).
2. `loadFromStorage()`:
- hydrate token/user au chargement module.
3. `setAuth(payload)`:
- met token + user,
- persiste storage.
4. `setToken`, `setUser`, `logout`.
5. `useAuthStore()` retourne API store.

### Pourquoi
Etat auth global accessible partout.

### Note JS pedagogique
Le store est un objet singleton, donc tout composant important le meme module partage le meme etat.

---

## 2) `src/store/billingStore.js`

### Quoi
Store billing minimal.

### Variables
- `status` (`unknown|inactive|active|past_due|canceled`).
- `portalUrl`.
- `inflight` (dedup fetch).

### Fonctions
1. `fetchStatus(force, includePortal)`:
- evite appels paralleles duplicats,
- met a jour state.
2. `reset()`.
3. `useBillingStore()`.

### Pourquoi
Partage statut abonnement entre router/pages account/abo.

---

## 3) `src/services/api.js`

### Quoi
Client Axios central.

### Bloc par bloc
1. Resolve `baseURL` via env:
- `VITE_API_URL` prioritaire,
- fallback `VITE_API_BASE_URL`.
2. `api = axios.create({ baseURL, timeout, headers... })`.
3. `getToken()` lit token local/session.
4. `clearAuthStorage()` purge `snk_token/snk_user`.
5. `redirectToLogin(reason)` construit URL auth query.
6. Interceptor request:
- ajoute `Authorization: Bearer token` si present.
7. Interceptor response error:
- detecte 401/403,
- ignore certains endpoints auth publics,
- purge auth + redirect login.

### Pourquoi
Evite repetition logique auth/erreur dans tous les services.

---

## 4) `src/services/AuthService.js`

### Quoi
Wrapper metier auth.

### Methodes
1. `register(payload)`.
2. `login(payload)`:
- parse token depuis body ou header Authorization.
- stocke token/user en localStorage.
3. `logout()`.
4. `changePassword`, `requestPasswordReset`, `resetPassword`.
5. `verifyEmail(payload)`:
- si token retourne, stocke session.
6. `resendVerification`.
7. `me`.
8. `deleteAccount`.

---

## 5) `src/services/BillingService.js`
- `status(includePortal)` -> GET `/billing/status`.
- `checkout(promoCode, discord)` -> POST `/billing/checkout`.

---

## 6) `src/services/SnkVenteServices.js`

### Quoi
API stock + attachments.

### Methodes
- lecture liste/recent/total/ca/marque,
- create/update/delete,
- bulk delete,
- import,
- attachments list/upload/delete/download.

### Pourquoi
Couche unique appelée par home/gestion.

---

## 7) `src/services/StatsServices.js`

### Quoi
Client stats evolué.

### Mecanismes internes
1. `buildCacheKey(path, params)` stable et deterministic.
2. `responseCache` (TTL court).
3. `inFlightRequests` (dedup promises).
4. Helpers range:
- `dateParams`,
- `resolveRange` (accepte signatures flexibles legacy).

### API exposee
- `summary`, `timeseries`, `brands`, `topSales`.
- `kpi`, `series`, `breakdown`, `rank`.
- `dateBounds`, `categories`.
- `billingStatus`, `billingCheckout` (legacy coupling).
- `getLayout`, `saveLayout`.

### Pourquoi
Absorbe heterogeneite appel widgets/pages.

---

## 8) `src/services/statsAdapters.js`

### Quoi
Normalisateurs robustes des payloads stats.

### Fonctions cle
- `toYmd`, `parseYmdLocal`, `prevPeriod`.
- `normalizeSummary`.
- `normalizeTimeseries`.
- `normalizeBrands`.
- `normalizeTopSales`.
- `normalizeKpi`.
- `normalizeSeries`.
- `normalizeRank`.
- `normalizeBreakdown`.

### Pourquoi
Les widgets peuvent supposer une forme stable meme si backend varie.

---

## 9) `src/composables/useTheme.js`
- Persist dark/light.
- Sync cross-tab via event storage.

## 10) `src/composables/useStatsRange.js`
- Persist plage date par user.
- Cle storage prefixee `snk_stats_range_v1_<userId>`.

## 11) `src/composables/useStatsDashboard.js`
- Ancien orchestrateur dashboard pre-canvas.
- Encore utile pour reference logique delta/periode precedente.

---

## 12) Conseils reprise
1. Garder `api.js` comme point unique interceptors.
2. Eviter duplication de caches stats hors `StatsServices.js`.
3. Si migration vers Pinia: traiter d'abord `authStore` puis `billingStore`.
