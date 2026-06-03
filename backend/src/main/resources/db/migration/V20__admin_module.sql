CREATE TABLE IF NOT EXISTS public.admin_states (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  payload JSONB DEFAULT '{}'::jsonb NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
  CONSTRAINT uk_admin_states_user UNIQUE (user_id)
);

CREATE TABLE IF NOT EXISTS public.admin_invoices (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  document_number VARCHAR(80),
  document_type VARCHAR(40) DEFAULT 'invoice' NOT NULL,
  status VARCHAR(32) DEFAULT 'draft' NOT NULL,
  document_date DATE,
  customer_name VARCHAR(240),
  total_ttc NUMERIC(14,2) DEFAULT 0 NOT NULL,
  payload JSONB DEFAULT '{}'::jsonb NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT now() NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_admin_invoices_user_created
  ON public.admin_invoices(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_admin_invoices_user_number
  ON public.admin_invoices(user_id, document_number);
