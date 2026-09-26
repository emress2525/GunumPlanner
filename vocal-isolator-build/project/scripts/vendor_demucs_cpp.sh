#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST="$ROOT/app/src/main/cpp/third_party/demucs_cpp"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

REPO="https://github.com/sevagh/demucs.cpp.git"
TAG="v0.0.4-alpha"

git clone --depth 1 --branch "$TAG" --recurse-submodules --shallow-submodules "$REPO" "$TMP/demucs.cpp"
rm -rf "$DEST"
mkdir -p "$DEST/vendor/eigen"
cp -R "$TMP/demucs.cpp/src" "$DEST/src"
cp -R "$TMP/demucs.cpp/vendor/eigen/Eigen" "$DEST/vendor/eigen/Eigen"
if [[ -d "$TMP/demucs.cpp/vendor/eigen/unsupported" ]]; then
  cp -R "$TMP/demucs.cpp/vendor/eigen/unsupported" "$DEST/vendor/eigen/unsupported"
fi
cp "$TMP/demucs.cpp/LICENSE" "$DEST/LICENSE"
printf '%s\n' "$TAG" > "$DEST/VERSION"

echo "Vendored demucs.cpp $TAG into $DEST"
