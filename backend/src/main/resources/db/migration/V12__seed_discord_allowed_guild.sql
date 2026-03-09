INSERT INTO discord_allowed_guild (guild_id, premium_role_id, label)
VALUES ('952160142286266430', '1149703162786230417', 'Discord client prod')
ON CONFLICT (guild_id)
DO UPDATE SET premium_role_id = EXCLUDED.premium_role_id,
              label = EXCLUDED.label;