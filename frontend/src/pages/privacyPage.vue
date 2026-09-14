<template>
  <LegalDocument title="Politique de confidentialite" description="Comment MyStash collecte, utilise, protege et supprime vos donnees personnelles.">
    <div class="legal-summary">
      <strong>Nos principes</strong>
      <p>Pas de publicite ciblee, pas de revente de donnees et pas de lecture Gmail sans action explicite de votre part.</p>
    </div>

    <h2>1. Responsable du traitement</h2>
    <p>
      Le responsable du traitement est Nathan Talvasson, editeur de MyStash.
      Pour toute question ou demande relative a vos donnees :
      <a href="mailto:nathantalvasson@gmail.com">nathantalvasson@gmail.com</a>.
    </p>

    <h2>2. Donnees traitees</h2>
    <table>
      <thead><tr><th>Donnees</th><th>Utilisation</th><th>Fondement</th></tr></thead>
      <tbody>
        <tr><td>Email, nom, prenom, mot de passe hache</td><td>Creer, securiser et administrer votre compte</td><td>Execution du service</td></tr>
        <tr><td>Identifiants Google ou Discord</td><td>Permettre la connexion externe demandee</td><td>Execution du service demande</td></tr>
        <tr><td>Inventaire, achats, ventes, statistiques, reglages et fichiers</td><td>Fournir les fonctions de gestion et d'export</td><td>Execution du service demande</td></tr>
        <tr><td>Informations administratives facultatives</td><td>Adapter les outils et documents a votre profil</td><td>Execution du service demande</td></tr>
        <tr><td>Numeros et evenements de suivi</td><td>Afficher l'avancement des livraisons</td><td>Execution du service demande</td></tr>
        <tr><td>Identifiants techniques Stripe</td><td>Gerer l'abonnement, le portail client, les factures et les evenements de paiement</td><td>Execution du service et obligations applicables</td></tr>
        <tr><td>Journaux techniques et donnees de securite</td><td>Prevenir les abus, diagnostiquer les erreurs et proteger le service</td><td>Interet legitime a securiser MyStash</td></tr>
      </tbody>
    </table>

    <h2>3. Connexion facultative a Gmail</h2>
    <p>
      Si vous activez cette fonction, MyStash obtient un acces en lecture limite a votre compte Gmail afin
      de rechercher les messages recents lies aux livraisons prises en charge. Le contenu utile est analyse
      pour extraire les numeros de suivi. MyStash conserve les identifiants techniques necessaires pour eviter
      les doublons, les candidats detectes et les informations de colis, mais n'utilise pas vos emails a des
      fins publicitaires.
    </p>
    <p>
      Les jetons Google sont chiffres cote serveur. Vous pouvez deconnecter Gmail depuis la gestion des
      livraisons et revoquer MyStash depuis les autorisations de votre compte Google.
    </p>

    <h2>4. Destinataires et prestataires</h2>
    <p>Vos donnees sont accessibles uniquement lorsque cela est necessaire au fonctionnement du service :</p>
    <ul>
      <li>Koyeb pour l'API et Neon pour la base de donnees ;</li>
      <li>Cloudflare pour la diffusion, la securite reseau et la protection du domaine ;</li>
      <li>Google et Discord lorsque vous choisissez leur connexion ;</li>
      <li>Brevo pour l'envoi des emails de verification et de recuperation du compte ;</li>
      <li>Stripe pour les paiements, les abonnements, le portail client et les factures ;</li>
      <li>les transporteurs ou services de suivi lorsque vous demandez l'actualisation d'un colis.</li>
    </ul>
    <p>
      Certains prestataires peuvent traiter des donnees hors de l'Espace economique europeen selon leur
      organisation et leurs garanties contractuelles. Leurs politiques sont disponibles sur leurs sites.
      MyStash ne vend ni ne loue vos donnees personnelles.
    </p>

    <h2>5. Durees de conservation</h2>
    <table>
      <thead><tr><th>Categorie</th><th>Duree ou critere</th></tr></thead>
      <tbody>
        <tr><td>Compte et donnees de service</td><td>Pendant la vie du compte, puis suppression a votre demande</td></tr>
        <tr><td>Jeton de connexion</td><td>Une heure au maximum</td></tr>
        <tr><td>Sessions temporaires OAuth</td><td>Dix minutes au maximum</td></tr>
        <tr><td>Jetons de verification et de reinitialisation</td><td>Jusqu'a expiration ou utilisation, puis purge technique</td></tr>
        <tr><td>Connexion Gmail</td><td>Jusqu'a sa revocation, sa suppression ou la suppression du compte</td></tr>
        <tr><td>Donnees Stripe et facturation</td><td>Duree necessaire a la gestion de l'abonnement et aux obligations applicables</td></tr>
        <tr><td>Journaux techniques</td><td>Duree necessaire au diagnostic et a la securite, selon la rotation des hebergeurs</td></tr>
        <tr><td>Sauvegardes</td><td>Jusqu'a leur rotation technique ; une suppression est reappliquee en cas de restauration</td></tr>
      </tbody>
    </table>

    <h2>6. Suppression et export</h2>
    <p>
      Vous pouvez telecharger un export JSON depuis votre compte. La suppression du compte efface le profil,
      l'inventaire, les documents, les suivis, les connexions Gmail et les reglages enregistres sur le serveur,
      sous reserve des donnees devant etre conservees pour respecter une obligation legale ou gerer un litige.
    </p>

    <h2>7. Vos droits</h2>
    <p>
      Vous pouvez demander l'acces, la rectification, l'effacement, la limitation ou la portabilite de vos
      donnees. Vous pouvez egalement vous opposer aux traitements fondes sur l'interet legitime et retirer
      votre autorisation Gmail a tout moment. Une reponse est apportee en principe sous un mois.
    </p>
    <p>
      Adressez votre demande a <a href="mailto:nathantalvasson@gmail.com">nathantalvasson@gmail.com</a>.
      Vous pouvez aussi deposer une reclamation aupres de la
      <a href="https://www.cnil.fr/fr/plaintes" target="_blank" rel="noopener noreferrer">CNIL</a>.
    </p>

    <h2>8. Stockage dans le navigateur</h2>
    <p>
      MyStash n'integre actuellement aucun outil publicitaire. Les stockages necessaires a la connexion et
      aux preferences sont detailles dans la <RouterLink to="/cookies">politique cookies et stockage local</RouterLink>.
    </p>
  </LegalDocument>
</template>

<script setup>
import { RouterLink } from 'vue-router'
import LegalDocument from '@/components/legal/LegalDocument.vue'
</script>
