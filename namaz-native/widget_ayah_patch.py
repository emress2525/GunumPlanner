#!/usr/bin/env python3
from pathlib import Path
import argparse

OLD = "function setDailyVerse(v){document.getElementById('verseAr').textContent=v.ar;document.getElementById('verseTr').textContent=v.tr;document.getElementById('verseSrc').textContent=v.src}"
NEW = "function setDailyVerse(v){document.getElementById('verseAr').textContent=v.ar;document.getElementById('verseTr').textContent=v.tr;document.getElementById('verseSrc').textContent=v.src;if(window.AndroidBridge&&AndroidBridge.updateAyah){try{AndroidBridge.updateAyah(JSON.stringify(v))}catch(e){}}}"


def patch_js(text: str) -> str:
    if 'AndroidBridge.updateAyah' in text:
        return text
    return text.replace(OLD, NEW)


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument('app_root')
    args = p.parse_args()
    path = Path(args.app_root) / 'app/src/main/assets/app.js'
    text = path.read_text(encoding='utf-8')
    patched = patch_js(text)
    if patched == text or 'AndroidBridge.updateAyah' not in patched:
        raise SystemExit('daily ayah bridge patch target not found')
    path.write_text(patched, encoding='utf-8')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
