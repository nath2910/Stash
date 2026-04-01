# Dossier racine

## 1) Role du dossier racine
La racine assemble:
1. orchestration locale (compose),
2. documentation minimale,
3. artefacts de travail (dumps, notes, zip),
4. deux applications distinctes (`backend/`, `frontend/`).

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

### `patch_debug.txt` et `Features a ajouter stash.txt`
- Notes ad-hoc (non code de prod).
- A migrer idealement vers issues/projet board.

### `dump16012026.dump`, `snkProjet_docker.dump`
- Backups DB.
- Impact possible RGPD / donnees perso.

### `frontend.zip`
- Archive hors pipeline principal.
- A classifier (obsolete ou backup).

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

### `.github/`
- Dossier present mais structure CI/CD exploitable non evidente dans l'etat observe.

## 4) Conseils reprise sur la racine
1. Clarifier le statut des artefacts (`*.dump`, `frontend.zip`).
2. Sortir credentials en clair de `docker-compose.yml`.
3. Ajouter une politique claire backup/restauration.
4. Eventuellement separer docs projet des notes perso.
