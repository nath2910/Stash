# Dossier backend/src/test/java/backend/

## 1) Objectif global des tests actuels
Le socle de tests backend est present mais limite.
Il couvre surtout:
1. quelques controllers,
2. quelques comportements service ciblés,
3. le chargement de contexte Spring minimal.

## 2) Fichiers de test

### `BackendApplicationTests.java`
- Test smoke: contexte Spring se charge.

### `controller/HealthControllerTest.java`
- Verifie `/health` et `/ping` en GET/HEAD.

### `controller/StatsControllerTest.java`
- Tests unitaires controller (mock `StatsService`).
- Verifie routage params/date vers service.

### `controller/BillingControllerTest.java`
- Tests unitaires controller billing.
- Cas stripe non configure.
- Cas checkout/portal nominal avec mocks.

### `service/SnkVenteServiceImportTest.java`
- Verifie garde-fous import (`MAX_IMPORT_ITEMS`, payload vide).
- Verifie creation d'entites lors import valide.

## 3) Manques notables
1. Peu de tests integration DB reels sur `SnkVenteRepository` SQL natif.
2. Peu de tests sur `StatsService` (metriques/edge cases).
3. Peu de tests securite (routes publiques/protegees, JWT invalide).
4. Peu de tests webhook Stripe et OAuth handlers.

## 4) Priorite recommandee
1. Ajouter tests integration sur requetes stats critiques.
2. Ajouter tests de non-regression sur `StatsService` pour chaque metric.
3. Ajouter tests de securite API (401/403/public routes).
