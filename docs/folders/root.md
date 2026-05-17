# Dossier racine

## 1) Role du dossier racine
La racine assemble:
1. orchestration locale (compose),
2. documentation minimale,
3. deux applications distinctes (`backend/`, `frontend/`),
4. stockage runtime local (`uploads/`, ignore par git).

## 2) Fichiers principaux

### `README.md`
- Raccourcis de lancement/tests/build.
- Resume fonctionnel historique.
- Contient du texte avec encodage imparfait (a nettoyer).

### `docker-compose.yml`
- Lance `db`, `backend`, `frontend`.
- Preconfigure ports 5433/8080/5173.
- Attention: credentials DB en clair dans ce fichier.

### `package.json` (racine)
- Sert surtout aux dependances CSS/outillage global.
- Ne pilote pas l'app metier complete.

### `package-lock.json` (racine)
- Lock correspondante.

### `.gitignore`
- Ignore `.env*` (sauf `.env.example`) et `uploads/`.
- Bonne base, mais verifier que secrets n'ont pas deja fuite ailleurs.

## 3) Dossiers principaux

### `backend/`
- API Java Spring + migrations DB.

### `frontend/`
- SPA Vue + composants metier.

### `uploads/`
- Stockage runtime pieces jointes.
- Contient des donnees utilisateurs reelles.

## 4) Conseils reprise sur la racine
1. Sortir credentials en clair de `docker-compose.yml`.
2. Ajouter une politique claire backup/restauration hors repository.
3. Eventuellement separer docs projet des notes perso.
