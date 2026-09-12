import unittest
import widget_ayah_patch


class WidgetAyahPatchTests(unittest.TestCase):
    def test_set_daily_verse_updates_native_widget(self):
        src = "function setDailyVerse(v){document.getElementById('verseAr').textContent=v.ar;document.getElementById('verseTr').textContent=v.tr;document.getElementById('verseSrc').textContent=v.src}"
        out = widget_ayah_patch.patch_js(src)
        self.assertIn('AndroidBridge.updateAyah', out)
        self.assertIn('JSON.stringify(v)', out)


if __name__ == '__main__':
    unittest.main()
