# Backend - config and build

## 1) `backend/pom.xml`

### Role
Definit toutes les dependances backend + plugin Spring Boot Maven.

### Points cle
1. Parent `spring-boot-starter-parent` 3.5.6.
2. Java 17.
3. Starters principaux:
- web,
- validation,
- data-jpa,
- cache,
- security,
- mail,
- oauth2-client,
- test.
4. DB:
- PostgreSQL runtime,
- H2 pour tests.
5. JWT:
- `jjwt-api`, `jjwt-impl`, `jjwt-jackson`.
6. Flyway:
- core + postgres plugin.
7. Stripe SDK Java.
8. Plugin build: `spring-boot-maven-plugin` avec `repackage`.

### Pourquoi c'est important
Ce fichier controle ce qui est disponible au runtime. Toute upgrade framework/lib passe ici.

---

## 2) `backend/Dockerfile`

### Role
Construit et execute l'API dans un conteneur.

### Etapes
1. Stage build:
- image Maven + Temurin 17,
- copie `pom.xml` + `src`,
- lance `mvn package spring-boot:repackage`.
2. Stage runtime:
- image JRE 17,
- copie jar construit,
- expose port 8080,
- lance `java -jar`.

### Pourquoi c'est important
Base du deploiement conteneurise (local/CI/hosted).

---

## 3) `backend/src/main/resources/application.yml`

### Structure
- bloc commun,
- bloc profil `dev`,
- bloc profil `prod`.

### Mecanismes critiques
1. Import `.env` optionnel.
2. Datasource configurable par env vars.
3. OAuth2 google/discord parametre.
4. JWT secret + expiration.
5. Stripe URLs et secrets.
6. CORS origins dynamiques.
7. Storage root uploads.

### Pourquoi c'est important
Fichier de verite runtime. Toute incoherence env -> bug d'auth, DB, mail ou billing.

---

## 4) `backend/src/main/resources/compose.yaml`

### Role
Compose local minimal backend (postgres + adminer).

### Particularite
- Different du `docker-compose.yml` racine qui lance aussi frontend/backend app.
- Contient credentials en clair (a durcir).

---

## 5) `backend/.env` (variables)

### Variables detectees
- DB (`SPRING_DATASOURCE_*`).
- Google OAuth (`GOOGLE_CLIENT_*`).
- SMTP (`SPRING_MAIL_*`, `APP_MAIL_FROM`).
- Stripe (`STRIPE_*`).
- Discord (`DISCORD_CLIENT_*`, `APP_DISCORD_BOT_TOKEN`).

### Regle
Ne jamais exposer ce fichier. Le remplacer en prod par secret manager.

---

## 6) `docker-compose.yml` (racine, impact backend)

### Role cote backend
- Construit image backend.
- Monte volume uploads.
- Injecte variables datasource.

### Risque
- credentials DB en clair.

---

## 7) `backend/src/main/resources/ss.sql`

### Etat
Fichier vide. Probable artefact.

### Action recommandee
Supprimer ou documenter explicitement son usage.

---

## 8) Fichiers outillage

### `backend/.mvn/wrapper/maven-wrapper.properties`
- Version wrapper Maven fixe (`3.9.11`).

### `backend/.gitignore`
- ignore `target`, logs, IDE files.

### `backend/.gitattributes`
- force EOL wrappers (`mvnw` en LF, `*.cmd` en CRLF).

### `backend/.vscode/settings.json`
- update build config Java en mode interactif.
