#!/usr/bin/env bash
set -euo pipefail

# BRouter 1.7.8 uses lookups.dat version 10, matching rd5 tiles in use.
# Upstream BRouter source and profiles are MIT licensed.
SHA="041488b36c761ff2dd0fd71c1538b646364223ea"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
VENDOR="$SCRIPT_DIR/vendor/brouter"
if [ ! -f "$VENDOR/LICENSE" ]; then
  mkdir -p "$VENDOR"
  curl -fL --retry 3 --connect-timeout 30 \
       "https://codeload.github.com/abrensch/brouter/tar.gz/$SHA" \
       -o "$SCRIPT_DIR/.brouter-src.tar.gz"
  tar -xzf "$SCRIPT_DIR/.brouter-src.tar.gz" -C "$VENDOR" --strip-components=1
  rm "$SCRIPT_DIR/.brouter-src.tar.gz"
fi
ASSETS="$SCRIPT_DIR/app/src/main/assets/brouter"
mkdir -p "$ASSETS"
for f in lookups.dat trekking.brf car-vario.brf hiking-mountain.brf; do
  test -s "$VENDOR/misc/profiles2/$f"
  cp "$VENDOR/misc/profiles2/$f" "$ASSETS/$f"
done
mkdir -p "$SCRIPT_DIR/app/src/main/assets/licenses"
cp "$VENDOR/LICENSE" "$SCRIPT_DIR/app/src/main/assets/licenses/BRouter-LICENSE.txt"
echo "Pinned BRouter engine $SHA with v10 routing profiles"
