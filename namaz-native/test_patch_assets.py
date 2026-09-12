import tempfile
import unittest
from pathlib import Path

import patch_assets

BASE_HTML = '''<style>.app{min-height:100vh;padding-bottom:90px}#toast{position:fixed;bottom:92px}#miniPlayer{position:fixed;bottom:86px}</style><div class="app"><section id="ibadet" class="screen"></section></div>'''
BASE_JS = '''function show(id){if(id==='qibla')initCompass()}\nfunction playGlobal(global,fromQueue){player.src=`https://cdn.islamic.network/quran/audio/128/ar.alafasy/${global}.mp3`;player.play()}\nfunction pauseAudio(){if(player.paused)player.play();else player.pause()}\nfunction stopAudio(){player.pause()}\nfunction speakMeal(){if(!('speechSynthesis' in window))return}\nfunction initCompass(){}\nfunction orient(e){}\nasync function loadPrayers(){prayerData=j.data;renderPrayerTimes()}'''

class PatchTests(unittest.TestCase):
    def test_ui_patch_reserves_space_above_bottom_nav(self):
        html = patch_assets.patch_html(BASE_HTML)
        self.assertIn('padding-bottom:calc(176px + env(safe-area-inset-bottom))', html)
        self.assertIn('bottom:calc(104px + env(safe-area-inset-bottom))', html)

    def test_js_patch_uses_native_audio_and_qibla_bridge(self):
        js = patch_assets.patch_js(BASE_JS)
        self.assertIn('AndroidBridge.playQuran', js)
        self.assertIn('AndroidBridge.speakTurkish', js)
        self.assertIn('AndroidBridge.startQibla', js)
        self.assertIn('window.onNativeHeading', js)

    def test_social_content_is_sourced(self):
        html = patch_assets.patch_html(BASE_HTML)
        self.assertIn('İslami Bilgiler', html)
        for ref in ['İsrâ 17:23-24','Hucurât 49:6','Nisâ 4:36','Bakara 2:282']:
            self.assertIn(ref, html)

if __name__ == '__main__':
    unittest.main()
