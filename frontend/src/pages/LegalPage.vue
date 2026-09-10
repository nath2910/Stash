<template>
  <LegalDocument title="Mentions légales" description="Informations relatives à l’édition et à l’hébergement de Stash.">
    <div class="legal-summary">
      <strong>Statut du service</strong>
      <p>Stash est actuellement un projet personnel proposé gratuitement en version bêta. Aucune offre commerciale ni souscription payante n’est ouverte.</p>
    </div>

    <h2>Édition et publication</h2>
    <p>
      Le service Stash est édité à titre non professionnel par Nathan Talvasson, personne physique.
      Directeur de la publication : Nathan Talvasson.
    </p>
    <p>
      Contact : <a href="mailto:nathantalvasson@gmail.com">nathantalvasson@gmail.com</a>.
    </p>
    <p>
      L’éditeur non professionnel a communiqué son identité et ses coordonnées aux prestataires
      d’hébergement. Elles peuvent être transmises aux autorités compétentes dans les conditions prévues
      par la loi.
    </p>

    <h2>Hébergement et infrastructure technique</h2>
    <ul>
      <li>
        site, diffusion et sécurité réseau : <a href="https://www.cloudflare.com/" target="_blank" rel="noopener noreferrer">Cloudflare, Inc.</a>,
        101 Townsend Street, San Francisco, CA 94107, États-Unis — téléphone : +1 650 319 8930 ;
      </li>
      <li>
        API : <a href="https://www.koyeb.com/docs/legal/msa" target="_blank" rel="noopener noreferrer">Koyeb SAS</a>,
        9 rue des Longs Prés, 92100 Boulogne-Billancourt, France — déploiement en région France ;
      </li>
      <li>
        base de données : <a href="https://neon.com/" target="_blank" rel="noopener noreferrer">Neon, Inc.</a>,
        209 Orange Street, Wilmington, Delaware 19801, États-Unis ;
      </li>
      <li>paiement : Stripe est accessible en mode test, sans débit réel ; le mode live reste désactivé tant que l’activité n’est pas déclarée.</li>
    </ul>
    <p>Les traitements associés sont détaillés dans la <RouterLink to="/privacy">politique de confidentialité</RouterLink>.</p>

    <h2>Objet du service</h2>
    <p>
      Stash permet de tester des fonctions de gestion d’inventaire, de suivi de ventes, de statistiques,
      de livraison et de préparation administrative. Les calculs, alertes et modèles fournis sont des
      outils d’aide. Ils ne remplacent ni un comptable, ni un avocat, ni une déclaration auprès d’une
      administration.
    </p>

    <h2>Propriété intellectuelle</h2>
    <p>
      L’interface, la marque Stash, les textes et le code non publié sont protégés par les règles
      applicables à la propriété intellectuelle. L’utilisateur reste propriétaire des informations,
      documents et fichiers qu’il importe dans son espace.
    </p>

    <h2>Signaler un problème</h2>
    <p>
      Pour signaler un contenu, une vulnérabilité, une atteinte à un droit ou demander une correction,
      écrivez à <a href="mailto:nathantalvasson@gmail.com">nathantalvasson@gmail.com</a> en décrivant précisément la demande.
    </p>
  </LegalDocument>
</template>

<script setup>
import { RouterLink } from 'vue-router'
import LegalDocument from '@/components/legal/LegalDocument.vue'
</script>
