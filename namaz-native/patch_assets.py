#!/usr/bin/env python3
from __future__ import annotations

import argparse
import sys
import unittest
from pathlib import Path

SOCIAL_SECTION = r'''
<section id="knowledge" class="screen"><div class="head"><h1>İslami Bilgiler</h1><p>Günlük ve sosyal hayata dair kaynaklı kısa rehber</p></div><div class="wrap">
<div class="card notice"><b>Kaynak ilkesi:</b> Bu bölüm fetva üretmez. Kur'an ayetlerine dayalı genel ahlâkî ilkeleri özetler. Fıkhî ayrıntı ve ihtilaflı meselelerde güvenilir bir din âlimine veya resmî fetva kaynağına danış.</div>
<div class="card knowledge"><h3>Anne-baba ve aile</h3><p>Anne-babaya saygılı davranmak, incitici sözden kaçınmak ve merhametle muamele etmek temel ilkedir.</p><small>Kaynak: İsrâ 17:23-24</small></div>
<div class="card knowledge"><h3>Eşler arası hayat</h3><p>Evlilikte huzur, sevgi ve merhamet öne çıkar; eşlere iyi ve makul davranmak esastır.</p><small>Kaynak: Rûm 30:21; Nisâ 4:19</small></div>
<div class="card knowledge"><h3>Komşuluk</h3><p>Yakın ve uzak komşuya iyilik etmek sosyal sorumluluğun bir parçası olarak zikredilir.</p><small>Kaynak: Nisâ 4:36</small></div>
<div class="card knowledge"><h3>Söz ve üslup</h3><p>İnsanlarla konuşurken daha güzel olan sözü seçmek, alay ve aşağılamadan kaçınmak gerekir.</p><small>Kaynak: İsrâ 17:53; Hucurât 49:11</small></div>
<div class="card knowledge"><h3>Haber ve sosyal medya</h3><p>Bir haberi yaymadan önce doğruluğunu araştır; yanlış bilgiyle insanlara zarar vermemeye dikkat et.</p><small>Kaynak: Hucurât 49:6</small></div>
<div class="card knowledge"><h3>Mahremiyet, zan ve gıybet</h3><p>Gereksiz şüpheden, insanların gizlisini araştırmaktan ve arkalarından konuşmaktan kaçın.</p><small>Kaynak: Hucurât 49:12</small></div>
<div class="card knowledge"><h3>İş ve ticaret</h3><p>Malı haksız yolla elde etmekten kaçın; alışverişi karşılıklı rızaya dayandır ve ölçü-tartıda dürüst ol.</p><small>Kaynak: Nisâ 4:29; Mutaffifîn 83:1-3</small></div>
<div class="card knowledge"><h3>Borç ve kayıt</h3><p>Vadeli borçlarda şartların açıkça yazılması, hakların korunmasına yardımcı olan bir ilkedir.</p><small>Kaynak: Bakara 2:282</small></div>
<div class="card knowledge"><h3>Adalet ve hak</h3><p>Kendi aleyhine bile olsa adaleti gözetmek ve hakkı ayakta tutmak emredilir.</p><small>Kaynak: Nisâ 4:135</small></div>
<div class="card knowledge"><h3>Öfke ve affetme</h3><p>Öfkeyi kontrol etmek ve affedici olmak övülen davranışlardandır.</p><small>Kaynak: Âl-i İmrân 3:134</small></div>
<div class="card knowledge"><h3>Arkadaşlık ve kırgınlık</h3><p>Müminler arasında barışı ve uzlaşmayı desteklemek; kötü karşılığa daha güzel olanla cevap vermek öğütlenir.</p><small>Kaynak: Hucurât 49:10; Fussilet 41:34</small></div>
<div class="card knowledge"><h3>Temizlik</h3><p>Abdest ve temizlik ibadete hazırlığın parçasıdır; temizlikte aşırılık yerine ölçülülük ve kolaylık esastır.</p><small>Kaynak: Mâide 5:6</small></div>
<div class="card knowledge"><h3>Yolculuk ve kolaylık</h3><p>Yolculukta bazı ibadet hükümlerinde ruhsatlar vardır. Ayrıntı mezhep ve şartlara göre değişebilir; tek bir görüş evrensel diye sunulmaz.</p><small>Kaynak: Bakara 2:185; Nisâ 4:101</small></div>
</div></section>
'''

KNOWLEDGE_BUTTON = '<button class="menu-card" onclick="show(\'knowledge\')"><i>☼</i><b>İslami Bilgiler</b><span>Kaynaklı sosyal hayat rehberi</span></button>'

ADHAN_FIELD = '''<div class="field"><label>Ezan ve arka plan bildirimi</label><select id="adhanMode"><option value="full">Tam ezan</option><option value="short">Kısa ezan</option><option value="notification">Sadece bildirim</option><option value="off">Kapalı</option></select></div><button class="pill" type="button" onclick="stopAdhan()" style="margin-bottom:14px">Çalan ezanı durdur</button>'''


def patch_html(html: str) -> str:
    html = html.replace('padding-bottom:90px', 'padding-bottom:calc(176px + env(safe-area-inset-bottom))')
    html = html.replace('#toast{position:fixed;left:50%;bottom:92px', '#toast{position:fixed;left:50%;bottom:calc(178px + env(safe-area-inset-bottom))')
    html = html.replace('#miniPlayer{position:fixed;left:14px;right:14px;bottom:86px', '#miniPlayer{position:fixed;left:14px;right:14px;bottom:calc(104px + env(safe-area-inset-bottom))')
    html = html.replace('.ayah{background:', '.ayah{scroll-margin-bottom:190px;background:')
    html = html.replace('</style></head>', '.knowledge h3{margin:0 0 8px}.knowledge p{line-height:1.62;margin:0 0 9px}.knowledge small{color:var(--muted);font-weight:700}.screen.active{padding-bottom:calc(94px + env(safe-area-inset-bottom))}</style></head>')

    qibla_button = '<button class="menu-card" onclick="show(\'qibla\')"><i>🕋</i><b>Kıble</b><span>Pusula ile yön</span></button>'
    if KNOWLEDGE_BUTTON not in html and qibla_button in html:
        html = html.replace(qibla_button, qibla_button + KNOWLEDGE_BUTTON)

    if 'id="knowledge"' not in html:
        marker = '<section id="qibla" class="screen">'
        html = html.replace(marker, SOCIAL_SECTION + marker)

    reminder_field = '<div class="field"><label>Uygulama açıkken namazdan kaç dakika önce hatırlatsın?</label><select id="reminder"><option value="0">Kapalı</option><option value="5">5 dakika</option><option value="10">10 dakika</option><option value="15">15 dakika</option><option value="30">30 dakika</option></select></div>'
    if 'id="adhanMode"' not in html and reminder_field in html:
        html = html.replace(reminder_field, reminder_field + ADHAN_FIELD)

    old_note = '<div class="card notice"><b>Hatırlatma notu:</b> Bu APK\'daki hatırlatma uygulama açıkken çalışır. Telefon kapalıyken veya uygulama tamamen sonlandırıldığında çalışan ezan/alarm ve sistem ana ekran widget\'ı için ayrı yerel Android servis katmanı gerekir.</div>'
    new_note = '<div class="card notice"><b>Ezan ve widget:</b> Arka plan ezanı Android alarm katmanı üzerinden çalışır. Android bildirim ve tam zamanlı alarm izinleri kapalıysa sistem gecikmeli veya sessiz bildirim uygulayabilir. Ayarlardan ezanı kapatabilir veya bildirim moduna alabilirsin.</div>'
    html = html.replace(old_note, new_note)

    qibla_notice = 'Pusula sensörünün doğruluğu metal nesneler ve mıknatıslı kılıflardan etkilenebilir. Gerekirse telefonu sekiz çizerek kalibre et.'
    html = html.replace(qibla_notice, '<span id="qiblaAccuracy">Sensör hazırlanıyor…</span><br>' + qibla_notice)

    html = html.replace('<button onclick="pauseAudio()">⏯</button></div><div id="toast">', '<button onclick="pauseAudio()">⏯</button><button onclick="stopAudio()">■</button></div><div id="toast">')
    return html


def patch_js(js: str) -> str:
    js = js.replace("let cfg=readJSON('cfg',{city:'Ankara',country:'Türkiye',fontSize:28,theme:'system',elder:false,reminder:10});", "let cfg=readJSON('cfg',{city:'Ankara',country:'Türkiye',fontSize:28,theme:'system',elder:false,reminder:10,adhanMode:'full'});")

    old_play = "function playGlobal(global,fromQueue){player.src=`https://cdn.islamic.network/quran/audio/128/ar.alafasy/${global}.mp3`;player.play().then(()=>setPlayerLabel(`${fromQueue?'Sure oynatılıyor':'Ayet oynatılıyor'} · ${currentSurah}. sure`)).catch(()=>toast('Ses başlatılamadı.'))}"
    new_play = "function playGlobal(global,fromQueue){const url=`https://cdn.islamic.network/quran/audio/128/ar.alafasy/${global}.mp3`,label=`${fromQueue?'Sure oynatılıyor':'Ayet oynatılıyor'} · ${currentSurah}. sure`;if(window.AndroidBridge&&AndroidBridge.playQuran){AndroidBridge.playQuran(url,label);setPlayerLabel(label);return}player.src=url;player.play().then(()=>setPlayerLabel(label)).catch(()=>toast('Ses başlatılamadı.'))}"
    js = js.replace(old_play, new_play)

    old_pause = "function pauseAudio(){if(player.paused)player.play();else player.pause();setPlayerLabel(player.paused?'Duraklatıldı':'Oynatılıyor')}"
    new_pause = "function pauseAudio(){if(window.AndroidBridge&&AndroidBridge.pauseResumeQuran){AndroidBridge.pauseResumeQuran();return}if(player.paused)player.play();else player.pause();setPlayerLabel(player.paused?'Duraklatıldı':'Oynatılıyor')}\nfunction stopAudio(){audioQueue=[];audioIndex=-1;if(window.AndroidBridge&&AndroidBridge.stopQuran){AndroidBridge.stopQuran()}else{player.pause();try{player.currentTime=0}catch(e){}}document.getElementById('miniPlayer').classList.remove('visible')}\nwindow.onNativeAudioEnded=function(){if(audioIndex>=0&&audioIndex+1<audioQueue.length){audioIndex++;playGlobal(audioQueue[audioIndex],true)}else{audioIndex=-1;audioQueue=[];document.getElementById('miniPlayer').classList.remove('visible')}};\nwindow.onNativeAudioState=function(state){if(state==='paused')setPlayerLabel('Duraklatıldı');else if(state==='playing')setPlayerLabel('Oynatılıyor');else if(state==='stopped')document.getElementById('miniPlayer').classList.remove('visible')};"
    js = js.replace(old_pause, new_pause)

    old_speak = "function speakMeal(){const text=currentAyahs.map(x=>x.tr).join(' ');if(!('speechSynthesis' in window)){toast('Bu cihazda sesli okuma desteklenmiyor.');return}speechSynthesis.cancel();const u=new SpeechSynthesisUtterance(text);u.lang='tr-TR';u.rate=.9;speechSynthesis.speak(u);toast('Türkçe meal sesli okunuyor')}"
    new_speak = "function speakMeal(){const text=currentAyahs.map(x=>x.tr).join(' ');if(window.AndroidBridge&&AndroidBridge.speakTurkish){AndroidBridge.speakTurkish(text);toast('Türkçe meal sesli okunuyor');return}if(!('speechSynthesis' in window)){toast('Sesli okuma kullanılamıyor.');return}speechSynthesis.cancel();const u=new SpeechSynthesisUtterance(text);u.lang='tr-TR';u.rate=.9;speechSynthesis.speak(u);toast('Türkçe meal sesli okunuyor')}"
    js = js.replace(old_speak, new_speak)

    old_compass = "function initCompass(){document.getElementById('qiblaText').textContent=`Kıble: ${Math.round(qibla)}°`;if(window.DeviceOrientationEvent){window.addEventListener('deviceorientationabsolute',orient,true);window.addEventListener('deviceorientation',orient,true)}}\nfunction orient(e){const heading=e.webkitCompassHeading!=null?e.webkitCompassHeading:(e.alpha!=null?360-e.alpha:0);document.getElementById('needle').style.transform=`translate(-50%,-93%) rotate(${qibla-heading}deg)`}"
    new_compass = "function initCompass(){document.getElementById('qiblaText').textContent=`Kıble: ${Math.round(qibla)}°`;if(window.AndroidBridge&&AndroidBridge.startQibla){AndroidBridge.startQibla(qibla);return}if(window.DeviceOrientationEvent){window.addEventListener('deviceorientationabsolute',orient,true);window.addEventListener('deviceorientation',orient,true)}}\nfunction orient(e){const heading=e.webkitCompassHeading!=null?e.webkitCompassHeading:(e.alpha!=null?360-e.alpha:0);applyHeading(heading,0)}\nfunction applyHeading(heading,accuracy){document.getElementById('needle').style.transform=`translate(-50%,-93%) rotate(${qibla-heading}deg)`;const a=document.getElementById('qiblaAccuracy');if(a)a.textContent=accuracy>=3?'Sensör doğruluğu: iyi':accuracy===2?'Sensör doğruluğu: orta':'Sensör doğruluğu düşük; telefonu sekiz çizerek kalibre et.'}\nwindow.onNativeHeading=function(heading,accuracy){applyHeading(Number(heading)||0,Number(accuracy)||0)};\nwindow.onNativeQiblaUnavailable=function(reason){const a=document.getElementById('qiblaAccuracy');if(a)a.textContent='Pusula kullanılamıyor: '+reason};"
    js = js.replace(old_compass, new_compass)

    old_show = "function show(id){document.querySelectorAll('.screen').forEach(x=>x.classList.remove('active'));const el=document.getElementById(id);if(el)el.classList.add('active');document.querySelectorAll('.nav button').forEach(x=>x.classList.toggle('active',x.dataset.s===id));if(id==='qibla')initCompass();"
    new_show = "function show(id){if(id!=='qibla'&&window.AndroidBridge&&AndroidBridge.stopQibla)AndroidBridge.stopQibla();document.querySelectorAll('.screen').forEach(x=>x.classList.remove('active'));const el=document.getElementById(id);if(el)el.classList.add('active');document.querySelectorAll('.nav button').forEach(x=>x.classList.toggle('active',x.dataset.s===id));if(id==='qibla')initCompass();"
    js = js.replace(old_show, new_show)

    js = js.replace("elder:document.getElementById('elder').checked,reminder:Number(document.getElementById('reminder').value)||0}", "elder:document.getElementById('elder').checked,reminder:Number(document.getElementById('reminder').value)||0,adhanMode:(document.getElementById('adhanMode')||{}).value||'full'}")
    js = js.replace("document.getElementById('reminder').value=String(cfg.reminder||0)}", "document.getElementById('reminder').value=String(cfg.reminder||0);const am=document.getElementById('adhanMode');if(am)am.value=cfg.adhanMode||'full'}")

    anchor = "function scheduleOpenReminder(){clearTimeout(reminderTimer);"
    helpers = "function syncNativeSchedule(){if(!prayerData||!window.AndroidBridge)return;const t=prayerData.timings,now=new Date(),keys=[['Fajr','İmsak'],['Dhuhr','Öğle'],['Asr','İkindi'],['Maghrib','Akşam'],['Isha','Yatsı']],prayers=[];for(const [k,name] of keys){const tm=C.cleanTime(t[k]),p=tm.split(':').map(Number),d=new Date(now);d.setHours(p[0]||0,p[1]||0,0,0);if(d<=now&&k==='Fajr')d.setDate(d.getDate()+1);if(d>now)prayers.push({key:k,name,time:tm,epoch:d.getTime()})}try{AndroidBridge.schedulePrayers(JSON.stringify({mode:cfg.adhanMode||'full',prayers}))}catch(e){}const next=C.computeNextPrayer(t,now);try{AndroidBridge.updateWidget(JSON.stringify({name:next.name,time:next.time,epoch:next.date.getTime()}))}catch(e){}}\nfunction stopAdhan(){if(window.AndroidBridge&&AndroidBridge.stopAdhan)AndroidBridge.stopAdhan();toast('Ezan sesi durduruldu')}\n"
    if 'function syncNativeSchedule()' not in js:
        js = js.replace(anchor, helpers + anchor)

    js = js.replace("renderPrayerTimes();updateNext();scheduleOpenReminder();renderRamadan()", "renderPrayerTimes();updateNext();scheduleOpenReminder();renderRamadan();syncNativeSchedule()")
    js = js.replace("document.getElementById('prayerSource').textContent='Çevrimdışı önbellek';renderRamadan()", "document.getElementById('prayerSource').textContent='Çevrimdışı önbellek';renderRamadan();syncNativeSchedule()")
    return js


def patch_manifest(xml: str) -> str:
    permissions = '''\n  <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />\n  <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />\n  <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />\n  <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />\n  <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />'''
    if 'SCHEDULE_EXACT_ALARM' not in xml:
        xml = xml.replace('<uses-permission android:name="android.permission.VIBRATE" />', '<uses-permission android:name="android.permission.VIBRATE" />' + permissions)
    components = '''\n    <service android:name=".AdhanService" android:exported="false" android:foregroundServiceType="mediaPlayback" />\n    <receiver android:name=".PrayerAlarmReceiver" android:exported="false" />\n    <receiver android:name=".BootReceiver" android:exported="true">\n      <intent-filter>\n        <action android:name="android.intent.action.BOOT_COMPLETED" />\n        <action android:name="android.intent.action.TIME_SET" />\n        <action android:name="android.intent.action.TIMEZONE_CHANGED" />\n        <action android:name="android.intent.action.DATE_CHANGED" />\n      </intent-filter>\n    </receiver>\n    <receiver android:name=".PrayerWidgetProvider" android:exported="true">\n      <intent-filter><action android:name="android.appwidget.action.APPWIDGET_UPDATE" /></intent-filter>\n      <meta-data android:name="android.appwidget.provider" android:resource="@xml/prayer_widget_info" />\n    </receiver>'''
    if '.AdhanService' not in xml:
        xml = xml.replace('</application>', components + '\n  </application>')
    return xml


def patch_project(root: Path) -> None:
    assets = root / 'app/src/main/assets'
    html_path = assets / 'index.html'
    js_path = assets / 'app.js'
    manifest_path = root / 'app/src/main/AndroidManifest.xml'
    html_path.write_text(patch_html(html_path.read_text(encoding='utf-8')), encoding='utf-8')
    js_path.write_text(patch_js(js_path.read_text(encoding='utf-8')), encoding='utf-8')
    manifest_path.write_text(patch_manifest(manifest_path.read_text(encoding='utf-8')), encoding='utf-8')


def self_test(root: Path | None = None) -> None:
    sample_html = '<style>.app{min-height:100vh;padding-bottom:90px}#toast{position:fixed;left:50%;bottom:92px}#miniPlayer{position:fixed;left:14px;right:14px;bottom:86px}.ayah{background:x}</style></head><body><section id="ibadet" class="screen"></section><section id="qibla" class="screen"></section><div id="toast"></div></body></html>'
    h = patch_html(sample_html)
    assert 'padding-bottom:calc(176px + env(safe-area-inset-bottom))' in h
    assert 'bottom:calc(104px + env(safe-area-inset-bottom))' in h
    assert 'İslami Bilgiler' in h and 'İsrâ 17:23-24' in h and 'Hucurât 49:6' in h
    if root:
        actual = (root / 'app/src/main/assets/index.html').read_text(encoding='utf-8')
        assert 'id="knowledge"' in actual
        actual_js = (root / 'app/src/main/assets/app.js').read_text(encoding='utf-8')
        assert 'AndroidBridge.playQuran' in actual_js and 'window.onNativeHeading' in actual_js


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument('--apply', metavar='ROOT')
    parser.add_argument('--self-test', nargs='?', const='')
    args = parser.parse_args()
    if args.apply:
        patch_project(Path(args.apply))
    if args.self_test is not None:
        self_test(Path(args.self_test) if args.self_test else None)
    if not args.apply and args.self_test is None:
        parser.print_help()
        return 2
    return 0

if __name__ == '__main__':
    raise SystemExit(main())
