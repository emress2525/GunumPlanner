import unittest
from pathlib import Path

import market_upgrade


BASE_HTML = '''<html><head><style>:root{--bg:#f6f7f4;--card:#fff;--text:#17231f;--muted:#748078;--green:#0d6b51;--green2:#084c3b;--line:#dfe6e1;--soft:#edf4f0;--shadow:0 10px 30px rgba(10,45,35,.08)}.top{padding:20px}.card{padding:16px}.quick{padding:12px}.menu-card{padding:16px}</style></head><body><div class="app"><section id="home" class="screen active"><div class="top"><div class="row"><div><div class="brand">Namaz</div><div id="locText" class="loc">Ankara, Türkiye</div></div></div><div class="hero"><small>Sıradaki namaz</small><div id="nextPrayer" class="next">—</div><div id="countdown" class="count">Vakitler yükleniyor…</div><div id="dateText" class="date"></div></div></div><div class="wrap"><div class="card"><h2 class="title">Bugünün namaz vakitleri</h2><div id="prayers" class="prayers"></div><div id="prayerSource" class="sub"></div></div><div class="card"><h2 class="title">Günün ayeti</h2><div id="verseAr" class="verse-ar"></div><div id="verseTr" class="verse-tr"></div><div id="verseSrc" class="verse-src"></div></div><div class="card"><h2 class="title">Kaldığın yer</h2><button id="lastRead" class="continue">Henüz okuma kaydı yok</button></div><div class="card"><h2 class="title">Hızlı erişim</h2><div class="grid3"></div></div></div></section><section id="ibadet" class="screen"><div class="head"><h1>İbadet</h1></div><div class="wrap"><div class="menu-list"><button class="menu-card" onclick="show('knowledge')"><i>☼</i><b>İslami Bilgiler</b><span>Kaynaklı sosyal hayat rehberi</span></button></div></div></section><section id="knowledge" class="screen"></section></div></body></html>'''

BASE_JS = '''function syncNativeSchedule(){if(!prayerData||!window.AndroidBridge)return;const t=prayerData.timings,now=new Date();const next=C.computeNextPrayer(t,now);try{AndroidBridge.updateWidget(JSON.stringify({name:next.name,time:next.time,epoch:next.date.getTime()}))}catch(e){}}'''


class MarketUpgradeTests(unittest.TestCase):
    def test_namaz_hocasi_guide_is_injected_with_required_topics_and_sources(self):
        html = market_upgrade.upgrade_html(BASE_HTML)
        self.assertIn('Namaz Hocası', html)
        for text in [
            'Namaza Hazırlık', 'Gusül', 'Abdest', 'Teyemmüm',
            'Beş Vakit Namaz', 'Cemaat ve Cuma', 'Tâdil-i Erkân',
            'Namazda Okunan Sûre ve Dualar', 'Hanefî', 'Şafiî',
            'DİB Namaz İlmihali'
        ]:
            self.assertIn(text, html)

    def test_home_visual_refresh_preserves_runtime_ids(self):
        html = market_upgrade.upgrade_html(BASE_HTML)
        for element_id in ['locText', 'nextPrayer', 'countdown', 'dateText', 'prayers', 'prayerSource', 'verseAr', 'verseTr', 'verseSrc', 'lastRead']:
            self.assertIn(f'id="{element_id}"', html)
        self.assertIn('market-polish-v1', html)
        self.assertIn('Namaz Hocası', html)

    def test_widget_payload_contains_all_prayer_times_hijri_city_tracker_and_resume(self):
        js = market_upgrade.upgrade_js(BASE_JS)
        for key in ['imsak:', 'gunes:', 'ogle:', 'ikindi:', 'aksam:', 'yatsi:', 'hijri:', 'city:', 'tracked:', 'resume:']:
            self.assertIn(key, js)

    def test_widget_layout_is_information_dense(self):
        xml = market_upgrade.WIDGET_XML
        for widget_id in [
            'widgetHijri', 'widgetPrayer', 'widgetRemaining', 'widgetTimes',
            'widgetTracker', 'widgetAyah'
        ]:
            self.assertIn(widget_id, xml)
        for label in ['İmsak', 'Güneş', 'Öğle', 'İkindi', 'Akşam', 'Yatsı']:
            self.assertIn(label, xml)


if __name__ == '__main__':
    unittest.main()
