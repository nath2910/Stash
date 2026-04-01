# Glossaire pedagogique

## 1) JavaScript / TypeScript / Vue

- `async`:
Fonction qui retourne une Promise. Elle peut utiliser `await`.

- `await`:
Pause l'execution de la fonction `async` jusqu'a resolution de la Promise.

- `Promise`:
Objet representant un resultat futur (succes ou erreur).

- `ref(...)` (Vue):
Boite reactive qui contient une valeur dans `.value`.

- `computed(...)` (Vue):
Valeur calculee automatiquement a partir d'autres valeurs reactives.

- `watch(...)` (Vue):
Ecoute un changement reactif et declenche une action.

- `onMounted(...)` (Vue):
Hook appele apres montage du composant dans le DOM.

- `defineProps(...)`:
Declare les entrees d'un composant Vue.

- `defineEmits(...)`:
Declare les evenements emis par un composant Vue.

- `Composable`:
Fonction reutilisable Vue (ex: `useTheme`, `useStatsRange`) qui encapsule de la logique reactive.

- `SFC` (Single File Component):
Fichier `.vue` avec template + script + style.

- `withDefaults(defineProps<...>(), {...})`:
Version TypeScript pour donner des valeurs par defaut aux props.

- `Map` (JS):
Collection cle -> valeur souvent utilisee pour cache/requetes en vol.

- `debounce`:
Technique pour retarder une action (souvent save API) et eviter les appels excessifs.

---

## 2) Backend Spring

- `@RestController`:
Classe exposant endpoints HTTP JSON.

- `@Service`:
Classe logique metier.

- `@Repository`:
Acces DB (JPA/SQL).

- `@Entity`:
Classe mappee sur une table SQL.

- `@Transactional`:
Execution sous transaction DB.

- `@Cacheable`:
Met en cache le resultat d'une methode.

- `@CacheEvict`:
Supprime des entrees de cache.

- `ResponseStatusException`:
Exception HTTP standard Spring (400/401/403/500...).

- `@AuthenticationPrincipal`:
Injecte l'utilisateur authentifie courant dans le controller.

- `JWT`:
Token signe contenant l'identite (ici `sub=userId`).

- `OAuth2`:
Delegation d'authentification vers Google/Discord.

---

## 3) Notions metier du projet

- `SnkVente`:
Item de stock suivi (achat/revente), meme si le nom vient de "sneaker".

- `ItemType`:
Type metier (`SNEAKER`, `TICKET`, `CARD`, etc.) conditionnant metadata et pieces jointes.

- `metadata`:
JSON libre mais filtre selon le type d'item.

- `sellThrough`:
Taux d'ecoulement: vendus / (vendus + en stock).

- `asp`:
Average Selling Price (prix moyen de revente).

- `avgMargin`:
Marge moyenne par item vendu.

- `roi`:
Retour sur investissement, approx `(profit / cout)*100`.

- `deathPileAge`:
Repartition de l'anciennete du stock non vendu.

- `layout stats`:
Configuration du canvas (positions, tailles, props widgets, profils).

- `profile` (dans stats):
Sous-espace de dashboard (P1/P2/P3) avec son propre layout et sa propre plage date.

---

## 4) Termes UI stats canvas

- `Widget`:
Bloc visuel de stat (KPI, graphe, texte).

- `Widget registry`:
Catalogue central des widgets (type, titre, props par defaut, schema settings).

- `Widget palette`:
Panneau de selection/ajout de widgets.

- `Widget frame`:
Conteneur interactif de chaque widget (drag/resize/actions/fullscreen).

- `Marquee selection`:
Selection par rectangle tire a la souris.

- `Group resize`:
Redimensionnement simultane d'un groupe de widgets selectionnes.

- `Pan/zoom camera`:
Deplacement et zoom de la vue canvas globale.

- `Snap guides`:
Aide d'alignement magnetique pendant drag/resize.

---

## 5) Risques de vocabulaire

- `snkVenteService` et `snkVenteController` commencent par minuscule: c'est atypique en Java mais fonctionnel.
- Plusieurs textes FR du repo sont encodes de facon incoherente (accents casses), donc interpretation a confirmer dans le code lui-meme.
