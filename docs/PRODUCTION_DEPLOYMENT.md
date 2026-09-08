# Mise en production

## Prérequis bloquants

* Utiliser PostgreSQL 18. La migration historique `V1__init.sql` est un export
  `pg_dump` PostgreSQL 18.1 : il contient des commandes `psql` et le paramètre
  `transaction_timeout`, inconnus des versions antérieures.
* Ne jamais modifier une migration déjà appliquée. Le démarrage ne lance plus
  `flyway repair` automatiquement : un checksum inattendu doit être analysé et
  réparé explicitement par un opérateur après revue.
* Renseigner les variables de production dans le gestionnaire de secrets de
  Koyeb, jamais dans Git : `SPRING_DATASOURCE_*`, `JWT_SECRET` (au moins 32
  octets aléatoires), `APP_TOKEN_ENCRYPTION_KEY`, les identifiants OAuth et,
  si la vente est activée, les secrets Stripe.

## Initialisation d'une base vide

Une base qui possède déjà `flyway_schema_history` ne doit pas suivre cette
procédure. Pour une base **vide** uniquement :

1. Exécuter `backend/src/main/resources/db/migration/V1__init.sql` avec le
   client `psql` de PostgreSQL 18, par exemple `psql "$DATABASE_URL" -f
   V1__init.sql`.
2. Lancer une seule fois l'application avec
   `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` et
   `SPRING_FLYWAY_BASELINE_VERSION=1`. Flyway enregistre V1 comme baseline et
   applique V2 à V27.
3. Retirer ces deux variables, redémarrer puis vérifier `/health` et le journal
   Flyway. Les déploiements suivants doivent garder la validation des checksums.

Cette étape est nécessaire car V1 est un dump `psql`, non une migration SQL
portable interprétable par Flyway. Elle a été validée sur PostgreSQL 18 avec
V1 à V27. Une tentative de démarrage direct de Flyway sur une base vide échoue
volontairement à V1 ; ne pas contourner ce point avec `repair` ou en modifiant
V1 après mise en production.

## Variables et services externes

Définir `SPRING_PROFILES_ACTIVE=prod`, les URL HTTPS réelles du frontend et de
l'API dans `APP_FRONTEND_BASE_URL`, `APP_BACKEND_PUBLIC_BASE_URL` et
`APP_CORS_ALLOWED_ORIGINS`. Les redirections OAuth doivent correspondre
exactement à ces URL chez Google et Discord.

Les paiements restent désactivés tant que `STRIPE_SALES_ENABLED=false`. Avant
de le passer à `true`, configurer `STRIPE_SECRET_KEY`, `STRIPE_PRICE_ID`,
`STRIPE_ANNUAL_PRICE_ID`, `STRIPE_WEBHOOK_SECRET`, créer le webhook Stripe et
vérifier une souscription mensuelle, annuelle, l'annulation, le portail client
et les remboursements avec le compte de production.

Les tâches de suivi de colis et de lecture Gmail sont désactivées par défaut
afin de laisser Neon descendre à zéro. Les activer seulement si leur coût et
leur fréquence sont acceptés.

## Vérifications après déploiement

* Vérifier la liste des migrations Flyway, `/health`, les journaux sans données
  personnelles ni jetons, et la connexion TLS vers la base Neon.
* Vérifier les en-têtes HTTPS, CSP, CORS depuis les seuls domaines autorisés,
  les flux inscription/connexion/réinitialisation/OAuth, puis les rôles admin.
* Conserver la preuve de version des CGV et de l'acceptation Stripe. Compléter
  les champs `[À RENSEIGNER]` des mentions légales, de la politique de
  confidentialité et des CGV avec l'identité, l'adresse et les contacts réels
  de l'éditeur avant ouverture au public.
