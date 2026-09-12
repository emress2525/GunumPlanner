import unittest

import patch_assets

BASE_HTML = '''<style>.app{min-height:100vh;padding-bottom:90px}#toast{position:fixed;left:50%;bottom:92px;transform:translate(-50%,20px)}#miniPlayer{position:fixed;left:14px;right:14px;bottom:86px;background:#063}.ayah{background:#fff}</style></head><body><div class="app"><section id="ibadet" class="screen"><button class="menu-card" onclick="show('qibla')"><i>🕋</i><b>Kıble</b><span>Pusula ile yön</span></button></section><section id="qibla" class="screen"><div class="card notice">Pusula sensörünün doğruluğu metal nesneler ve mıknatıslı kılıflardan etkilenebilir. Gerekirse telefonu sekiz çizerek kalibre et.</div></section></div><div id="miniPlayer" class="row"><span id="miniPlayerText">Ses oynatılıyor</span><button onclick="pauseAudio()">⏯</button></div><div id="toast"></div></body></html>'''

BASE_JS = '''let cfg=readJSON('cfg',{city:'Ankara',country:'Türkiye',fontSize:28,theme:'system',elder:false,reminder:10});
function show(id){document.querySelectorAll('.screen').forEach(x=>x.classList.remove('active'));const el=document.getElementById(id);if(el)el.classList.add('active');document.querySelectorAll('.nav button').forEach(x=>x.classList.toggle('active',x.dataset.s===id));if(id==='qibla')initCompass();if(id==='tracker')renderTracker();if(id==='hatim')renderHatim();if(id==='favorites')renderFavorites();if(id==='ramadan')renderRamadan();if(id==='ibadet')renderIbadetSummary();window.scrollTo(0,0)}
function playGlobal(global,fromQueue){player.src=`https://cdn.islamic.network/quran/audio/128/ar.alafasy/${global}.mp3`;player.play().then(()=>setPlayerLabel(`${fromQueue?'Sure oynatılıyor':'Ayet oynatılıyor'} · ${currentSurah}. sure`)).catch(()=>toast('Ses başlatılamadı.'))}
function pauseAudio(){if(player.paused)player.play();else player.pause();setPlayerLabel(player.paused?'Duraklatıldı':'Oynatılıyor')}
function speakMeal(){const text=currentAyahs.map(x=>x.tr).join(' ');if(!('speechSynthesis' in window)){toast('Bu cihazda sesli okuma desteklenmiyor.');return}speechSynthesis.cancel();const u=new SpeechSynthesisUtterance(text);u.lang='tr-TR';u.rate=.9;speechSynthesis.speak(u);toast('Türkçe meal sesli okunuyor')}
function initCompass(){document.getElementById('qiblaText').textContent=`Kıble: ${Math.round(qibla)}°`;if(window.DeviceOrientationEvent){window.addEventListener('deviceorientationabsolute',orient,true);window.addEventListener('deviceorientation',orient,true)}}
function orient(e){const heading=e.webkitCompassHeading!=null?e.webkitCompassHeading:(e.alpha!=null?360-e.alpha:0);document.getElementById('needle').style.transform=`translate(-50%,-93%) rotate(${qibla-heading}deg)`}
function scheduleOpenReminder(){clearTimeout(reminderTimer);}
'''


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
        for ref in ['İsrâ 17:23-24', 'Hucurât 49:6', 'Nisâ 4:36', 'Bakara 2:282']:
            self.assertIn(ref, html)


if __name__ == '__main__':
    unittest.main()
