<template>
  <LegalDocument title="Conditions générales d’utilisation" description="Règles d’accès et d’utilisation de la version bêta de Stash.">
    <div class="legal-summary">
      <strong>À retenir</strong>
      <p>Stash est une bêta gratuite, sans abonnement payant ni engagement de disponibilité. Vous gardez la maîtrise de vos données et pouvez supprimer votre compte.</p>
    </div>

    <h2>1. Objet et acceptation</h2>
    <p>
      Les présentes conditions encadrent l’utilisation de Stash. La création d’un compte vaut acceptation
      de ces conditions et de la <RouterLink to="/privacy">politique de confidentialité</RouterLink>.
      Si vous n’acceptez pas ces documents, n’utilisez pas le service.
    </p>

    <h2>2. Version bêta et absence d’offre commerciale</h2>
    <p>
      Stash est mis à disposition gratuitement à des fins de test et d’amélioration. Aucun prix n’est dû,
      aucun abonnement payant n’est proposé et aucune relation vendeur-consommateur n’est créée à ce stade.
      Les fonctions, limites et modalités d’accès peuvent évoluer pendant la bêta.
    </p>
    <p>
      Une éventuelle offre payante fera l’objet, avant son ouverture, de conditions générales de vente
      distinctes, de tarifs complets et de l’identification légale du professionnel.
    </p>

    <h2>3. Compte utilisateur</h2>
    <p>
      Vous devez fournir des informations exactes, protéger vos identifiants et nous prévenir rapidement
      en cas d’accès non autorisé. Un compte est personnel. Vous êtes responsable des opérations réalisées
      depuis votre compte, sauf utilisation frauduleuse indépendante de votre volonté.
    </p>

    <h2>4. Utilisation autorisée</h2>
    <p>Vous vous engagez à ne pas :</p>
    <ul>
      <li>utiliser Stash à des fins illicites, frauduleuses ou portant atteinte aux droits d’un tiers ;</li>
      <li>tenter de contourner la sécurité, les contrôles d’accès ou les limites techniques ;</li>
      <li>importer des données, fichiers ou contenus que vous n’êtes pas autorisé à traiter ;</li>
      <li>perturber le service, automatiser des requêtes abusives ou introduire un contenu malveillant.</li>
    </ul>

    <h2>5. Données et contenus importés</h2>
    <p>
      Vous conservez vos droits sur vos contenus. Vous accordez uniquement les autorisations techniques
      nécessaires pour les héberger, les traiter et vous fournir les fonctions demandées. Si vous saisissez
      des données relatives à des clients ou à d’autres tiers, vous devez disposer d’une base légale et ne
      collecter que les informations nécessaires.
    </p>

    <h2>6. Services externes</h2>
    <p>
      Les connexions à Google, Discord ou Gmail sont facultatives. Leur utilisation est également soumise
      aux règles de ces fournisseurs. Le suivi des colis dépend des informations communiquées par les
      transporteurs, qui peuvent être retardées, incomplètes ou indisponibles.
    </p>

    <h2>7. Disponibilité et limites</h2>
    <p>
      La bêta est fournie avec un objectif raisonnable de sécurité et de continuité, sans garantie de
      disponibilité permanente. Des interruptions peuvent intervenir pour maintenance, correction ou en
      raison d’un prestataire. Pensez à exporter régulièrement les données importantes.
    </p>
    <p>
      Stash ne garantit pas l’exactitude juridique, fiscale ou comptable des calculs et documents générés.
      Vous devez les vérifier avant toute utilisation officielle. Rien dans ces conditions n’exclut une
      responsabilité qui ne pourrait pas légalement être limitée.
    </p>

    <h2>8. Suspension et suppression</h2>
    <p>
      Un accès peut être suspendu en cas de risque de sécurité, d’abus ou de violation de ces conditions.
      Vous pouvez exporter vos données puis supprimer votre compte depuis votre espace personnel. La
      suppression entraîne la fermeture de l’accès et l’effacement des données associées, sous réserve des
      délais techniques strictement nécessaires.
    </p>

    <h2>9. Évolution des conditions</h2>
    <p>
      La version applicable est celle publiée sur cette page. Toute modification importante sera signalée
      de manière visible. Si une modification nécessite votre accord, il sera demandé avant sa prise d’effet.
    </p>

    <h2>10. Droit applicable et contact</h2>
    <p>
      Ces conditions sont soumises au droit français. En cas de difficulté, contactez d’abord
      <a href="mailto:nathantalvasson@gmail.com">nathantalvasson@gmail.com</a> afin de rechercher une solution amiable.
    </p>
  </LegalDocument>
</template>

<script setup>
import { RouterLink } from 'vue-router'
import LegalDocument from '@/components/legal/LegalDocument.vue'
</script>
