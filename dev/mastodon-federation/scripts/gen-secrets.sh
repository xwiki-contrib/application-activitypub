#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
set -a; . ./.env; set +a

run() { docker run --rm "$MASTODON_IMAGE" bash -lc "$1" | tr -d '\r'; }

set_var() { # name value
  local name="$1" value="$2"
  if grep -q "^${name}=$" .env; then
    # portable in-place edit
    local tmp; tmp="$(mktemp)"
    sed "s#^${name}=\$#${name}=${value}#" .env > "$tmp" && mv "$tmp" .env
    echo "  set ${name}"
  fi
}

echo "Generating Mastodon secrets (empty ones only)..."
[ -z "${SECRET_KEY_BASE}" ] && set_var SECRET_KEY_BASE "$(run 'bin/rails secret')"
[ -z "${OTP_SECRET}" ]      && set_var OTP_SECRET "$(run 'bin/rails secret')"

if [ -z "${VAPID_PRIVATE_KEY}" ]; then
  vapid="$(run 'bin/rails mastodon:webpush:generate_vapid_key')"
  set_var VAPID_PRIVATE_KEY "$(echo "$vapid" | sed -n 's/^VAPID_PRIVATE_KEY=//p')"
  set_var VAPID_PUBLIC_KEY  "$(echo "$vapid" | sed -n 's/^VAPID_PUBLIC_KEY=//p')"
fi

if [ -z "${ACTIVE_RECORD_ENCRYPTION_PRIMARY_KEY}" ]; then
  enc="$(run 'bin/rails db:encryption:init')"
  set_var ACTIVE_RECORD_ENCRYPTION_DETERMINISTIC_KEY   "$(echo "$enc" | sed -n 's/^ACTIVE_RECORD_ENCRYPTION_DETERMINISTIC_KEY=//p')"
  set_var ACTIVE_RECORD_ENCRYPTION_KEY_DERIVATION_SALT "$(echo "$enc" | sed -n 's/^ACTIVE_RECORD_ENCRYPTION_KEY_DERIVATION_SALT=//p')"
  set_var ACTIVE_RECORD_ENCRYPTION_PRIMARY_KEY         "$(echo "$enc" | sed -n 's/^ACTIVE_RECORD_ENCRYPTION_PRIMARY_KEY=//p')"
fi
echo "Done."
