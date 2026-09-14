# Mise en production

## Prerequis bloquants

* Utiliser PostgreSQL 18. La migration historique `V1__init.sql` est un export
  `pg_dump` PostgreSQL 18.1 : il contient des commandes `psql` et le parametre
  `transaction_timeout`, inconnus des versions anterieures.
* Ne jamais modifier une migration deja appliquee. Le demarrage ne lance plus
  `flyway repair` automatiquement : un checksum inattendu doit etre analyse et
  repare explicitement par un operateur apres revue.
* Renseigner les variables de production dans le gestionnaire de secrets de
  Koyeb, jamais dans Git : `SPRING_DATASOURCE_*`, `JWT_SECRET` (au moins 32
  octets aleatoires), `APP_TOKEN_ENCRYPTION_KEY`, les identifiants OAuth et les
  secrets Stripe.

## Initialisation d'une base vide

Une base qui possede deja `flyway_schema_history` ne doit pas suivre cette
procedure. Pour une base vide uniquement :

1. Executer `backend/src/main/resources/db/migration/V1__init.sql` avec le
   client `psql` de PostgreSQL 18, par exemple `psql "$DATABASE_URL" -f
   V1__init.sql`.
2. Lancer une seule fois l'application avec
   `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` et
   `SPRING_FLYWAY_BASELINE_VERSION=1`. Flyway enregistre V1 comme baseline et
   applique les migrations suivantes.
3. Retirer ces deux variables, redemarrer puis verifier `/health` et le journal
   Flyway. Les deploiements suivants doivent garder la validation des checksums.

Cette etape est necessaire car V1 est un dump `psql`, non une migration SQL
portable interpretable par Flyway. Une tentative de demarrage direct de Flyway
sur une base vide echoue volontairement a V1 ; ne pas contourner ce point avec
`repair` ou en modifiant V1 apres mise en production.

## Variables et services externes

Definir `SPRING_PROFILES_ACTIVE=prod`, les URL HTTPS reelles du frontend et de
l'API dans `APP_FRONTEND_BASE_URL`, `APP_BACKEND_PUBLIC_BASE_URL` et
`APP_CORS_ALLOWED_ORIGINS`. Les redirections OAuth doivent correspondre
exactement a ces URL chez Google et Discord.

En profil `prod`, le backend refuse de demarrer avec une cle Stripe de test ou
une configuration de paiement incomplete. Avant d'accepter des clients payants,
configurer `STRIPE_SECRET_KEY` avec une cle `sk_live_`, `STRIPE_PRICE_ID`,
`STRIPE_ANNUAL_PRICE_ID`, `STRIPE_WEBHOOK_SECRET`, `STRIPE_SUCCESS_URL`,
`STRIPE_CANCEL_URL`, puis passer `STRIPE_SALES_ENABLED` et
`STRIPE_COMMERCIAL_REGISTRATION_COMPLETE` a `true` seulement apres validation
commerciale et legale. Creer le webhook Stripe live et verifier une
souscription mensuelle, annuelle, l'annulation, le portail client et les
remboursements avec le compte de production.

Les taches de suivi de colis et de lecture Gmail sont desactivees par defaut
afin de laisser Neon descendre a zero. Les activer seulement si leur cout et
leur frequence sont acceptes.

## Protection anti-abus

Le backend applique un rate limit local de secours, mais la protection de
production doit aussi etre configuree a l'entree HTTP. Sur Cloudflare ou Koyeb,
limiter au minimum les routes suivantes par IP :

* `/auth/login`, `/auth/register`, `/auth/forgot-password`,
  `/auth/resend-verification` : faible quota par minute pour eviter brute force
  et spam email.
* `/billing/checkout`, `/billing/validate-promo`, `/billing/portal` : quota
  modere pour eviter l'abus Stripe.
* `/delivery/*` en POST/PUT/DELETE : quota modere pour eviter les scans ou
  rafraichissements massifs.

## Verifications apres deploiement

* Verifier la liste des migrations Flyway, `/health`, les journaux sans donnees
  personnelles ni jetons, et la connexion TLS vers la base Neon.
* Verifier les en-tetes HTTPS, CSP, CORS depuis les seuls domaines autorises,
  les flux inscription/connexion/reinitialisation/OAuth, puis les roles admin.
* Verifier la version publiee des CGU, des mentions legales, de la politique de
  confidentialite et de la politique de cookies. Avant toute activation du mode
  live Stripe, publier les informations professionnelles et les CGV applicables.

