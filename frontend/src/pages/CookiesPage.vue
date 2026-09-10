<template>
  <LegalDocument title="Cookies et stockage local" description="Les données strictement techniques enregistrées par Stash dans votre navigateur.">
    <div class="legal-summary">
      <strong>Aucun cookie publicitaire</strong>
      <p>Stash n’utilise actuellement ni publicité ciblée, ni outil de profilage, ni mesure d’audience nécessitant votre consentement.</p>
    </div>

    <h2>1. Pourquoi aucun bandeau de consentement n’est affiché</h2>
    <p>
      Les seuls cookies et stockages déposés directement par Stash sont nécessaires à la connexion, à la
      sécurité ou aux fonctions que vous demandez. Ils ne sont pas utilisés pour suivre votre navigation à
      des fins publicitaires. Un mécanisme de consentement sera ajouté avant l’introduction de tout traceur
      non essentiel.
    </p>

    <h2>2. Liste des stockages utilisés</h2>
    <table>
      <thead><tr><th>Nom ou catégorie</th><th>Support</th><th>Finalité</th><th>Durée</th></tr></thead>
      <tbody>
        <tr><td><code>snk_token</code></td><td>Stockage local ou de session</td><td>Authentifier les appels à l’API</td><td>Jeton valable une heure ; effacé à la déconnexion</td></tr>
        <tr><td><code>snk_user</code></td><td>Stockage local ou de session</td><td>Afficher le profil connecté</td><td>Jusqu’à la déconnexion ou l’effacement du navigateur</td></tr>
        <tr><td><code>JSESSIONID</code></td><td>Cookie HttpOnly</td><td>Sécuriser temporairement une connexion Google ou Discord</td><td>Dix minutes au maximum ; session invalidée à la fin du parcours</td></tr>
        <tr><td><code>gmail_link_state</code></td><td>Cookie HttpOnly</td><td>Rattacher de manière sûre une autorisation Gmail à sa demande initiale</td><td>Dix minutes au maximum ; supprimé au retour de Google</td></tr>
        <tr><td><code>snk_post_auth_redirect</code></td><td>Stockage de session</td><td>Revenir à la bonne page après connexion</td><td>Une minute</td></tr>
        <tr><td>Clés préfixées par <code>snk_</code></td><td>Stockage local</td><td>Conserver catégories, tableaux de bord, filtres, affichage et étapes d’accueil</td><td>Jusqu’à leur remplacement ou l’effacement des données du site</td></tr>
      </tbody>
    </table>

    <h2>3. Services externes</h2>
    <p>
      Google et Discord peuvent déposer leurs propres cookies sur leurs domaines lorsque vous choisissez de
      vous connecter avec eux. Stripe peut également utiliser les cookies nécessaires lorsque vous lancez
      volontairement une simulation de checkout. Ces cookies ne sont pas déposés lors de la simple
      consultation de Stash.
    </p>

    <h2>4. Gérer les données du navigateur</h2>
    <p>
      Vous pouvez effacer les données associées à <code>mystash.fr</code> depuis les paramètres de votre
      navigateur. Cette opération vous déconnecte et réinitialise les préférences locales, mais ne supprime
      pas les données conservées sur le serveur. Pour cela, utilisez la suppression du compte.
    </p>

    <h2>5. Évolution de cette politique</h2>
    <p>
      Cette page sera mise à jour avant l’ajout d’un nouveau traceur ou d’une nouvelle finalité. Lorsqu’un
      consentement est requis, refuser doit rester aussi simple qu’accepter et le choix doit pouvoir être
      modifié à tout moment.
    </p>
  </LegalDocument>
</template>

<script setup>
import LegalDocument from '@/components/legal/LegalDocument.vue'
</script>
