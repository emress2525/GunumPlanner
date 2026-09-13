import unittest
from pathlib import Path


class V6PackagingTests(unittest.TestCase):
    def test_workflow_builds_distinct_v6_and_verifies_academy_assets(self):
        workflow = Path('.github/workflows/namaz-native-build.yml').read_text(encoding='utf-8')
        for token in [
            'Build Namaz V6 APK',
            "applicationId 'app.namaz.tr.v6'",
            'versionCode 6',
            "versionName '6.0.0'",
            'Namaz V6',
            'v6_academy.py --apply namazapp',
            'v6_academy.py --self-test namazapp',
            'academy/academy-data.js',
            'academy/lessons-hanafi.js',
            'academy/lessons-shafii-notes.js',
            'academy/academy.js',
            'academy/academy.css',
            'node --check namazapp/app/src/main/assets/academy/academy.js',
            'app.namaz.tr.v6',
            'Namaz-V6.apk',
            'Namaz-V6-APK',
            'apksigner verify --verbose',
            'unzip -t Namaz-V6.apk',
        ]:
            self.assertIn(token, workflow)

    def test_overlay_label_is_v6(self):
        strings = Path(
            'namaz-native/overlay/app/src/main/res/values/strings.xml'
        ).read_text(encoding='utf-8')
        self.assertIn('<string name="app_name">Namaz V6</string>', strings)

    def test_widget_header_is_v6(self):
        widget = Path(
            'namaz-native/overlay/app/src/main/res/layout/prayer_widget.xml'
        ).read_text(encoding='utf-8')
        self.assertIn('android:text="☾  Namaz V6"', widget)

    def test_workflow_relocks_exact_widget_xml_attribute_after_legacy_patchers(self):
        workflow = Path('.github/workflows/namaz-native-build.yml').read_text(encoding='utf-8')
        self.assertIn(
            "text=text.replace('android:text=\"☾  Namaz V5\"', 'android:text=\"☾  Namaz V6\"')",
            workflow,
        )
        self.assertIn(
            "text=text.replace('android:text=\"☾  Namaz\"', 'android:text=\"☾  Namaz V6\"')",
            workflow,
        )
        self.assertIn(
            "if 'android:text=\"☾  Namaz V6\"' not in text:",
            workflow,
        )

    def test_adhan_fetch_avoids_unauthenticated_github_api_rate_limit(self):
        workflow = Path('.github/workflows/namaz-native-build.yml').read_text(encoding='utf-8')
        self.assertNotIn('api.github.com/repos/Kiwifu/adhan-mp3/git/blobs', workflow)
        self.assertIn('raw.githubusercontent.com/Kiwifu/adhan-mp3', workflow)
        self.assertIn('curl -fL --retry 3', workflow)
        self.assertIn('adhan audio unexpectedly small', workflow)


if __name__ == '__main__':
    unittest.main()
