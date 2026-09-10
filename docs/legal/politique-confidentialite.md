# POLITIQUE DE CONFIDENTIALITÉ

**Dernière mise à jour : 10 septembre 2026**

**Conforme au Règlement Général sur la Protection des Données (RGPD) — Règlement (UE) 2016/679**

---

## 1. INFORMATIONS GÉNÉRALES

### 1.1 Responsable de traitement

**Responsable du traitement :** Nathan Talvasson
**Forme juridique :** Particulier hébergeant une application web à titre personnel — sans statut professionnel
**Adresse :** [À COMPLÉTER]
**E-mail :** [À COMPLÉTER]

### 1.2 DPO (Délégué à la Protection des Données)

À la date de la présente politique, le Responsable de traitement ne dispose pas de DPO désigné formellement. Toute demande relative à la protection des données personnelles doit être adressée directement au Responsable de traitement (voir article 8).

**L'Éditeur s'engage à désigner un DPD (Data Protection Officer) dès lors que le traitement de données personnelles atteindra un seuil nécessitant sa désignation conformément à l'article 37 du RGPD.**

### 1.3 Objet de la politique

La présente Politique de Confidentialité décrit comment l'application **Mystash** collecte, utilise, conserve, protège et partage les données personnelles des Utilisateurs.

### 1.4 Champ d'application

Cette Politique s'applique à :
- Toute donnée personnelle collectée via le Service (site web et application)
- Toute donnée personnelle collectée par e-mail, téléphone ou tout autre canal
- Toute donnée personnelle collectée par l'intermédiaire de tiers (prestataires, partenaires)
- Tout contenu soumis par les Utilisateurs

**Cette Politique ne s'applique pas aux données collectées par des tiers (y compris Stripe, Google, Discord, Brevo) ni aux pratiques de ces tiers.**

---

## 2. DONNÉES PERSONNELLES COLLECTÉES

### 2.1 Catégories de données

Le Service collecte les catégories de données personnelles suivantes :

#### a) Données d'identité
| Donnée | Finalité | Obligatoire / Facultatif |
|---|---|---|
| Prénom | Identification | Obligatoire |
| Nom | Identification | Obligatoire |
| Adresse e-mail | Authentification, communication | Obligatoire |
| Identifiant OAuth (Google/Discord) | Connexion via tiers | Facultatif |

#### b) Données d'authentification
| Donnée | Finalité | Obligatoire / Facultatif |
|---|---|---|
| Hash de mot de passe | Authentification locale | Obligatoire (si connexion locale) |
| Tokens JWT | Session authentifiée | Obligatoire |
| Tokens de reset/verification email | Réinitialisation et vérification | Obligatoire |

#### c) Données de contenu
| Donnée | Finalité | Obligatoire / Facultatif |
|---|---|---|
| Données de stock (marque, modèle, taille, prix, description) | Fonctionnement du Service | Obligatoire |
| Images / pièces jointes | Fiches produits | Facultatif |
| Historique des transactions | Statistiques | Obligatoire |
| Paramètres du dashboard | Personnalisation | Facultatif |

#### d) Données techniques
| Donnée | Finalité | Obligatoire / Facultatif |
|---|---|---|
| Adresse IP | Sécurité, statistiques | Obligatoire |
| Type de navigateur | Compatibilité, débogage | Obligatoire |
| Système d'exploitation | Compatibilité, débogage | Obligatoire |
| Logs de connexion | Sécurité, audit | Obligatoire |
| Cookies | Fonctionnement, analytics | Facultatif |

#### e) Données financières (abonnement uniquement)
| Donnée | Finalité | Obligatoire / Facultatif |
|---|---|---|
| Informations de paiement (Stripe) | Facturation | Obligatoire (si abonnement) |
| Historique des abonnements | Gestion client | Obligatoire (si abonnement) |
| Factures | Archivage | Obligatoire (si abonnement) |

### 2.2 Source des données

Les données sont collectées directement auprès de l'Utilisateur lors de :
- L'inscription au compte
- La connexion via OAuth (Google/Discord)
- La saisie de données dans le Service
- L'utilisation du Service (logs techniques)
- Le paiement via Stripe (pour les abonnés)
- Le contact avec l'Éditeur (e-mail, formulaire)

### 2.3 Données sensibles

**Le Service ne collecte intentionnellement aucune donnée sensible au sens de l'article 9 du RGPD**, à savoir :
- Origine raciale ou ethnique
- Opinions politiques, philosophiques ou religieuses
- Appartenance syndicale
- Données génétiques
- Données biométriques
- Données relatives à la santé
- Données relatives à la vie sexuelle ou orientation sexuelle

**Exception :** Si l'Utilisateur choisit de partager des données sensibles via les pièces jointes ou le contenu du Service, l'Éditeur les traite conformément à la présente Politique et aux obligations légales applicables.

---

## 3. BASES LEGALES DU TRAITEMENT

Le traitement des données personnelles est fondé sur les bases légales suivantes :

### 3.1 Consentement

- Acceptation des CGU et de la présente Politique de Confidentialité
- Consentement aux cookies (le cas échéant)
- Consentement à la réception d'e-mails (newsletter, notifications)

### 3.2 Exécution d'un contrat

- Authentification et gestion du compte (nécessaire à l'exécution du contrat)
- Traitement des données de stock (nécessaire au fonctionnement du Service)
- Facturation et paiement (nécessaire à l'exécution du contrat d'abonnement)

### 3.3 Intérêts légitimes

- Sécurité du Service (détection de fraudes, prévention des attaques)
- Amélioration du Service (analytics, statistiques d'utilisation)
- Respect des obligations légales (conservation des logs)

### 3.4 Obligation légale

- Conservation des données comptables et fiscales (durée : 10 ans)
- Conformité au RGPD (archivage des consentements)
- Coopération avec les autorités judiciaires (sur réquisition)基础

---

## 4. FINALITÉS DU TRAITEMENT

### 4.1 Finalités principales

| Finalité | Données | Base légale | Durée |
|---|---|---|---|
| Authentification et connexion | Email, mot de passe, OAuth | Contrat | Durée du compte + 3 ans |
| Gestion du compte | Identité, préférences | Contrat | Durée du compte + 3 ans |
| Exécution du Service (stock, stats) | Données de contenu | Contrat | Durée du compte + 3 ans |
| Facturation et paiement | Informations Stripe | Contrat | 10 ans (obligation fiscale) |
| Envoi d'e-mails (reset, verify) | Email, tokens | Contrat | Jusqu'à complétion + 1 an |
| Sécurité du Service | IP, logs, activité | Intérêt légitime | 1 an |
| Statistiques et analytics | Utilisation, performance | Intérêt légitime | 1 an |
| Conformité légale | Divers | Obligation légale | Selon la loi applicable |

### 4.2 Traitement des données de stock

Les données saisies par l'Utilisateur (marques, modèles, prix, descriptions, images) sont traitées pour le bon fonctionnement du Service de gestion de stock. L'Éditeur ne revendique aucun droit sur ces données et ne les utilise pas à d'autres fins que le fonctionnement du Service.

### 4.3 Traitement des données statistiques

Les données agrégées et anonymisées (statistiques de fréquentation, temps de réponse, erreurs) sont collectées pour améliorer le Service. Ces données ne sont pas considérées comme des données personnelles.

---

## 5. PARTAGE DES DONNÉES

### 5.1 Destinataires des données personnelles

Les données personnelles peuvent être transmises aux catégories de destinataires suivantes :

#### a) Prestataires techniques (sous-traitants)

| Prestataire | Données | Finalité | Localisation |
|---|---|---|---|
| **Stripe** | Informations de paiement | Facturation | États-Unis (certifié PCI-DSS) |
| **Brevo (ex Sendinblue)** | Adresse e-mail | Emails transactionnels | France/UE |
| **Google** | Identifiant OAuth | Authentification | États-Unis |
| **Discord** | Identifiant OAuth | Authentification | États-Unis |
| **OVH/Scaleway/Hetzner** | Données serveur | Hébergement | Union Européenne |

**Note sur le transfert hors UE :** Le transfert de données personnelles hors de l'Union Européenne est effectué uniquement vers des pays disposant d'une décision d'adéquation de la Commission européenne ou via les mécanismes légaux suivants :
- **Clauses contractuelles types (SCC)** de la Commission européenne
- **Certification** du prestataire (ex : Privacy Shield/Schrems II — non validé, remplacé par le Data Privacy Framework pour les USA)
- **Consentement explicite** de l'Utilisateur après information sur les risques

#### b) Autorités publiques

Les données peuvent être communiquées aux autorités publiques dans les cas suivants :
- Réquisition judiciaire (ordonnance du Tribunal)
- Obligation légale (Loi sur la sécurité intérieure, etc.)
- Coopération avec les autorités fiscales

#### c) Cession d'entreprise

En cas de cession, fusion ou transformation de l'Éditeur en entreprise, les données personnelles peuvent être transférées au nouvel propriétaire. L'Éditeur s'engage à informer les Utilisateurs avant tout transfert de cette nature.

### 5.2 Vente ou partage de données

**Les données personnelles ne sont NI VENDUES, NI PARTAGÉES À DES TITRES PUBLICITAIRES, NI CÉDÉES À DES TIERCES PARTIES À DES FINS COMMERCIALES.**

Ceci constitue un engagement fondamental de l'Éditeur, d'autant plus important qu'il ne dispose pas d'un modèle commercial basé sur la monétisation des données.

---

## 6. CONSERVATION DES DONNÉES

### 6.1 Durées de conservation

| Catégorie de données | Durée de conservation | Justification |
|---|---|---|
| Données d'authentification (hash mdp) | Durée du compte + 3 ans | Sécurité, audit |
| Données de compte (identité) | Durée du compte + 3 ans | Gestion client |
| Données de stock | Durée du compte + 3 ans | Fonctionnement du Service |
| Logs techniques (IP, activités) | 1 an | Sécurité, débogage |
| Données de facturation (Stripe) | 10 ans | Obligation fiscale |
| Tokens de reset/verification | 1 heure (reset) / 48h (verify) | Sécurité |
| E-mails de contact | 3 ans | Gestion des demandes |
| Consentements RGPD | Durée du compte + 3 ans | Preuve de consentement |

### 6.2 Suppression

À l'expiration de la durée de conservation, les données personnelles sont supprimées de manière sécurisée et irréversible, conformément aux normes de l'ANSSI.

**Exception :** Les données nécessaires à la conformité légale (facturation, données fiscales) sont conservées pendant toute la durée de l'obligation légale, même après la suppression du compte.

### 6.3 Suppression sur demande

L'Utilisateur peut demander la suppression de ses données à tout moment (voir article 8). La suppression est effective dans un délai de **30 jours** suivant la demande, sauf obligation légale de conservation.

---

## 7. MESURES DE SÉCURITÉ

### 7.1 Mesures techniques

L'Éditeur met en œuvre les mesures techniques suivantes pour protéger les données personnelles :

1. **Chiffrement des données :**
   - SSL/TLS pour la transmission des données (HTTPS)
   - Chiffrement AES-256 pour les données au repos
   - Hachage bcrypt pour les mots de passe
   - Chiffrement JWT (HMAC-SHA256)

2. **Contrôle d'accès :**
   - Authentification forte (JWT)
   - Autorisation basée sur les rôles (user/admin)
   - Validation des tokens à chaque requête
   - Protection CSRF (via Spring Security)

3. **Sécurité réseau :**
   - Pare-feu applicatif (WAF)
   - Filtrage des adresses IP suspects
   - Limitation du taux de requêtes (rate limiting)
   - Détection d'anomalies

4. **Sauvegarde :**
   - Sauvegardes régulières de la base de données PostgreSQL
   - Sauvegarde des fichiers uploadés
   - Plan de reprise d'activité (DRP)

### 7.2 Mesures organisationnelles

1. **Formation :** L'Éditeur s'engage à former toute personne ayant accès aux données personnelles aux principes de sécurité des données
2. **Procédures :** Mise en place de procédures de gestion des incidents de sécurité
3. **Audit :** Vérification régulière des mesures de sécurité
4. **Documentation :** Tenue d'un registre des traitements (article 30 du RGPD)

### 7.3 Limites des mesures

**Avertissement :** L'Éditeur met en œuvre des mesures de sécurité raisonnables, mais ne peut garantir une sécurité absolue. L'Utilisateur reconnaît que la transmission de données via Internet comporte des risques inhérents (interception, accès non autorisé).

---

## 8. DROITS DES UTILISATEURS

Conformément aux articles 15 à 22 du RGPD, chaque Utilisateur dispose des droits suivants :

### 8.1 Droit d'accès (article 15)

L'Utilisateur a le droit d'obtenir la confirmation que ses données personnelles sont traitées et de recevoir une copie de ces données, ainsi que les informations sur les finalités, les catégories de données, les destinataires et la durée de conservation.

### 8.2 Droit de rectification (article 16)

L'Utilisateur a le droit de faire rectifier ses données personnelles inexactes ou de compléter ses données incomplètes.

### 8.3 Droit à l'effacement (article 17)

L'Utilisateur a le droit d'obtenir l'effacement de ses données personnelles dans les cas suivants :
- Les données ne sont plus nécessaires au regard des finalités pour lesquelles elles ont été collectées
- L'Utilisateur retire son consentement
- L'Utilisateur s'oppose au traitement
- Les données ont été traitées illégalement
- L'Utilisateur retire son compte

**Exception :** Le droit à l'effacement ne s'applique pas lorsque le traitement est nécessaire pour se conformer à une obligation légale (données fiscales, archivage comptable) ou pour l'établissement, l'exercice ou la défense de droits en justice.

### 8.4 Droit de restriction du traitement (article 18)

L'Utilisateur a le droit de demander la restriction du traitement de ses données dans les cas suivants :
- Contestation de l'exactitude des données (en attendant vérification)
- Traitement illégal (mais refus d'effacement)
- Données nécessaires à la défense de droits en justice
- Opposition au traitement (en attendant vérification)

### 8.5 Droit à la portabilité (article 20)

L'Utilisateur a le droit de recevoir ses données personnelles dans un format structuré, couramment utilisé et lisible par machine, et de les transmettre à un autre responsable de traitement.

**Application :** L'Utilisateur peut demander l'export de ses données (stock, paramètres, historique) via la fonction d'export CSV disponible dans le Service.

### 8.6 Droit d'opposition (article 21)

L'Utilisateur a le droit de s'opposer au traitement de ses données personnelles, notamment pour des motifs liés à sa situation particulière, dans les cas suivants :
- Traitement fondé sur l'intérêt légitime (statistics, sécurité)
- Traitement à des fins de marketing direct
- Traitement à des fins de recherche scientifique ou historique

### 8.7 Droit de retirer son consentement (article 7, paragraphe 3)

L'Utilisateur peut retirer son consentement à tout moment, sans affecter la licéité du traitement effectué avant le retrait du consentement.

### 8.8 Droit de plainte auprès d'une autorité de contrôle

L'Utilisateur a le droit de déposer une réclamation auprès de la **CNIL** (Commission Nationale de l'Informatique et des Libertés) ou de l'autorité de contrôle compétente dans son État membre, s'il estime que le traitement de ses données personnelles constitue une violation du RGPD.

**CNIL :** [https://www.cnil.fr/fr/contact](https://www.cnil.fr/fr/contact)

### 8.9 Exercice des droits

Pour exercer ses droits, l'Utilisateur peut contacter le Responsable de traitement par :

| Moyen | Détail |
|---|---|
| **E-mail** | nathantalvasson@gmail.com |
| **Formulaire** | [via le formulaire de contact du Service] |
| **Adresse postale** | [À COMPLÉTER] |

**Délai de réponse :** Le Responsable de traitement répond dans un délai d'**1 mois** (30 jours), renouvelable une fois dans des cas complexes (45 jours). Une information sur les motifs du retard peut être communiquée.

**Gratuité :** L'exercice des droits est gratuit. Le Responsable peut facturer des frais raisonnables en cas de demandes manifestement infondées, répétitives ou excessives.

**Preuve d'identité :** Avant de traiter une demande, le Responsable peut demander la preuve de l'identité de l'Utilisateur (vérification du compte, e-mail de confirmation, etc.).

---

## 9. COOKIES ET TECHNOLOGIES DE SUIVI

### 9.1 Types de cookies utilisés

| Cookie | Finalité | Durée | Obligatoire |
|---|---|---|---|
| Cookies de session | Authentification, navigation | Session | Oui |
| Cookies de préférence | Langue, thème, paramètres | 30 jours | Non |
| Cookies analytics | Statistiques d'utilisation | 6 mois | Non |
| Cookies de sécurité | Protection CSRF, anti-forgery | Session | Oui |

### 9.2 Désactivation des cookies

L'Utilisateur peut refuser ou supprimer les cookies à tout moment via les paramètres de son navigateur. Tefois, la désactivation des cookies peut affecter le fonctionnement du Service.

**Note :** Le Service n'utilise pas de cookies tiers à des fins de publicité ciblée. Aucune balise publicitaire (pixel tracker) n'est présente.

### 9.3 Cookies tiers

Les cookies tiers suivants peuvent être présents via les intégrations tierces :
- **Google Analytics** (si activé) — statistiques d'utilisation
- **Stripe.js** — traitement sécurisé des paiements
- **Google OAuth** — cookies d'authentification
- **Discord OAuth** — cookies d'authentification

**L'Utilisateur est informé que le rejet des cookies tiers peut empêcher le bon fonctionnement des intégrations (notamment l'authentification OAuth).**

---

## 10. TRANSFERTS INTERNATIONAUX DE DONNÉES

### 10.1 Localisation des données

Les données personnelles sont stockées sur des serveurs situés dans l'**Union Européenne** (serveur OVH/Scaleway/Hetzner situé en France/UE).

### 10.2 Transferts hors UE

Certains prestataires peuvent traiter des données en dehors de l'UE :
- **Stripe** (États-Unis) — traitement des paiements, avec clauses contractuelles types
- **Google** (États-Unis) — authentification OAuth, avec certification DPF (Data Privacy Framework)
- **Discord** (États-Unis) — authentification OAuth

**L'Utilisateur accepte que ses données soient transférées hors de l'UE dans le cadre de l'utilisation du Service. L'Éditeur s'engage à garantir un niveau de protection adéquat conformément aux exigences du RGPD.**

### 10.3 Mécanismes juridiques

Les transferts hors UE sont effectués conformément aux mécanismes suivants :
1. Décision d'adéquation de la Commission européenne (pays considérés comme offrant un niveau de protection adéquat)
2. Clauses contractuelles types (SCC) adoptées par la Commission européenne
3. Certification du prestataire (Data Privacy Framework pour les USA)
4. Consentement explicite de l'Utilisateur après information sur les risques

---

## 11. SAUVEGARDE DES DONNÉES ET PLAN DE REPRISE

### 11.1 Sauvegarde

- Sauvegardes régulières de la base de données PostgreSQL
- Sauvegarde des fichiers uploadés (pièces jointes)
- Tests de restauration périodiques

### 11.2 Plan de reprise d'activité (PRA)

En cas de sinistre (perte de données, panne serveur, cyberattaque) :
1. Restauration à partir de la dernière sauvegarde
2. Notification des Utilisateurs dans un délai de **72 heures** (conformément à l'article 33 du RGPD)
3. Communication à la CNIL dans un délai de **72 heures** (en cas de violation de données)

### 11.3 Perte accidentelle

En cas de perte accidentelle de données, l'Éditeur ne pourra en aucun cas être tenu responsable, conformément aux limitations de responsabilité définies dans les CGU et la CGV. **L'Utilisateur est invité à exporter régulièrement ses données.**

---

## 12. CONFORMITÉ RGPD

### 12.1 Registre des traitements

L'Éditeur tient un registre des traitements de données personnelles, conformément à l'article 30 du RGPD. Ce registre documente :
- La finalité de chaque traitement
- Les catégories de données concernées
- Les destinataires des données
- Les durées de conservation
- Les mesures de sécurité

### 12.2 Analyse d'impact (AIPD/DPIA)

Conformément à l'article 35 du RGPD, une Analyse d'Impact relative à la Protection des Données (AIPD) est réalisée pour tout traitement présentant un risque élevé pour les droits et libertés des personnes (traitement à grande échelle, données sensibles, surveillance systématique, etc.).

### 12.3 Notification des violations de données

En cas de violation de données personnelles :
1. **Notification à la CNIL dans les 72 heures** suivant la constatation de la violation (article 33 du RGPD)
2. **Notification aux Utilisateurs** sans retard injustifié si la violation est susceptible de constituer un risque élevé pour leurs droits (article 34 du RGPD)
3. **Documentation interne** de l'incident, de ses causes et des mesures correctives (article 33, paragraphe 5 du RGPD)

### 12.4 Sous-traitance

Les sous-traitants (Stripe, Brevo, hébergeur, etc.) sont désignés conformément à l'article 28 du RGPD via des contrats précisant :
- Les finalités et la nature du traitement
- Les catégories de données personnelles
- Les obligations et droits du sous-traitant
- Les mesures de sécurité à mettre en œuvre

---

## 13. MODIFICATIONS DE LA POLITIQUE DE CONFIDENTIALITÉ

L'Éditeur se réserve le droit de modifier la présente Politique de Confidentialité à tout moment. Les modifications prennent effet à leur publication sur le Service.

**L'Éditeur s'engage à :**
1. Publier la nouvelle Politique sur le Service
2. Informer les Utilisateurs des modifications substantielles par e-mail, avec un préavis d'au moins **15 jours**
3. Maintenir un historique des versions précédentes

L'utilisation continue du Service après la publication des modifications constitue l'acceptation de la nouvelle Politique.

---

## 14. CONTACT

Pour toute question relative à la présente Politique de Confidentialité ou pour exercer vos droits :

| Moyen | Détail |
|---|---|
| **E-mail** | nathantalvasson@gmail.com |
| **Adresse postale** | [À COMPLÉTER] |
| **Délai de réponse** | 30 jours |

En cas de réclamation auprès de la CNIL, l'Utilisateur peut contacter :
**CNIL** — [https://www.cnil.fr](https://www.cnil.fr)

---

## 15. DÉFINITIONS

| Terme | Définition |
|---|---|
| **Donnée personnelle** | Toute information se rapportant à une personne physique identifiée ou identifiable (article 4, paragraphe 1 du RGPD) |
| **Traitement** | Toute opération réalisée sur des données personnelles (collecte, enregistrement, conservation, modification, suppression, etc.) |
| **Responsable de traitement** | La personne physique qui détermine les finalités et les moyens du traitement (article 4, paragraphe 7 du RGPD) |
| **Sous-traitant** | La personne physique ou morale traitant les données pour le compte du Responsable (article 4, paragraphe 8 du RGPD) |
| **Consentement** | Toute manifestation de volonté libre, spécifique, éclairée et univoque (article 4, paragraphe 11 du RGPD) |
| **Violation de données** | Toute violation de sécurité entraînant la destruction, la perte, l'altération, la divulgation non autorisée ou l'accès aux données personnelles (article 4, paragraphe 12 du RGPD) |

---

*Ce document constitue un **brouillon juridique préparatoire**. L'Éditeur recommande vivement de consulter un **avocat spécialisé en droit de la protection des données** avant toute publication définitive, notamment pour vérifier la conformité au RGPD et adapter les clauses à son statut particulier.*

---

**Références légales :**
- Règlement (UE) 2016/679 du 27 avril 2016 relatif à la protection des données (RGPD)
- Directive 2002/58/CE du 12 juillet 2002 concernant le traitement des données à caractère personnel et la protection de la vie privée dans le secteur des communications électroniques (directive « ePrivacy »)
- Loi n° 78-17 du 6 janvier 1978 relative à l'informatique, aux fichiers et aux libertés (abrogée par le RGPD)
- Code pénal français (articles 322-16 à 322-24 — protection des données)
- Directive 95/46/CE du 24 octobre 1995 (abrogée par le RGPD)
- Délibérations de la CNIL (méthodologie, guides, recommandations)
- Standards de sécurité PCI-DSS (pour le traitement des données de paiement par Stripe)
