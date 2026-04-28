#!/usr/bin/env bash
set -euo pipefail

# Simple cross-platform build script for the Beeping Android SDK.

detect_os() {
  local uname_out
  uname_out="$(uname -s 2>/dev/null || echo unknown)"
  case "${uname_out}" in
    Darwin)   echo "Detected macOS";;
    Linux)    echo "Detected Linux";;
    MINGW*|MSYS*|CYGWIN*) echo "Detected Windows (Git Bash/MSYS)";;
    *)        echo "Detected unknown OS (${uname_out})";;
  esac
}

ensure_gradlew() {
  if [[ ! -f "./gradlew" ]]; then
    echo "Error: ./gradlew not found. Please run from the repository root."
    exit 1
  fi
  if [[ ! -x "./gradlew" ]]; then
    echo "Info: ./gradlew is not executable, attempting to fix permissions."
    chmod +x ./gradlew
  fi
}

java_command() {
  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
    echo "${JAVA_HOME}/bin/java"
  else
    command -v java || true
  fi
}

check_java() {
  local java_bin
  java_bin="$(java_command)"
  if [[ -z "${java_bin}" ]]; then
    echo "Error: Java not found. Please install JDK 8–11 (JDK 11 recommended)."
    exit 1
  fi

  local version_output version_line version_major
  version_output="$("${java_bin}" -version 2>&1)"
  version_line="$(echo "${version_output}" | head -n1)"

  # Extract major version: "1.8" -> 8, otherwise take number before first dot or space.
  if [[ "${version_line}" =~ \"1\.8\.[^\"]*\" ]]; then
    version_major=8
  else
    version_major="$(echo "${version_line}" | sed -E 's/.*\"([0-9]+).*\".*/\1/')"
  fi

  echo "Using Java: ${java_bin}"
  echo "Java version: ${version_line}"

  case "${version_major}" in
    8|9|10|11)
      return 0
      ;;
    17|21)
      echo "Warning: Detected Java ${version_major}. Gradle/AGP in this repo works best with JDK 8–11."
      echo "Please consider setting JAVA_HOME to a JDK 11 installation (e.g., JAVA_HOME=/path/to/jdk11 ./build.sh)."
      ;;
    *)
      echo "Error: Unsupported Java major version (${version_major}). Please use JDK 8–11 (JDK 11 recommended)."
      exit 1
      ;;
  esac
}

run_gradle() {
  ./gradlew "$@"
}

main() {
  detect_os
  ensure_gradlew
  check_java

  local run_tests=1 do_clean=0
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --clean)
        do_clean=1
        shift
        ;;
      --no-tests)
        run_tests=0
        shift
        ;;
      *)
        echo "Unknown option: $1"
        echo "Usage: $0 [--clean] [--no-tests]"
        exit 1
        ;;
    esac
  done

  if [[ "${do_clean}" -eq 1 ]]; then
    echo "Running clean..."
    run_gradle clean
  fi

  if [[ "${run_tests}" -eq 1 ]]; then
    echo "Running :AndroidBeepingCore:test ..."
    run_gradle :AndroidBeepingCore:test
  else
    echo "Skipping tests (--no-tests)."
  fi

  echo "Building app debug APK ..."
  run_gradle :app:assembleDebug

  echo ""
  echo "Outputs:"
  echo "  - AndroidBeepingCore test reports: AndroidBeepingCore/build/reports/tests/test/index.html"
  echo "  - AndroidBeepingCore AARs: AndroidBeepingCore/build/outputs/aar/"
  echo "  - App debug APK: app/build/outputs/apk/debug/app-debug.apk"
  echo ""
  echo "BUILD OK"
}

main "$@"
