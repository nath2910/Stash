# Backend - services (hors deep dive stats)

## 1) `snkVenteService.java`

### Role
Logique metier stock CRUD/import.

### Methodes principales
1. `creer(userId, dto)`:
- cree item user owner,
- invalide cache stats.
2. `rechercherParUser(userId, limit?)`:
- retourne items tries date achat desc.
3. `lire(userId, id)`:
- check ownership.
4. `getDernieresVentesParUser(userId, limit)`.
5. `totalBenef`, `totalBenefYear`, `sumPrixResell`, `graphMarque`.
6. `deleteVente`, `deleteBulk`:
- check ownership,
- invalide cache stats.
7. `updateVente`:
- check ownership,
- applique payload,
- invalide cache stats.
8. `importBulk`:
- garde max 500,
- ignore lignes invalides,
- sanitize metadata,
- saveAll,
- invalide cache stats.

### Detail utile JS
Le frontend envoie parfois snake_case/camelCase melanges. Les helpers backend compensent via DTO/entite.

---

## 2) `AttachmentService.java`

### Role
Gestion metier des pieces jointes.

### Methodes
1. `list(userId, venteId)`.
2. `add(userId, venteId, file)`:
- check ownership item,
- validate taille/type MIME selon `ItemType`,
- store fichier physique,
- save metadata DB.
3. `delete(userId, venteId, attachmentId)`:
- delete metadata + fichier.
4. `download(userId, venteId, attachmentId)`:
- renvoie `Resource` securise.

### Points critiques
- limite 10MB,
- validation renforcée pour `ItemType.TICKET`.

---

## 3) `FileStorageService.java`

### Role
Abstraction stockage local disque.

### Methodes
1. `store(...)`:
- protege contre noms invalides / path traversal,
- genere cle stockage,
- ecrit sur disque,
- deduit MIME.
2. `loadAsResource(storageKey)`:
- path normalize + check prefix root,
- renvoie resource lisible.
3. `delete(storageKey)`:
- suppression best effort.

### Usage
Utilise uniquement via `AttachmentService`.

---

## 4) `BillingService.java`

### Role
Orchestration Stripe.

### Methodes
1. `isConfigured()`.
2. `ensureCustomer(user)`.
3. `createCheckout(user, promoCode, discordId)`.
4. `createPortal(user)`.
5. `handleWebhook(payload, sig)`.
6. `refreshStatus(user)`.

### Details notables
- peut lier `discordId` pendant checkout.
- synchronise statut abonnement depuis webhook ou polling.
- fallback recherche user par email Stripe si customerId absent.

### Ou utilise
`BillingController`.

---

## 5) `DiscordAccessService.java`

### Role
Verification eligibilite premium Discord.

### Methodes
1. `isConfigured()`.
2. `isEligible(user)`:
- lit guild allowlist DB,
- appelle API Discord guild member via bot token,
- verifie role premium.

### Ou utilise
- `OAuth2SuccessHandler` (gate avant activation login Discord).
- `DiscordController`.

---

## 6) `UserService.java`
Voir doc auth detaillee: `docs/files/backend/security-and-auth.md`.

---

## 7) `PasswordResetService.java`
Voir doc auth detaillee: `docs/files/backend/security-and-auth.md`.

---

## 8) `EmailVerificationService.java`
Voir doc auth detaillee: `docs/files/backend/security-and-auth.md`.

---

## 9) `StatsLayoutService.java`

### Role
Persist layout dashboard JSON par utilisateur.

### Methodes
1. `getLayout(userId)`:
- lit `user_stats_layouts.layout_json`, parse JSON.
2. `saveLayout(userId, layoutJson)`:
- delete row si layout null,
- sinon upsert row user.

### Ou utilise
`StatsController` (`GET/PUT /stats/layout`).

---

## 10) `StatsCacheKeys.java`

### Role
Genere cles de cache stables pour `StatsService`.

### Points cle
- Nettoie null/empty.
- Trie listes categories/types.
- Inclut user + periode + metric + options.

### Pourquoi
Evite collisions cache et rend invalidation predictable.

---

## 11) `StatsService.java`
Deep dive dedie dans `docs/files/backend/stats-service.md`.
