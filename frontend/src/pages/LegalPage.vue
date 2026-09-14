<template>
  <LegalDocument title="Mentions legales" description="Informations relatives a l'edition et a l'hebergement de MyStash.">
    <div class="legal-summary">
      <strong>Statut du service</strong>
      <p>MyStash est un service en ligne de gestion d'inventaire et de suivi d'activite accessible selon les conditions et offres presentees dans l'application.</p>
    </div>

    <h2>Edition et publication</h2>
    <p>
      Le service MyStash est edite par Nathan Talvasson.
      Directeur de la publication : Nathan Talvasson.
    </p>
    <p>
      Contact : <a href="mailto:nathantalvasson@gmail.com">nathantalvasson@gmail.com</a>.
    </p>

    <h2>Hebergement et prestataires techniques</h2>
    <ul>
      <li>diffusion du site et securite reseau : <a href="https://www.cloudflare.com/" target="_blank" rel="noopener noreferrer">Cloudflare</a> ;</li>
      <li>hebergement de l'API : <a href="https://www.koyeb.com/" target="_blank" rel="noopener noreferrer">Koyeb</a> ;</li>
      <li>base de donnees : <a href="https://neon.com/" target="_blank" rel="noopener noreferrer">Neon</a> ;</li>
      <li>paiements et abonnements : <a href="https://stripe.com/" target="_blank" rel="noopener noreferrer">Stripe</a>.</li>
    </ul>
    <p>Les traitements associes sont detailles dans la <RouterLink to="/privacy">politique de confidentialite</RouterLink>.</p>

    <h2>Objet du service</h2>
    <p>
      MyStash propose des fonctions de gestion d'inventaire, de suivi de ventes, de statistiques,
      de livraison et de preparation administrative. Les calculs, alertes et documents generes sont
      des outils d'aide. Ils ne remplacent ni un comptable, ni un avocat, ni une declaration officielle
      aupres d'une administration.
    </p>

    <h2>Propriete intellectuelle</h2>
    <p>
      L'interface, la marque MyStash, les textes et le code non publie sont proteges par les regles
      applicables a la propriete intellectuelle. L'utilisateur reste proprietaire des informations,
      documents et fichiers qu'il importe dans son espace.
    </p>

    <h2>Signaler un probleme</h2>
    <p>
      Pour signaler un contenu, une vulnerabilite, une atteinte a un droit ou demander une correction,
      ecrivez a <a href="mailto:nathantalvasson@gmail.com">nathantalvasson@gmail.com</a> en decrivant precisement la demande.
    </p>
  </LegalDocument>
</template>

<script setup>
import { RouterLink } from 'vue-router'
import LegalDocument from '@/components/legal/LegalDocument.vue'
</script>
