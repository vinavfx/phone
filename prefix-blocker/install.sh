#!/usr/bin/env bash
set -euo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")"

require_tools() {
    if ! command -v adb >/dev/null 2>&1; then
        printf 'ADB is required. Install Android Platform Tools first.\n' >&2
        exit 1
    fi

    if [[ ! -f dist/prefix-blocker.apk ]]; then
        printf 'APK not found. Run ./build.sh first.\n' >&2
        exit 1
    fi

    adb start-server >/dev/null
}

select_device() {
    local -a devices
    local serial="${1:-}"

    if [[ -z "$serial" ]]; then
        mapfile -t devices < <(adb devices | awk 'NR > 1 && NF >= 2 { print $1 }')
        if [[ ${#devices[@]} -ne 1 ]]; then
            adb devices -l >&2
            printf 'Connect one phone, or run ./install.sh SERIAL.\n' >&2
            exit 1
        fi
        serial="${devices[0]}"
    fi

    if [[ "$(adb -s "$serial" get-state 2>/dev/null)" != device ]]; then
        printf 'Unlock the phone and allow USB debugging, then try again.\n' >&2
        exit 1
    fi

    printf '%s\n' "$serial"
}

main() {
    local serial
    local sdk

    require_tools
    serial=$(select_device "${1:-}")
    sdk=$(adb -s "$serial" shell getprop ro.build.version.sdk | tr -d '\r')
    if [[ ! "$sdk" =~ ^[0-9]+$ ]] || ((sdk < 28)); then
        printf 'Android 9 or newer is required.\n' >&2
        exit 1
    fi

    adb -s "$serial" install -r dist/prefix-blocker.apk
    adb -s "$serial" shell am start -n app.prefixblocker/.MainActivity
    printf 'Installed. Enable call blocking in the app and add your prefixes.\n'
}

main "$@"
