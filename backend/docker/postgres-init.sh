#!/bin/sh
set -eu

# V1 comes from pg_dump/psql 18. Strip the three psql/PostgreSQL 18-only lines
# so a fresh local PostgreSQL 17 container can import the historical schema.
sed \
  -e '/^\\restrict /d' \
  -e '/^\\unrestrict /d' \
  -e '/^SET transaction_timeout =/d' \
  /opt/stash-migrations/V1__init.sql \
  | psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB"
