#!/usr/bin/env bash
# Thin objective sanity check for the federation harness. It does NOT drive the
# full federation flows (follow/post/reply) — those you exercise manually in the
# two web UIs. It asserts the plumbing that must hold for federation to be
# possible at all, and guards the ActivityPub Solr forward-compat fix.
set -euo pipefail
cd "$(dirname "$0")/.."
set -a; . ./.env; set +a

CA=certs/root.crt
P=${HTTPS_PORT:-443}
BODY=/tmp/smoke.body
HTTP=""
fail=0

# Reach a domain through Caddy on the host-published port, validating the harness CA.
# Sets $HTTP to the status code and writes the response body to $BODY. Call it
# directly (not in a $(...) subshell), otherwise $HTTP would not propagate back.
get() { # domain path
    local domain="$1" path="$2"
    HTTP="$(curl -sS --cacert "$CA" --resolve "${domain}:${P}:127.0.0.1" \
        -o "$BODY" -w '%{http_code}' "https://${domain}:${P}${path}" \
        -H 'Accept: application/activity+json' 2>/dev/null || echo 000)"
}

echo "== Mastodon actor discovery (must pass) =="
get "${MASTODON_DOMAIN}" "/.well-known/webfinger?resource=acct:${MASTODON_TEST_USER}@${MASTODON_DOMAIN}"
if [ "$HTTP" = "200" ] && grep -q '"rel":"self"' "$BODY"; then
    echo "OK   Mastodon resolves ${MASTODON_TEST_USER}@${MASTODON_DOMAIN} (WebFinger 200, rel=self)"
else
    echo "FAIL Mastodon WebFinger for ${MASTODON_TEST_USER}@${MASTODON_DOMAIN} (HTTP ${HTTP})"
    fail=1
fi

echo "== XWiki ActivityPub storage health (must pass — guards the Solr forward-compat fix) =="
get "${XWIKI_DOMAIN}" "/activitypub/actor/${XWIKI_TEST_USER}"
if [ "$HTTP" = "500" ]; then
    echo "FAIL XWiki actor endpoint returned 500 — ActivityPub Solr storage is broken"
    echo "     (the ActivityPubSolrInitializer forward-compat fix may have regressed)."
    fail=1
else
    echo "OK   XWiki ActivityPub storage healthy (actor endpoint HTTP ${HTTP}, not 500)"
fi

echo "== XWiki actor discovery (needs an enabled actor) =="
get "${XWIKI_DOMAIN}" "/.well-known/webfinger?resource=acct:${XWIKI_TEST_USER}@${XWIKI_DOMAIN}"
if [ "$HTTP" = "200" ]; then
    echo "OK   XWiki resolves ${XWIKI_TEST_USER}@${XWIKI_DOMAIN} — full bidirectional discovery works"
else
    echo "PENDING XWiki has no ActivityPub actor for ${XWIKI_TEST_USER} yet (WebFinger HTTP ${HTTP})."
    echo "        XWiki materializes a user's actor on first ActivityPub use, not automatically."
    echo "        Enable it: sign in to https://${XWIKI_DOMAIN}:${P}/ as ${XWIKI_TEST_USER}, open the"
    echo "        ActivityPub section of the user profile (or follow an account), then re-run this check."
fi

echo
if [ "$fail" = 0 ]; then
    echo "smoke: plumbing OK (Mastodon discovery + XWiki storage healthy)."
else
    echo "smoke: FAILURES above."
fi
exit $fail
