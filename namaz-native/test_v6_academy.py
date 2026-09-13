import tempfile
import unittest
from pathlib import Path

import v6_academy


BASE_HTML = '''<!doctype html><html><head><style>:root{--bg:#f6f7f4;--card:#fff;--text:#17231f;--muted:#748078;--green:#0d6b51;--line:#dfe6e1;--soft:#edf4f0}</style></head><body><div class="app">
<section id="home" class="screen active"></section>
<section id="learn" class="screen"><div class="v5-learn-head"><h1>Öğren</h1></div><div class="v5-wrap"><button onclick="show('knowledge')">İman</button><button onclick="show('namazHocasi')">Abdest</button><button onclick="show('duas')">Dualar</button></div></section>
<section id="ibadet" class="screen"></section><section id="settings" class="screen"></section></div>
<nav class="nav"><button data-s="home">Bugün</button><button data-s="learn">Öğren</button></nav>
<script src="core.js"></script><script src="app.js"></script></body></html>'''


class V6AcademyTests(unittest.TestCase):
    def test_learn_shell_never_routes_core_lessons_to_legacy_screens(self):
        out = v6_academy.upgrade_html(BASE_HTML)
        learn = v6_academy.learn_section(out)
        self.assertIn('id="academyRoot"', learn)
        self.assertNotIn("show('knowledge')", learn)
        self.assertNotIn("show('duas')", learn)
        self.assertNotIn("show('namazHocasi')", learn)

    def test_assets_have_nine_categories_core_lessons_sources_and_shafii_notes(self):
        files = v6_academy.asset_files()
        v6_academy.validate_assets(files)
        joined = '\n'.join(files.values())
        for token in [
            'islam-ve-iman', '32-farz', 'abdest', 'gusul', 'teyemmum',
            'namazin-hareketleri', 'namazda-okunanlar',
            'sabah-namazi', 'ogle-namazi', 'ikindi-namazi', 'aksam-namazi', 'yatsi-namazi',
            'sehiv-secdesi', 'seferilik', 'hastalikta-namaz',
            'aile', 'ticaret', 'borc-kul-hakki', 'giybet', 'mahremiyet-sosyal-medya',
            'Şafiî', 'Kaynak'
        ]:
            self.assertIn(token, joined)

    def test_five_daily_prayer_lessons_are_rakat_by_rakat(self):
        joined = '\n'.join(v6_academy.asset_files().values())
        expected = {
            'sabah-namazi': ['2 rekât sünnet', '2 rekât farz'],
            'ogle-namazi': ['4 rekât ilk sünnet', '4 rekât farz', '2 rekât son sünnet'],
            'ikindi-namazi': ['4 rekât sünnet', '4 rekât farz'],
            'aksam-namazi': ['3 rekât farz', '2 rekât sünnet'],
            'yatsi-namazi': ['4 rekât ilk sünnet', '4 rekât farz', '2 rekât son sünnet', 'Vitir'],
        }
        for lesson_id, tokens in expected.items():
            self.assertIn(lesson_id, joined)
            for token in tokens:
                self.assertIn(token, joined)
        self.assertIn('Şimdi ne yapıyorum?', joined)

    def test_prayer_readings_have_arabic_pronunciation_and_meaning(self):
        joined = '\n'.join(v6_academy.asset_files().values())
        for token in ['Sübhaneke', 'Fâtiha', 'İhlâs', 'Kevser', 'Ettehiyyâtü', 'Allahümme Salli', 'Allahümme Bârik', 'Rabbenâ Âtinâ']:
            self.assertIn(token, joined)
        for field in ['arabic:', 'pronunciation:', 'meaning:']:
            self.assertIn(field, joined)

    def test_controller_has_internal_navigation_search_and_persistence(self):
        js = v6_academy.asset_files()['academy/academy.js']
        for token in [
            'function academyHome(', 'function academyOpenCategory(', 'function academyOpenLesson(',
            'function academyBack(', 'function academyNextLesson(', 'function academyPreviousLesson(',
            'function academySearch(', 'function academyHandleAndroidBack(',
            'academy.completed', 'academy.lastLesson', 'academy.quiz', 'academy.review', 'academy.scroll'
        ]:
            self.assertIn(token, js)
        self.assertNotIn("show('knowledge')", js)
        self.assertNotIn("show('duas')", js)
        self.assertNotIn("show('namazHocasi')", js)

    def test_academy_css_reserves_bottom_space_and_supports_dark_and_small_screens(self):
        css = v6_academy.asset_files()['academy/academy.css']
        self.assertIn('env(safe-area-inset-bottom)', css)
        self.assertIn('@media(max-width:390px)', css)
        self.assertIn('[data-theme="dark"]', css)
        self.assertIn('.academy-shafii', css)
        self.assertIn('.academy-arabic', css)

    def test_apply_writes_five_academy_assets_and_self_test_passes(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            asset_root = root / 'app/src/main/assets'
            asset_root.mkdir(parents=True)
            (asset_root / 'index.html').write_text(BASE_HTML, encoding='utf-8')
            v6_academy.apply(root)
            for rel in [
                'academy/academy-data.js', 'academy/lessons-hanafi.js',
                'academy/lessons-shafii-notes.js', 'academy/academy.js', 'academy/academy.css'
            ]:
                self.assertTrue((asset_root / rel).is_file(), rel)
            v6_academy.self_test(root)


if __name__ == '__main__':
    unittest.main()
