#!/usr/bin/env bash
set -euo pipefail
PKG="org.afetatlas.offline"
APK="atlas/app/build/outputs/apk/debug/app-debug.apk"
MAP="atlas/tests/turkiye-full.map"
mkdir -p atlas/tests/evidence

adb wait-for-device
adb install -r "$APK"
adb shell am start -n "$PKG/.MainActivity"
sleep 5
adb shell am force-stop "$PKG"

adb root
adb wait-for-device
adb shell "mkdir -p /data/data/$PKG/files"
adb push "$MAP" "/data/data/$PKG/files/turkiye.map"
uid="$(adb shell stat -c %u "/data/data/$PKG" | tr -d '\r')"
gid="$(adb shell stat -c %g "/data/data/$PKG" | tr -d '\r')"
adb shell chown "$uid:$gid" "/data/data/$PKG/files/turkiye.map"
adb shell chmod 600 "/data/data/$PKG/files/turkiye.map"
adb shell ls -lh "/data/data/$PKG/files/turkiye.map"

adb logcat -c
adb shell am start -n "$PKG/.MainActivity"
sleep 5
# AVD pixel_2 1080px wide @ 420dpi; "Türkiye Haritası" top-right.
adb shell input tap 770 95
sleep 25
adb logcat -d -b crash > atlas/tests/evidence/crash.txt
adb shell dumpsys activity activities > atlas/tests/evidence/activity.txt
adb shell uiautomator dump /sdcard/atlas_ui.xml
adb pull /sdcard/atlas_ui.xml atlas/tests/evidence/ui.xml
adb exec-out screencap -p > atlas/tests/evidence/turkey_map.png
if grep -q 'FATAL EXCEPTION' atlas/tests/evidence/crash.txt; then
   echo "Android crashed while opening full Türkiye map"
   tail -150 atlas/tests/evidence/crash.txt
   exit 1
fi
if grep -q 'Harita açılamadı' atlas/tests/evidence/ui.xml; then
   echo "Atlas reported error loading real Türkiye map"; exit 1
fi
if ! grep -q 'Türkiye haritası cihazdan yükleniyor' atlas/tests/evidence/ui.xml; then
   echo "Did not confirm the real all-Türkiye map view status"
   head -c 3000 atlas/tests/evidence/ui.xml
   exit 1
fi
echo "FULL_TURKEY_MAP_ANDROID_EMULATOR_VIEW_PASS"
