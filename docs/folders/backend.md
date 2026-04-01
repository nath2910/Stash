# Dossier backend/

## 1) Mission du backend
Le backend expose l'API REST securisee du produit:
1. Authentification.
2. Gestion stock.
3. Statistiques.
4. Billing.
5. Upload pieces jointes.

## 2) Sous-dossiers et role

### `src/main/java/backend/`
Code applicatif principal.

### `src/main/resources/`
Configuration runtime + migrations SQL.

### `src/test/java/backend/`
Tests unitaires / controllers ciblés.

### `.mvn/`, `mvnw`, `mvnw.cmd`
Wrapper Maven pour build reproductible.

## 3) Fichiers de pilotage

### `pom.xml`
- Liste dependances Spring, JWT, OAuth2, Stripe, Flyway, cache.
- Java 17.

### `Dockerfile`
- Build `mvn package` en stage 1.
- Runtime JRE 17 en stage 2.
- Lance `app.jar`.

### `.env`
- Variables sensibles locales (DB/OAuth/SMTP/Stripe/Discord).
- Ne pas versionner publiquement.

### `HELP.md`
- fichier template Spring, peu specifique projet.

## 4) Modele de couches backend
1. `controller`: endpoints HTTP.
2. `service`: logique metier.
3. `repository`: acces DB.
4. `entity`: mapping table SQL.
5. `dto`: contrats API.
6. `security`: JWT/OAuth/CORS.
7. `config`: beans infrastructure.

## 5) Dependances externes backend
- PostgreSQL.
- Stripe API.
- SMTP.
- Google OAuth endpoints.
- Discord OAuth + Discord REST (guild/member).

## 6) Points de vigilance backend
1. `SnkVenteRepository.java` tres volumineux (SQL analytics centralise).
2. Credentials hardcodes visibles dans certains fichiers config.
3. Encodage FR parfois casse dans commentaires/messages.
4. Nommage `snkVente*` (minuscule initiale) non convention Java.

## 7) Documents associes
- Details architecture Java: `docs/folders/backend-src-main-java.md`
- Details ressources/migrations: `docs/folders/backend-resources.md`
- Details tests: `docs/folders/backend-tests.md`
- Deep dive fichiers: `docs/files/backend/*.md`
