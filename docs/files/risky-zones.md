# Zones complexes, ambigues, legacy ou risquees

## 1) Zone critique #1 - `StatsCanvas.vue`

### Risque
- Taille et complexite elevees.
- Melange logique metier, interaction DOM bas niveau, persistence, state machine.

### Impact
- Forte probabilite de regressions sur modifications locales.
- Difficulte onboarding.

### Actions recommandees
1. Extraire sous-modules (selection, drag/resize, persistence).
2. Ajouter tests geometriques unitaires.
3. Ajouter tests e2e interactions utilisateur.

---

## 2) Zone critique #2 - `WidgetFrame.vue`

### Risque
- Gestion `contenteditable` + fullscreen + observers + pointers.

### Impact
- Bugs subtils: perte contenu texte, focus, handlers orphelins.

### Actions
1. Couvrir par tests e2e edition texte.
2. Isoler composables internes pour lisibilite.

---

## 3) Zone critique #3 - `SnkVenteRepository.java`

### Risque
- Monolithe SQL natif (~700+ lignes).
- Duplications day/week/month.
- Heuristiques marque via `LIKE`.

### Impact
- Evolution schema/metrique couteuse et risquee.

### Actions
1. Split repository par domaine query.
2. Ajouter tests integration SQL de reference.
3. Normaliser marque en colonne dediee.

---

## 4) Zone critique #4 - Mapping metriques frontend/backend

### Observation
Les widgets legacy qui appelaient des metrics non mappees explicitement ont ete retires du runtime frontend.

### Incertitude explicite
Les metrics backend correspondantes ne sont pas exposees par l'UI actuelle. Toute reintroduction doit passer par un mapping backend confirme et un enregistrement explicite dans `widgetRegistry.js`.

### Actions
1. Ne pas reintroduire de widget sans endpoint teste.
2. Ajouter tests widget/service avant tout retour de ces metrics.

---

## 5) Zone critique #5 - Secrets et donnees sensibles

### Observation
- `backend/.env` contient des cles sensibles.
- `docker-compose.yml` et `backend/resources/compose.yaml` contiennent credentials DB en clair.
- `uploads/` contient des fichiers utilisateurs.

### Risques
1. fuite credentials.
2. fuite donnees personnelles.
3. non-conformite securite/RGPD.

### Actions
1. secret manager / `.env.example` sanitize.
2. rotation credentials.
3. politique backup/chiffrement/suppression uploads.

---

## 6) Zone critique #6 - Encodage texte (mojibake)

### Observation
Plusieurs fichiers contiennent caracteres FR mal encodes.

### Risque
- comprehension partielle de commentaires/messages.
- mauvaise UX textes utilisateurs.

### Actions
1. homogeniser UTF-8 partout.
2. corriger messages visibles utilisateur.

---

## 7) Zone legacy potentielle - layout stats

### Observation
Les composants stats legacy non references ont ete supprimes. Des cles layout historiques restent presentes pour migration douce.

### Risque
- confusion pour nouveau developpeur.

### Action
Conserver les migrations de layout tant que des utilisateurs peuvent avoir des layouts anciens en localStorage.

---

## 8) Zone process - artefacts hors pipeline

### Observation
Presence de dumps, zip, notes textes a la racine.

### Risque
- bruit projet,
- risque d'usage de mauvaise source,
- fuite data si commit accidentel.

### Action
Classer ces artefacts hors repo applicatif ou dans dossier `ops/backup` documente.
