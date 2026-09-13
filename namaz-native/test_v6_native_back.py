import unittest
from pathlib import Path


class V6NativeBackTests(unittest.TestCase):
    def test_android_back_delegates_to_academy_before_webview_history(self):
        src = Path(
            'namaz-native/overlay/app/src/main/java/app/namaz/tr/MainActivity.java'
        ).read_text(encoding='utf-8')
        self.assertIn('academyHandleAndroidBack', src)
        self.assertIn('evaluateJavascript', src)
        academy_pos = src.index('academyHandleAndroidBack')
        web_history_pos = src.index('webView.canGoBack()', academy_pos)
        self.assertLess(academy_pos, web_history_pos)
        self.assertIn('MainActivity.super.onBackPressed()', src)


if __name__ == '__main__':
    unittest.main()
