import unittest
from pathlib import Path


class V6PackagingTests(unittest.TestCase):
    def test_v7_workflow_still_builds_v6_academy_assets(self):
        workflow = Path('.github/workflows/namaz-native-build.yml').read_text(encoding='utf-8')
        for token in [
            'Build Namaz V7 APK',
            "applicationId 'app.namaz.tr.v7'",
            'versionCode 7',
            "versionName '7.0.0'",
            'Namaz V7',
            'v6_academy.py --apply namazapp',
            'v6_academy.py --self-test namazapp',
            'academy/academy-data.js',
            'academy/lessons-hanafi.js',
            'academy/lessons-shafii-notes.js',
            'academy/academy.js',
            'academy/academy.css',
            'node --check namazapp/app/src/main/assets/academy/academy.js',
            'app.namaz.tr.v7',
            'Namaz-V7.apk',
            'Namaz-V7-APK',
            'apksigner verify --verbose',
            'unzip -t Namaz-V7.apk',
        ]:
            self.assertIn(token, workflow)

    def test_overlay_label_is_v7(self):
        strings = Path(
            'namaz-native/overlay/app/src/main/res/values/strings.xml'
        ).read_text(encoding='utf-8')
        self.assertIn('<string name="app_name">Namaz V7</string>', strings)

    def test_widget_description_avoids_raw_apostrophe_that_breaks_aapt(self):
        strings = Path(
            'namaz-native/overlay/app/src/main/res/values/strings.xml'
        ).read_text(encoding='utf-8')
        self.assertIn('Namaz Akademisi içeriklerini gösterir.', strings)
        self.assertNotIn("Namaz Akademisi'ni", strings)
        workflow = Path('.github/workflows/namaz-native-build.yml').read_text(encoding='utf-8')
        self.assertIn('Namaz Akademisi içeriklerini gösterir.', workflow)
        self.assertNotIn("Namaz Akademisi'ni", workflow)

    def test_legacy_widget_header_is_kept_for_existing_installations(self):
        widget = Path(
            'namaz-native/overlay/app/src/main/res/layout/prayer_widget.xml'
        ).read_text(encoding='utf-8')
        self.assertIn('android:text="☾  Namaz V6"', widget)

    def test_adhan_fetch_avoids_unauthenticated_github_api_rate_limit(self):
        workflow = Path('.github/workflows/namaz-native-build.yml').read_text(encoding='utf-8')
        self.assertNotIn('api.github.com/repos/Kiwifu/adhan-mp3/git/blobs', workflow)
        self.assertIn('raw.githubusercontent.com/Kiwifu/adhan-mp3', workflow)
        self.assertIn('curl -fL --retry 3', workflow)
        self.assertIn('adhan audio unexpectedly small', workflow)


if __name__ == '__main__':
    unittest.main()
