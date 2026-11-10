#!/bin/bash
# Loads database/schema into $MYSQL_DATABASE on first container start.
# The schema file begins with CREATE DATABASE, which the MySQL image has
# already done for us, so that line is skipped.
set -euo pipefail

echo "flagman: loading database/schema into ${MYSQL_DATABASE}"
grep -viE '^[[:space:]]*CREATE DATABASE' /flagman-sql/schema \
  | mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}"
