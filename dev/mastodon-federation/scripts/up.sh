#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[ -f .env ] || { echo "Copy .env.example to .env first."; exit 1; }
set -a; . ./.env; set +a

echo "==> Building extension (JDK 8 required)"
# The extension targets Java 8. Prefer a caller-provided JDK 8 (JAVA_8_HOME, or a JAVA_HOME
# that already points at a JDK 8); otherwise fall back to the newest sdkman-installed JDK 8.
if [ -n "${JAVA_8_HOME:-}" ]; then
    export JAVA_HOME="$JAVA_8_HOME"
elif ! { [ -x "${JAVA_HOME:-}/bin/java" ] && "${JAVA_HOME}/bin/java" -version 2>&1 | grep -q '"1\.8'; }; then
    JAVA_HOME="$(ls -d "$HOME"/.sdkman/candidates/java/8.* 2>/dev/null | sort -V | tail -1 || true)"
    export JAVA_HOME
fi
if [ ! -x "${JAVA_HOME:-}/bin/java" ] || ! "${JAVA_HOME}/bin/java" -version 2>&1 | grep -q '"1\.8'; then
    echo "ERROR: a JDK 8 is required to build the extension. Set JAVA_8_HOME (or JAVA_HOME) to a" >&2
    echo "       JDK 8 install, or install one (e.g. 'sdk install java 8.0.492-tem')." >&2
    exit 1
fi
( cd ../.. && mvn -B -ntp -DskipTests install )

echo "==> Generating Mastodon secrets"
./scripts/gen-secrets.sh
set -a; . ./.env; set +a   # reload with filled secrets

echo "==> Rendering .env.mastodon"
envsubst < scripts/mastodon.env.tmpl > .env.mastodon

echo "==> Starting infra (caddy, DBs, redis)"
docker compose up -d caddy xwiki-db mastodon-db redis

echo "==> Initializing Mastodon DB (first run only)"
# db:prepare only seeds a database it creates itself: a database left created but unseeded by an interrupted run
# would stay without user roles or instance actor, so account provisioning fails. Seeding again is idempotent.
docker compose run --rm mastodon-web bash -lc 'bin/rails db:prepare && bin/rails db:seed'

echo "==> Starting all services"
docker compose up -d

echo "==> Waiting for XWiki and Mastodon"
timeout 600 bash -c 'until docker compose exec -T xwiki curl -sf -o /dev/null http://localhost:8080/xwiki/; do sleep 5; done'
# Mastodon only answers requests for its own domain (Rails host authorization rejects "localhost:3000" with a 403),
# so probe it with the Host header Caddy forwards.
timeout 300 bash -c 'until docker compose exec -T mastodon-web curl -sf -o /dev/null \
    -H "Host: $MASTODON_DOMAIN" -H "X-Forwarded-Proto: https" http://localhost:3000/api/v1/instance; do sleep 5; done'

echo "==> Injecting CA trust"
./scripts/inject-ca.sh

echo "==> Provisioning accounts + extension"
./scripts/provision.sh

./scripts/info.sh
