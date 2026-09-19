# Developpement, pre-production et production

Ce projet utilise trois environnements distincts :

| Environnement | Branche Git | Adresse | Donnees |
| --- | --- | --- | --- |
| Local | branche de travail (`feat/...` ou `fix/...`) | `localhost` | base Docker locale uniquement |
| Pre-production | `staging` | `preprod.mystash.fr` et `api.preprod.mystash.fr` | base de donnees et comptes de test dedies |
| Production | `main` | `mystash.fr` et `api.mystash.fr` | vraies donnees et paiements live |

`local` n'est pas une branche : c'est l'environnement sur ton ordinateur. La branche `main` reste la version actuellement donnee aux clients. Il ne faut jamais travailler directement dessus.

## Workflow quotidien

1. Partir de `staging` a jour et creer une branche pour une seule evolution :

   ```powershell
   git switch staging
   git pull --ff-only
   git switch -c feat/nom-court-de-la-fonction
   ```

2. Developper et tester en local avec Docker Compose. Ne jamais utiliser la base de production ni les secrets de production en local.
3. Ouvrir une pull request de `feat/...` vers `staging`. La validation GitHub doit etre verte avant la fusion.
4. La fusion dans `staging` declenche le deploiement automatique de pre-production. Tester l'application en ligne avec des comptes et donnees de test.
5. Ouvrir une pull request de `staging` vers `main` seulement lorsque la recette est validee. Sa fusion deploie la production.

Pour une correction urgente en production, creer `fix/...` depuis `main`, la tester aussi dans `staging`, puis la fusionner dans `main`. Reporter ensuite la correction dans `staging` si necessaire.

## Protections GitHub a activer

Dans **GitHub > Settings > Branches**, creer une regle pour `main` puis une autre pour `staging` :

* exiger une pull request avant fusion ;
* exiger que le workflow **Validation** soit vert ;
* interdire les force-push et la suppression de branche ;
* ne pas autoriser de contournement de ces regles, y compris pour l'administrateur, une fois le flux pris en main.

Pour `main`, activer aussi l'approbation obligatoire si une autre personne rejoint le projet. Quand tu es seul, la pull request reste utile comme etape de verification avant de publier.

## Configuration de la pre-production

La pre-production doit etre un second ensemble de services, et non une copie qui partage les secrets ou la base de production.

1. Dans Koyeb, creer un service frontend et un service backend distincts qui suivent la branche `staging`. Laisser les services actuels suivre `main`.
2. Ajouter dans Cloudflare les sous-domaines `preprod.mystash.fr` et `api.preprod.mystash.fr`, chacun vers son service Koyeb correspondant.
3. Creer une base Neon distincte (ou une branche Neon isolee) pour la pre-production. Aucun acces de l'application de pre-production a la base de production ne doit etre possible.
4. Configurer les secrets uniquement dans Koyeb, jamais dans Git :

   ```text
   SPRING_PROFILES_ACTIVE=prod
   SPRING_DATASOURCE_URL=<base preprod>
   SPRING_DATASOURCE_USERNAME=<utilisateur preprod>
   SPRING_DATASOURCE_PASSWORD=<mot de passe preprod>
   JWT_SECRET=<secret aleatoire different de prod>
   APP_TOKEN_ENCRYPTION_KEY=<cle differente de prod>
   APP_FRONTEND_BASE_URL=https://preprod.mystash.fr
   APP_BACKEND_PUBLIC_BASE_URL=https://api.preprod.mystash.fr
   APP_CORS_ALLOWED_ORIGINS=https://preprod.mystash.fr
   ```

5. Utiliser les cles Stripe de test en pre-production, un prix Stripe de test et un webhook de test. Ne jamais y copier une cle `sk_live_`.
6. Creer, si OAuth est active, des identifiants Google/Discord de test ou ajouter exactement les nouvelles URL de redirection de pre-production. Ne jamais melanger les redirections production et pre-production.
7. Ne pas importer de donnees clients dans cet environnement. Utiliser des comptes de recette sans adresses e-mail reelles ou un fournisseur e-mail de test.

Le `SPRING_PROFILES_ACTIVE=prod` de pre-production est volontaire : il garde les memes garde-fous techniques que la production (HTTPS, secrets forts, migrations Flyway). Ce sont les URLs, secrets, base et cles Stripe qui sont differents.

## Premiere mise en place des branches

Une fois les changements commits sur `main`, creer puis publier `staging` :

```powershell
git switch main
git pull --ff-only
git switch -c staging
git push -u origin staging
git switch main
```

Ensuite, ne publier en production que par pull request `staging` vers `main`.

## Retour arriere

Si une version posee probleme, redeployer dans Koyeb la derniere revision saine, puis faire une pull request de revert. Ne pas utiliser `git push --force` sur `main` ou `staging`, et ne pas modifier une migration Flyway deja appliquee.
