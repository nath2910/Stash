<template>
  <LegalDocument title="Conditions generales d'utilisation" description="Regles d'acces et d'utilisation de MyStash.">
    <div class="legal-summary">
      <strong>A retenir</strong>
      <p>MyStash est un outil de gestion. L'acces aux fonctions payantes depend d'un abonnement valide ou d'un acces special explicitement accorde.</p>
    </div>

    <h2>1. Objet et acceptation</h2>
    <p>
      Les presentes conditions encadrent l'utilisation de MyStash. La creation d'un compte vaut
      acceptation de ces conditions et de la <RouterLink to="/privacy">politique de confidentialite</RouterLink>.
      Si vous n'acceptez pas ces documents, n'utilisez pas le service.
    </p>

    <h2>2. Acces au service et abonnement</h2>
    <p>
      Certaines fonctions necessitent un abonnement actif, gere par Stripe, ou un acces special accorde
      par MyStash. Les prix, periodes et conditions applicables sont ceux affiches dans l'application ou
      dans le parcours de paiement au moment de la souscription.
    </p>
    <p>
      L'acces peut etre interrompu en cas de paiement refuse, d'abonnement annule, d'expiration d'un acces
      special ou de violation des presentes conditions.
    </p>

    <h2>3. Compte utilisateur</h2>
    <p>
      Un compte est personnel. Vous devez fournir des informations exactes, proteger vos identifiants et
      nous prevenir rapidement en cas d'acces non autorise. Vous etes responsable des operations realisees
      depuis votre compte, sauf utilisation frauduleuse independante de votre volonte.
    </p>

    <h2>4. Utilisation autorisee</h2>
    <p>Vous vous engagez a ne pas :</p>
    <ul>
      <li>utiliser MyStash a des fins illicites, frauduleuses ou portant atteinte aux droits d'un tiers ;</li>
      <li>tenter de contourner la securite, les controles d'acces, l'abonnement ou les limites techniques ;</li>
      <li>importer des donnees, fichiers ou contenus que vous n'etes pas autorise a traiter ;</li>
      <li>perturber le service, automatiser des requetes abusives ou introduire un contenu malveillant.</li>
    </ul>

    <h2>5. Donnees et contenus importes</h2>
    <p>
      Vous conservez vos droits sur vos contenus. Vous accordez uniquement les autorisations techniques
      necessaires pour les heberger, les traiter et fournir les fonctions demandees. Si vous saisissez des
      donnees relatives a des clients ou a d'autres tiers, vous devez disposer d'une base legale et ne
      collecter que les informations necessaires.
    </p>

    <h2>6. Paiement et resiliation</h2>
    <p>
      Les paiements, factures, moyens de paiement et resilations sont traites via Stripe lorsque
      l'abonnement est souscrit par ce biais. Sauf indication contraire affichee lors du paiement,
      l'abonnement se renouvelle automatiquement jusqu'a sa resiliation.
    </p>

    <h2>7. Services externes</h2>
    <p>
      Les connexions a Google, Discord ou Gmail sont facultatives. Leur utilisation est egalement soumise
      aux regles de ces fournisseurs. Le suivi des colis depend des informations communiquees par les
      transporteurs, qui peuvent etre retardees, incompletes ou indisponibles.
    </p>

    <h2>8. Disponibilite et limites</h2>
    <p>
      MyStash met en oeuvre des moyens raisonnables de securite et de continuite, sans garantie de
      disponibilite permanente. Des interruptions peuvent intervenir pour maintenance, correction ou en
      raison d'un prestataire. Pensez a exporter regulierement les donnees importantes.
    </p>
    <p>
      MyStash ne garantit pas l'exactitude juridique, fiscale ou comptable des calculs et documents
      generes. Vous devez les verifier avant toute utilisation officielle. Rien dans ces conditions
      n'exclut une responsabilite qui ne pourrait pas legalement etre limitee.
    </p>

    <h2>9. Suspension et suppression</h2>
    <p>
      Un acces peut etre suspendu en cas de risque de securite, d'abus, de paiement non valide ou de
      violation de ces conditions. Vous pouvez exporter vos donnees puis supprimer votre compte depuis
      votre espace personnel. La suppression entraine la fermeture de l'acces et l'effacement des donnees
      associees, sous reserve des delais techniques strictement necessaires.
    </p>

    <h2>10. Evolution des conditions</h2>
    <p>
      La version applicable est celle publiee sur cette page. Toute modification importante sera signalee
      de maniere visible. Si une modification necessite votre accord, il sera demande avant sa prise d'effet.
    </p>

    <h2>11. Droit applicable et contact</h2>
    <p>
      Ces conditions sont soumises au droit francais. En cas de difficulte, contactez d'abord
      <a href="mailto:nathantalvasson@gmail.com">nathantalvasson@gmail.com</a> afin de rechercher une solution amiable.
    </p>
  </LegalDocument>
</template>

<script setup>
import { RouterLink } from 'vue-router'
import LegalDocument from '@/components/legal/LegalDocument.vue'
</script>
