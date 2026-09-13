#!/usr/bin/env python3
from __future__ import annotations
import argparse, re
from pathlib import Path

V5_CSS = r'''
/* namaz-v5 */
:root{--v5gold:#c89b4a;--v5cream:#f6f2e9;--v5ink:#17372f}
.app{padding-bottom:calc(138px + env(safe-area-inset-bottom))}.screen.active{padding-bottom:calc(138px + env(safe-area-inset-bottom))}
.v5-home{background:linear-gradient(180deg,color-mix(in srgb,var(--green) 5%,var(--bg)),var(--bg) 36%)}
.v5-hero{position:relative;overflow:hidden;padding:20px 18px 28px;color:#fff;background:linear-gradient(145deg,#063d31,#0b684f 58%,#16775d);border-radius:0 0 34px 34px;box-shadow:0 20px 48px rgba(7,72,55,.24)}
.v5-hero:before,.v5-hero:after{content:'';position:absolute;border-radius:50%;border:1px solid rgba(255,255,255,.15)}.v5-hero:before{width:220px;height:220px;right:-110px;top:-110px}.v5-hero:after{width:145px;height:145px;right:-70px;top:-70px}
.v5-brandrow,.v5-nextrow,.v5-sectionhead,.v5-duorow{display:flex;align-items:center;justify-content:space-between;gap:12px}.v5-brandwrap{display:flex;align-items:center;gap:10px}.v5-mark{width:42px;height:42px;border-radius:14px;display:grid;place-items:center;border:1px solid rgba(255,255,255,.2);background:rgba(255,255,255,.11);font-weight:900;font-size:22px}.v5-brand{font-size:23px;font-weight:950;letter-spacing:-.4px}.v5-version{display:inline-flex;align-items:center;border-radius:999px;padding:4px 7px;font-size:9px;letter-spacing:.08em;font-weight:900;background:rgba(255,255,255,.15);margin-left:6px}.v5-location{font-size:12px;opacity:.78;margin-top:2px}.v5-date{font-size:10px;font-weight:800;padding:8px 10px;border:1px solid rgba(255,255,255,.16);border-radius:999px;background:rgba(255,255,255,.1);max-width:42%;text-align:center}.v5-next{position:relative;z-index:1;margin-top:28px}.v5-kicker{font-size:10px;opacity:.7;text-transform:uppercase;letter-spacing:.13em;font-weight:850}.v5-next #nextPrayer{font-size:36px;font-weight:950;letter-spacing:-1.1px;margin-top:3px}.v5-count{font-size:12px;font-weight:850;padding:9px 11px;border-radius:13px;background:rgba(255,255,255,.12);border:1px solid rgba(255,255,255,.14);white-space:nowrap}.v5-actions{display:grid;grid-template-columns:repeat(4,1fr);gap:7px;margin-top:18px}.v5-actions button{border:1px solid rgba(255,255,255,.12);border-radius:14px;background:rgba(255,255,255,.1);color:#fff;padding:10px 4px;font-size:10px;font-weight:800}.v5-actions b{display:block;font-size:17px;margin-bottom:3px}
.v5-wrap{padding:15px}.v5-card{background:var(--card);border:1px solid var(--line);border-radius:23px;padding:17px;margin-bottom:12px;box-shadow:0 12px 34px rgba(20,54,43,.075)}.v5-eyebrow{font-size:9px;color:var(--muted);font-weight:900;letter-spacing:.12em;text-transform:uppercase}.v5-title{font-size:18px;font-weight:900;margin:4px 0 0}.v5-prayer-card .prayers{margin-top:12px}.v5-spirit-grid{display:grid;grid-template-columns:1fr;gap:10px}.v5-spirit{padding:16px;border:1px solid var(--line);border-radius:20px;background:linear-gradient(145deg,var(--card),color-mix(in srgb,var(--green) 4%,var(--card)))}.v5-spirit .verse-ar{font-size:calc(var(--arabic-size) * .78)}.v5-source{display:inline-block;margin-top:9px;font-size:10px;font-weight:800;color:var(--green);background:var(--soft);padding:6px 8px;border-radius:999px}.v5-hadith{font-size:16px;line-height:1.5;font-weight:750;margin-top:10px}.v5-dua{font-size:14px;line-height:1.6;margin-top:9px}.v5-progress-card{background:linear-gradient(135deg,#f1e8d4,color-mix(in srgb,var(--green) 5%,#f1e8d4));color:#213a31}.v5-progressline{height:9px;border-radius:999px;background:rgba(9,91,69,.12);overflow:hidden;margin-top:12px}.v5-progressline i{display:block;width:16%;height:100%;background:linear-gradient(90deg,var(--green),#1b8d6b)}.v5-cta{display:flex;align-items:center;justify-content:space-between;gap:12px;width:100%;border:0;border-radius:17px;background:var(--green);color:#fff;padding:13px 14px;margin-top:12px;text-align:left}.v5-cta b,.v5-cta span{display:block}.v5-cta span{font-size:10px;opacity:.8;margin-top:2px}.v5-cta i{font-style:normal;font-size:21px}.v5-tracker{display:grid;grid-template-columns:repeat(5,1fr);gap:6px;margin-top:12px}.v5-trackdot{background:var(--soft);border:1px solid var(--line);border-radius:12px;padding:10px 3px;text-align:center;font-size:9px;color:var(--muted)}.v5-trackdot b{display:block;color:var(--green);font-size:17px;margin-bottom:2px}.v5-continue{display:flex;gap:12px;align-items:center}.v5-book{width:48px;height:48px;border-radius:15px;background:linear-gradient(145deg,#0b654e,#0a4e3e);display:grid;place-items:center;color:#fff;font-weight:900}.v5-continue #lastRead{background:transparent;padding:0;flex:1}.v5-quick{display:grid;grid-template-columns:repeat(4,1fr);gap:7px;margin-top:12px}.v5-quick button{border:1px solid var(--line);background:var(--soft);color:var(--text);border-radius:14px;padding:11px 4px;font-size:10px;font-weight:800}
.v5-learn-head{padding:22px 18px 18px;background:linear-gradient(145deg,#102f29,#0b5f49);color:#fff;border-radius:0 0 30px 30px}.v5-learn-head h1{margin:0;font-size:29px}.v5-learn-head p{font-size:13px;opacity:.8;line-height:1.55}.v5-path{display:grid;gap:10px}.v5-step{display:flex;gap:12px;align-items:flex-start;border:1px solid var(--line);border-radius:19px;background:var(--card);padding:14px;color:var(--text);text-align:left}.v5-stepno{width:34px;height:34px;border-radius:12px;background:var(--green);color:#fff;display:grid;place-items:center;font-weight:900;flex:0 0 auto}.v5-step b,.v5-step span{display:block}.v5-step span{font-size:11px;color:var(--muted);line-height:1.5;margin-top:3px}.v5-module-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:9px}.v5-module{border:1px solid var(--line);border-radius:18px;background:var(--card);padding:14px;text-align:left;color:var(--text);min-height:120px}.v5-module strong,.v5-module small{display:block}.v5-module small{color:var(--muted);font-size:10px;line-height:1.45;margin-top:6px}.v5-module .tag{display:inline-block;margin-bottom:9px;padding:5px 7px;border-radius:999px;background:var(--soft);color:var(--green);font-size:9px;font-weight:850}.v5-life{display:grid;grid-template-columns:repeat(2,1fr);gap:8px}.v5-life button{border:1px solid var(--line);border-radius:15px;background:var(--card);color:var(--text);padding:12px;text-align:left;font-size:11px;font-weight:800}.v5-sourcepolicy{padding:13px;border-radius:16px;background:color-mix(in srgb,var(--green) 7%,var(--card));border:1px solid color-mix(in srgb,var(--green) 20%,var(--line));font-size:11px;line-height:1.55;color:var(--muted)}
.v5-more-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:9px}.v5-more-grid button{border:1px solid var(--line);background:var(--card);border-radius:18px;color:var(--text);padding:16px;text-align:left;min-height:95px}.v5-more-grid b,.v5-more-grid span{display:block}.v5-more-grid span{font-size:10px;color:var(--muted);margin-top:4px}.nav{height:72px;left:10px;right:10px;border-radius:24px}.nav button{font-size:9px}.nav button span{font-size:18px}.nav button[data-s="learn"]{color:var(--green);font-weight:900}.nav button[data-s="learn"] span{width:30px;height:30px;margin:0 auto 2px;border-radius:11px;background:color-mix(in srgb,var(--green) 11%,var(--card));display:grid;place-items:center}
@media(min-width:520px){.v5-spirit-grid{grid-template-columns:1fr 1fr}.v5-module-grid{grid-template-columns:repeat(3,1fr)}}
'''

HOME = r'''
<section id="home" class="screen active v5-home">
  <div class="v5-hero">
    <div class="v5-brandrow"><div class="v5-brandwrap"><div class="v5-mark">N</div><div><div class="v5-brand">Namaz <span class="v5-version">V5</span></div><div id="locText" class="v5-location">Konum hazırlanıyor</div></div></div><div id="dateText" class="v5-date"></div></div>
    <div class="v5-next"><div class="v5-kicker">Sıradaki namaz</div><div class="v5-nextrow"><div id="nextPrayer">—</div><div id="countdown" class="v5-count">Vakitler yükleniyor…</div></div></div>
    <div class="v5-actions"><button onclick="show('learn')"><b>01</b>Öğren</button><button onclick="show('namazHocasi')"><b>02</b>Namaz Hocası</button><button onclick="show('qibla')"><b>03</b>Kıble</button><button onclick="show('quran')"><b>04</b>Kur'an</button></div>
  </div>
  <div class="v5-wrap">
    <div class="v5-card v5-prayer-card"><div class="v5-sectionhead"><div><div class="v5-eyebrow">BUGÜN</div><div class="v5-title">Namaz vakitleri</div></div><div class="sub">Diyanet yöntemi</div></div><div id="prayers" class="prayers"></div><div id="prayerSource" class="sub" style="margin-top:10px"></div></div>
    <div class="v5-card v5-progress-card"><div class="v5-eyebrow">BUGÜN ÖĞREN</div><div class="v5-title">Sıfırdan Namaz Öğren</div><div class="sub" style="margin-top:6px;color:#45645a">Abdestten beş vakit namaza kadar adım adım ilerleyen başlangıç yolu.</div><div class="v5-progressline"><i></i></div><button class="v5-cta" onclick="show('learn')"><div><b>Öğrenme yoluna devam et</b><span>6 temel aşama · kaynaklı anlatım</span></div><i>›</i></button></div>
    <div class="v5-spirit-grid">
      <div class="v5-spirit"><div class="v5-eyebrow">GÜNÜN AYETİ</div><div id="verseAr" class="verse-ar">اِنَّ مَعَ الْعُسْرِ يُسْرًاۙ</div><div id="verseTr" class="verse-tr">Şüphesiz güçlükle beraber bir kolaylık vardır.</div><div id="verseSrc" class="v5-source">İnşirah 94:6</div></div>
      <div class="v5-spirit"><div class="v5-eyebrow">GÜNÜN HADİSİ</div><div class="v5-hadith">“Ameller niyetlere göredir.”</div><div class="v5-source">Buhârî, Bed'ü'l-vahy 1</div><div class="v5-eyebrow" style="margin-top:15px">GÜNÜN DUASI</div><div class="v5-dua">Rabbimiz! Bize dünyada iyilik, ahirette de iyilik ver ve bizi ateş azabından koru.</div><div class="v5-source">Bakara 2:201</div></div>
    </div>
    <div class="v5-card"><div class="v5-sectionhead"><div><div class="v5-eyebrow">İBADET TAKİBİ</div><div class="v5-title">Bugün 0 / 5</div></div><button class="pill" onclick="show('tracker')">Aç</button></div><div class="v5-tracker"><div class="v5-trackdot"><b>○</b>Sabah</div><div class="v5-trackdot"><b>○</b>Öğle</div><div class="v5-trackdot"><b>○</b>İkindi</div><div class="v5-trackdot"><b>○</b>Akşam</div><div class="v5-trackdot"><b>○</b>Yatsı</div></div></div>
    <div class="v5-card"><div class="v5-eyebrow">KUR'AN</div><div class="v5-title">Kaldığın yerden devam et</div><div class="v5-continue" style="margin-top:12px"><div class="v5-book">Q</div><button id="lastRead" class="continue">Henüz okuma kaydı yok</button></div></div>
    <div class="v5-card"><div class="v5-eyebrow">HIZLI ERİŞİM</div><div class="v5-quick"><button onclick="show('duas')">Dualar</button><button onclick="show('tasbih')">Tesbih</button><button onclick="show('ramadan')">Ramazan</button><button onclick="show('knowledge')">Sosyal hayat</button></div></div>
  </div>
</section>
'''

LEARN = r'''
<section id="learn" class="screen">
  <div class="v5-learn-head"><div class="v5-eyebrow" style="color:#d5e8df">NAMAZ V5 AKADEMİ</div><h1>Öğren</h1><p>Dini hiç bilmeyen biri için temelden başlayan, namazı hem hareketleri hem anlamıyla öğreten yol. İçeriklerde kaynak ve mezhep farkı görünür tutulur.</p></div>
  <div class="v5-wrap">
    <div class="v5-card"><div class="v5-eyebrow">BAŞLANGIÇ YOLU</div><div class="v5-title">Sıfırdan Namaz Öğren</div><div class="sub" style="margin:6px 0 13px">Sırayla ilerle; bilmediğin yerde geri dön.</div><div class="v5-path">
      <button class="v5-step" onclick="show('knowledge')"><span class="v5-stepno">1</span><span><b>İman ve ibadetin temeli</b><span>İslam, niyet, temizlik ve namazın hayatımızdaki yeri.</span></span></button>
      <button class="v5-step" onclick="show('namazHocasi')"><span class="v5-stepno">2</span><span><b>Abdest: adım adım</b><span>Abdestin farzları, sırası, bozanlar; gusül ve teyemmüme giriş.</span></span></button>
      <button class="v5-step" onclick="show('namazHocasi')"><span class="v5-stepno">3</span><span><b>Namazın hareketleri</b><span>Kıyam, rükû, secde ve oturuşu hangi sırayla yaptığını öğren.</span></span></button>
      <button class="v5-step" onclick="show('duas')"><span class="v5-stepno">4</span><span><b>Namazda okunanlar</b><span>Sübhaneke, Fâtiha, tahiyyat, salavat ve Rabbena duaları; anlamlarıyla.</span></span></button>
      <button class="v5-step" onclick="show('namazHocasi')"><span class="v5-stepno">5</span><span><b>Beş vakit namaz</b><span>Sabah, öğle, ikindi, akşam ve yatsı; rekât düzenleri ve uygulama.</span></span></button>
      <button class="v5-step" onclick="show('namazHocasi')"><span class="v5-stepno">6</span><span><b>Cemaat, cuma ve günlük uygulama</b><span>Cami adabı, cemaat, cuma ve özel durumlara giriş.</span></span></button>
    </div></div>
    <div class="v5-card"><div class="v5-eyebrow">NAMAZ HOCASI</div><div class="v5-title">İlmihal kütüphanesi</div><div class="v5-module-grid" style="margin-top:13px">
      <button class="v5-module" onclick="show('namazHocasi')"><span class="tag">TEMEL</span><strong>32 Farz</strong><small>İman, İslam, abdest, gusül, teyemmüm ve namaz farzlarının öğretim şeması.</small></button>
      <button class="v5-module" onclick="show('namazHocasi')"><span class="tag">TEMİZLİK</span><strong>Gusül · Abdest · Teyemmüm</strong><small>Farz, sünnet, bozan durumlar ve mezhep notları.</small></button>
      <button class="v5-module" onclick="show('namazHocasi')"><span class="tag">NAMAZ</span><strong>Farz · Vacip · Sünnet</strong><small>Namazın şartları, rükünleri ve günlük pratikte yeri.</small></button>
      <button class="v5-module" onclick="show('namazHocasi')"><span class="tag">DÜZELTME</span><strong>Sehiv secdesi</strong><small>Unutma veya yanılma durumlarında temel çerçeve ve mezhep farkları.</small></button>
      <button class="v5-module" onclick="show('namazHocasi')"><span class="tag">YOLCULUK</span><strong>Seferîlik ve yolculuk</strong><small>Yolculukta namaz ruhsatları; şartların mezheplere göre değişebileceği notuyla.</small></button>
      <button class="v5-module" onclick="show('namazHocasi')"><span class="tag">KALP</span><strong>Huşû ve vesvese</strong><small>Namazı aceleye getirmemek, dikkati toplamak ve vesvesede ölçülü davranmak.</small></button>
    </div><button class="v5-cta" onclick="show('namazHocasi')"><div><b>Namaz Hocası'nın tamamını aç</b><span>Hazırlık · kılınış · özel durumlar · dualar</span></div><i>›</i></button></div>
    <div class="v5-card"><div class="v5-eyebrow">SOSYAL HAYAT</div><div class="v5-title">İslam günlük hayatta</div><p class="sub" style="line-height:1.55">Sadece namaz değil; davranış, hak ve sorumluluk tarafını da öğren.</p><div class="v5-life">
      <button onclick="show('knowledge')">Aile ve anne-baba</button><button onclick="show('knowledge')">Komşuluk</button><button onclick="show('knowledge')">İş ve ticaret</button><button onclick="show('knowledge')">Borç ve kul hakkı</button><button onclick="show('knowledge')">Gıybet</button><button onclick="show('knowledge')">Öfke</button><button onclick="show('knowledge')">Mahremiyet ve sosyal medya</button><button onclick="show('knowledge')">Adalet ve hak</button>
    </div></div>
    <div class="v5-sourcepolicy"><b>Kaynak ilkesi:</b> Öğrenme bölümündeki dinî hükümler kaynak notuyla sunulur. Hanefî ve Şafiî gibi mezhepler arasında fark bulunan konular “tek doğru budur” diye genellenmez. Uygulama kişisel fetva üretmez. Kaynak: Kur'an ayetleri, DİB Namaz İlmihali ve açıkça belirtilen hadis referansları.</div>
  </div>
</section>
'''

MORE = r'''
<section id="more" class="screen"><div class="head"><h1>Daha Fazla</h1><p>Araçlar, rehberler ve uygulama ayarları</p></div><div class="v5-wrap"><div class="v5-more-grid">
<button onclick="show('qibla')"><b>Kıble</b><span>Sensörlü yön bulma ve kalibrasyon</span></button><button onclick="show('settings')"><b>Ayarlar</b><span>Ezan, tema, şehir ve erişilebilirlik</span></button><button onclick="show('knowledge')"><b>İslami Bilgiler</b><span>Kaynaklı sosyal hayat rehberi</span></button><button onclick="show('namazHocasi')"><b>Namaz Hocası</b><span>İlmihal ve uygulamalı namaz rehberi</span></button><button onclick="show('ramadan')"><b>Ramazan</b><span>İmsak, iftar ve oruç takibi</span></button><button onclick="show('duas')"><b>Dualar</b><span>Namaz ve günlük hayat duaları</span></button>
</div><div class="v5-sourcepolicy" style="margin-top:12px"><b>NAMAZ V5</b> · Reklamsız · Hesap zorunlu değil · Analiz SDK'sı yok. Kişisel kayıtlar cihazda tutulur.</div></div></section>
'''

NAV = r'''<nav class="nav"><button data-s="home" class="active" onclick="show('home')"><span>⌂</span>Bugün</button><button data-s="quran" onclick="show('quran')"><span>▤</span>Kur'an</button><button data-s="learn" onclick="show('learn')"><span>◎</span>Öğren</button><button data-s="ibadet" onclick="show('ibadet')"><span>✓</span>İbadet</button><button data-s="more" onclick="show('more')"><span>•••</span>Daha Fazla</button></nav>'''

def upgrade_html(html: str) -> str:
    if 'namaz-v5' not in html:
        html = html.replace('</style>', V5_CSS + '\n</style>', 1)
    html = re.sub(r'<section id="home" class="screen active[^>]*>.*?</section>', HOME.strip(), html, count=1, flags=re.S)
    if 'id="learn"' not in html:
        html = html.replace('<section id="ibadet" class="screen">', LEARN.strip() + '\n<section id="ibadet" class="screen">', 1)
    if 'id="more"' not in html:
        html = html.replace('<section id="settings" class="screen">', MORE.strip() + '\n<section id="settings" class="screen">', 1)
    html = re.sub(r'<nav class="nav">.*?</nav>', NAV, html, count=1, flags=re.S)
    return html

def apply(root: Path) -> None:
    p = root / 'app/src/main/assets/index.html'
    p.write_text(upgrade_html(p.read_text(encoding='utf-8')), encoding='utf-8')

def self_test(root: Path) -> None:
    html=(root/'app/src/main/assets/index.html').read_text(encoding='utf-8')
    required=['namaz-v5','NAMAZ V5','id="learn"','Sıfırdan Namaz Öğren','Abdest: adım adım','Namazın hareketleri','Namazda okunanlar','Beş vakit namaz','32 Farz','Sehiv secdesi','Seferîlik ve yolculuk','Huşû ve vesvese','Aile ve anne-baba','Komşuluk','İş ve ticaret','Borç ve kul hakkı','Gıybet','Öfke','Mahremiyet ve sosyal medya','Kaynak:','data-s="more"','padding-bottom:calc(138px + env(safe-area-inset-bottom))']
    for token in required:
        if token not in html: raise SystemExit(f'missing v5 token: {token}')

def main():
    ap=argparse.ArgumentParser(); ap.add_argument('--apply'); ap.add_argument('--self-test'); a=ap.parse_args()
    if a.apply: apply(Path(a.apply))
    if a.self_test: self_test(Path(a.self_test))
    if not a.apply and not a.self_test: ap.error('use --apply ROOT and/or --self-test ROOT')
    return 0
if __name__=='__main__': raise SystemExit(main())
