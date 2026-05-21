ALTER TABLE public.tableauventes
  ALTER COLUMN type TYPE VARCHAR(80);

UPDATE public.tableauventes
SET type = 'SNEAKER'
WHERE type IS NULL OR trim(type) = '';

ALTER TABLE public.tableauventes
  ALTER COLUMN type SET DEFAULT 'SNEAKER',
  ALTER COLUMN type SET NOT NULL;
