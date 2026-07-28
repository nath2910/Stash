#!/bin/sh
set -eu

TRACKING_PROVIDER="${APP_DELIVERY_TRACKING_PROVIDER:-DIRECT}"

require_browser_runtime() {
  case "${TRACKING_PROVIDER}" in
    DIRECT|direct)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

resolve_browser_bin() {
  if [ -n "${PUPPETEER_EXECUTABLE_PATH:-}" ] && [ -x "${PUPPETEER_EXECUTABLE_PATH}" ]; then
    printf '%s\n' "${PUPPETEER_EXECUTABLE_PATH}"
    return 0
  fi

  for candidate in /usr/bin/chromium /usr/bin/chromium-browser /usr/bin/google-chrome /usr/bin/google-chrome-stable; do
    if [ -x "${candidate}" ]; then
      printf '%s\n' "${candidate}"
      return 0
    fi
  done

  if command -v chromium >/dev/null 2>&1; then
    command -v chromium
    return 0
  fi
  if command -v chromium-browser >/dev/null 2>&1; then
    command -v chromium-browser
    return 0
  fi
  if command -v google-chrome >/dev/null 2>&1; then
    command -v google-chrome
    return 0
  fi
  if command -v google-chrome-stable >/dev/null 2>&1; then
    command -v google-chrome-stable
    return 0
  fi

  return 1
}

if [ ! -f /app/tracking-scripts/laposte-browser-scrape.mjs ]; then
  echo "[startup] missing tracking scripts in /app/tracking-scripts" >&2
  exit 1
fi

if ! command -v node >/dev/null 2>&1; then
  echo "[startup] node runtime missing from backend container" >&2
  exit 1
fi

echo "[startup] node version: $(node --version)"

if require_browser_runtime; then
  BROWSER_BIN="$(resolve_browser_bin || true)"
  if [ -z "${BROWSER_BIN}" ]; then
    echo "[startup] chromium or chrome runtime missing from backend container" >&2
    exit 1
  fi

  echo "[startup] browser runtime: ${BROWSER_BIN}"
  "${BROWSER_BIN}" --version || true
fi

exec java -Dserver.port="${PORT:-8080}" -jar /app/app.jar
