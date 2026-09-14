<template>
  <LegalDocument title="Cookies et stockage local" description="Les donnees strictement techniques enregistrees par MyStash dans votre navigateur.">
    <div class="legal-summary">
      <strong>Aucun cookie publicitaire</strong>
      <p>MyStash n'utilise actuellement ni publicite ciblee, ni outil de profilage, ni mesure d'audience necessitant votre consentement.</p>
    </div>

    <h2>1. Pourquoi aucun bandeau de consentement n'est affiche</h2>
    <p>
      Les seuls cookies et stockages deposes directement par MyStash sont necessaires a la connexion, a la
      securite ou aux fonctions que vous demandez. Ils ne sont pas utilises pour suivre votre navigation a
      des fins publicitaires. Un mecanisme de consentement sera ajoute avant l'introduction de tout traceur
      non essentiel.
    </p>

    <h2>2. Liste des stockages utilises</h2>
    <table>
      <thead><tr><th>Nom ou categorie</th><th>Support</th><th>Finalite</th><th>Duree</th></tr></thead>
      <tbody>
        <tr><td><code>snk_token</code></td><td>Stockage local ou de session</td><td>Authentifier les appels a l'API</td><td>Jeton valable une heure ; efface a la deconnexion</td></tr>
        <tr><td><code>snk_user</code></td><td>Stockage local ou de session</td><td>Afficher le profil connecte</td><td>Jusqu'a la deconnexion ou l'effacement du navigateur</td></tr>
        <tr><td><code>snk_billing_status_cache</code></td><td>Stockage local</td><td>Garder temporairement l'etat d'acces pour eviter des appels repetes</td><td>Quelques minutes, puis actualisation</td></tr>
        <tr><td><code>JSESSIONID</code></td><td>Cookie HttpOnly</td><td>Securiser temporairement une connexion Google ou Discord</td><td>Dix minutes au maximum ; session invalidee a la fin du parcours</td></tr>
        <tr><td><code>gmail_link_state</code></td><td>Cookie HttpOnly</td><td>Rattacher de maniere sure une autorisation Gmail a sa demande initiale</td><td>Dix minutes au maximum ; supprime au retour de Google</td></tr>
        <tr><td><code>snk_post_auth_redirect</code></td><td>Stockage de session</td><td>Revenir a la bonne page apres connexion</td><td>Une minute</td></tr>
        <tr><td>Cles prefixees par <code>snk_</code></td><td>Stockage local</td><td>Conserver categories, tableaux de bord, filtres, affichage et etapes d'accueil</td><td>Jusqu'a leur remplacement ou l'effacement des donnees du site</td></tr>
      </tbody>
    </table>

    <h2>3. Services externes</h2>
    <p>
      Google et Discord peuvent deposer leurs propres cookies sur leurs domaines lorsque vous choisissez de
      vous connecter avec eux. Stripe peut egalement utiliser les cookies necessaires lorsque vous lancez
      volontairement un paiement ou ouvrez le portail client. Ces cookies ne sont pas deposes lors de la
      simple consultation de MyStash.
    </p>

    <h2>4. Gerer les donnees du navigateur</h2>
    <p>
      Vous pouvez effacer les donnees associees a <code>mystash.fr</code> depuis les parametres de votre
      navigateur. Cette operation vous deconnecte et reinitialise les preferences locales, mais ne supprime
      pas les donnees conservees sur le serveur. Pour cela, utilisez la suppression du compte.
    </p>

    <h2>5. Evolution de cette politique</h2>
    <p>
      Cette page sera mise a jour avant l'ajout d'un nouveau traceur ou d'une nouvelle finalite. Lorsqu'un
      consentement est requis, refuser doit rester aussi simple qu'accepter et le choix doit pouvoir etre
      modifie a tout moment.
    </p>
  </LegalDocument>
</template>

<script setup>
import LegalDocument from '@/components/legal/LegalDocument.vue'
</script>
