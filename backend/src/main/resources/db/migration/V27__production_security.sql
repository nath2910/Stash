ALTER TABLE public.users ADD COLUMN session_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE public.users ADD COLUMN stripe_subscription_id VARCHAR(255);
ALTER TABLE public.users ADD COLUMN subscription_cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX uq_users_stripe_customer ON public.users(stripe_customer_id)
  WHERE stripe_customer_id IS NOT NULL;

-- Store only an event identifier, never the webhook payload or personal data.
CREATE TABLE public.billing_webhook_receipts (
  event_id VARCHAR(255) PRIMARY KEY,
  processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Issued accounting documents survive account deletion in a restricted archive.
CREATE FUNCTION public.accounting_retention_end(document_date DATE, end_month INTEGER, end_day INTEGER)
RETURNS DATE LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE
  closing DATE;
  first_of_month DATE;
BEGIN
  end_month := CASE WHEN end_month BETWEEN 1 AND 12 THEN end_month ELSE 12 END;
  end_day := CASE WHEN end_day BETWEEN 1 AND 31 THEN end_day ELSE 31 END;
  first_of_month := make_date(extract(year FROM document_date)::integer, end_month, 1);
  closing := first_of_month + (least(end_day, extract(day FROM first_of_month + interval '1 month - 1 day')::integer) - 1);
  IF closing < document_date THEN
    first_of_month := (first_of_month + interval '1 year')::date;
    closing := first_of_month + (least(end_day, extract(day FROM first_of_month + interval '1 month - 1 day')::integer) - 1);
  END IF;
  RETURN (closing + interval '10 years')::date;
END;
$$;

CREATE TABLE public.accounting_archives (
  id BIGSERIAL PRIMARY KEY,
  archived_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  retain_until DATE NOT NULL,
  document JSONB NOT NULL
);

CREATE TABLE public.storage_cleanup_queue (
  user_id BIGINT PRIMARY KEY,
  requested_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE public.gmail_oauth_states (
  state_hash VARCHAR(64) PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  expires_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE public.tableauventes DROP CONSTRAINT fk_tableauventes_user;
ALTER TABLE public.tableauventes ADD CONSTRAINT fk_tableauventes_user
  FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;
