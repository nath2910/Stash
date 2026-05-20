ALTER TABLE parcels
  ADD COLUMN IF NOT EXISTS last_tracking_refresh_at TIMESTAMPTZ NULL,
  ADD COLUMN IF NOT EXISTS next_tracking_refresh_at TIMESTAMPTZ NULL;

UPDATE parcels
SET next_tracking_refresh_at = COALESCE(next_tracking_refresh_at, updated_at, first_seen_at, now())
WHERE status IN ('PENDING', 'REGISTERED', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'UNKNOWN');

CREATE INDEX IF NOT EXISTS idx_parcels_tracking_refresh
  ON parcels(next_tracking_refresh_at, status);
