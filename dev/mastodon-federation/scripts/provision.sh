#!/usr/bin/env bash
# Provisions the mastodon-federation dev harness: makes XWiki reverse-proxy
# aware, gets it past the (fresh-install) Distribution Wizard with a usable
# flavor, creates an XWiki test user + Mastodon accounts, and installs the
# locally-built ActivityPub extension.
#
# NOTE on URL shape: the xwiki:lts-postgres-tomcat image deploys the webapp
# at the Tomcat ROOT context with xwiki.webapppath empty, so the working
# canonical paths are https://<domain>[:port]/bin/... and .../rest/...
# (there is NO "/xwiki" path segment in this harness, unlike some other
# XWiki deployments/docs).
set -euo pipefail
cd "$(dirname "$0")/.."
set -a; . ./.env; set +a
: > .info

P=${HTTPS_PORT:-443}
XWIKI_BASE="https://${XWIKI_DOMAIN}:${P}"
RESOLVE=(--cacert certs/root.crt --resolve "${XWIKI_DOMAIN}:${P}:127.0.0.1")
CURL=(curl -sS "${RESOLVE[@]}" -u "${XWIKI_ADMIN_USER}:${XWIKI_ADMIN_PASSWORD}")

# ── XWiki: make it proxy-aware + enable superadmin (idempotent) ──────────
# - xwiki.home (no port) so ActivityPub/WebFinger emit https URLs without
#   the host-only :8443 port, matching how Mastodon resolves xwiki.local
#   internally (via Caddy on the container network's port 443).
# - RemoteIpValve so request.getScheme() reflects X-Forwarded-Proto from
#   Caddy (see dev/mastodon-federation/xwiki/reverse-proxy.txt).
# - superadmin is DISABLED by default in this image (no xwiki.superadminpassword
#   set) — without this, every REST/basic-auth call below silently runs as
#   XWiki.XWikiGuest instead of superadmin.
# - extension.repositories: add a maven-file repository pointing at the
#   host's ~/.m2/repository (bind-mounted read-only at /host-m2-repository,
#   see docker-compose.yml) so the Extension Manager can resolve the locally
#   built activitypub-ui SNAPSHOT, in addition to the normal public repos.
docker compose exec -T \
  -e HARNESS_XWIKI_HOME="https://${XWIKI_DOMAIN}/" \
  -e HARNESS_SUPERADMIN_PW="${XWIKI_ADMIN_PASSWORD}" \
  xwiki bash -lc '
  CFG=/usr/local/tomcat/webapps/ROOT/WEB-INF/xwiki.cfg
  # Rewrite these two keys by stripping any existing line then appending the value with printf.
  # The values arrive as container env vars (never spliced into this script text) and printf never
  # interprets them, so passwords/domains containing sed metacharacters (# & \) or quotes are safe.
  grep -v "^xwiki.home=" "$CFG" > "$CFG.tmp" && mv "$CFG.tmp" "$CFG"
  printf "xwiki.home=%s\n" "$HARNESS_XWIKI_HOME" >> "$CFG"
  grep -v "^xwiki.superadminpassword=" "$CFG" > "$CFG.tmp" && mv "$CFG.tmp" "$CFG"
  printf "xwiki.superadminpassword=%s\n" "$HARNESS_SUPERADMIN_PW" >> "$CFG"

  SRV=/usr/local/tomcat/conf/server.xml
  grep -q "RemoteIpValve" "$SRV" || sed -i "/unpackWARs=\"true\" autoDeploy=\"true\">/a\\
\\
        <Valve className=\"org.apache.catalina.valves.RemoteIpValve\" remoteIpHeader=\"X-Forwarded-For\" protocolHeader=\"X-Forwarded-Proto\" />" "$SRV"

  PROPS=/usr/local/tomcat/webapps/ROOT/WEB-INF/xwiki.properties
  grep -q "^extension.repositories = maven-local:maven:file:///host-m2-repository" "$PROPS" || cat >> "$PROPS" <<EOF

# ── mastodon-federation harness: resolve locally-built SNAPSHOT extensions ──
extension.repositories = maven-local:maven:file:///host-m2-repository/
extension.repositories = maven-xwiki:maven:https://nexus.xwiki.org/nexus/content/groups/public
extension.repositories = store.xwiki.com:xwiki:https://store.xwiki.com/xwiki/rest/
extension.repositories = extensions.xwiki.org:xwiki:https://extensions.xwiki.org/xwiki/rest/
EOF
'
docker compose restart xwiki
# wait for XWiki to answer again
timeout 300 bash -c 'until docker compose exec -T xwiki curl -sf -o /dev/null http://localhost:8080/xwiki/; do sleep 5; done'
sleep 3

# ── XWiki: complete the Distribution Wizard + install a flavor (idempotent) ─
# A fresh instance blocks ALL bin/rest requests behind the Distribution
# Wizard until it is completed. Driving its multi-step AJAX flow over raw
# curl is fragile, so instead we choose "Never" (action=CANCEL — "I can do
# this by myself") to permanently dismiss the wizard, then install the
# recommended main-wiki flavor ourselves via the Extension Manager job API.
# (xwiki-platform-distribution-flavor-wiki is for SUBWIKIS only — it refuses
# to install on the main wiki with a NamespaceNotAllowedException; the main
# wiki needs xwiki-platform-distribution-flavor-mainwiki.)
if [ "$("${CURL[@]}" -o /dev/null -w '%{http_code}' "${XWIKI_BASE}/bin/view/Main/")" != "200" ]; then
  curl -sS "${RESOLVE[@]}" -u "${XWIKI_ADMIN_USER}:${XWIKI_ADMIN_PASSWORD}" -X POST \
    "${XWIKI_BASE}/bin/distribution/XWiki/Distribution" --data-urlencode "action=CANCEL" -o /dev/null || true

  XWIKI_RUNTIME_VERSION="$("${CURL[@]}" -sI "${XWIKI_BASE}/rest/wikis/xwiki" | grep -i '^xwiki-version:' | tr -d '\r' | awk '{print $2}')"

  FLAVOR_JOB="$(mktemp)"
  cat > "$FLAVOR_JOB" <<EOF
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<jobRequest xmlns="http://www.xwiki.org">
  <id>
    <element>extension</element><element>provision</element><element>flavor-mainwiki-install</element>
  </id>
  <interactive>false</interactive>
  <remote>false</remote>
  <verbose>true</verbose>
  <property><key>extensions</key><value><list xmlns="" xmlns:ns2="http://www.xwiki.org">
    <org.xwiki.extension.ExtensionId>
      <id>org.xwiki.platform:xwiki-platform-distribution-flavor-mainwiki</id>
      <version class="org.xwiki.extension.version.internal.DefaultVersion" serialization="custom">
        <org.xwiki.extension.version.internal.DefaultVersion><string>${XWIKI_RUNTIME_VERSION}</string></org.xwiki.extension.version.internal.DefaultVersion>
      </version>
    </org.xwiki.extension.ExtensionId>
  </list></value></property>
  <property><key>extensions.excluded</key><value><set xmlns="" xmlns:ns2="http://www.xwiki.org"/></value></property>
  <property><key>interactive</key><value><boolean xmlns="" xmlns:ns2="http://www.xwiki.org">false</boolean></value></property>
  <property><key>namespaces</key><value><list xmlns="" xmlns:ns2="http://www.xwiki.org"><string>wiki:xwiki</string></list></value></property>
</jobRequest>
EOF
  "${CURL[@]}" -X PUT -H "Content-Type: text/xml" \
    "${XWIKI_BASE}/rest/jobs?jobType=install&async=false" --upload-file "$FLAVOR_JOB" -o /dev/null || true
  rm -f "$FLAVOR_JOB"
fi
if [ "$("${CURL[@]}" -o /dev/null -w '%{http_code}' "${XWIKI_BASE}/bin/view/Main/")" = "200" ]; then
  echo "XWiki: flavor installed, Main/ serves 200" >> .info
else
  echo "XWiki: WARNING — Main/ still not serving 200 after flavor install attempt" >> .info
fi

# ── XWiki: recreate the XWiki.Admin user the Distribution Wizard would have made ──
# The main-wiki flavor pages are content-authored by XWiki.Admin, and the wiki grants
# script + programming rights to XWiki.XWikiAdminGroup. Because we install the flavor through
# the Extension Manager (having cancelled the Distribution Wizard, which normally creates this
# user), XWiki.Admin is absent — so those pages fail their [script] authorization and the wiki
# renders with "Access denied" errors. Create XWiki.Admin and add it to XWikiAdminGroup so its
# authored pages get the expected rights. Idempotent on the presence of the user object.
if [ "$("${CURL[@]}" -o /dev/null -w '%{http_code}' "${XWIKI_BASE}/rest/wikis/xwiki/spaces/XWiki/pages/Admin/objects/XWiki.XWikiUsers/0")" = "200" ]; then
  echo "XWiki.Admin already exists" >> .info
else
  ADMIN_TOKEN="$("${CURL[@]}" -i "${XWIKI_BASE}/rest/wikis/xwiki" | grep -i '^xwiki-form-token:' | tr -d '\r' | awk '{print $2}')"
  "${CURL[@]}" -X PUT "${XWIKI_BASE}/rest/wikis/xwiki/spaces/XWiki/pages/Admin" \
    --data-urlencode "title=Admin" -o /dev/null || true
  "${CURL[@]}" -X POST -H "XWiki-Form-Token: ${ADMIN_TOKEN}" \
    "${XWIKI_BASE}/rest/wikis/xwiki/spaces/XWiki/pages/Admin/objects" \
    --data-urlencode "className=XWiki.XWikiUsers" \
    --data-urlencode "property#active=1" \
    --data-urlencode "property#first_name=Admin" \
    --data-urlencode "property#password=${XWIKI_ADMIN_PASSWORD}" -o /dev/null || true
  "${CURL[@]}" -X POST -H "XWiki-Form-Token: ${ADMIN_TOKEN}" \
    "${XWIKI_BASE}/rest/wikis/xwiki/spaces/XWiki/pages/XWikiAdminGroup/objects" \
    --data-urlencode "className=XWiki.XWikiGroups" \
    --data-urlencode "property#member=XWiki.Admin" -o /dev/null || true
  echo "XWiki: created XWiki.Admin (password = XWIKI_ADMIN_PASSWORD) and added it to XWikiAdminGroup" >> .info
fi

# ── XWiki: create test user via REST (idempotent) ─────────────────────────
# Note: creating the XWiki.XWikiUsers object via REST POST requires the
# XWiki-Form-Token header (CSRF protection applies to this specific
# "objects" resource even for Basic-Auth-only requests); get the token from
# any prior GET response header first.
if [ "$("${CURL[@]}" -o /dev/null -w '%{http_code}' "${XWIKI_BASE}/rest/wikis/xwiki/spaces/XWiki/pages/${XWIKI_TEST_USER}/objects/XWiki.XWikiUsers/0")" = "200" ]; then
  echo "XWiki test user: ${XWIKI_TEST_USER} (already existed)" >> .info
else
  "${CURL[@]}" -X PUT "${XWIKI_BASE}/rest/wikis/xwiki/spaces/XWiki/pages/${XWIKI_TEST_USER}" \
    --data-urlencode "title=${XWIKI_TEST_USER}" -o /dev/null || true
  FORM_TOKEN="$("${CURL[@]}" -i "${XWIKI_BASE}/rest/wikis/xwiki" | grep -i '^xwiki-form-token:' | tr -d '\r' | awk '{print $2}')"
  "${CURL[@]}" -X POST -H "XWiki-Form-Token: ${FORM_TOKEN}" \
    "${XWIKI_BASE}/rest/wikis/xwiki/spaces/XWiki/pages/${XWIKI_TEST_USER}/objects" \
    --data-urlencode "className=XWiki.XWikiUsers" \
    --data-urlencode "property#password=${XWIKI_TEST_PASSWORD}" \
    --data-urlencode "property#active=1" -o /dev/null || true
  echo "XWiki test user: ${XWIKI_TEST_USER}" >> .info
fi

# ── Mastodon: owner + test user (tootctl generates passwords) ────────────
create_masto_account() { # username role
  local user="$1" role="$2"
  local out
  out="$(docker compose exec -T mastodon-web bin/tootctl accounts create "$user" \
        --email "${user}@${MASTODON_DOMAIN}" --confirmed --approve --role "$role" 2>&1 || true)"
  # tootctl prints: "New password: <pw>"
  local pw; pw="$(echo "$out" | sed -n 's/.*[Pp]assword: *//p' | head -1)"
  if [ -z "$pw" ]; then
    # already exists → reset to capture a known value
    out="$(docker compose exec -T mastodon-web bin/tootctl accounts modify "$user" --reset-password 2>&1 || true)"
    pw="$(echo "$out" | sed -n 's/.*[Pp]assword: *//p' | head -1)"
  fi
  # Approve regardless of create/exists path: tootctl leaves accounts unapproved (approved=false),
  # so without this the Mastodon web UI shows "Your application is pending review" and blocks login.
  docker compose exec -T mastodon-web bin/tootctl accounts approve "$user" >/dev/null 2>&1 || true
  echo "Mastodon ${role} ${user}@${MASTODON_DOMAIN}: password ${pw:-<unknown, check web>}" >> .info
}
create_masto_account "${MASTODON_OWNER_USER}" Owner
create_masto_account "${MASTODON_TEST_USER}"  Owner

# ── ActivityPub extension: install activitypub-ui SNAPSHOT (idempotent) ──
# Resolved via the maven-local extension repository configured above
# (host ~/.m2/repository bind-mounted at /host-m2-repository). Uses the
# same REST job API as the xwiki-deploy-extension skill.
AP_JOB="$(mktemp)"
cat > "$AP_JOB" <<'EOF'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<jobRequest xmlns="http://www.xwiki.org">
  <id>
    <element>extension</element><element>provision</element><element>activitypub-ui-install</element>
  </id>
  <interactive>false</interactive>
  <remote>false</remote>
  <verbose>true</verbose>
  <property><key>extensions</key><value><list xmlns="" xmlns:ns2="http://www.xwiki.org">
    <org.xwiki.extension.ExtensionId>
      <id>org.xwiki.contrib:activitypub-ui</id>
      <version class="org.xwiki.extension.version.internal.DefaultVersion" serialization="custom">
        <org.xwiki.extension.version.internal.DefaultVersion><string>1.7.12-SNAPSHOT</string></org.xwiki.extension.version.internal.DefaultVersion>
      </version>
    </org.xwiki.extension.ExtensionId>
  </list></value></property>
  <property><key>extensions.excluded</key><value><set xmlns="" xmlns:ns2="http://www.xwiki.org"/></value></property>
  <property><key>interactive</key><value><boolean xmlns="" xmlns:ns2="http://www.xwiki.org">false</boolean></value></property>
  <property><key>namespaces</key><value><list xmlns="" xmlns:ns2="http://www.xwiki.org"><string>wiki:xwiki</string></list></value></property>
</jobRequest>
EOF
AP_RESULT="$("${CURL[@]}" -X PUT -H "Content-Type: text/xml" \
  "${XWIKI_BASE}/rest/jobs?jobType=install&async=false" --upload-file "$AP_JOB" || true)"
rm -f "$AP_JOB"
if echo "$AP_RESULT" | grep -q "already installed"; then
  echo "Extension: org.xwiki.contrib:activitypub-ui:1.7.12-SNAPSHOT already installed in wiki:xwiki" >> .info
elif echo "$AP_RESULT" | grep -q "<state>FINISHED</state>" && ! echo "$AP_RESULT" | grep -qi "error\|exception\|failed"; then
  echo "Extension: org.xwiki.contrib:activitypub-ui:1.7.12-SNAPSHOT installed in wiki:xwiki" >> .info
else
  echo "Extension: org.xwiki.contrib:activitypub-ui:1.7.12-SNAPSHOT install FAILED — see below" >> .info
  echo "$AP_RESULT" | head -c 2000 >> .info
  echo >> .info
fi
# Server URL is derived from xwiki.home (set above), not a separate setting;
# verify actual actor IDs in the AP admin UI / via a live actor fetch.

# Restart so the newly-created XWiki.Admin (and its group-derived script/programming rights)
# are reflected in XWiki's security/render caches, then wait until the wiki serves again.
docker compose restart xwiki >/dev/null 2>&1 || true
timeout 300 bash -c 'until docker compose exec -T xwiki curl -sf -o /dev/null http://localhost:8080/bin/view/Main/; do sleep 5; done'
