# Import/export frontend

## Responsabilites
Le systeme d'import/export frontend est partage entre:
- `components/gestion/CsvImportExportWidget.vue` pour l'interface, le drag/drop, les etats et l'appel API;
- `utils/stockImportExport.ts` pour les helpers purs et testables.

## Formats supportes
- CSV, TSV, TXT delimite via PapaParse.
- XLS/XLSX via XLSX charge en lazy import.
- JSON simple contenant un tableau direct ou une cle `items`, `rows`, `data`, `stock`, `inventory`, `products`.
- Texte key/value par blocs.

## Pipeline import
1. Selection ou drop fichier.
2. Detection format.
3. Extraction table `{ headers, rows }`.
4. Resolution mapping colonnes.
5. Preview avec erreurs, warnings, lignes valides, doublons et payload.
6. Ajustement manuel du mapping si besoin.
7. Envoi `SnkVenteServices.importBulk(payload)`.

## Regles importantes
- Ne pas modifier le contrat `/snkVente/import`.
- Garder la detection doublon avec le stock deja affiche.
- Limiter a 500 items par import et 50 quantites par ligne.
- Garder PapaParse/XLSX en lazy import pour eviter d'alourdir le chargement initial.
- Ajouter des tests dans `tests/stockImportExport.test.ts` pour toute evolution de mapping ou validation.
