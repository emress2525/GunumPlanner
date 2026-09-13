import unittest
import book_expansion

BASE = '''<section id="namazHocasi" class="screen"><div class="wrap guide"><div class="card guide-intro">Giriş</div><details open><summary>Namazın Yeri ve Önemi</summary></details></div></section>'''

class BookExpansionTests(unittest.TestCase):
    def test_adds_remaining_core_book_topics_without_copying_book_text(self):
        html = book_expansion.expand_html(BASE)
        for text in [
            '32 Farz Öğretim Şeması',
            'İmanın 6 esası',
            'İslâmın 5 esası',
            'Namazın 12 farzı',
            'Allah’ın Sıfatları',
            'Ef‘âl-i Mükellefîn',
            'Farz', 'Vacip', 'Sünnet', 'Müstehap', 'Mubah', 'Mekruh', 'Haram',
            'Misvak ve Giyim Adabı',
            'namazın geçerlilik şartı değildir',
            'Telifli risale metinleri birebir aktarılmaz'
        ]:
            self.assertIn(text, html)
        self.assertIn('Diyanet', html)

if __name__ == '__main__':
    unittest.main()
