-- Ajouter le type d'item et les métadonnées spécifiques
ALTER TABLE public.tableauventes
  ADD COLUMN IF NOT EXISTS type VARCHAR(30) DEFAULT 'SNEAKER';

ALTER TABLE public.tableauventes
  ADD COLUMN IF NOT EXISTS metadata JSONB;

UPDATE public.tableauventes SET type = 'SNEAKER' WHERE type IS NULL;
ALTER TABLE public.tableauventes ALTER COLUMN type SET NOT NULL;

-- Table des pièces jointes (PDF, images...)
CREATE TABLE IF NOT EXISTS public.attachments (
  id BIGSERIAL PRIMARY KEY,
  item_id INTEGER NOT NULL REFERENCES public.tableauventes(id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  filename VARCHAR(260) NOT NULL,
  mime_type VARCHAR(120),
  size_bytes BIGINT NOT NULL,
  storage_key VARCHAR(400) NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now() NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_attachments_item ON public.attachments(item_id);
CREATE INDEX IF NOT EXISTS idx_attachments_user ON public.attachments(user_id);
