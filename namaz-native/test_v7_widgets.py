import unittest
from pathlib import Path

ROOT = Path('namaz-native/overlay/app/src/main')
JAVA = ROOT / 'java/app/namaz/tr'
RES = ROOT / 'res'

PROVIDERS = {
    'NextPrayerWidgetProvider.java': 'widget_next_prayer',
    'CountdownWidgetProvider.java': 'widget_countdown',
    'PrayerTimesWidgetProvider.java': 'widget_prayer_times',
    'PrayerTrackerWidgetProvider.java': 'widget_tracker',
    'DailyAyahWidgetProvider.java': 'widget_daily_ayah',
    'HijriDateWidgetProvider.java': 'widget_hijri',
    'QuranResumeWidgetProvider.java': 'widget_quran_resume',
}

INFO_XML = [
    'widget_next_prayer_info.xml',
    'widget_countdown_info.xml',
    'widget_prayer_times_info.xml',
    'widget_tracker_info.xml',
    'widget_daily_ayah_info.xml',
    'widget_hijri_info.xml',
    'widget_quran_resume_info.xml',
]


class V7WidgetSuiteTests(unittest.TestCase):
    def test_shared_widget_data_layer_exists(self):
        repo = (JAVA / 'WidgetDataRepository.java')
        utils = (JAVA / 'WidgetRenderUtils.java')
        self.assertTrue(repo.exists())
        self.assertTrue(utils.exists())
        text = repo.read_text(encoding='utf-8')
        self.assertIn('class Snapshot', text)
        self.assertIn('Snapshot load(Context context)', text)
        self.assertIn('remainingText(long epoch)', text)
        self.assertIn('quranResumeText', text)

    def test_seven_focused_providers_and_layouts_exist(self):
        for filename, layout in PROVIDERS.items():
            path = JAVA / filename
            self.assertTrue(path.exists(), filename)
            text = path.read_text(encoding='utf-8')
            self.assertIn('extends AppWidgetProvider', text)
            self.assertIn(f'R.layout.{layout}', text)
            self.assertNotIn('R.layout.prayer_widget', text)
            self.assertTrue((RES / 'layout' / f'{layout}.xml').exists(), layout)

    def test_seven_provider_metadata_files_exist(self):
        for filename in INFO_XML:
            path = RES / 'xml' / filename
            self.assertTrue(path.exists(), filename)
            text = path.read_text(encoding='utf-8')
            self.assertIn('<appwidget-provider', text)
            self.assertIn('android:widgetCategory="home_screen|keyguard"', text)

    def test_suite_updater_refreshes_every_provider_and_legacy(self):
        path = JAVA / 'WidgetSuiteUpdater.java'
        self.assertTrue(path.exists())
        text = path.read_text(encoding='utf-8')
        for provider in [p.replace('.java', '') for p in PROVIDERS]:
            self.assertIn(f'{provider}.updateAll(context);', text)
        self.assertIn('PrayerWidgetProvider.updateAll(context);', text)

    def test_lock_screen_notification_is_silent_public_and_permission_safe(self):
        path = JAVA / 'PrayerStatusNotification.java'
        self.assertTrue(path.exists())
        text = path.read_text(encoding='utf-8')
        for token in [
            'CHANNEL_ID = "prayer_status"',
            'NotificationManager.IMPORTANCE_LOW',
            'setSound(null, null)',
            'enableVibration(false)',
            'Notification.VISIBILITY_PUBLIC',
            'Manifest.permission.POST_NOTIFICATIONS',
            'void cancel(Context context)',
        ]:
            self.assertIn(token, text)
        self.assertNotIn('dailyAyahText', text)

    def test_widget_picker_labels_are_present(self):
        strings = (RES / 'values/strings.xml').read_text(encoding='utf-8')
        for label in [
            'Namaz — Sıradaki Vakit',
            'Namaz — Büyük Geri Sayım',
            'Namaz — Tüm Vakitler',
            'Namaz — Namaz Takibi',
            'Namaz — Günün Ayeti',
            'Namaz — Hicrî Tarih',
            "Namaz — Kur'an'a Devam",
        ]:
            self.assertIn(label, strings)

    def test_workflow_packages_distinct_v7_and_checks_assets(self):
        workflow = Path('.github/workflows/namaz-native-build.yml').read_text(encoding='utf-8')
        for token in [
            'Build Namaz V7 APK',
            "applicationId 'app.namaz.tr.v7'",
            'versionCode 7',
            "versionName '7.0.0'",
            'Namaz V7',
            'Namaz-V7.apk',
            'Namaz-V7-APK',
            'app.namaz.tr.v7',
            'widget_next_prayer.xml',
            'widget_countdown.xml',
            'widget_prayer_times.xml',
            'widget_tracker.xml',
            'widget_daily_ayah.xml',
            'widget_hijri.xml',
            'widget_quran_resume.xml',
            'apksigner verify --verbose',
            'unzip -t Namaz-V7.apk',
        ]:
            self.assertIn(token, workflow)


if __name__ == '__main__':
    unittest.main()
