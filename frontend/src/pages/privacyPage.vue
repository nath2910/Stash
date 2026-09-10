<template>
  <LegalDocument title="Politique de confidentialité" description="Comment Stash collecte, utilise, protège et supprime vos données personnelles.">
    <div class="legal-summary">
      <strong>Nos principes</strong>
      <p>Pas de publicité ciblée, pas de revente de données et pas de lecture de Gmail sans action explicite de votre part.</p>
    </div>

    <h2>1. Responsable du traitement</h2>
    <p>
      Le responsable du traitement est Nathan Talvasson, éditeur non professionnel de Stash.
      Pour toute question ou demande relative à vos données :
      <a href="mailto:nathantalvasson@gmail.com">nathantalvasson@gmail.com</a>.
    </p>

    <h2>2. Données traitées</h2>
    <table>
      <thead><tr><th>Données</th><th>Utilisation</th><th>Fondement</th></tr></thead>
      <tbody>
        <tr><td>Email, nom, prénom, mot de passe haché</td><td>Créer, sécuriser et administrer votre compte</td><td>Exécution des conditions d’utilisation</td></tr>
        <tr><td>Identifiants et photo Google ou Discord</td><td>Permettre la connexion externe demandée</td><td>Exécution du service demandé</td></tr>
        <tr><td>Inventaire, achats, ventes, statistiques, réglages et fichiers</td><td>Fournir les fonctions de gestion et d’export</td><td>Exécution du service demandé</td></tr>
        <tr><td>Informations administratives facultatives</td><td>Adapter les outils et documents à votre profil</td><td>Exécution du service demandé</td></tr>
        <tr><td>Numéros et événements de suivi</td><td>Afficher l’avancement des livraisons</td><td>Exécution du service demandé</td></tr>
        <tr><td>Identifiants techniques Stripe, le cas échéant</td><td>Tester le parcours ou gérer un abonnement déjà existant sans conserver les données de carte</td><td>Exécution du service concerné</td></tr>
        <tr><td>Journaux techniques et données de sécurité</td><td>Prévenir les abus, diagnostiquer les erreurs et protéger le service</td><td>Intérêt légitime à sécuriser Stash</td></tr>
      </tbody>
    </table>

    <h2>3. Connexion facultative à Gmail</h2>
    <p>
      Si vous activez cette fonction, Stash obtient un accès en lecture limité à votre compte Gmail afin de
      rechercher les messages récents liés aux livraisons Colissimo et Chronopost. Le contenu utile est
      analysé pour extraire les numéros de suivi. Stash conserve les identifiants techniques nécessaires pour
      éviter les doublons, les candidats détectés et les informations de colis, mais n’utilise pas vos emails
      à des fins publicitaires.
    </p>
    <p>
      Les jetons Google sont chiffrés côté serveur. Vous pouvez déconnecter Gmail depuis la gestion des
      livraisons et révoquer Stash depuis les autorisations de votre compte Google.
    </p>

    <h2>4. Destinataires et prestataires</h2>
    <p>Vos données sont accessibles uniquement lorsque cela est nécessaire au fonctionnement du service :</p>
    <ul>
      <li>Koyeb pour l’API et Neon pour la base PostgreSQL ;</li>
      <li>Cloudflare pour la diffusion, la sécurité réseau et la protection du domaine ;</li>
      <li>Google et Discord lorsque vous choisissez leur connexion ;</li>
      <li>Brevo pour l’envoi des emails de vérification et de récupération du compte ;</li>
      <li>Stripe pour les simulations de paiement et, le cas échéant, l’administration d’un abonnement existant ;</li>
      <li>les transporteurs ou services de suivi lorsque vous demandez l’actualisation d’un colis.</li>
    </ul>
    <p>
      Certains prestataires peuvent traiter des données hors de l’Espace économique européen selon leur
      organisation et leurs garanties contractuelles. Leurs politiques sont disponibles sur leurs sites.
      Stash ne vend ni ne loue vos données personnelles.
    </p>

    <h2>5. Durées de conservation</h2>
    <table>
      <thead><tr><th>Catégorie</th><th>Durée ou critère</th></tr></thead>
      <tbody>
        <tr><td>Compte et données de service</td><td>Pendant la vie du compte, puis suppression à votre demande</td></tr>
        <tr><td>Jeton de connexion</td><td>Une heure au maximum</td></tr>
        <tr><td>Sessions temporaires OAuth</td><td>Dix minutes au maximum</td></tr>
        <tr><td>Jetons de vérification et de réinitialisation</td><td>Jusqu’à expiration ou utilisation, puis purge technique</td></tr>
        <tr><td>Connexion Gmail</td><td>Jusqu’à sa révocation, sa suppression ou la suppression du compte</td></tr>
        <tr><td>Journaux techniques</td><td>Durée nécessaire au diagnostic et à la sécurité, selon la rotation des hébergeurs</td></tr>
        <tr><td>Sauvegardes</td><td>Jusqu’à leur rotation technique ; une suppression est réappliquée en cas de restauration</td></tr>
      </tbody>
    </table>

    <h2>6. Suppression et export</h2>
    <p>
      Vous pouvez télécharger un export JSON depuis votre compte. La suppression du compte efface le profil,
      l’inventaire, les documents, les suivis, les connexions Gmail et les réglages enregistrés sur le serveur.
      Les fichiers associés sont placés dans une procédure de suppression technique après validation de la
      transaction. Les données présentes uniquement dans votre navigateur doivent être effacées depuis ce navigateur.
    </p>

    <h2>7. Vos droits</h2>
    <p>
      Vous pouvez demander l’accès, la rectification, l’effacement, la limitation ou la portabilité de vos
      données. Vous pouvez également vous opposer aux traitements fondés sur l’intérêt légitime et retirer
      votre autorisation Gmail à tout moment. Une réponse est apportée en principe sous un mois. Une preuve
      d’identité peut être demandée uniquement en cas de doute raisonnable.
    </p>
    <p>
      Adressez votre demande à <a href="mailto:nathantalvasson@gmail.com">nathantalvasson@gmail.com</a>.
      Vous pouvez aussi déposer une réclamation auprès de la
      <a href="https://www.cnil.fr/fr/plaintes" target="_blank" rel="noopener noreferrer">CNIL</a>.
    </p>

    <h2>8. Stockage dans le navigateur</h2>
    <p>
      Stash n’intègre actuellement aucun outil publicitaire ni mesure d’audience. Les stockages nécessaires
      à la connexion et aux préférences sont détaillés dans la <RouterLink to="/cookies">politique cookies et stockage local</RouterLink>.
    </p>
  </LegalDocument>
</template>

<script setup>
import { RouterLink } from 'vue-router'
import LegalDocument from '@/components/legal/LegalDocument.vue'
</script>
