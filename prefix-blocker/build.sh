#!/usr/bin/env bash
set -euo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")"

if [[ -d ../.tools/jdk ]]; then
    export JAVA_HOME="$PWD/../.tools/jdk"
fi

if [[ -d ../.tools/sdk ]]; then
    export ANDROID_HOME="$PWD/../.tools/sdk"
fi

if [[ -x ../.tools/gradle-8.7/bin/gradle ]]; then
    gradle_command=("$PWD/../.tools/gradle-8.7/bin/gradle")
else
    gradle_command=(gradle)
fi

if [[ $# -eq 0 ]]; then
    set -- :app:assembleDebug :app:lintDebug
fi

"${gradle_command[@]}" --console=plain "$@"

if [[ -f app/build/outputs/apk/debug/app-debug.apk ]]; then
    mkdir -p dist
    cp app/build/outputs/apk/debug/app-debug.apk dist/prefix-blocker.apk
    sha256sum dist/prefix-blocker.apk >dist/prefix-blocker.apk.sha256
fi
