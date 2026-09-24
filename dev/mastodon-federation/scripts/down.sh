#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [ "${1:-}" = "-v" ]; then
  docker compose down -v
  rm -f .env.mastodon .info certs/root.crt
  echo "Harness stopped and all data wiped."
else
  docker compose down
  echo "Harness stopped (data preserved). Use -v to wipe."
fi
