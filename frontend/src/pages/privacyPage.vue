<template>
  <section class="privacy-page w-full">
    <header class="privacy-hero">
      <p class="privacy-kicker">Confidentialite</p>
      <h1>Politique de confidentialite et cadre minimal de protection des donnees.</h1>
      <p>
        Cette page pose une base claire pour expliquer ce que l'application collecte, pourquoi, et
        comment l'utilisateur peut agir sur ses donnees.
      </p>
      <small>Derniere mise a jour: 09/07/2026</small>
    </header>

    <div class="privacy-grid">
      <article class="privacy-card">
        <h2>1. Donnees traitees</h2>
        <p>Compte utilisateur, inventaire, ventes, achats, pieces jointes, statut d'abonnement et historiques utiles au fonctionnement.</p>
      </article>

      <article class="privacy-card">
        <h2>2. Finalites</h2>
        <p>Assurer la connexion, afficher le stock, suivre les performances, preparer des rappels et generer des exports ou documents utiles.</p>
      </article>

      <article class="privacy-card">
        <h2>3. Paiement</h2>
        <p>Les operations de paiement et d'abonnement sont gerees par le prestataire dedie. L'application ne doit pas stocker les donnees bancaires completes.</p>
      </article>

      <article class="privacy-card">
        <h2>4. Conservation</h2>
        <p>Les donnees sont conservees le temps necessaire au service, a la gestion du compte et aux obligations legales ou comptables applicables.</p>
      </article>

      <article class="privacy-card">
        <h2>5. Acces et suppression</h2>
        <p>L'utilisateur doit pouvoir demander la consultation, la correction ou la suppression de ses donnees via le support ou le compte.</p>
      </article>

      <article class="privacy-card">
        <h2>6. Securite</h2>
        <p>Authentification, restrictions d'acces, journalisation utile et reduction des donnees sensibles exposees doivent rester la base du dispositif.</p>
      </article>
    </div>

    <article class="privacy-panel">
      <p class="privacy-kicker">Points a afficher pour vous proteger</p>
      <ul>
        <li>Indiquer un contact responsable des demandes liees aux donnees.</li>
        <li>Expliquer que les rappels administratifs sont une aide et non un depot officiel automatique.</li>
        <li>Preciser les outils tiers utilises pour paiement, auth ou email si c'est le cas.</li>
        <li>Documenter la suppression de compte et la duree de retention des pieces.</li>
      </ul>
    </article>

    <article class="privacy-panel privacy-panel--accent">
      <p class="privacy-kicker">Important</p>
      <p>
        Ce contenu aide a structurer votre page de confidentialite, mais ne remplace pas une
        validation juridique avant publication si l'application est ouverte au public.
      </p>
      <RouterLink to="/a-propos" class="privacy-link">Retour a la page A propos</RouterLink>
    </article>
  </section>
</template>

<script setup>
import { RouterLink } from 'vue-router'
</script>

<style scoped>
.privacy-page {
  display: grid;
  gap: 1.2rem;
  padding: clamp(5.5rem, 8vw, 7rem) 0 7rem;
}

.privacy-hero,
.privacy-card,
.privacy-panel {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 1.5rem;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(248, 250, 252, 0.96)),
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.08), transparent 40%);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.07);
}

.privacy-hero {
  padding: clamp(1.5rem, 3vw, 2.2rem);
}

.privacy-kicker {
  color: #1d4ed8;
  font-size: 0.72rem;
  font-weight: 900;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.privacy-hero h1 {
  margin-top: 0.85rem;
  max-width: 13ch;
  color: #0f172a;
  font-size: clamp(2rem, 4.6vw, 4rem);
  font-weight: 800;
  line-height: 1;
}

.privacy-hero p,
.privacy-hero small,
.privacy-card p,
.privacy-panel p,
.privacy-panel li {
  color: #475569;
  line-height: 1.75;
}

.privacy-hero p {
  margin-top: 1rem;
  max-width: 64ch;
}

.privacy-hero small {
  display: inline-block;
  margin-top: 0.9rem;
}

.privacy-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 17rem), 1fr));
  gap: 1rem;
}

.privacy-card,
.privacy-panel {
  padding: 1.3rem;
}

.privacy-card h2 {
  color: #0f172a;
  font-size: 1.08rem;
  font-weight: 760;
}

.privacy-card p {
  margin-top: 0.7rem;
}

.privacy-panel ul {
  margin-top: 0.8rem;
  display: grid;
  gap: 0.65rem;
  padding-left: 1.1rem;
}

.privacy-panel--accent {
  background:
    linear-gradient(180deg, rgba(15, 23, 42, 0.96), rgba(30, 41, 59, 0.96)),
    radial-gradient(circle at top left, rgba(96, 165, 250, 0.16), transparent 38%);
}

.privacy-panel--accent p,
.privacy-panel--accent .privacy-kicker {
  color: #e2e8f0;
}

.privacy-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 1rem;
  min-height: 2.9rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: white;
  font-weight: 800;
  padding: 0.72rem 1rem;
}

</style>
