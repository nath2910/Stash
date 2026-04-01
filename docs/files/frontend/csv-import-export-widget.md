# Frontend - deep dive `CsvImportExportWidget.vue`

## 1) Role du composant
Composant central pour:
1. exporter la liste filtrée en CSV.
2. importer un CSV/XLSX vers l'API backend.

C'est un fichier sensible car il traduit des donnees heterogenes en payload metier strict.

---

## 2) Bloc Export CSV

### Variables cle
- `EXPORT_METADATA_COLUMNS`.
- `EXPORT_HEADERS`.
- `rowsToExport` (`computed` depuis `filteredRows`).

### `exportCsv()` pas a pas
1. Prend les lignes a exporter.
2. Helpers internes:
- `safe(v)` pour echapper guillemets/newlines,
- `toIsoDate(d)`,
- `toCsvNumber(value)`.
3. Construit une ligne CSV par item:
- extrait champs standards,
- calcule profit,
- injecte metadata colonnes dediees.
4. Prefixe BOM UTF-8 (`\uFEFF`) pour compat Excel FR.
5. Genere Blob + URL temporaire.
6. Declenche telechargement via `<a download>`.

Pedagogie JS:
- Le BOM evite les accents casses dans Excel.
- Le Blob permet telechargement sans serveur.

---

## 3) Etat import et UX
Variables:
- `fileInput`, `selectedFile`, `fileName`.
- `importing`, `progress`, `successMsg`, `errorMsg`.
- `prettySize` computed.

Fonctions UX:
- `pickFile()` ouvre le selecteur.
- `onFilePicked(e)` stocke le fichier choisi.
- `startProgress()` / `stopProgress()` animent barre progression.

---

## 4) Normalisation des entetes

### Fonctions
- `normalize(s)`.
- `findHeader(headers, synonyms)`.
- `looksBadName(name)`.

### Logique
1. Normalise entetes (lowercase, trim, suppression ponctuation).
2. Cherche correspondance exacte puis partielle via synonymes.
3. Tolere variantes FR/EN et noms historiques.

---

## 5) Conversion valeurs robustes

### Fonctions
- `toNumberSmart(v)`.
- `excelSerialToIso(value)`.
- `parseDateSmart(v)`.
- `parseMetadata(v)`.
- `buildMetadataFromColumns(row, headers, typeValue)`.

### Cas couverts
1. nombres avec virgule/point.
2. dates ISO, FR (`dd/mm/yyyy`), serial Excel.
3. metadata en JSON string ou colonnes dispersees.
4. metadata whitelist par type item.

Pedagogie JS:
- `XLSX.SSF.parse_date_code` convertit un numero interne Excel en date.

---

## 6) Parsing fichier (CSV ou Excel)

### Detection
- `isExcelFile(file)` selon extension MIME/nom.

### Parse Excel
- `parseExcel(file)`:
1. lit ArrayBuffer,
2. ouvre workbook,
3. choisit feuille contenant vraies donnees,
4. convertit feuille en tableau 2D,
5. `extractTableFrom2D` pour header+rows.

### Parse CSV
- `parseCsv(file)`:
1. tentative auto delimiter,
2. tentative `;` explicite,
3. fallback parse tableau 2D brut,
4. choix meilleur resultat.

### Orchestration
- `parseFileSmart(file)` appelle excel ou csv puis nettoie resultat.

---

## 7) Construction payload backend

### `buildPayload(rows, headers)`
1. Mappe chaque colonne metier (nom item, prix, dates, categorie, description, type, metadata).
2. Applique parse smart sur nombres/dates.
3. Elimine lignes invalides (nom vide, prix incoherents, etc.).
4. Retourne tableau DTO import prêt backend.

### `importNow()`
1. verifie fichier selectionne.
2. lance progression.
3. parse fichier.
4. build payload.
5. appelle `SnkVenteServices.importBulk(payload)`.
6. affiche resultat (`created`).
7. emit `imported` pour refresh parent.

---

## 8) Couplages
1. Parent `gestionPage.vue` passe `filteredRows` et ecoute `imported`.
2. Backend attend `SnkVenteImportDto` (noms/cas/type précis).
3. `constants/itemTypes.js` pilote metadata autorisee.

---

## 9) Risques
1. Heuristiques header/date peuvent manquer des fichiers atypiques.
2. Gestion locale de multiples formats: risque regressions silencieuses.
3. Ce composant fait beaucoup de choses (UI + parser + mapping metier).

---

## 10) Reco refactor
1. Extraire parser pur dans `src/lib/import/`.
2. Ajouter tests unitaires dataset fixtures (CSV FR/US, XLSX serial dates).
3. Garder ce composant sur role UI/orchestration uniquement.
