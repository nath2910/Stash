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
Certains widgets frontend semblent appeler des metrics non mappees explicitement en backend actuel:
1. `OpexWidget` -> `breakdown('opex')`.
2. `PlatformSplitWidget` -> `breakdown('platformSplit')`.
3. `ReturnRateWidget` -> `series('returnRate')`.

### Incertitude explicite
Sans endpoint ou switch metrique correspondant, ces widgets peuvent renvoyer vide.
A confirmer avec tests runtime et historique produit.

### Actions
1. Verifier usage reel en prod.
2. Soit implementer backend manquant, soit retirer widgets legacy.

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

## 7) Zone legacy potentielle - `StatBase.vue`

### Observation
Composant stats ancien encore present, avec references de cles layout historiques.

### Risque
- confusion pour nouveau developpeur.

### Action
Documenter comme legacy ou supprimer apres verification d'usage.

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
