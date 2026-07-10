# Guide de configuration - Suivi Livraison

Ce guide part du code actuel du repo `SNK V5`.

## 1. Variables d'environnement backend

1. Genere une cle de chiffrement AES-GCM pour les tokens OAuth :

```powershell
$rng=[System.Security.Cryptography.RandomNumberGenerator]::Create()
$bytes=New-Object byte[] 32
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

2. Ajoute ou complete `backend/.env` :

```env
SPRING_PROFILES_ACTIVE=dev

GOOGLE_CLIENT_ID=ton_client_id_google
GOOGLE_CLIENT_SECRET=ton_client_secret_google

APP_TOKEN_ENCRYPTION_KEY=la_cle_base64_generee
APP_FRONTEND_BASE_URL=http://localhost:5173
APP_BACKEND_PUBLIC_BASE_URL=http://localhost:8080
APP_DELIVERY_GMAIL_REDIRECT_URI=http://localhost:8080/delivery/mail-accounts/gmail/callback
APP_DELIVERY_SCAN_FIXED_DELAY_MS=300000
APP_DELIVERY_SCAN_BATCH_SIZE=25
APP_DELIVERY_TRACKING_PROVIDER=DIRECT
APP_DELIVERY_TRACKING_REFRESH_FIXED_DELAY_MS=900000
APP_DELIVERY_TRACKING_REFRESH_BATCH_SIZE=50

LAPOSTE_API_KEY=

MONDIAL_RELAY_ENSEIGNE=
MONDIAL_RELAY_PRIVATE_KEY=

AFTERSHIP_API_KEY=
AFTERSHIP_WEBHOOK_SECRET=un_secret_long_aleatoire
```

Notes :
- `APP_TOKEN_ENCRYPTION_KEY` est obligatoire en production.
- `APP_FRONTEND_BASE_URL` et `APP_BACKEND_PUBLIC_BASE_URL` servent de base aux redirects OAuth Gmail, liens emails et retours frontend.
- `APP_DELIVERY_TRACKING_PROVIDER=DIRECT` active le suivi direct France en priorite.
- En dev, `AFTERSHIP_API_KEY` peut rester vide : les colis seront detectes et stockes localement, mais pas inscrits chez AfterShip.
- Ne mets jamais de mot de passe Gmail dans la config. Le code utilise OAuth 2.0 + `gmail.readonly`.

## 2. Configuration Google Cloud / Gmail

1. Va dans Google Cloud Console.
2. Cree ou selectionne ton projet.
3. Va dans `APIs & Services > Library`.
4. Active `Gmail API`.
5. Va dans `APIs & Services > OAuth consent screen`.
6. Configure l'application :
   - type `External` si tu utilises un compte Gmail classique ;
   - app name ;
   - support email ;
   - developer contact email.
7. Ajoute le scope exact :

```text
https://www.googleapis.com/auth/gmail.readonly
```

8. En mode test, ajoute ton adresse Gmail dans les test users.
9. Va dans `APIs & Services > Credentials`.
10. Clique `Create credentials > OAuth client ID`.
11. Choisis `Web application`.
12. Ajoute cet Authorized redirect URI en local :

```text
http://localhost:8080/delivery/mail-accounts/gmail/callback
```

13. Copie le `Client ID` dans `GOOGLE_CLIENT_ID`.
14. Copie le `Client secret` dans `GOOGLE_CLIENT_SECRET`.

Pour la prod, ajoute aussi l'URI HTTPS publique, par exemple :

```text
https://api.ton-domaine.fr/delivery/mail-accounts/gmail/callback
```

Puis mets la meme valeur dans `APP_DELIVERY_GMAIL_REDIRECT_URI`.

## 2.b Configuration minimale production

Pour que le module livraison marche en prod sans retomber sur `localhost`, pose au minimum :

```env
SPRING_PROFILES_ACTIVE=prod

APP_FRONTEND_BASE_URL=https://mystash.fr
APP_BACKEND_PUBLIC_BASE_URL=https://api.mystash.fr
APP_CORS_ALLOWED_ORIGINS=https://mystash.fr,https://www.mystash.fr

OAUTH2_SUCCESS_REDIRECT=https://mystash.fr/auth/callback
APP_DELIVERY_GMAIL_REDIRECT_URI=https://api.mystash.fr/delivery/mail-accounts/gmail/callback

GOOGLE_CLIENT_ID=ton_client_id_google
GOOGLE_CLIENT_SECRET=ton_client_secret_google
APP_TOKEN_ENCRYPTION_KEY=ta_cle_base64
```

Si `OAUTH2_SUCCESS_REDIRECT` ou `APP_DELIVERY_GMAIL_REDIRECT_URI` sont omis, le backend derive maintenant ces URLs a partir des bases publique frontend/backend. L'objectif est d'eviter les redirects localhost en production.

## 3. Lancer en local

Depuis la racine du repo :

```powershell
docker compose up -d db
```

Backend :

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Frontend :

```powershell
cd frontend
npm run dev
```

Va ensuite sur :

```text
http://localhost:5173/gestion?tab=delivery
```

## 4. Test Gmail end-to-end

1. Connecte-toi a l'application avec un utilisateur actif.
2. Va dans `Gestion > Suivi Livraison`.
3. Dans `Boites mail`, saisis l'adresse Gmail a ajouter.
4. Clique `Ajouter`.
5. Google ouvre son ecran d'autorisation, avec l'adresse pre-remplie quand c'est possible.
6. Accepte l'autorisation Google.
7. Tu dois revenir sur `Gestion > Suivi Livraison` avec le message `Compte Gmail lie.`
8. Pour tester sans attendre un vrai transporteur, envoie-toi un email dont :
   - le sujet contient `Colissimo suivi` ;
   - le body contient un tracking de test comme `LA-123456789-FR`.
9. Clique l'icone refresh du compte Gmail pour scanner maintenant.
10. Le colis doit apparaitre dans la liste.

Le scanner ne stocke pas les corps d'emails. Il stocke seulement les comptes lies, les messages deja vus et les colis detectes.

Important : l'utilisateur ne peut ajouter que des boites mail qu'il controle. Saisir une adresse sert a pre-remplir le flow Google, mais Google demandera toujours une autorisation OAuth. C'est volontaire : sans cette autorisation, scanner une boite mail serait impossible et dangereux.

## 5. Suivi direct France

Le provider par defaut est :

```env
APP_DELIVERY_TRACKING_PROVIDER=DIRECT
```

Il cible d'abord :
- Colissimo ;
- La Poste ;
- Chronopost ;
- Mondial Relay.

### La Poste / Colissimo / Chronopost

1. Va sur Okapi La Poste.
2. Active l'API `Suivi`.
3. Recupere la cle Okapi.
4. Mets-la dans `backend/.env` :

```env
LAPOSTE_API_KEY=ta_cle_okapi
```

Le backend appelle par defaut :

```text
https://api.laposte.fr/suivi/v2/idships/{tracking}?lang=fr_FR
```

avec le header :

```text
X-Okapi-Key: ...
```

### Mondial Relay

Mondial Relay utilise un WebService SOAP. Il faut les parametres fournis par Mondial Relay :

```env
MONDIAL_RELAY_ENSEIGNE=ton_code_enseigne
MONDIAL_RELAY_PRIVATE_KEY=ta_cle_privee
```

Le backend appelle `WSI2_TracingColisDetaille` et signe la requete avec le hash MD5 attendu par Mondial Relay.

### Refresh automatique

Le backend planifie aussi les mises a jour de colis :

```env
APP_DELIVERY_TRACKING_REFRESH_FIXED_DELAY_MS=900000
APP_DELIVERY_TRACKING_REFRESH_BATCH_SIZE=50
```

Les colis actifs sont rafraichis avec backoff simple :
- en livraison : plus frequent ;
- en transit : toutes les quelques heures ;
- livre : plus de refresh automatique.

Si une API transporteur n'est pas configuree, l'app garde le colis localement et affiche le lien officiel de suivi transporteur.

## 6. Configuration AfterShip optionnelle

1. Cree une cle API AfterShip Tracking.
2. Mets-la dans :

```env
AFTERSHIP_API_KEY=ta_cle_api_aftership
```

3. Dans AfterShip, cree un webhook Tracking vers :

```text
https://ton-backend-public/delivery/webhooks/aftership
```

En local, utilise un tunnel HTTPS comme ngrok ou Cloudflare Tunnel, puis configure l'URL publique.

4. Recupere le webhook secret AfterShip et mets-le dans :

```env
AFTERSHIP_WEBHOOK_SECRET=le_secret_webhook_aftership
```

Le backend verifie le header AfterShip `aftership-hmac-sha256` avec HMAC SHA-256 base64.

Avec `AFTERSHIP_API_KEY`, deux chemins alimentent le meme suivi :
- scan Gmail : le backend extrait le numero, cree le colis, puis l'enregistre chez AfterShip ;
- saisie manuelle : l'utilisateur ajoute un numero dans `Ajouter un colis`, puis le backend interroge AfterShip.

Le bouton refresh sur la timeline appelle :

```text
POST /delivery/parcels/{id}/refresh
```

Il force une recuperation du tracking par le provider configure. En `DIRECT`, il appelle d'abord La Poste/Mondial Relay. En `AFTERSHIP`, il appelle AfterShip.

## 7. Verification base de donnees

Flyway cree automatiquement les tables au demarrage backend.

Pour verifier :

```sql
select table_name
from information_schema.tables
where table_schema = 'public'
  and table_name in (
    'mail_accounts',
    'seen_mail_messages',
    'parcels',
    'parcel_events',
    'tracking_webhook_events'
  );
```

## 8. Commandes de validation

Backend :

```powershell
cd backend
.\mvnw.cmd test
```

Frontend :

```powershell
cd frontend
npm test
npm run build
```

## 9. Problemes courants

`redirect_uri_mismatch`

La valeur dans Google Cloud doit etre strictement identique a `APP_DELIVERY_GMAIL_REDIRECT_URI`, schema inclus (`http`/`https`), port inclus et sans slash final en trop.

`OAuth Gmail non configure`

`GOOGLE_CLIENT_ID` ou `GOOGLE_CLIENT_SECRET` manque cote backend.

`APP_TOKEN_ENCRYPTION_KEY is required`

La cle de chiffrement manque. En prod, le backend refuse de demarrer sans elle.

`Refresh token Gmail absent`

Relance la liaison Gmail. Si le probleme persiste, supprime l'acces de l'app dans ton compte Google, puis refais `Lier Gmail`.

AfterShip ne met pas a jour les colis

Verifie :
- `AFTERSHIP_API_KEY` ;
- `AFTERSHIP_WEBHOOK_SECRET` ;
- URL publique du webhook ;
- header `aftership-hmac-sha256` ;
- reponse HTTP 2xx du backend.

Suivi direct France non disponible

Verifie :
- `APP_DELIVERY_TRACKING_PROVIDER=DIRECT` ;
- `LAPOSTE_API_KEY` pour La Poste / Colissimo / Chronopost ;
- `MONDIAL_RELAY_ENSEIGNE` et `MONDIAL_RELAY_PRIVATE_KEY` pour Mondial Relay ;
- le bouton refresh sur la timeline.

## 10. References officielles

- Gmail API scopes : https://developers.google.com/workspace/gmail/api/auth/scopes
- OAuth 2.0 web server apps : https://developers.google.com/identity/protocols/oauth2/web-server
- Okapi La Poste : https://developer.laposte.fr/
- API Suivi La Poste : https://developer.laposte.fr/products/suivi/latest
- WebService Mondial Relay `WSI2_TracingColisDetaille` : https://www.mondialrelay.fr/WebService/WebService.asmx?op=WSI2_TracingColisDetaille
- AfterShip Tracking webhook signature : https://www.aftership.com/docs/tracking/webhook/webhook-signature
- AfterShip Tracking API quick start : https://www.aftership.com/docs/tracking/quickstart/api-quick-start
