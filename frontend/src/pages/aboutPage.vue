<template>
  <section class="about-page w-full">
    <div class="about-hero">
      <div class="about-copy">
        <p class="about-kicker">À propos</p>
        <h1>Stash aide les revendeurs à garder une gestion claire, rapide et exploitable.</h1>
        <p class="about-lead">
          L’application rassemble le stock, les ventes, l’abonnement et l’administratif dans une
          interface simple à piloter au quotidien.
        </p>
      </div>

      <div class="about-highlight-card">
        <span>Ce que l’application doit faire</span>
        <strong>Moins de friction, plus de suivi.</strong>
        <p>
          Des rappels utiles, un dossier propre, et assez de contexte pour agir sans transformer
          chaque écran en usine à gaz.
        </p>
      </div>
    </div>

    <div class="about-grid">
      <article class="about-card">
        <p class="about-card-kicker">Mission</p>
        <h2>Rendre la revente plus lisible</h2>
        <p>
          Suivre le stock, les marges, les ventes et les pièces administratives sans multiplier les
          outils ni perdre du temps sur des détails répétitifs.
        </p>
      </article>

      <article class="about-card">
        <p class="about-card-kicker">Approche</p>
        <h2>Une application utile avant d’être démonstrative</h2>
        <p>
          Les rappels, les exports et les contrôles sont là pour aider à prendre une décision ou à
          corriger un dossier, pas pour faire joli.
        </p>
      </article>

      <article class="about-card">
        <p class="about-card-kicker">Protection</p>
        <h2>Cadre et transparence</h2>
        <p>
          Une page de confidentialité dédiée explique les données traitées, leur usage, leur
          conservation et les droits utilisateur.
        </p>
      </article>
    </div>

    <div class="about-band">
      <article class="about-band-card">
        <p class="about-card-kicker">Ce que couvre Stash</p>
        <ul>
          <li>Stock, achats, ventes et recherche rapide.</li>
          <li>Alertes par domaine : revente, abonnement et administratif.</li>
          <li>Préparation du dossier administratif et accès aux exports utiles.</li>
        </ul>
      </article>

      <article class="about-band-card">
        <p class="about-card-kicker">À garder en tête</p>
        <ul>
          <li>L’application aide à préparer, contrôler et exporter les informations utiles.</li>
          <li>La validation officielle reste effectuée par l’utilisateur.</li>
          <li>Les outils fournis restent indicatifs : l’utilisateur garde la responsabilité de ses démarches.</li>
        </ul>
      </article>
    </div>

    <div class="about-cta">
      <div>
        <p class="about-card-kicker">Confidentialité</p>
        <h2>Consultez la page dédiée aux données et à la protection des utilisateurs.</h2>
      </div>
      <RouterLink to="/confidentialite" class="about-cta-link">Voir la confidentialité</RouterLink>
    </div>
  </section>
</template>

<script setup>
import { RouterLink } from 'vue-router'
</script>

<style scoped>
.about-page {
  display: grid;
  gap: 1.6rem;
  padding: clamp(5.5rem, 8vw, 7rem) 0 7rem;
}

.about-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(18rem, 0.95fr);
  gap: 1.2rem;
  align-items: stretch;
}

.about-copy,
.about-highlight-card,
.about-card,
.about-band-card,
.about-cta {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 1.6rem;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(248, 250, 252, 0.94)),
    radial-gradient(circle at top left, rgba(20, 184, 166, 0.08), transparent 40%);
  box-shadow: 0 28px 70px rgba(15, 23, 42, 0.08);
}

.about-copy {
  padding: clamp(1.5rem, 3vw, 2.4rem);
}

.about-kicker,
.about-card-kicker {
  color: #0f766e;
  font-size: 0.72rem;
  font-weight: 900;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.about-copy h1 {
  margin-top: 0.9rem;
  max-width: 12ch;
  color: #0f172a;
  font-size: clamp(2.2rem, 5vw, 4.4rem);
  font-weight: 800;
  line-height: 0.98;
}

.about-lead {
  margin-top: 1rem;
  max-width: 58ch;
  color: #334155;
  font-size: 1rem;
  line-height: 1.75;
}

.about-highlight-card {
  display: grid;
  align-content: end;
  gap: 0.8rem;
  padding: 1.5rem;
  background:
    linear-gradient(180deg, rgba(15, 23, 42, 0.94), rgba(17, 24, 39, 0.95)),
    radial-gradient(circle at top left, rgba(56, 189, 248, 0.18), transparent 45%);
  color: #e2e8f0;
}

.about-highlight-card span {
  color: #67e8f9;
  font-size: 0.72rem;
  font-weight: 900;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.about-highlight-card strong {
  font-size: 1.5rem;
  font-weight: 800;
  line-height: 1.1;
}

.about-highlight-card p {
  color: #cbd5e1;
  line-height: 1.7;
}

.about-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 16rem), 1fr));
  gap: 1rem;
}

.about-card,
.about-band-card {
  padding: 1.35rem;
}

.about-card h2,
.about-band-card h2,
.about-cta h2 {
  margin-top: 0.65rem;
  color: #0f172a;
  font-size: 1.25rem;
  font-weight: 750;
  line-height: 1.25;
}

.about-card p,
.about-band-card li,
.about-cta p {
  margin-top: 0.7rem;
  color: #475569;
  line-height: 1.7;
}

.about-band {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 18rem), 1fr));
  gap: 1rem;
}

.about-band-card ul {
  margin-top: 0.85rem;
  display: grid;
  gap: 0.65rem;
  padding-left: 1.1rem;
}

.about-cta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1.4rem;
}

.about-cta-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 3rem;
  border-radius: 999px;
  background: linear-gradient(135deg, #0f766e, #2563eb);
  color: white;
  font-size: 0.92rem;
  font-weight: 800;
  padding: 0.75rem 1.1rem;
  transition:
    transform 160ms ease,
    box-shadow 160ms ease;
}

.about-cta-link:hover {
  box-shadow: 0 18px 34px rgba(37, 99, 235, 0.2);
  transform: translateY(-1px);
}

@media (max-width: 920px) {
  .about-hero {
    grid-template-columns: 1fr;
  }

  .about-cta {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (prefers-reduced-motion: reduce) {
  .about-cta-link {
    transition: none;
  }

  .about-cta-link:hover {
    transform: none;
  }
}

</style>
