#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
set -a; . ./.env; set +a

# Show :port only when non-standard (443 stays clean).
PS=""; [ "${HTTPS_PORT:-443}" = "443" ] || PS=":${HTTPS_PORT}"

cat <<EOF

════════ Federation harness ready ════════
XWiki       https://${XWIKI_DOMAIN}${PS}/bin/view/Main/     ${XWIKI_ADMIN_USER} / ${XWIKI_ADMIN_PASSWORD}   (superadmin)
            test user: ${XWIKI_TEST_USER} / ${XWIKI_TEST_PASSWORD}   actor: @${XWIKI_TEST_USER}@${XWIKI_DOMAIN}
Mastodon    https://${MASTODON_DOMAIN}${PS}/         (accounts + generated passwords below)
$(sed 's/^/            /' .info 2>/dev/null)
CA root     dev/mastodon-federation/certs/root.crt  (import into your browser to silence TLS warnings)
Hosts       ensure /etc/hosts has:  127.0.0.1 ${XWIKI_DOMAIN} ${MASTODON_DOMAIN}
Try         from Mastodon search  @${XWIKI_TEST_USER}@${XWIKI_DOMAIN}   ·   from XWiki follow  @${MASTODON_TEST_USER}@${MASTODON_DOMAIN}
Teardown    scripts/down.sh   (add -v to wipe all data)
═══════════════════════════════════════════
EOF
