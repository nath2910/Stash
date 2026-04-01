# Backend - controllers (fichier par fichier)

## 1) `HealthController.java`

### Role
Endpoint de vie technique.

### Methodes
- `GET /health` -> `ok`.
- `GET /ping` -> `ok`.

### Usage
- monitoring,
- test deploy,
- smoke checks.

---

## 2) `snkVenteController.java`

### Role
Expose toute l'API metier stock/items.

### Endpoints
1. `POST /snkVente`:
- create via DTO `SnkVenteCreateDto`.
2. `GET /snkVente/{id}`:
- lecture unitaire.
3. `GET /snkVente`:
- liste user (option `limit`).
4. `GET /snkVente/recent`:
- dernieres ventes.
5. `POST /snkVente/add`:
- alias historique create (payload entite partielle).
6. `GET /snkVente/total`:
- benef total ou annuel (`year` query param).
7. `GET /snkVente/ca`:
- chiffre d'affaires total.
8. `GET /snkVente/marque`:
- breakdown marques.
9. `DELETE /snkVente/{id}`:
- suppression unitaire.
10. `POST /snkVente/bulk-delete`:
- suppression masse IDs.
11. `GET /snkVente/topVentes`:
- top ventes annee courante.
12. `PUT /snkVente/{id}`:
- update item.
13. `POST /snkVente/import`:
- import bulk.
14. attachments:
- `GET /snkVente/{id}/attachments`
- `POST /snkVente/{id}/attachments`
- `DELETE /snkVente/{id}/attachments/{attachmentId}`
- `GET /snkVente/{id}/attachments/{attachmentId}/download`

### Pourquoi important
Controller tres utilisé par page gestion et home.

---

## 3) `StatsController.java`

### Role
Expose l'API analytics.

### Endpoints
1. `GET /stats/summary`
2. `GET /stats/timeseries`
3. `GET /stats/brands`
4. `GET /stats/top-sales` (alias `/topVentes`)
5. `GET /stats/kpi/{metric}`
6. `GET /stats/series/{metric}`
7. `GET /stats/breakdown/{metric}`
8. `GET /stats/rank/{metric}`
9. `GET /stats/categories`
10. `GET /stats/date-bounds`
11. `GET /stats/layout`
12. `PUT /stats/layout`

### Parametres recurrentes
- dates: `from`, `to`, ou fallback `startDate`, `endDate`, parfois `asOf`.
- filtres: `categories[]`, `types[]`.
- options: `granularity`, `limit`.

### Note
Il expose aussi alias prefix `snkVente/stats` pour compat legacy.

---

## 4) `BillingController.java`

### Role
Expose billing Stripe.

### Endpoints
1. `GET /billing/status?includePortal=`:
- renvoie statut abonnement,
- peut renvoyer URL portal.
2. `POST /billing/checkout`:
- cree session checkout.
3. `POST /billing/webhook`:
- reception events Stripe.

### Conditions
Si Stripe non configure => `503 SERVICE_UNAVAILABLE`.

---

## 5) `DiscordController.java`

### Role
Gestion eligibilite premium via Discord.

### Endpoints
1. `POST /discord/link`:
- lie `discordId` user puis check eligibilite.
2. `GET /discord/check`:
- check eligibilite compte courant.

### Conditions
- si backend Discord non configure -> `503`.
- si discordId absent -> `400`.

---

## 6) `AuthController.java`
Voir detail complet dans `docs/files/backend/security-and-auth.md`.

---

## 7) `GlobalExceptionHandler.java`

### Role
Format global erreurs non gerees.

### Methodes
1. handler `ResponseStatusException`:
- conserve status,
- JSON `{error, message}`.
2. handler generic `Exception`:
- `500` + message standard.

### Limite
Pas de mapping fin domaine par code erreur metier; a etendre si besoin.
