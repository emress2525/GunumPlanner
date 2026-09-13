#!/usr/bin/env python3
from __future__ import annotations

import argparse
from pathlib import Path

WIDGET_RECEIVERS = [
    ("NextPrayerWidgetProvider", "widget_next_prayer_info"),
    ("CountdownWidgetProvider", "widget_countdown_info"),
    ("PrayerTimesWidgetProvider", "widget_prayer_times_info"),
    ("PrayerTrackerWidgetProvider", "widget_tracker_info"),
    ("DailyAyahWidgetProvider", "widget_daily_ayah_info"),
    ("HijriDateWidgetProvider", "widget_hijri_info"),
    ("QuranResumeWidgetProvider", "widget_quran_resume_info"),
]

LOCK_SCREEN_FIELD = '''<div class="field"><label style="display:flex;gap:10px;align-items:center"><input id="prayerStatus" type="checkbox" checked> Kilit ekranında namaz durumu</label><div class="sub" style="margin-top:6px">Telefon üreticisine göre gerçek kilit ekranı widget desteği değişebilir. Bu seçenek sessiz, herkese görünür namaz durumu bildirimi kullanır.</div></div>'''


def patch_manifest(xml: str) -> str:
    if '.NextPrayerWidgetProvider' in xml:
        return xml
    parts = []
    for cls, meta in WIDGET_RECEIVERS:
        parts.append(
            f'''\n    <receiver android:name=".{cls}" android:exported="true">\n'''
            f'''      <intent-filter><action android:name="android.appwidget.action.APPWIDGET_UPDATE" /></intent-filter>\n'''
            f'''      <meta-data android:name="android.appwidget.provider" android:resource="@xml/{meta}" />\n'''
            f'''    </receiver>'''
        )
    return xml.replace('</application>', ''.join(parts) + '\n  </application>')


def patch_html(html: str) -> str:
    if 'id="prayerStatus"' not in html:
        needle = '<button class="pill" type="button" onclick="stopAdhan()" style="margin-bottom:14px">Çalan ezanı durdur</button>'
        if needle in html:
            html = html.replace(needle, needle + LOCK_SCREEN_FIELD)
    html = html.replace('NAMAZ V6', 'NAMAZ V7').replace('Namaz V6', 'Namaz V7')
    return html


def patch_js(js: str) -> str:
    js = js.replace("adhanMode:'full'});", "adhanMode:'full',prayerStatus:true});")
    js = js.replace(
        "adhanMode:(document.getElementById('adhanMode')||{}).value||'full'}",
        "adhanMode:(document.getElementById('adhanMode')||{}).value||'full',prayerStatus:(document.getElementById('prayerStatus')||{}).checked!==false}"
    )
    js = js.replace(
        "const am=document.getElementById('adhanMode');if(am)am.value=cfg.adhanMode||'full'}",
        "const am=document.getElementById('adhanMode');if(am)am.value=cfg.adhanMode||'full';const ps=document.getElementById('prayerStatus');if(ps)ps.checked=cfg.prayerStatus!==false}"
    )
    js = js.replace(
        "try{AndroidBridge.schedulePrayers(JSON.stringify({mode:cfg.adhanMode||'full',prayers}))}catch(e){}const next=",
        "try{AndroidBridge.schedulePrayers(JSON.stringify({mode:cfg.adhanMode||'full',prayers}))}catch(e){}try{AndroidBridge.setPrayerStatusEnabled(cfg.prayerStatus!==false)}catch(e){}const next="
    )
    return js


def write_strings(path: Path) -> None:
    path.write_text('''<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <string name="app_name">Namaz V7</string>\n    <string name="widget_description">Namaz vakitlerini, sıradaki namaza kalan süreyi, Hicrî tarihi, günlük ayeti, ibadet takibini ve Namaz Akademisi içeriklerini gösterir.</string>\n    <string name="widget_next_prayer_label">Namaz — Sıradaki Vakit</string>\n    <string name="widget_countdown_label">Namaz — Büyük Geri Sayım</string>\n    <string name="widget_prayer_times_label">Namaz — Tüm Vakitler</string>\n    <string name="widget_tracker_label">Namaz — Namaz Takibi</string>\n    <string name="widget_daily_ayah_label">Namaz — Günün Ayeti</string>\n    <string name="widget_hijri_label">Namaz — Hicrî Tarih</string>\n    <string name="widget_quran_resume_label">Namaz — Kur’an’a Devam</string>\n</resources>\n''', encoding='utf-8')


def apply(root: Path) -> None:
    manifest = root / 'app/src/main/AndroidManifest.xml'
    html = root / 'app/src/main/assets/index.html'
    js = root / 'app/src/main/assets/app.js'
    strings = root / 'app/src/main/res/values/strings.xml'

    manifest.write_text(patch_manifest(manifest.read_text(encoding='utf-8')), encoding='utf-8')
    html.write_text(patch_html(html.read_text(encoding='utf-8')), encoding='utf-8')
    js.write_text(patch_js(js.read_text(encoding='utf-8')), encoding='utf-8')
    write_strings(strings)


def self_test(root: Path | None = None) -> None:
    sample_manifest = '<manifest><application><activity android:name=".MainActivity" /></application></manifest>'
    patched = patch_manifest(sample_manifest)
    for cls, meta in WIDGET_RECEIVERS:
        assert cls in patched and meta in patched

    sample_html = '<button class="pill" type="button" onclick="stopAdhan()" style="margin-bottom:14px">Çalan ezanı durdur</button><b>NAMAZ V6</b>'
    patched_html = patch_html(sample_html)
    assert 'id="prayerStatus"' in patched_html and 'NAMAZ V7' in patched_html

    sample_js = "let cfg=readJSON('cfg',{adhanMode:'full'});try{AndroidBridge.schedulePrayers(JSON.stringify({mode:cfg.adhanMode||'full',prayers}))}catch(e){}const next=x;"
    patched_js = patch_js(sample_js)
    assert 'prayerStatus:true' in patched_js
    assert 'AndroidBridge.setPrayerStatusEnabled' in patched_js

    if root:
        actual_manifest = (root / 'app/src/main/AndroidManifest.xml').read_text(encoding='utf-8')
        actual_html = (root / 'app/src/main/assets/index.html').read_text(encoding='utf-8')
        actual_js = (root / 'app/src/main/assets/app.js').read_text(encoding='utf-8')
        for cls, meta in WIDGET_RECEIVERS:
            assert cls in actual_manifest and meta in actual_manifest
        assert 'id="prayerStatus"' in actual_html
        assert 'AndroidBridge.setPrayerStatusEnabled' in actual_js


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument('--apply', metavar='ROOT')
    parser.add_argument('--self-test', nargs='?', const='')
    args = parser.parse_args()
    if args.apply:
        apply(Path(args.apply))
    if args.self_test is not None:
        self_test(Path(args.self_test) if args.self_test else None)
    if not args.apply and args.self_test is None:
        parser.print_help()
        return 2
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
