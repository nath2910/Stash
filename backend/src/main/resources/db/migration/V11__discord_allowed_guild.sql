CREATE TABLE IF NOT EXISTS discord_allowed_guild (
    id BIGSERIAL PRIMARY KEY,
    guild_id VARCHAR(32) NOT NULL UNIQUE,
    premium_role_id VARCHAR(32),
    label VARCHAR(128)
);
