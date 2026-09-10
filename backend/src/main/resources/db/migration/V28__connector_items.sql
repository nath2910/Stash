CREATE TABLE public.connector_items (
  id BIGSERIAL PRIMARY KEY,
  platform VARCHAR(24) NOT NULL,
  platform_item_id VARCHAR(255) NOT NULL,
  title VARCHAR(500) NOT NULL,
  price NUMERIC(14, 2),
  currency VARCHAR(8),
  condition VARCHAR(80),
  seller_id VARCHAR(255),
  images_json TEXT,
  url VARCHAR(2000),
  posted_at TIMESTAMP,
  category VARCHAR(60),
  item_type VARCHAR(80),
  fetched_at TIMESTAMP NOT NULL,
  synced BOOLEAN NOT NULL DEFAULT FALSE,
  user_id BIGINT REFERENCES public.users(id) ON DELETE CASCADE,
  CONSTRAINT uq_connector_item_owner UNIQUE (user_id, platform, platform_item_id)
);

CREATE INDEX idx_connector_items_owner_fetched ON public.connector_items(user_id, fetched_at DESC);
CREATE INDEX idx_connector_items_owner_sync ON public.connector_items(user_id, synced);
