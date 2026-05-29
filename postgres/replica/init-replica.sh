#!/usr/bin/env bash
set -euo pipefail

DATA_DIR="${PGDATA:-/var/lib/postgresql/data}"
PRIMARY_HOST="${PGHOST:-postgres-primary}"
PRIMARY_PORT="${PGPORT:-5432}"
SLOT="${REPLICA_SLOT:?REPLICA_SLOT is required}"

until pg_isready -h "$PRIMARY_HOST" -p "$PRIMARY_PORT" -U "$PGUSER"; do
  echo "Waiting for primary PostgreSQL at ${PRIMARY_HOST}:${PRIMARY_PORT}..."
  sleep 2
done

if [ ! -s "${DATA_DIR}/PG_VERSION" ]; then
  echo "Bootstrapping replica from ${PRIMARY_HOST} using slot ${SLOT}"
  rm -rf "${DATA_DIR:?}/"*
  pg_basebackup \
    -h "$PRIMARY_HOST" \
    -p "$PRIMARY_PORT" \
    -D "$DATA_DIR" \
    -U "$PGUSER" \
    -Fp \
    -Xs \
    -P \
    -R \
    --slot="$SLOT"

  cat >> "${DATA_DIR}/postgresql.auto.conf" <<EOF
primary_conninfo = 'host=${PRIMARY_HOST} port=${PRIMARY_PORT} user=${PGUSER} password=${PGPASSWORD} application_name=${SLOT}'
primary_slot_name = '${SLOT}'
hot_standby = on
EOF
  touch "${DATA_DIR}/standby.signal"
  chown -R postgres:postgres "$DATA_DIR"
  chmod 700 "$DATA_DIR"
fi

if grep -q "^max_connections" "${DATA_DIR}/postgresql.auto.conf"; then
  sed -i "s/^max_connections.*/max_connections = 200/" "${DATA_DIR}/postgresql.auto.conf"
else
  echo "max_connections = 200" >> "${DATA_DIR}/postgresql.auto.conf"
fi

exec docker-entrypoint.sh postgres
