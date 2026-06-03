# Module administratif

## Source de vérité frontend
La configuration admin frontend est centralisée dans `frontend/src/constants/adminModule.js`.

Elle contient :
- `ADMIN_PROFILES` : profils administratifs, labels, champs requis et onglets de base ;
- `ADMIN_TABS` : ordre et labels des onglets ;
- `getAdminTabsForProfile(settings)` : logique d'onglets dynamiques ;
- `ADMIN_STORAGE_KEYS` : clés de fallback local ;
- `ADMIN_DOCUMENT_TYPES` et `ADMIN_PERIOD_OPTIONS`.

`frontend/src/pages/adminPage.vue` consomme ces constantes et garde la logique d'écran : chargement backend/local, calculs préparatoires, exports, documents, registres et checklists.

## Profils
Profils gérés :
- Particulier occasionnel ;
- Achat-revente à régulariser ;
- Micro-entreprise achat/revente ;
- Micro-entreprise avec TVA à surveiller ;
- Boutique en ligne indépendante ;
- Boutique physique ;
- Entreprise au réel / société ;
- Structure avec employés.

## Onglets dynamiques
Les onglets viennent du profil, puis sont ajustés par options métier :
- `hasPhysicalShop` ajoute `Caisse` ;
- `hasEmployees` ajoute `Personnel` ;
- `sellsOnline`, `sellsOnPlatforms`, `hasPhysicalShop` ou `buysFromIndividuals` ajoutent `Boutique` ;
- `sellsTickets` ajoute `Justificatifs` et `Boutique`.

Si l'onglet actif disparaît après un changement de profil, la page revient sur `Accueil`.

## Conformité et langage
L'admin doit rester un outil de préparation. Utiliser les termes :
- estimation ;
- préparation ;
- à vérifier ;
- à compléter ;
- document préparatoire ;
- information indicative.

Ne pas présenter l'interface comme une garantie fiscale, juridique ou comptable. La page affiche un rappel indiquant qu'une vérification est recommandée avant déclaration ou émission définitive.
