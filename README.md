Lancer stack : docker compose up -d db backend frontend
Front tests : cd frontend && npm test
Build front : cd frontend && npm run build
Tests backend : cd backend && ./mvnw test -q

# Stash

Application web destinée à l’assistance dans la gestion du **stock de sneakers** et de la **comptabilité** dans un contexte de projet entrepreneurial.

L’application est découpée en plusieurs pages :

- une page principale axée sur un **aperçu simple** du stock,
- une page **statistiques** (diagrammes + chiffres),
- une page **gestion** pour l’ajout / suppression / modification de la base de données.

---

## Fonctionnalités

- Parcourir la liste des sneakers (image, prix, description).
- Statistiques concernant les résultats (bénéfice, CA…).
- Ajout / suppression / modification du stock.
- Recherche / filtres (marque, taille, prix).
- Système de création / connexion de compte.
- Espace personnel pour gérer son stock.
- Export CSV.

---

## Stack technique

- **Frontend** : Vue 3 (Vite)
- **UI** : Tailwind CSS
- **Backend** : Spring Boot (Spring Web + Spring Security + JWT + OAuth2)
- **DB** : PostgreSQL
- **Charts** : ApexCharts
- **HTTP client** : Axios

---

## Structure du projet

SNK V5/  
├─ frontend/ # Vue/Vite  
└─ backend/ # Spring Boot (Maven)

## Prérequis

- **Node.js** : `20.19+` (ou `22.12+`)  
  (voir `frontend/package.json` → champ `engines`)
- **Java** : 17 (Spring Boot 3.x)
- **Maven** : (ou `./mvnw` si présent)
- **Docker** (recommandé) : pour lancer PostgreSQL facilement

---
