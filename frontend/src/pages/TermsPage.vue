<template>
  <LegalDocument title="Conditions générales de vente et d’abonnement">
    <h2>1. Vendeur et champ d’application</h2>
    <p>Le vendeur est [À RENSEIGNER : identité et coordonnées complètes], identifié dans les <RouterLink to="/legal">mentions légales</RouterLink>. Les présentes conditions concernent l’abonnement au service Stash, destiné à la gestion d’inventaire, de ventes et de documents. Les droits impératifs des consommateurs restent applicables.</p>
    <h2>2. Prix et commande</h2>
    <p>Le prix total en euros TTC, la formule mensuelle ou annuelle et sa périodicité sont affichés avant paiement. L’abonnement annuel est payé en une fois pour douze mois ; l’abonnement mensuel couvre un mois. Aucune option payante n’est précochée. La commande suppose l’acceptation des présentes conditions puis la confirmation d’une obligation de paiement dans le parcours Stripe.</p>
    <p>Le paiement est traité par Stripe. L’accès commence après confirmation fiable du paiement. Une confirmation de commande, les conditions acceptées et les justificatifs doivent être remis sur support durable. Les factures sont consultables dans l’espace de facturation.</p>
    <h2>3. Durée et renouvellement</h2>
    <p>L’abonnement se renouvelle automatiquement pour la même période au prix indiqué lors de la souscription, sauf résiliation préalable. Toute évolution tarifaire est communiquée avant son application et permet de refuser le renouvellement. Les informations préalables de reconduction sont adressées dans les cas et délais légaux applicables.</p>
    <h2>4. Résiliation en ligne</h2>
    <p>Depuis le compte, ouvrez « Mon abonnement », puis « Résilier mon abonnement » et confirmez. La date de fin d’accès est affichée. La résiliation arrête le renouvellement à la fin de la période payée, sans supprimer le compte ni les données de service. Une confirmation doit être adressée sur support durable. Le portail de facturation permet aussi de gérer le moyen de paiement et les factures.</p>
    <h2>5. Rétractation des consommateurs</h2>
    <p>Vous disposez de quatorze jours à compter de la conclusion du contrat pour vous rétracter, sans motif. Vous pouvez notifier votre décision sans ambiguïté à [À RENSEIGNER : email et adresse] ou utiliser le modèle ci-dessous. L’accès anticipé au service ne vous fait pas renoncer à ce droit. Pour cette offre, aucun prorata n’est retenu lors d’une rétractation exercée dans ce délai. Le remboursement intervient dans le délai légal de quatorze jours à compter de la notification, par le moyen de paiement initial sauf accord exprès.</p>
    <p>Modèle : « À l’attention de [À RENSEIGNER], je vous notifie ma rétractation du contrat d’abonnement Stash souscrit le [date]. Nom : […]. Adresse : […]. Email du compte : […]. Date : […]. Signature uniquement sur papier : […]. »</p>
    <h2>6. Impayés et remboursement</h2>
    <p>Un paiement en échec ou un abonnement suspendu peut interrompre l’accès aux fonctions payantes. La gestion du compte et de l’abonnement reste accessible. La résiliation ordinaire ne rembourse pas la période commencée, sous réserve du droit de rétractation, des garanties légales et d’un éventuel accord commercial. Un remboursement et une résiliation sont deux opérations distinctes.</p>
    <h2>7. Garanties et responsabilité</h2>
    <p>Le consommateur bénéficie des garanties légales applicables aux contenus et services numériques, notamment la conformité, les mises à jour nécessaires et les recours prévus par le Code de la consommation. L’éditeur demeure responsable de la bonne exécution de ses obligations. Aucune clause ne limite les droits impératifs du consommateur.</p>
    <p>Modalités d’assistance, compatibilité, disponibilité et encadré réglementaire de garantie adapté au service : [À RENSEIGNER avant commercialisation]. L’utilisateur vérifie les documents administratifs avant toute utilisation officielle.</p>
    <h2>8. Suppression du compte et données</h2>
    <p>La suppression volontaire du compte met fin à l’accès et arrête les abonnements en cours. Elle est distincte d’une simple résiliation. Exportez vos documents au préalable. Les pièces comptables requises par la loi sont isolées avec un accès restreint ; les autres données sont supprimées selon la <RouterLink to="/privacy">politique de confidentialité</RouterLink>.</p>
    <h2>9. Réclamations et litiges</h2>
    <p>Contact réclamations : [À RENSEIGNER]. En cas de différend non résolu, le consommateur peut saisir gratuitement le médiateur indiqué dans les mentions légales. Le droit français s’applique, sans priver le consommateur des protections impératives de son pays de résidence ni des règles de compétence qui le protègent.</p>
  </LegalDocument>
</template>
<script setup>
import { RouterLink } from 'vue-router'
import LegalDocument from '@/components/legal/LegalDocument.vue'
</script>

