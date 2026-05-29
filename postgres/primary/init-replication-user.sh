#!/usr/bin/env bash
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-SQL
  DO \$\$
  BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${POSTGRES_REPLICATION_USER}') THEN
      CREATE ROLE ${POSTGRES_REPLICATION_USER} WITH REPLICATION LOGIN PASSWORD '${POSTGRES_REPLICATION_PASSWORD}';
    END IF;
  END
  \$\$;

  SELECT pg_create_physical_replication_slot('replica_1')
  WHERE NOT EXISTS (SELECT 1 FROM pg_replication_slots WHERE slot_name = 'replica_1');

  SELECT pg_create_physical_replication_slot('replica_2')
  WHERE NOT EXISTS (SELECT 1 FROM pg_replication_slots WHERE slot_name = 'replica_2');
SQL
