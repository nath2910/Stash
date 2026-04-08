CREATE TABLE IF NOT EXISTS public.notifications (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  type VARCHAR(64) NOT NULL,
  title VARCHAR(180) NOT NULL,
  message VARCHAR(500) NOT NULL,
  severity VARCHAR(16) NOT NULL DEFAULT 'INFO',
  entity_type VARCHAR(32) NOT NULL,
  entity_id BIGINT,
  milestone_key VARCHAR(120) NOT NULL,
  cta_route VARCHAR(160),
  cta_label VARCHAR(80),
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  read_at TIMESTAMPTZ,
  dismissed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_created_at
  ON public.notifications(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_user_unread_created_at
  ON public.notifications(user_id, is_read, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_user_entity
  ON public.notifications(user_id, entity_type, entity_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_notifications_business_key
  ON public.notifications (
    user_id,
    type,
    entity_type,
    COALESCE(entity_id, 0),
    milestone_key
  );
