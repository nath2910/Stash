<template>
  <LegalDocument title="Politique de confidentialité">
    <h2>Responsable et contact</h2>
    <p>Responsable du traitement : [À RENSEIGNER : identité et adresse de l’éditeur]. Contact pour exercer vos droits : [À RENSEIGNER : email dédié].</p>
    <h2>Données, finalités et bases légales</h2>
    <p>Pour exécuter le contrat, nous traitons votre email, vos noms, l’identifiant et la photo éventuellement transmis par Google ou Discord, votre mot de passe haché pour les comptes locaux, ainsi que vos stocks, ventes, achats, documents, réglages et données de suivi des colis. Les identifiants de facturation Stripe, l’état de l’abonnement et ses échéances servent à gérer l’accès au service. Stash ne conserve pas les numéros complets de carte bancaire.</p>
    <p>Les coordonnées professionnelles, SIREN/SIRET, régime fiscal et informations de clients figurant dans vos documents sont traités lorsque vous utilisez les fonctions administratives. Vous êtes responsable des données de tiers que vous y ajoutez ; ne saisissez que les informations nécessaires.</p>
    <p>La sécurité et la prévention des abus reposent sur notre intérêt légitime : jetons temporaires de vérification et de récupération, sessions, compteurs de requêtes et journaux techniques. L’adresse IP peut être traitée par l’hébergement et les protections réseau. Les justificatifs comptables sont conservés pour répondre aux obligations légales.</p>
    <h2>Connexion optionnelle à Gmail</h2>
    <p>Si vous reliez Gmail, le service demande un accès en lecture à votre boîte afin de rechercher les messages utiles au suivi des livraisons. Les corps des messages sont analysés pour extraire des numéros de suivi ; des identifiants de messages, dates, candidats et informations de colis sont conservés. Les jetons d’accès et de renouvellement sont chiffrés sur le serveur. Aucune donnée Gmail n’est utilisée pour la publicité.</p>
    <p>Vous pouvez retirer cette connexion dans la gestion des livraisons et révoquer l’accès depuis les autorisations de votre compte Google. L’accès cesse lorsque vous retirez l’autorisation ou supprimez le compte Stash.</p>
    <h2>Destinataires et transferts</h2>
    <p>Les prestataires concernés sont Koyeb (API), Neon (PostgreSQL), Stripe (paiement), Google et Discord (connexion demandée), le fournisseur SMTP [À RENSEIGNER] et, selon les fonctions utilisées, les transporteurs et AfterShip. L’hébergeur frontend, le stockage de fichiers, leurs régions et les entités contractantes sont [À RENSEIGNER].</p>
    <p>Les prestataires ne reçoivent que les données utiles à leur fonction. Pays de traitement, sous-traitants ultérieurs, accords de sous-traitance et garanties des transferts éventuels hors Espace économique européen : [À RENSEIGNER après vérification des contrats].</p>
    <h2>Conservation et suppression</h2>
    <p>Les données de service sont conservées pendant l’utilisation du compte. Le compte peut être supprimé depuis l’espace personnel : les abonnements sont arrêtés, les données actives et jetons Gmail sont supprimés et les fichiers sont effacés, avec reprise technique en cas d’échec. Les factures émises et éléments de vente requis sont isolés dans une archive comptable, conservée dix ans à compter de la clôture de l’exercice concerné. Renseignez votre clôture d’exercice dans le profil administratif ; le 31 décembre est utilisé par défaut. Les justificatifs Stripe restent soumis à la conservation comptable.</p>
    <p>Durées et procédure effectives de purge des comptes inactifs, journaux, archives et sauvegardes : [À RENSEIGNER]. Les sauvegardes doivent avoir une durée définie et les suppressions doivent être réappliquées après une restauration. Une restriction légale, un litige ou une obligation comptable peut justifier une conservation limitée, distincte des données actives.</p>
    <h2>Vos droits</h2>
    <p>Vous pouvez demander l’accès, la rectification, l’effacement, la limitation, la portabilité et, selon la base légale, vous opposer à un traitement ou retirer votre consentement. Contactez [À RENSEIGNER]. Une réponse est apportée dans un mois, sauf prolongation motivée prévue par le RGPD. Une preuve d’identité proportionnée n’est demandée qu’en cas de doute raisonnable.</p>
    <p>Les préférences et informations administratives se modifient dans le compte. Les exports d’inventaire sont accessibles dans la gestion ; un export des données de compte est également disponible. Vous pouvez adresser une réclamation à la <a href="https://www.cnil.fr/fr/plaintes" target="_blank" rel="noopener noreferrer">CNIL</a>.</p>
    <h2>Cookies et stockage local</h2>
    <p>Consultez la <RouterLink to="/cookies">politique cookies et stockage</RouterLink> pour les sessions, jetons et réglages du navigateur. Aucun outil publicitaire ou de mesure d’audience n’est chargé par le site.</p>
  </LegalDocument>
</template>
<script setup>
import { RouterLink } from 'vue-router'
import LegalDocument from '@/components/legal/LegalDocument.vue'
</script>
