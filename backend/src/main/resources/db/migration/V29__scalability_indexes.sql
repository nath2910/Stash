-- Extra indexes for the inventory and dashboard read paths.
-- They are additive only; no data is changed.

CREATE INDEX IF NOT EXISTS idx_tableauventes_user_visible_dateachat_desc
  ON public.tableauventes (user_id, date_achat DESC, id DESC)
  WHERE COALESCE(is_group_parent, false) = false;

CREATE INDEX IF NOT EXISTS idx_tableauventes_user_stock_timeline
  ON public.tableauventes (user_id, date_achat, date_vente)
  WHERE date_achat IS NOT NULL;
