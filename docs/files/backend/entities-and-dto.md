# Backend - entities and DTOs (fichier par fichier)

## 1) Entites

### `entity/User.java`
- Table `users`.
- Champs auth: email, password, provider, providerId, emailVerified.
- Champs profil: firstName, lastName, pictureUrl.
- Champs billing: stripeCustomerId, subscriptionStatus, period end.
- Champ Discord: discordId.
- Timestamp creation/update.

### `entity/SnkVente.java`
- Table `tableauventes`.
- Champs metier item:
  - nom, prixRetail, prixResell,
  - dateAchat/dateVente,
  - categorie,
  - description,
  - type (`ItemType`),
  - metadata JSON,
  - createdAt,
  - relation user.

### `entity/Attachment.java`
- Table `attachments`.
- Relation item + user.
- Meta fichier: filename, mimeType, sizeBytes, storageKey, createdAt.

### `entity/PasswordResetToken.java`
- Token reset password.
- user, token unique, expiresAt, usedAt, createdAt.

### `entity/EmailVerificationToken.java`
- Token verification email.
- structure proche reset token.

### `entity/UserStatsLayout.java`
- Layout JSON stats par user.
- one-to-one logique user.

### `entity/DiscordAllowedGuild.java`
- Guild Discord autorisee + role premium optionnel.

### `entity/ItemType.java`
- Enum type item.
- Supporte extension metier (sneaker/ticket/card/etc.).

---

## 2) DTOs auth/account

### Requetes
- `RegisterRequest`
- `LoginRequest`
- `ChangePasswordRequest`
- `ForgotPasswordRequest`
- `ResetPasswordRequest`
- `EmailVerificationRequest`

### Reponses
- `LoginResponse`
- `UserMeResponse`

### Mapping
- `UserMapper`.

---

## 3) DTOs billing/discord
- `BillingStatusResponse`.
- `CheckoutRequest`.
- `CheckoutResponse`.
- `DiscordLinkRequest`.
- `DiscordEligibilityResponse`.

---

## 4) DTOs stock
- `SnkVenteCreateDto`: creation standard.
- `SnkVenteImportDto`: import CSV/XLSX (inclut `type`, `metadata`).
- `AttachmentDto`: projection API attachment.

---

## 5) DTOs stats
- `StatsSummaryResponse`.
- `StatsPointResponse`.
- `StatsSeriesPointResponse`.
- `StatsBreakdownResponse`.
- `StatsLabelValueResponse`.
- `StatsKpiResponse`.
- `StatsDateBoundsResponse`.
- `StatsLayoutRequest`.
- `StatsLayoutResponse`.
- `TopVenteProjection` (interface projection).

---

## 6) Repositories simples associes

### `UserRepository.java`
- Lookup user par email/provider/providerId/stripeCustomerId.

### `AttachmentRepository.java`
- CRUD attachments par user/item.

### `PasswordResetTokenRepository.java`
- lookup token + purge par user.

### `EmailVerificationTokenRepository.java`
- lookup token + purge expire/used + purge par user.

### `UserStatsLayoutRepository.java`
- lookup/delete by userId.

### `DiscordAllowedGuildRepository.java`
- read allowlist guilds.

---

## 7) Notes reprise
1. Les `record` DTO simplifient lecture (immutables) cote output.
2. Les classes DTO request ont validation annotations (`@NotBlank`, etc.).
3. Bien distinguer entite JPA interne et DTO API exposee.
