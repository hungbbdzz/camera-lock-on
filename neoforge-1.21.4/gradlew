#!/usr/bin/env sh
set -eu
GRADLE_VERSION="8.10.2"
GRADLE_DIST="gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_URL="https://services.gradle.org/distributions/${GRADLE_DIST}"
CACHE_ROOT="${HOME}/.gradle/camera-lockon-distributions"
GRADLE_HOME="${CACHE_ROOT}/gradle-${GRADLE_VERSION}"
GRADLE_BIN="${GRADLE_HOME}/bin/gradle"

if [ ! -x "${GRADLE_BIN}" ]; then
  mkdir -p "${CACHE_ROOT}"
  ZIP_PATH="${TMPDIR:-/tmp}/${GRADLE_DIST}"
  echo "Downloading ${GRADLE_URL} ..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL "${GRADLE_URL}" -o "${ZIP_PATH}"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "${ZIP_PATH}" "${GRADLE_URL}"
  else
    echo "curl or wget is required to download Gradle." >&2
    exit 1
  fi
  rm -rf "${GRADLE_HOME}"
  unzip -q "${ZIP_PATH}" -d "${CACHE_ROOT}"
  rm -f "${ZIP_PATH}"
fi

exec "${GRADLE_BIN}" "$@"
