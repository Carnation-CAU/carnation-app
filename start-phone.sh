#!/usr/bin/env bash

set -Eeuo pipefail

readonly CARNATION_PORT="8080"
readonly CARNATION_PACKAGE="com.carnation.fallalert"
readonly CARNATION_PROJECT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly CARNATION_STATE_DIR="${CARNATION_PROJECT_DIR}/.carnation-local"
readonly CARNATION_SERVER_LOG="${CARNATION_STATE_DIR}/server.log"
readonly CARNATION_SERVER_PID_FILE="${CARNATION_STATE_DIR}/server.pid"

CARNATION_INSTALL_APP=true
CARNATION_TEST_ALERT=false

usage() {
    printf '%s\n' \
        "Usage: ./start-phone.sh [--skip-install] [--test-alert]" \
        "" \
        "  --skip-install  Keep the currently installed app (faster)." \
        "  --test-alert    Put the app in background and send one test alert."
}

fail() {
    printf '[FAIL] %s\n' "$1" >&2
    exit 1
}

for argument in "$@"; do
    case "$argument" in
        --skip-install) CARNATION_INSTALL_APP=false ;;
        --test-alert) CARNATION_TEST_ALERT=true ;;
        -h|--help) usage; exit 0 ;;
        *) usage; fail "Unknown option: $argument" ;;
    esac
done

if [[ -z "${JAVA_HOME:-}" ]] && [[ "$(uname -s)" == "Darwin" ]]; then
    if /usr/libexec/java_home -v 17 >/dev/null 2>&1; then
        export JAVA_HOME
        JAVA_HOME="$(/usr/libexec/java_home -v 17)"
    fi
fi

command -v java >/dev/null 2>&1 || fail "Java 17 was not found. Set JAVA_HOME to a JDK 17 installation."

CARNATION_JAVA_MAJOR="$(java -version 2>&1 | awk -F '[\".]' '/version/ { print $2; exit }')"
[[ "$CARNATION_JAVA_MAJOR" == "17" ]] || fail "Java 17 is required (current major version: ${CARNATION_JAVA_MAJOR:-unknown})."

if command -v adb >/dev/null 2>&1; then
    CARNATION_ADB="$(command -v adb)"
elif [[ -n "${ANDROID_HOME:-}" && -x "${ANDROID_HOME}/platform-tools/adb" ]]; then
    CARNATION_ADB="${ANDROID_HOME}/platform-tools/adb"
elif [[ -x "/opt/homebrew/bin/adb" ]]; then
    CARNATION_ADB="/opt/homebrew/bin/adb"
else
    fail "adb was not found. Install Android SDK platform-tools and add adb to PATH."
fi
readonly CARNATION_ADB

mkdir -p "$CARNATION_STATE_DIR"

printf '%s\n' '[1/7] Checking the local server'
if curl -fsS "http://localhost:${CARNATION_PORT}/health" >/dev/null 2>&1; then
    printf '%s\n' '      Server is already running.'
else
    printf '%s\n' "      Starting the server (log: ${CARNATION_SERVER_LOG})"
    (
        cd "$CARNATION_PROJECT_DIR"
        nohup bash gradlew :server:run >"$CARNATION_SERVER_LOG" 2>&1 &
        printf '%s\n' "$!" >"$CARNATION_SERVER_PID_FILE"
    )

    CARNATION_SERVER_READY=false
    for _ in {1..60}; do
        if curl -fsS "http://localhost:${CARNATION_PORT}/health" >/dev/null 2>&1; then
            CARNATION_SERVER_READY=true
            break
        fi
        sleep 1
    done

    if [[ "$CARNATION_SERVER_READY" != true ]]; then
        tail -n 40 "$CARNATION_SERVER_LOG" >&2 || true
        fail "The server did not become ready within 60 seconds."
    fi
fi

printf '%s\n' '[2/7] Checking the connected Android phone'
CARNATION_DEVICE_LIST="$("$CARNATION_ADB" devices | awk 'NR > 1 && $2 == "device" { print $1 }')"
CARNATION_DEVICE_COUNT="$(printf '%s\n' "$CARNATION_DEVICE_LIST" | awk 'NF { count += 1 } END { print count + 0 }')"
if [[ "$CARNATION_DEVICE_COUNT" -eq 0 ]]; then
    "$CARNATION_ADB" devices -l
    fail "No authorized Android phone was found. Unlock the phone and approve USB debugging."
fi
if [[ "$CARNATION_DEVICE_COUNT" -gt 1 ]]; then
    "$CARNATION_ADB" devices -l
    fail "More than one Android device is connected. Disconnect all but the test phone."
fi
readonly CARNATION_SERIAL="$(printf '%s\n' "$CARNATION_DEVICE_LIST" | awk 'NF { print; exit }')"
printf '      Device: %s\n' "$CARNATION_SERIAL"

if [[ "$CARNATION_INSTALL_APP" == true ]]; then
    printf '%s\n' '[3/7] Building and installing the debug app'
    (
        cd "$CARNATION_PROJECT_DIR"
        bash gradlew :app:installDebug \
            -Pserver.host=localhost \
            -Pserver.port="$CARNATION_PORT"
    )
else
    printf '%s\n' '[3/7] Keeping the currently installed app (--skip-install)'
    "$CARNATION_ADB" -s "$CARNATION_SERIAL" shell pm path "$CARNATION_PACKAGE" >/dev/null \
        || fail "The app is not installed. Run again without --skip-install."
fi

printf '%s\n' '[4/7] Recreating the USB port tunnel'
"$CARNATION_ADB" -s "$CARNATION_SERIAL" reverse --remove "tcp:${CARNATION_PORT}" >/dev/null 2>&1 || true
"$CARNATION_ADB" -s "$CARNATION_SERIAL" reverse "tcp:${CARNATION_PORT}" "tcp:${CARNATION_PORT}"

printf '%s\n' '[5/7] Verifying phone-to-server access'
CARNATION_PHONE_HEALTH="$(
    "$CARNATION_ADB" -s "$CARNATION_SERIAL" shell \
        curl -fsS --max-time 5 "http://localhost:${CARNATION_PORT}/health" 2>/dev/null \
        || true
)"
[[ "$CARNATION_PHONE_HEALTH" == *'"status":"ok"'* ]] \
    || fail "The phone could not reach the server through adb reverse."

printf '%s\n' '[6/7] Granting notification permission and restarting the app'
"$CARNATION_ADB" -s "$CARNATION_SERIAL" shell pm grant \
    "$CARNATION_PACKAGE" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1 || true
"$CARNATION_ADB" -s "$CARNATION_SERIAL" shell am force-stop "$CARNATION_PACKAGE"
"$CARNATION_ADB" -s "$CARNATION_SERIAL" shell monkey \
    -p "$CARNATION_PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null

printf '%s\n' '[7/7] Connection ready'
printf '%s\n' '      Keep the USB cable connected and leave the server process running.'

if [[ "$CARNATION_TEST_ALERT" == true ]]; then
    printf '%s\n' '      Sending one test alert with the app in background...'
    sleep 2
    "$CARNATION_ADB" -s "$CARNATION_SERIAL" shell input keyevent KEYCODE_HOME
    sleep 1
    curl -fsS -X POST "http://localhost:${CARNATION_PORT}/api/v1/debug/simulate"
    printf '\n%s\n' '      Test event sent. Check the phone notification shade.'
else
    printf '%s\n' \
        '      Test later with:' \
        "      curl -X POST http://localhost:${CARNATION_PORT}/api/v1/debug/simulate"
fi
