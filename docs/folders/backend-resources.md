# Dossier backend/src/main/resources/

## 1) `application.yml`
Fichier central de configuration Spring.

### 1.1 Sections principales
- `spring.datasource`: pool + URL/user/password (selon profil).
- `spring.jpa`: dialecte PostgreSQL + `ddl-auto: validate`.
- `spring.flyway`: migrations SQL.
- `spring.security.oauth2.client`: config Google/Discord.
- `spring.mail`: SMTP.
- `app.jwt`: secret + expiration.
- `app.oauth2.success-redirect`: URL callback frontend.
- `app.cors.allowed-origins`: liste origins frontend.
- `app.storage.*`: root uploads + taille max.
- `stripe.*`: config Stripe checkout/webhook.

### 1.2 Profils
- Profil `dev`: defaults locaux (dont DB locale `localhost:5433`).
- Profil `prod`: variables obligatoires externes.

### 1.3 Point d'attention
- Certains defaults de credentials dev sont explicites dans ce fichier.

## 2) `compose.yaml`
Compose minimal backend (postgres + adminer), distinct du compose racine.

## 3) `ss.sql`
Fichier present mais vide (artefact probable).

## 4) Dossier `db/migration/`

### `V1__init.sql`
- Schema initial issu dump PostgreSQL (`users`, `tableauventes`, sequences, contraintes).

### `V2__smoke_test.sql`
- Table temporaire smoke test Flyway.

### `V3__add_phone_to_users.sql`
- Ajout colonne `phone` sur `users`.

### `V4__drop_flyway_smoke_test.sql`
- Suppression table smoke test.

### `V5__add_indexes_tableauventes.sql`
- Index performance pour requetes frequentes stats/stock/recent.

### `V6__fixImportCsV.sql`
- Nettoyage lignes orphelines.
- `user_id` rendu NOT NULL.
- Reajout FK user.

### `V7__password_reset_tokens.sql`
- Table tokens reset password + indexes.

### `V8__email_verification_tokens.sql`
- Table tokens verification email + indexes.

### `V9__user_stats_layouts.sql`
- Table layout stats par user.

### `V10__billing_columns.sql`
- Colonnes Stripe/subscription/discord sur `users`.

### `V11__discord_allowed_guild.sql`
- Table allowlist guild Discord.

### `V12__seed_discord_allowed_guild.sql`
- Seed initial guild + role premium.

### `V13__update_discord_role.sql`
- Update role id premium guild seed.

### `V14__typed_items_and_attachments.sql`
- Ajout `type` + `metadata` JSONB a `tableauventes`.
- Creation table `attachments` + indexes.

## 5) Implication reprise
1. Flyway est source de verite schema.
2. Toute evolution DB doit passer par nouvelle migration versionnee.
3. Verifier coherence des migrations seed Discord selon env prod/dev.
