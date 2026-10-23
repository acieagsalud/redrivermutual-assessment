#!/bin/sh
# Runs once, when the Postgres volume is first created.
# Each service gets its own database and login, and cannot connect to the other's database.
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-EOSQL
    CREATE USER claims WITH PASSWORD '${CLAIMS_DB_PASSWORD}';
    CREATE DATABASE claims OWNER claims;
    REVOKE ALL ON DATABASE claims FROM PUBLIC;

    CREATE USER notifications WITH PASSWORD '${NOTIFICATIONS_DB_PASSWORD}';
    CREATE DATABASE notifications OWNER notifications;
    REVOKE ALL ON DATABASE notifications FROM PUBLIC;
EOSQL
