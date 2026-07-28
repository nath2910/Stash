ALTER TABLE public.users
  ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now();

UPDATE public.users
SET created_at = COALESCE(created_at, now())
WHERE created_at IS NULL;
