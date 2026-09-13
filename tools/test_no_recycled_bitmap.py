from pathlib import Path
import sys

root = Path(sys.argv[1])
viewer = root / "app/src/main/java/com/cepscan/app/ui/PdfViewerScreen.kt"
text = viewer.read_text(encoding="utf-8")

forbidden = [
    "DisposableEffect(bitmap)",
    "bitmap?.takeIf { !it.isRecycled }?.recycle()",
]
found = [token for token in forbidden if token in text]
if found:
    raise SystemExit("FAIL: displayed PDF bitmap is still manually recycled: " + ", ".join(found))

print("PASS: PdfViewerScreen does not manually recycle displayed Compose bitmaps.")
