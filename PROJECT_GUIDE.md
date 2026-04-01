# PROJECT_GUIDE.md - Guide principal de reprise

## 1) Objectif de ce guide
Ce document sert de point d'entree pour une passation technique complete du projet **SNK V5 (Stash)**.

Ce que vous trouverez:
1. Une vision globale rapide du produit.
2. L'architecture technique complete (frontend, backend, DB, integrations externes).
3. Une cartographie commentee du repository.
4. Des lectures detaillees par dossier et par fichier.
5. Une section de vigilance (legacy, risques, zones ambigues).



## 2) Resume fonctionnel du produit
Le produit aide un utilisateur a suivre un stock de produits (initialement sneakers, mais avec extension vers tickets/cartes).

Fonctions principales:
1. Authentification locale + OAuth (Google, Discord).
2. CRUD stock (ajout, modification, suppression, import CSV/XLSX, pieces jointes).
3. Dashboard "home" simplifie.
4. Dashboard statistiques avance (canvas libre + widgets configurables).
5. Abonnement Stripe + gating d'acces.

---

## 3) Stack technique
- Frontend: Vue 3 + Vite, composants `.vue`, quelques fichiers TypeScript (`.ts`).
- UI/UX: Tailwind + CSS custom, `vue-echarts` / `echarts`, `@panzoom/panzoom`.
- Backend: Spring Boot 3.5.6 (Java 17), Spring Security, JWT, OAuth2 client.
- Data: PostgreSQL + Spring Data JPA + Flyway migrations SQL.
- Integrations: Stripe, Google OAuth, Discord API, SMTP.
- Caching: Caffeine (backend) + petit cache memo cote frontend pour stats.

---

## 4) Parcours de lecture recommande
Si vous reprenez le projet aujourd'hui, l'ordre conseille est:
1. Lire [docs/architecture.md](docs/architecture.md).
2. Lire [docs/repository-map.md](docs/repository-map.md).
3. Lire [docs/flows.md](docs/flows.md).
4. Lire [docs/folders/root.md](docs/folders/root.md), puis backend, puis frontend.
5. Lire en profondeur les gros fichiers:
- [docs/files/frontend/stats-canvas.md](docs/files/frontend/stats-canvas.md)
- [docs/files/frontend/widget-frame.md](docs/files/frontend/widget-frame.md)
- [docs/files/backend/stats-service.md](docs/files/backend/stats-service.md)
- [docs/files/backend/snkvente-repository.md](docs/files/backend/snkvente-repository.md)
6. Finir avec [docs/files/risky-zones.md](docs/files/risky-zones.md) et [docs/glossary.md](docs/glossary.md).

---

## 5) Demarrage local rapide

### Option A - Docker compose (stack complete)
Depuis la racine du repo:
```bash
# lance DB + backend + frontend
docker compose up -d db backend frontend
```

Ports principaux:
- frontend: `http://localhost:5173`
- backend: `http://localhost:8080`
- postgres expose: `localhost:5433`

### Option B - Lancement manuel
1. Backend:
```bash
cd backend
./mvnw spring-boot:run
```
2. Frontend:
```bash
cd frontend
npm install
npm run dev
```

---

## 6) Commandes utiles reprise
- Tests backend:
```bash
cd backend
./mvnw test -q
```
- Tests frontend:
```bash
cd frontend
npm test
```
- Build frontend:
```bash
cd frontend
npm run build
```

---

## 7) Structure de documentation livree

### Guides transverses
- [docs/architecture.md](docs/architecture.md)
- [docs/repository-map.md](docs/repository-map.md)
- [docs/flows.md](docs/flows.md)
- [docs/glossary.md](docs/glossary.md)

### Dossiers (cartographie + role)
- [docs/folders/root.md](docs/folders/root.md)
- [docs/folders/backend.md](docs/folders/backend.md)
- [docs/folders/backend-src-main-java.md](docs/folders/backend-src-main-java.md)
- [docs/folders/backend-resources.md](docs/folders/backend-resources.md)
- [docs/folders/backend-tests.md](docs/folders/backend-tests.md)
- [docs/folders/frontend.md](docs/folders/frontend.md)
- [docs/folders/frontend-src.md](docs/folders/frontend-src.md)
- [docs/folders/frontend-stats.md](docs/folders/frontend-stats.md)
- [docs/folders/frontend-gestion.md](docs/folders/frontend-gestion.md)

### Fichiers (lecture detaillee)
- Backend:
  - [docs/files/backend/config-and-build.md](docs/files/backend/config-and-build.md)
  - [docs/files/backend/security-and-auth.md](docs/files/backend/security-and-auth.md)
  - [docs/files/backend/controllers.md](docs/files/backend/controllers.md)
  - [docs/files/backend/services.md](docs/files/backend/services.md)
  - [docs/files/backend/stats-service.md](docs/files/backend/stats-service.md)
  - [docs/files/backend/snkvente-repository.md](docs/files/backend/snkvente-repository.md)
  - [docs/files/backend/entities-and-dto.md](docs/files/backend/entities-and-dto.md)
- Frontend:
  - [docs/files/frontend/bootstrap-routing.md](docs/files/frontend/bootstrap-routing.md)
  - [docs/files/frontend/stores-and-api.md](docs/files/frontend/stores-and-api.md)
  - [docs/files/frontend/auth-and-account-pages.md](docs/files/frontend/auth-and-account-pages.md)
  - [docs/files/frontend/home-and-gestion-pages.md](docs/files/frontend/home-and-gestion-pages.md)
  - [docs/files/frontend/csv-import-export-widget.md](docs/files/frontend/csv-import-export-widget.md)
  - [docs/files/frontend/widget-registry-and-settings.md](docs/files/frontend/widget-registry-and-settings.md)
  - [docs/files/frontend/widget-palette-deep-dive.md](docs/files/frontend/widget-palette-deep-dive.md)
  - [docs/files/frontend/stats-canvas.md](docs/files/frontend/stats-canvas.md)
  - [docs/files/frontend/widget-frame.md](docs/files/frontend/widget-frame.md)
  - [docs/files/frontend/stats-widgets.md](docs/files/frontend/stats-widgets.md)
  - [docs/files/frontend/utility-and-style-files.md](docs/files/frontend/utility-and-style-files.md)
  - [docs/files/frontend/tests.md](docs/files/frontend/tests.md)

### Zone speciale risques / legacy
- [docs/files/risky-zones.md](docs/files/risky-zones.md)

---

## 8) Premiere semaine de reprise (plan pragmatique)
1. Jour 1: environnement local + lecture architecture + verification endpoints `/health`, `/auth/me`, `/stats/date-bounds`.
2. Jour 2: lecture complete frontend auth/router/layout + tests de navigation protegee.
3. Jour 3: lecture gestion stock + test import CSV/XLSX + validation pieces jointes.
4. Jour 4: lecture module stats canvas + palette + registry + widgets.
5. Jour 5: lecture backend stats/repository SQL + etablir plan de refactor progressif.

---

## 9) Incertitudes connues a verifier en priorite
1. Quelques widgets frontend demandent des metriques qui ne sont pas explicitement supportees dans `StatsService` (possible legacy ou code mort).
2. Plusieurs fichiers contiennent du texte mal encode (mojibake), pouvant cacher des intentions metier dans des commentaires.
3. Presence de secrets reels dans des fichiers d'environnement locaux et credentials en clair dans certains compose: necessite d'assainir avant toute diffusion.

Consultez la section dediee: [docs/files/risky-zones.md](docs/files/risky-zones.md).
