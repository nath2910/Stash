CREATE TABLE IF NOT EXISTS public.mail_tracking_candidates (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  mail_account_id BIGINT REFERENCES public.mail_accounts(id) ON DELETE SET NULL,
  parcel_id BIGINT REFERENCES public.parcels(id) ON DELETE SET NULL,
  dedupe_key VARCHAR(512) NOT NULL,
  provider_message_id VARCHAR(255),
  source_sender VARCHAR(500),
  source_subject VARCHAR(500),
  received_at TIMESTAMPTZ,
  tracking_number VARCHAR(80) NOT NULL,
  normalized_tracking_number VARCHAR(80) NOT NULL,
  carrier_slug VARCHAR(80),
  tracking_url TEXT,
  merchant_name VARCHAR(255),
  raw_status VARCHAR(120),
  context_snippet VARCHAR(700),
  confidence_score INT NOT NULL,
  confidence_level VARCHAR(20) NOT NULL,
  candidate_status VARCHAR(40) NOT NULL DEFAULT 'NEEDS_REVIEW',
  reason VARCHAR(700),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_mail_tracking_candidates_user_dedupe UNIQUE (user_id, dedupe_key)
);

CREATE INDEX IF NOT EXISTS idx_mail_tracking_candidates_user_status
  ON public.mail_tracking_candidates(user_id, candidate_status, received_at DESC);

CREATE INDEX IF NOT EXISTS idx_mail_tracking_candidates_tracking
  ON public.mail_tracking_candidates(normalized_tracking_number);
