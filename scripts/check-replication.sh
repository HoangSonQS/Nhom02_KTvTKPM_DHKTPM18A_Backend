#!/usr/bin/env sh
set -eu

COMPOSE="${COMPOSE:-docker compose}"
PRIMARY="${PRIMARY_SERVICE:-postgres-primary}"

echo "Primary replication slots:"
$COMPOSE exec -T "$PRIMARY" psql -U "${DB_USER:-appuser}" -d "${DB_NAME:-sebook}" -c \
  "SELECT slot_name, active, restart_lsn, confirmed_flush_lsn FROM pg_replication_slots ORDER BY slot_name;"

echo
echo "Primary sender state:"
$COMPOSE exec -T "$PRIMARY" psql -U "${DB_USER:-appuser}" -d "${DB_NAME:-sebook}" -c \
  "SELECT application_name, client_addr, state, sync_state, write_lag, flush_lag, replay_lag FROM pg_stat_replication ORDER BY application_name;"

for replica in postgres-replica-1 postgres-replica-2; do
  echo
  echo "${replica} receiver state:"
  $COMPOSE exec -T "$replica" psql -U "${POSTGRES_REPLICATION_USER:-replicator}" -d postgres -c \
    "SELECT status, conninfo, latest_end_lsn, last_msg_receipt_time FROM pg_stat_wal_receiver;"
done
