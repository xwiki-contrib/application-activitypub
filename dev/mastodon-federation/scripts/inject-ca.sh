#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

mkdir -p certs
docker compose exec -T caddy cat /data/caddy/pki/authorities/local/root.crt > certs/root.crt
echo "Exported CA root to certs/root.crt"

# ── XWiki JVM truststore ─────────────────────────────────────────
# XWiki's ActivityPub federation is a Java HTTP client, so it consults the
# JVM truststore, not the OS trust store.
docker compose cp certs/root.crt xwiki:/tmp/harness-ca.crt
docker compose exec -T xwiki bash -lc '
  JT="$JAVA_HOME/lib/security/cacerts"; [ -f "$JT" ] || JT="$JAVA_HOME/jre/lib/security/cacerts"
  keytool -importcert -noprompt -trustcacerts -alias harness-ca \
    -file /tmp/harness-ca.crt -keystore "$JT" -storepass changeit 2>/dev/null || \
  keytool -importcert -noprompt -trustcacerts -alias harness-ca \
    -file /tmp/harness-ca.crt -keystore "$JT" -storepass changeit -deststoretype JKS || true
'
echo "Imported CA into XWiki JVM truststore"

# ── XWiki OS trust store ─────────────────────────────────────────
# Also import into the container's OS CA bundle. XWiki's own federation
# code never reads this, but it makes container-internal curl-based
# verification checks meaningful against the same CA the JVM now trusts.
docker compose cp certs/root.crt xwiki:/usr/local/share/ca-certificates/harness-ca.crt
docker compose exec -T xwiki update-ca-certificates
echo "Imported CA into XWiki OS trust store"

# ── Mastodon system trust (web + sidekiq run the outbound HTTP) ──
# The default container user (mastodon) can't write to the system CA
# directories, so run update-ca-certificates as root.
for svc in mastodon-web mastodon-sidekiq; do
  docker compose cp certs/root.crt "$svc":/usr/local/share/ca-certificates/harness-ca.crt
  docker compose exec -T -u root "$svc" update-ca-certificates
done
echo "Imported CA into Mastodon trust store"

# Restart so the new trust is picked up.
docker compose restart xwiki mastodon-web mastodon-sidekiq
