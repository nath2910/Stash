# Conventions UX

## Structure de page
Une page métier doit exposer :
- un titre clair ;
- un sous-titre utile ;
- une zone d'actions principale ;
- des cartes/surfaces cohérentes ;
- des états loading, erreur et vide visibles.

La page `Gestion` sert de référence pour le shell clair et les onglets métier.

## Actions
Conventions :
- action principale : `app-button--primary`, une par zone quand possible ;
- action secondaire : `app-button--secondary` ou `app-button--ghost` ;
- action destructive : `app-button--danger` avec confirmation si la donnée est supprimée ;
- action contextuelle : icône ou bouton discret proche de l'objet concerné.

Les libellés doivent rester stables : `Ajouter`, `Enregistrer`, `Annuler`, `Supprimer`, `Exporter`, `Modifier`.

## Statuts
Ne pas créer de couleur locale pour un statut métier récurrent. Utiliser `frontend/src/constants/statuses.js`.

Statuts communs :
- `Complet` / `À compléter` ;
- `À vérifier` / `Bloquant` ;
- `Actif` / `Archivé` ;
- `Vendu` / `En stock` ;
- `Livré` / `En transit` ;
- `Erreur`.

## Tableaux et données
Les tableaux doivent :
- garder un header lisible ;
- gérer l'overflow horizontal ;
- afficher un état vide explicite ;
- formater les montants via `formatEUR` ou `formatCurrency` ;
- formater les dates via `formatDateFR` ou `formatDate`.

## Modales
Une modale doit avoir :
- titre + contexte ;
- bouton de fermeture ;
- actions en bas ;
- `Annuler` en secondaire ;
- l'action de validation en primaire ;
- erreur et succès dans la même zone visuelle.
