#!/usr/bin/env bash
set -euo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")"

readonly APP_DIR="$HOME/.local/share/ph"
readonly LAUNCHER="$HOME/.local/bin/ph"

if [[ ! -f "$HOME/.config/sms/config.json" ]]; then
    printf 'Pair the phone first with ../sms/install.sh\n' >&2
    exit 1
fi

mkdir -p "$APP_DIR" "$(dirname -- "$LAUNCHER")"
cp ph.py codes.py ../sms/linux/client.py "$APP_DIR/"
printf '#!/usr/bin/env bash\nexec /usr/bin/python3 "%s/ph.py" "$@"\n' "$APP_DIR" \
    >"$LAUNCHER"
chmod +x "$LAUNCHER"
printf 'Installed. Run: ph code\n'
