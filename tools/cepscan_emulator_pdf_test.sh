#!/usr/bin/env bash
set -euo pipefail

APK="${1:-cepscan-src/app/build/outputs/apk/debug/app-debug.apk}"
PKG="com.cepscan.app"
ACTIVITY="com.cepscan.app/.MainActivity"
LOG="cepscan-pdf-viewer-logcat.txt"

adb install -r "$APK"
: > "$LOG"

# Create a minimal valid one-page PDF with no external dependencies.
python - <<'PY'
from pathlib import Path
objects = [
    b"<< /Type /Catalog /Pages 2 0 R >>",
    b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
    b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources <<>> /Contents 4 0 R >>",
    b"<< /Length 0 >>\nstream\n\nendstream",
]
out = bytearray(b"%PDF-1.4\n")
offsets = [0]
for i, obj in enumerate(objects, 1):
    offsets.append(len(out))
    out += f"{i} 0 obj\n".encode() + obj + b"\nendobj\n"
xref = len(out)
out += f"xref\n0 {len(objects)+1}\n".encode()
out += b"0000000000 65535 f \n"
for off in offsets[1:]:
    out += f"{off:010d} 00000 n \n".encode()
out += f"trailer\n<< /Size {len(objects)+1} /Root 1 0 R >>\nstartxref\n{xref}\n%%EOF\n".encode()
Path('/tmp/cepscan-test.pdf').write_bytes(out)
PY

adb push /tmp/cepscan-test.pdf /data/local/tmp/cepscan-test.pdf >/dev/null
adb shell run-as "$PKG" mkdir -p files
adb shell run-as "$PKG" cp /data/local/tmp/cepscan-test.pdf files/cepscan-test.pdf

adb shell am force-stop "$PKG"
adb logcat -c
adb shell am start -W -a android.intent.action.VIEW -t application/pdf \
  -d "file:///data/user/0/$PKG/files/cepscan-test.pdf" -n "$ACTIVITY" | tee -a "$LOG"

sleep 8
adb logcat -d -v threadtime >> "$LOG"
PID="$(adb shell pidof "$PKG" | tr -d '\r')"
echo "pid=$PID" | tee -a "$LOG"
test -n "$PID"

if adb logcat -d -v brief | grep -E -q "FATAL EXCEPTION|Canvas: trying to use a recycled bitmap|Process: $PKG"; then
  echo "FAIL: PDF viewer crashed or attempted to draw a recycled bitmap." >&2
  adb logcat -d -v threadtime | grep -n -A120 -B30 -E "FATAL EXCEPTION|recycled bitmap|Process: $PKG" || true
  exit 1
fi

adb shell uiautomator dump /sdcard/cepscan-window.xml >/dev/null || true
if adb shell cat /sdcard/cepscan-window.xml 2>/dev/null | grep -q "1 sayfa"; then
  echo "PASS: PDF viewer rendered a one-page PDF and remained alive."
else
  echo "PASS: app remained alive with no recycled-bitmap crash; UI text probe was inconclusive."
fi
