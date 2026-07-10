# Module administratif

## Source de verite frontend

La configuration admin frontend est centralisee dans `frontend/src/constants/adminModule.js`.

Elle contient :
- les statuts juridiques supportes (`none`, `personal`, `micro`, `ei_real`, `eurl`, `sarl`, `sasu`, `sas`, `other`) ;
- les activites administratives disponibles ;
- les regimes TVA ;
- les declarations suivies ;
- les helpers de normalisation du profil administratif.

`frontend/src/pages/adminPage.vue` consomme cette configuration avec `frontend/src/rules/administrativeRules.js`.

## Experience ecran

La page admin est affichee dans `Gestion > Administratif` et `/admin` redirige vers cet onglet.

L'ecran doit fonctionner comme un centre de services administratifs, pas comme une notice ni comme un tableau de bord general :
- action principale visible : declarer, generer un dossier, creer les factures ou nettoyer les ventes ;
- services concrets sous forme de cartes avec un bouton qui lance vraiment une action ;
- montant et profil visibles, mais au service de l'action ;
- champs a recopier gardes en support, pas comme finalite principale ;
- vrais blocages a corriger dans la gestion des ventes ;
- plan de dossier adapte et controles de donnees replies par defaut ;
- documents et exports accessibles, mais regroupes derriere les services.

La page doit rester complete sans devenir lourde. Le premier ecran donne l'action principale ; le reste est range par niveaux :
- micro-entreprise : preparer declaration URSSAF, creer factures, generer dossier complet, archiver la periode ;
- societe ou EI au reel : generer dossier comptable, creer factures, controler donnees, exporter les documents ;
- particulier : generer recap ventes et verifier le cadre de vente ;
- profil non qualifie : choisir le bon statut et nettoyer les ventes.

Eviter les blocs trop bavards dans cet ecran. Les obligations, alertes et controles backend doivent guider les services ou rester dans les details, mais l'ecran principal doit donner des choses a faire.

## Donnees backend importantes

`AdministrativeSummaryResponse` expose deja les donnees a privilegier dans l'UX :
- `periodRevenue`, `annualRevenue`, `periodSaleCount`, `annualSaleCount` ;
- `periodPurchaseTotal`, `periodPurchaseCount` ;
- `missingSaleDateCount`, `missingSaleAmountCount`, `missingPurchaseAmountCount` ;
- `missingProofCount`, `missingInvoiceCount`, `periodMismatchCount` ;
- `obligations`, `alerts`, `qualityChecks`, `sources`.

La page doit afficher ces informations directement quand elles aident l'utilisateur a savoir quoi faire.

## Conformite et langage

L'admin reste un outil de preparation. Utiliser les termes :
- estimation ;
- preparation ;
- a verifier ;
- a completer ;
- document preparatoire ;
- information indicative.

Ne pas presenter l'interface comme une garantie fiscale, juridique ou comptable. La validation officielle reste manuelle sur les portails ou avec le comptable.
