from pathlib import Path
import re
import sys

root = Path(sys.argv[1])
viewer = root / "app/src/main/java/com/cepscan/app/ui/PdfViewerScreen.kt"
gradle = root / "app/build.gradle.kts"

text = viewer.read_text(encoding="utf-8")
bad = '''\n    DisposableEffect(bitmap) {\n        onDispose { bitmap?.takeIf { !it.isRecycled }?.recycle() }\n    }\n'''
if bad not in text:
    raise SystemExit("Expected recycled-bitmap lifecycle block was not found; refusing an unsafe blind patch.")
text = text.replace(bad, "\n", 1)
viewer.write_text(text, encoding="utf-8")

build = gradle.read_text(encoding="utf-8")
build = re.sub(r'versionCode\s*=\s*\d+', 'versionCode = 42', build, count=1)
build, count = re.subn(r'versionName\s*=\s*"4\.1\.0"', 'versionName = "4.2.0"', build, count=1)
if count != 1:
    raise SystemExit("Expected V4.1 versionName was not found.")
gradle.write_text(build, encoding="utf-8")
print("CepScan V4.2 bitmap lifecycle fix applied.")
