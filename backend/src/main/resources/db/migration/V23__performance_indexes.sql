-- Indexes added for production read paths introduced by typed items and dashboard filters.
-- They are additive and safe: no data changes, only query planner options.

CREATE INDEX IF NOT EXISTS idx_tableauventes_user_normtype_datevente
  ON public.tableauventes (user_id, (COALESCE(type, 'OTHER')), date_vente);

CREATE INDEX IF NOT EXISTS idx_tableauventes_user_normcategorie_datevente
  ON public.tableauventes (
    user_id,
    (COALESCE(NULLIF(btrim(categorie), ''), 'Sans sous-categorie')),
    date_vente
  );

CREATE INDEX IF NOT EXISTS idx_tableauventes_user_normtype_stock
  ON public.tableauventes (user_id, (COALESCE(type, 'OTHER')))
  WHERE date_vente IS NULL;

CREATE INDEX IF NOT EXISTS idx_tableauventes_user_normcategorie_stock
  ON public.tableauventes (
    user_id,
    (COALESCE(NULLIF(btrim(categorie), ''), 'Sans sous-categorie'))
  )
  WHERE date_vente IS NULL;

CREATE INDEX IF NOT EXISTS idx_tableauventes_user_type_dateachat
  ON public.tableauventes (user_id, type, date_achat);
