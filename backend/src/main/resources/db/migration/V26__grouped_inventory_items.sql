ALTER TABLE public.tableauventes
  ADD COLUMN IF NOT EXISTS parent_id INTEGER REFERENCES public.tableauventes(id) ON DELETE CASCADE,
  ADD COLUMN IF NOT EXISTS is_group_parent BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS unit_index INTEGER;

CREATE INDEX IF NOT EXISTS idx_tableauventes_parent_id ON public.tableauventes(parent_id);
CREATE INDEX IF NOT EXISTS idx_tableauventes_group_parent ON public.tableauventes(user_id, is_group_parent);
