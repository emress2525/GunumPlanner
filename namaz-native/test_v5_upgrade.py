import unittest
from v5_upgrade import upgrade_html


class V5UpgradeTests(unittest.TestCase):
    def setUp(self):
        self.html = '''<!doctype html><html><head><style>.x{}</style></head><body><div class="app">
<section id="home" class="screen active"><div id="nextPrayer"></div><div id="countdown"></div><div id="prayers"></div><div id="verseAr"></div><div id="verseTr"></div><div id="verseSrc"></div><button id="lastRead"></button><div id="dateText"></div><div id="locText"></div></section>
<section id="quran" class="screen"></section>
<section id="ibadet" class="screen"></section>
<section id="knowledge" class="screen"></section>
<section id="settings" class="screen"></section>
</div><nav class="nav"><button data-s="home">Ana Sayfa</button><button data-s="quran">Kur'an</button><button data-s="ibadet">İbadet</button><button data-s="qibla">Kıble</button><button data-s="settings">Ayarlar</button></nav></body></html>'''

    def test_adds_v5_identity_and_five_tab_navigation(self):
        out = upgrade_html(self.html)
        self.assertIn('NAMAZ V5', out)
        self.assertIn('data-s="learn"', out)
        self.assertIn('Öğren', out)
        self.assertIn('data-s="more"', out)
        self.assertNotIn('data-s="qibla" onclick="show(\'qibla\')"', out)

    def test_adds_rich_today_and_preserves_dynamic_ids(self):
        out = upgrade_html(self.html)
        for token in ['id="nextPrayer"', 'id="countdown"', 'id="prayers"', 'id="verseAr"', 'id="verseTr"', 'id="verseSrc"', 'id="lastRead"', 'id="dateText"', 'id="locText"']:
            self.assertIn(token, out)
        self.assertIn('Bugün öğren', out)
        self.assertIn('Namaz Hocası', out)
        self.assertIn('Günün duası', out)

    def test_adds_beginner_learning_path_and_reference_library(self):
        out = upgrade_html(self.html)
        required = [
            'id="learn"', 'Sıfırdan Namaz Öğren', 'Abdest: adım adım',
            'Namazın hareketleri', 'Namazda okunanlar', 'Beş vakit namaz',
            '32 Farz', 'Sehiv secdesi', 'Seferîlik ve yolculuk', 'Huşû ve vesvese'
        ]
        for token in required:
            self.assertIn(token, out)

    def test_adds_social_life_topics_and_visible_source_policy(self):
        out = upgrade_html(self.html)
        for token in ['Aile ve anne-baba', 'Komşuluk', 'İş ve ticaret', 'Borç ve kul hakkı', 'Gıybet', 'Öfke', 'Mahremiyet ve sosyal medya', 'Kaynak:']:
            self.assertIn(token, out)

    def test_reserves_bottom_safe_area(self):
        out = upgrade_html(self.html)
        self.assertIn('padding-bottom:calc(138px + env(safe-area-inset-bottom))', out)


if __name__ == '__main__':
    unittest.main()
