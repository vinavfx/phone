#!/usr/bin/env bash
set -euo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")"

readonly PACKAGE="app.phone.sms"
readonly APK="android/dist/sms-sync.apk"
readonly APP_DIR="$HOME/.local/share/sms"
readonly LAUNCHER="$HOME/.local/bin/sms"
readonly DESKTOP_FILE="$HOME/.local/share/applications/sms.desktop"
readonly CONFIG_FILE="$HOME/.config/sms/config.json"
readonly PERMISSIONS=(
    android.permission.READ_SMS
    android.permission.READ_CONTACTS
    android.permission.POST_NOTIFICATIONS
)

select_device() {
    local -a devices

    mapfile -t devices < <(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')
    if [[ ${#devices[@]} -ne 1 ]]; then
        adb devices -l >&2
        printf 'Connect exactly one phone with USB debugging allowed.\n' >&2
        exit 1
    fi

    printf '%s\n' "${devices[0]}"
}

install_phone_app() {
    local serial="$1"
    local permission

    if [[ ! -f "$APK" ]]; then
        android/build.sh
    fi

    adb -s "$serial" install -r "$APK"
    for permission in "${PERMISSIONS[@]}"; do
        adb -s "$serial" shell pm grant "$PACKAGE" "$permission" || true
    done

    adb -s "$serial" shell dumpsys deviceidle whitelist "+$PACKAGE" >/dev/null
    adb -s "$serial" shell am start -n "$PACKAGE/.MainActivity" >/dev/null
}

read_token() {
    local serial="$1"
    local token=""
    local attempt

    for attempt in {1..20}; do
        token=$(adb -s "$serial" shell run-as "$PACKAGE" cat shared_prefs/sync.xml \
            2>/dev/null | sed -n 's/.*name="token">\([0-9a-f]*\)<.*/\1/p')
        if [[ -n "$token" ]]; then
            printf '%s\n' "$token"
            return
        fi
        sleep 0.5
    done

    printf 'Could not read the pairing token (attempt %s).\n' "$attempt" >&2
    exit 1
}

read_phone_address() {
    local serial="$1"
    local address

    address=$(adb -s "$serial" shell ip -4 addr show wlan0 \
        | awk '/inet / { split($2, parts, "/"); print parts[1] }')
    if [[ -z "$address" ]]; then
        printf 'Connect the phone to the same Wi-Fi network as this computer.\n' >&2
        exit 1
    fi

    printf '%s\n' "$address"
}

write_config() {
    local host="$1"
    local token="$2"

    mkdir -p "$(dirname -- "$CONFIG_FILE")"
    printf '{\n    "host": "%s",\n    "token": "%s"\n}\n' "$host" "$token" \
        >"$CONFIG_FILE"
    chmod 600 "$CONFIG_FILE"
}

install_desktop_app() {
    mkdir -p "$APP_DIR" "$(dirname -- "$LAUNCHER")" "$(dirname -- "$DESKTOP_FILE")"
    cp linux/client.py linux/theme.py linux/state.py "$APP_DIR/"
    cp linux/window.py linux/sms.py "$APP_DIR/"
    printf '#!/usr/bin/env bash\nexec /usr/bin/python3 "%s/sms.py" "$@"\n' "$APP_DIR" \
        >"$LAUNCHER"
    chmod +x "$LAUNCHER"

    printf '%s\n' \
        '[Desktop Entry]' \
        'Type=Application' \
        'Name=SMS' \
        'Comment=View phone SMS over the local network' \
        "Exec=$LAUNCHER" \
        'Icon=mail-message-new' \
        'Terminal=false' \
        'Categories=Network;Chat;' \
        'StartupWMClass=sms' >"$DESKTOP_FILE"
    update-desktop-database "$(dirname -- "$DESKTOP_FILE")" 2>/dev/null || true
}

main() {
    local serial
    local token
    local host

    serial=$(select_device)
    install_phone_app "$serial"
    token=$(read_token "$serial")
    host=$(read_phone_address "$serial")
    write_config "$host" "$token"
    install_desktop_app
    printf 'Paired with %s. Open "SMS" from the menu or run: sms\n' "$host"
}

main "$@"
