<template>
  <LegalDocument title="Cookies et stockage dans votre navigateur">
    <p>Stash utilise un stockage technique pour la connexion et les réglages du service. Aucun outil de mesure d’audience publicitaire n’est intégré au code du site. Aucun bandeau marketing n’est nécessaire pour les seuls usages techniques décrits ici.</p>
    <h2>Connexion et sécurité</h2>
    <p>Le jeton « snk_token » et le profil « snk_user » sont enregistrés dans le stockage local du navigateur, avec repli sur le stockage de session si nécessaire. Le jeton expire après une heure ; les valeurs sont effacées lors de la déconnexion. Une déconnexion, un changement ou une réinitialisation du mot de passe révoquent les sessions côté serveur.</p>
    <p>Lors d’une connexion Google ou Discord, un cookie technique « JSESSIONID » conserve temporairement la demande de connexion, pour dix minutes au plus côté serveur. La session est invalidée à la fin du parcours. Ce cookie est HttpOnly, SameSite=Lax et Secure en production.</p>
    <h2>Préférences du service</h2>
    <p>Si vous reliez Gmail, le cookie « gmail_link_state » lie cette autorisation au navigateur qui l’a demandée. Il expire après dix minutes, est supprimé au retour de Google et ne permet pas de se connecter à l’API. Il est HttpOnly, SameSite=Lax et Secure en HTTPS.</p>
    <p>Les clés commençant notamment par « snk_ » enregistrent les catégories et sous-catégories personnalisées, les tableaux de bord, plages de dates, préférences d’affichage, étapes d’accueil et notifications lues. Ces réglages restent dans le stockage local jusqu’à leur effacement ou la suppression du compte sur ce navigateur. « snk_post_auth_redirect » est un marqueur de redirection de session valable une minute. Le cache de facturation est rafraîchi après deux à cinq minutes et n’accorde aucun droit côté serveur.</p>
    <h2>Prestataires externes</h2>
    <p>Stripe, Google et Discord peuvent utiliser leurs propres cookies techniques sur leurs domaines lors d’un paiement ou d’une connexion que vous demandez. Les noms et durées dépendent de leur service et des réglages de votre navigateur. Consultez leurs politiques au moment du parcours. Aucune bibliothèque Stripe ni vidéo YouTube embarquée n’est chargée automatiquement sur Stash.</p>
    <h2>Gérer les données locales</h2>
    <p>Vous pouvez effacer les données du site depuis les réglages du navigateur. Cela déconnecte le compte et réinitialise les préférences locales, sans effacer les données stockées sur le serveur. La suppression du compte s’effectue depuis l’espace personnel.</p>
  </LegalDocument>
</template>
<script setup>
import LegalDocument from '@/components/legal/LegalDocument.vue'
</script>
