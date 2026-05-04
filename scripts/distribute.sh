#!/usr/bin/env bash
# Build a release APK and upload it to Firebase App Distribution.
#
# Prerequisites:
#   1. keystore.properties exists at repo root (see keystore.properties.example)
#   2. Authenticated with Firebase — either:
#      a. Run `firebase login` once (uses cached credentials), or
#      b. Set FIREBASE_TOKEN env var (from `firebase login:ci`)
#
# Usage:
#   ./scripts/distribute.sh
#   FIREBASE_TOKEN=... ./scripts/distribute.sh

set -euo pipefail
cd "$(dirname "$0")/.."

if [[ ! -f keystore.properties ]]; then
  echo "Error: keystore.properties not found. Copy keystore.properties.example and fill in your values."
  exit 1
fi

export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"

echo "Building release APK..."
./gradlew assembleRelease

echo "Uploading to Firebase App Distribution..."
./gradlew appDistributionUploadRelease

echo "Done."
