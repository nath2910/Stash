CREATE TABLE IF NOT EXISTS public.mail_accounts (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  provider VARCHAR(32) NOT NULL,
  provider_account_id VARCHAR(255) NOT NULL,
  email_address VARCHAR(320) NOT NULL,
  scopes TEXT NOT NULL,
  encrypted_refresh_token TEXT NOT NULL,
  encrypted_access_token TEXT,
  access_token_expires_at TIMESTAMPTZ,
  scan_cursor TEXT,
  last_scan_at TIMESTAMPTZ,
  next_scan_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  error_count INT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_mail_accounts_provider_account UNIQUE (provider, provider_account_id),
  CONSTRAINT uq_mail_accounts_user_provider_email UNIQUE (user_id, provider, email_address)
);

CREATE INDEX IF NOT EXISTS idx_mail_accounts_user_created_at
  ON public.mail_accounts(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_mail_accounts_due_scan
  ON public.mail_accounts(status, next_scan_at);

CREATE TABLE IF NOT EXISTS public.seen_mail_messages (
  id BIGSERIAL PRIMARY KEY,
  mail_account_id BIGINT NOT NULL REFERENCES public.mail_accounts(id) ON DELETE CASCADE,
  provider_message_id VARCHAR(255) NOT NULL,
  received_at TIMESTAMPTZ,
  parsed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_seen_mail_messages_account_message UNIQUE (mail_account_id, provider_message_id)
);

CREATE INDEX IF NOT EXISTS idx_seen_mail_messages_account_parsed_at
  ON public.seen_mail_messages(mail_account_id, parsed_at DESC);

CREATE TABLE IF NOT EXISTS public.parcels (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  mail_account_id BIGINT REFERENCES public.mail_accounts(id) ON DELETE SET NULL,
  tracking_number VARCHAR(80) NOT NULL,
  normalized_tracking_number VARCHAR(80) NOT NULL,
  carrier_slug VARCHAR(80),
  aggregator VARCHAR(64) NOT NULL DEFAULT 'AFTERSHIP',
  aggregator_tracking_id VARCHAR(255),
  status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
  status_label VARCHAR(255),
  estimated_delivery_at TIMESTAMPTZ,
  delivered_at TIMESTAMPTZ,
  source_provider_message_id VARCHAR(255),
  raw_current_payload JSONB,
  first_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_event_at TIMESTAMPTZ,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_parcels_user_tracking_carrier UNIQUE (user_id, normalized_tracking_number, carrier_slug)
);

CREATE INDEX IF NOT EXISTS idx_parcels_user_updated_at
  ON public.parcels(user_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_parcels_tracking
  ON public.parcels(normalized_tracking_number);

CREATE INDEX IF NOT EXISTS idx_parcels_aggregator_tracking
  ON public.parcels(aggregator, aggregator_tracking_id);

CREATE TABLE IF NOT EXISTS public.parcel_events (
  id BIGSERIAL PRIMARY KEY,
  parcel_id BIGINT NOT NULL REFERENCES public.parcels(id) ON DELETE CASCADE,
  event_hash VARCHAR(128) NOT NULL,
  status VARCHAR(40) NOT NULL,
  substatus VARCHAR(120),
  description TEXT,
  location VARCHAR(255),
  event_time TIMESTAMPTZ NOT NULL,
  raw_payload JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_parcel_events_hash UNIQUE (parcel_id, event_hash)
);

CREATE INDEX IF NOT EXISTS idx_parcel_events_parcel_time
  ON public.parcel_events(parcel_id, event_time DESC);

CREATE TABLE IF NOT EXISTS public.tracking_webhook_events (
  id BIGSERIAL PRIMARY KEY,
  aggregator VARCHAR(64) NOT NULL,
  external_event_id VARCHAR(255),
  signature_valid BOOLEAN NOT NULL DEFAULT FALSE,
  payload JSONB NOT NULL,
  processed_at TIMESTAMPTZ,
  received_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_tracking_webhook_events_external
  ON public.tracking_webhook_events(aggregator, external_event_id)
  WHERE external_event_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_tracking_webhook_events_received_at
  ON public.tracking_webhook_events(received_at DESC);
