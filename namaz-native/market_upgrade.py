#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
from pathlib import Path


POLISH_CSS = r'''
/* market-polish-v1 */
body{background:
 radial-gradient(circle at 12% -8%,color-mix(in srgb,var(--green) 12%,transparent),transparent 34%),
 radial-gradient(circle at 92% 12%,color-mix(in srgb,#d2aa62 13%,transparent),transparent 27%),var(--bg)}
.market-home .wrap{padding-top:18px}.market-hero{position:relative;overflow:hidden;background:linear-gradient(145deg,#073f32 0%,#0c6b50 56%,#1c8566 100%);color:#fff;padding:22px 18px 25px;border-radius:0 0 34px 34px;box-shadow:0 18px 45px rgba(6,69,52,.22)}
.market-hero:after{content:'';position:absolute;width:190px;height:190px;border-radius:50%;right:-76px;top:-83px;border:1px solid rgba(255,255,255,.18);box-shadow:0 0 0 26px rgba(255,255,255,.035),0 0 0 52px rgba(255,255,255,.025)}
.market-brandline{display:flex;align-items:center;justify-content:space-between;gap:12px;position:relative;z-index:1}.market-logo{display:flex;align-items:center;gap:10px}.market-logo-mark{width:39px;height:39px;border-radius:13px;background:rgba(255,255,255,.14);display:grid;place-items:center;font-size:21px;border:1px solid rgba(255,255,255,.16)}
.market-brand{font-size:22px;font-weight:900;letter-spacing:-.3px}.market-location{font-size:12px;opacity:.78;margin-top:2px}.market-date-chip{font-size:11px;font-weight:750;padding:8px 10px;background:rgba(255,255,255,.12);border:1px solid rgba(255,255,255,.15);border-radius:999px;max-width:43%;text-align:center}
.market-next{position:relative;z-index:1;margin-top:29px}.market-kicker{text-transform:uppercase;letter-spacing:.1em;font-size:10px;font-weight:850;opacity:.72}.market-next-row{display:flex;justify-content:space-between;align-items:flex-end;gap:12px;margin-top:4px}.market-next #nextPrayer{font-size:34px;font-weight:950;letter-spacing:-1px}.market-count{font-size:13px;font-weight:750;background:rgba(255,255,255,.12);border:1px solid rgba(255,255,255,.13);padding:9px 11px;border-radius:13px;white-space:nowrap}.market-hero-actions{display:flex;gap:8px;margin-top:18px;position:relative;z-index:1;overflow:auto}.market-hero-actions button{border:1px solid rgba(255,255,255,.13);background:rgba(255,255,255,.11);color:#fff;border-radius:13px;padding:10px 12px;font-weight:780;white-space:nowrap}
.market-card{border-radius:23px!important;padding:17px!important;box-shadow:0 12px 34px rgba(20,54,43,.075)!important}.market-card-head{display:flex;align-items:flex-start;justify-content:space-between;gap:10px;margin-bottom:13px}.market-card-head .title{margin:0}.market-eyebrow{font-size:10px;color:var(--muted);font-weight:800;text-transform:uppercase;letter-spacing:.08em}.market-prayer-card .prayers{gap:7px}.market-prayer-card .prayer{padding:12px 4px;border-radius:15px}.market-prayer-card .prayer.active{box-shadow:0 6px 18px color-mix(in srgb,var(--green) 18%,transparent)}
.market-duo{display:grid;grid-template-columns:1fr;gap:12px}.market-verse{background:linear-gradient(145deg,var(--card),color-mix(in srgb,var(--green) 5%,var(--card)))}.market-verse .verse-ar{font-size:calc(var(--arabic-size) * .82)}.market-source-pill{display:inline-block;margin-top:10px;border-radius:999px;padding:6px 9px;background:var(--soft);font-size:10px;color:var(--muted);font-weight:800}.market-continue{display:flex;align-items:center;gap:12px}.market-continue-icon{width:43px;height:43px;border-radius:14px;background:var(--soft);display:grid;place-items:center;font-size:20px;flex:0 0 auto}.market-continue #lastRead{flex:1;background:transparent;padding:0}.market-quick-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:8px}.market-quick-grid .quick{min-height:76px;padding:10px 4px;border:1px solid var(--line);box-shadow:none}.market-quick-grid .quick i{font-size:20px}.market-guide-cta{width:100%;border:0;background:linear-gradient(135deg,var(--green2),var(--green));color:#fff;border-radius:18px;padding:15px 16px;text-align:left;display:flex;align-items:center;justify-content:space-between;gap:12px;margin-top:11px}.market-guide-cta b,.market-guide-cta span{display:block}.market-guide-cta span{font-size:11px;opacity:.78;margin-top:3px}.market-guide-cta i{font-style:normal;font-size:25px}
.guide-intro{background:linear-gradient(145deg,color-mix(in srgb,var(--green) 9%,var(--card)),var(--card));border-color:color-mix(in srgb,var(--green) 22%,var(--line))}.guide-badges{display:flex;gap:7px;flex-wrap:wrap;margin-top:12px}.guide-badge{font-size:10px;font-weight:850;color:var(--green);background:var(--soft);border:1px solid var(--line);border-radius:999px;padding:7px 9px}.guide details{background:var(--card);border:1px solid var(--line);border-radius:18px;margin-bottom:10px;overflow:hidden;box-shadow:var(--shadow)}.guide summary{list-style:none;cursor:pointer;padding:16px;font-weight:850;display:flex;align-items:center;justify-content:space-between;gap:12px}.guide summary::-webkit-details-marker{display:none}.guide summary:after{content:'＋';color:var(--green);font-size:19px}.guide details[open] summary:after{content:'−'}.guide-body{padding:0 16px 16px;color:var(--text);font-size:13px;line-height:1.65}.guide-body p{margin:0 0 9px}.guide-body ul{padding-left:19px;margin:8px 0}.guide-body li{margin:6px 0}.guide-source{margin-top:12px;padding-top:10px;border-top:1px dashed var(--line);font-size:11px;color:var(--muted);font-weight:700}.rakat-table{width:100%;border-collapse:separate;border-spacing:0 7px;font-size:11px}.rakat-table td{padding:9px;background:var(--soft)}.rakat-table td:first-child{border-radius:10px 0 0 10px;font-weight:850}.rakat-table td:last-child{border-radius:0 10px 10px 0;text-align:right}.fiqh-note{font-size:11px;color:var(--muted);padding:10px 12px;background:var(--soft);border-radius:12px;margin-top:10px}
@media(min-width:520px){.market-duo{grid-template-columns:1fr 1fr}.market-quick-grid{grid-template-columns:repeat(4,1fr)}}
'''


HOME_SECTION = r'''
<section id="home" class="screen active market-home">
  <div class="market-hero">
    <div class="market-brandline">
      <div class="market-logo"><div class="market-logo-mark">☾</div><div><div class="market-brand">Namaz</div><div id="locText" class="market-location">Ankara, Türkiye</div></div></div>
      <div id="dateText" class="market-date-chip"></div>
    </div>
    <div class="market-next">
      <div class="market-kicker">Sıradaki namaz</div>
      <div class="market-next-row"><div id="nextPrayer">—</div><div id="countdown" class="market-count">Vakitler yükleniyor…</div></div>
    </div>
    <div class="market-hero-actions">
      <button onclick="show('qibla')">🕋 Kıble</button><button onclick="show('quran')">📖 Kur'an</button><button onclick="show('tracker')">✓ Namaz takibi</button><button onclick="show('namazHocasi')">☼ Namaz Hocası</button>
    </div>
  </div>
  <div class="wrap">
    <div class="card market-card market-prayer-card">
      <div class="market-card-head"><div><div class="market-eyebrow">Bugün</div><h2 class="title">Namaz vakitleri</h2></div><span class="sub">Diyanet yöntemi</span></div>
      <div id="prayers" class="prayers"></div><div id="prayerSource" class="sub" style="margin-top:10px"></div>
    </div>
    <div class="market-duo">
      <div class="card market-card market-verse"><div class="market-eyebrow">Günün ayeti</div><div id="verseAr" class="verse-ar">اِنَّ مَعَ الْعُسْرِ يُسْرًاۙ</div><div id="verseTr" class="verse-tr">Şüphesiz güçlükle beraber bir kolaylık vardır.</div><div id="verseSrc" class="market-source-pill">İnşirah 94:6</div></div>
      <div class="card market-card"><div class="market-eyebrow">Kur'an</div><h2 class="title" style="margin-top:5px">Kaldığın yer</h2><div class="market-continue"><div class="market-continue-icon">↗</div><button id="lastRead" class="continue">Henüz okuma kaydı yok</button></div></div>
    </div>
    <div class="card market-card"><div class="market-card-head"><div><div class="market-eyebrow">Kısayollar</div><h2 class="title">Hızlı erişim</h2></div></div><div class="market-quick-grid">
      <button class="quick" onclick="show('quran')"><i>📖</i>Kur'an</button><button class="quick" onclick="show('ramadan')"><i>🌙</i>Ramazan</button><button class="quick" onclick="show('tracker')"><i>✓</i>Takip</button><button class="quick" onclick="show('qibla')"><i>🕋</i>Kıble</button><button class="quick" onclick="show('hatim')"><i>◫</i>Hatim</button><button class="quick" onclick="show('tasbih')"><i>◉</i>Tesbih</button><button class="quick" onclick="show('duas')"><i>🤲</i>Dua</button><button class="quick" onclick="show('knowledge')"><i>☼</i>Bilgiler</button>
    </div><button class="market-guide-cta" onclick="show('namazHocasi')"><div><b>Namaz Hocası</b><span>Abdestten namazın kılınışına adım adım rehber</span></div><i>›</i></button></div>
  </div>
</section>
'''


GUIDE_SECTION = r'''
<section id="namazHocasi" class="screen"><div class="head"><h1>Namaz Hocası</h1><p>Namaza hazırlıktan günlük uygulamaya, kaynaklı ve mezhep farklarını belirten rehber</p></div><div class="wrap guide">
<div class="card guide-intro"><b>Nasıl hazırlandı?</b><p class="sub" style="margin-top:7px;line-height:1.55">Bölüm yapısı, Hayrat Neşriyat'ın <i>Tam Namaz Hocası</i> (Hanefî/Şafiî) eserindeki konu sıralamasından ilham alınarak; açıklamalar Diyanet İşleri Başkanlığı'nın <i>Namaz İlmihali</i> ve açık Kur'an referansları esas alınarak özgün biçimde özetlendi. Kitap metni birebir aktarılmamıştır.</p><div class="guide-badges"><span class="guide-badge">Hanefî temel akış</span><span class="guide-badge">Şafiî farkları belirtilir</span><span class="guide-badge">Kaynak görünür</span></div></div>

<details open><summary>Namazın Yeri ve Önemi</summary><div class="guide-body"><p>Namaz, belirlenmiş vakitlerde yerine getirilen temel ibadetlerden biridir. Uygulamadaki amaç yalnız hareketleri öğretmek değil; vakit, niyet, temizlik, huşû ve devamlılık bilincini birlikte vermektir.</p><ul><li>Namaz vakitli bir yükümlülüktür.</li><li>Namazın insanın davranış dünyasına etkisi vurgulanır.</li><li>Öğrenirken güvenilir kaynak ve mezhep bilgisi birlikte gösterilir.</li></ul><div class="guide-source">Kaynak: Nisâ 4:103; Ankebût 29:45; DİB Namaz İlmihali — namazın dindeki yeri ve önemi.</div></div></details>

<details><summary>Namaza Hazırlık: Gusül, Abdest, Teyemmüm</summary><div class="guide-body"><p><b>Gusül:</b> Gerektiği durumlarda bütün bedenin usulüne uygun yıkanmasıdır. Ayrıntılı farz ve sünnetler mezhebe göre farklılık gösterebilir.</p><p><b>Abdest:</b> Yüzün ve kolların yıkanması, başın mesh edilmesi ve ayakların yıkanması Kur'an'da açıkça zikredilir. Uygulamada adım adım abdest anlatımı bu sırayı korur.</p><p><b>Teyemmüm:</b> Suyun bulunmadığı veya kullanılamadığı geçerli durumlarda temiz toprak/cinsinden yüzeye yönelerek yapılan ruhsattır.</p><div class="fiqh-note"><b>Mezhep notu:</b> Gusül, abdest ve teyemmümün niyet, farz ve sünnet ayrıntılarında Hanefî ve Şafiî hükümler arasında farklar vardır; uygulama bu farkları ayrıca etiketler.</div><div class="guide-source">Kaynak: Mâide 5:6; Nisâ 4:43; DİB Namaz İlmihali — namaza hazırlık bölümü.</div></div></details>

<details><summary>Namaza Hazırlık: Temizlik, Örtünme, Vakit, Niyet, Kıble</summary><div class="guide-body"><ul><li><b>Temizlik:</b> Beden, elbise ve namaz yerinin namaza engel olacak kirlerden arındırılması.</li><li><b>Örtünme:</b> Namazda örtülmesi gereken yerlerin uygun biçimde kapatılması.</li><li><b>Vakit:</b> Her farz namaz kendi vaktinde kılınır.</li><li><b>Niyet:</b> Hangi namazın kılındığının bilinmesi ve kalben yöneliş.</li><li><b>Kıble:</b> Namazda Kâbe yönüne yönelme.</li></ul><div class="guide-source">Kaynak: Bakara 2:144; Nisâ 4:103; DİB Namaz İlmihali — namazın şartları.</div></div></details>

<details><summary>Namazın Kılınışı</summary><div class="guide-body"><p>Genel akış; niyet ve başlangıç tekbiri, kıyam ve kıraat, rükû, secdeler, rekâtların tamamlanması, son oturuş ve selâmdır. Uygulamada hareketler ayrı adımlar halinde gösterilir; kadın/erkek ve mezhep uygulamasındaki ayrıntı farkları gerektiğinde not edilir.</p><div class="fiqh-note">Tâdil-i erkân; rükû, secde ve ara duruşlarda hareketleri aceleye getirmeden yerli yerince yapmayı ifade eder. Hanefî ve Şafiî fıkhında hükmün derecelendirilmesinde terminolojik ayrıntılar farklılaşabilir.</div><div class="guide-source">Kaynak: DİB Namaz İlmihali — namazın kılınışı, farz/vacip/sünnetler ve tâdil-i erkân bölümleri.</div></div></details>

<details><summary>Beş Vakit Namaz</summary><div class="guide-body"><table class="rakat-table"><tr><td>Sabah</td><td>2 sünnet + 2 farz</td></tr><tr><td>Öğle</td><td>4 ilk sünnet + 4 farz + 2 son sünnet</td></tr><tr><td>İkindi</td><td>4 sünnet + 4 farz</td></tr><tr><td>Akşam</td><td>3 farz + 2 sünnet</td></tr><tr><td>Yatsı</td><td>4 sünnet + 4 farz + 2 son sünnet + vitir</td></tr></table><div class="fiqh-note"><b>Hanefî notu:</b> Yatsıdan sonra vitir üç rekât ve vacip kabul edilir. <b>Şafiî notu:</b> Vitir sünnet kabul edilir ve rekât uygulamasında farklı seçenekler vardır. Sünnet namazların hüküm dereceleri de aynı değildir.</div><div class="guide-source">Kaynak: DİB Namaz İlmihali — beş vakit namaz ve vitir bölümleri. Rekât tablosu temel Hanefî uygulamasını özetler.</div></div></details>

<details><summary>Cemaat ve Cuma</summary><div class="guide-body"><p>Cemaatle namaz, birlikte ibadet ve dayanışma bilincini güçlendirir. Cuma namazı için cuma vaktinde alışverişi bırakıp Allah'ı anmaya yönelme emredilir.</p><p>Cemaatte imama uyma, safların düzeni, geç kalan kişinin durumu ve cuma namazının şartları gibi ayrıntılar fıkıh başlıklarıdır; uygulama bunları mezhep etiketiyle gösterir.</p><div class="guide-source">Kaynak: Cum'a 62:9; DİB Namaz İlmihali — cemaatle namaz ve cuma namazı.</div></div></details>

<details><summary>Namaz Çeşitleri</summary><div class="guide-body"><p>Farz namazların yanında vitir, teravih, bayram, cenaze, teheccüd ve diğer nafile namazlar ayrı başlıklarda öğrenilebilir. Her birinin vakti, rekâtı ve hükmü aynı değildir.</p><div class="guide-source">Kaynak: DİB Namaz İlmihali — namaz çeşitleri; ilgili fıkıh bölümleri.</div></div></details>

<details><summary>Tâdil-i Erkân, Huşû ve Vesvese</summary><div class="guide-body"><p>Namazı aceleye getirmemek, rükünleri yerli yerince yapmak ve dikkati mümkün olduğunca ibadette tutmak hedeflenir. İstem dışı düşünceler için namazı sürekli baştan alma alışkanlığına dönüşen vesveseye karşı ölçülü davranmak gerekir.</p><div class="guide-source">Kaynak: Mü'minûn 23:1-2; DİB Namaz İlmihali — huşû, tâdil-i erkân ve ilgili fıkhî açıklamalar.</div></div></details>

<details><summary>Özel Durumlar ve Fıkhî Konular</summary><div class="guide-body"><ul><li>Yolculukta namaz ve ruhsatlar</li><li>Hastalık veya hareket kısıtlılığında namaz</li><li>Sehiv secdesi</li><li>Kaza namazları</li><li>Namazı bozan veya mekruh olan durumlar</li><li>Kerahat vakitleri</li></ul><p>Bu başlıklarda mezhep farklılıkları belirgindir. Uygulama tek görüşü tüm Müslümanlar için mutlak hüküm gibi göstermemelidir.</p><div class="guide-source">Kaynak: Nisâ 4:101; Bakara 2:286; DİB Namaz İlmihali — seferîlik, hastalık/özür, sehiv ve namazla ilgili fıkhî hükümler.</div></div></details>

<details><summary>Namazda Okunan Sûre ve Dualar</summary><div class="guide-body"><p>Başlangıç duası, Fâtiha, Kur'an'dan ilave sûre/ayetler, tahiyyat, salavat ve Rabbena duaları namaz öğretiminde ayrı çalışılabilir. Metni ezberlerken telaffuz sesini dinleme ve Türkçe anlamını görme seçenekleri birlikte sunulur.</p><button class="pill" onclick="show('quran')">Kur'an bölümüne git</button> <button class="pill" onclick="show('duas')">Dua bölümüne git</button><div class="guide-source">Kaynak: DİB Namaz İlmihali — namazda okunan sûre, dua ve tesbihat bölümü.</div></div></details>

<div class="card notice"><b>Telif ve doğruluk notu:</b> Bu rehber <i>Tam Namaz Hocası</i> kitabının metnini kopyalamaz. Kitabın konu iskeletinden yararlanır; içerik özgün özet ve DİB kaynak kontrolüyle sunulur. Fıkhî ihtilaflarda mezhep etiketi gösterilir.</div>
</div></section>
'''


WIDGET_XML = r'''<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/widgetRoot" android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:padding="14dp" android:background="#F7FBF8">

    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="horizontal" android:gravity="center_vertical">
        <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:text="☾  Namaz" android:textColor="#0A5B45" android:textStyle="bold" android:textSize="13sp" />
        <TextView android:id="@+id/widgetHijri" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="—" android:textColor="#64776E" android:textStyle="bold" android:textSize="10sp" />
    </LinearLayout>

    <TextView android:id="@+id/widgetCity" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="2dp" android:text="—" android:textColor="#829087" android:textSize="9sp" />
    <TextView android:id="@+id/widgetPrayer" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="6dp" android:text="Namaz · —" android:textColor="#15251E" android:textStyle="bold" android:textSize="22sp" />
    <TextView android:id="@+id/widgetRemaining" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="1dp" android:text="Vakitleri güncellemek için uygulamayı aç" android:textColor="#0D6B51" android:textStyle="bold" android:textSize="11sp" />

    <LinearLayout android:id="@+id/widgetTimes" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="10dp" android:orientation="horizontal" android:weightSum="6">
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical" android:gravity="center"><TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="İmsak" android:textSize="8sp" android:textColor="#7A8981"/><TextView android:id="@+id/widgetImsak" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="—" android:textStyle="bold" android:textSize="10sp" android:textColor="#24362E"/></LinearLayout>
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical" android:gravity="center"><TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Güneş" android:textSize="8sp" android:textColor="#7A8981"/><TextView android:id="@+id/widgetGunes" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="—" android:textStyle="bold" android:textSize="10sp" android:textColor="#24362E"/></LinearLayout>
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical" android:gravity="center"><TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Öğle" android:textSize="8sp" android:textColor="#7A8981"/><TextView android:id="@+id/widgetOgle" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="—" android:textStyle="bold" android:textSize="10sp" android:textColor="#24362E"/></LinearLayout>
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical" android:gravity="center"><TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="İkindi" android:textSize="8sp" android:textColor="#7A8981"/><TextView android:id="@+id/widgetIkindi" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="—" android:textStyle="bold" android:textSize="10sp" android:textColor="#24362E"/></LinearLayout>
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical" android:gravity="center"><TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Akşam" android:textSize="8sp" android:textColor="#7A8981"/><TextView android:id="@+id/widgetAksam" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="—" android:textStyle="bold" android:textSize="10sp" android:textColor="#24362E"/></LinearLayout>
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical" android:gravity="center"><TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Yatsı" android:textSize="8sp" android:textColor="#7A8981"/><TextView android:id="@+id/widgetYatsi" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="—" android:textStyle="bold" android:textSize="10sp" android:textColor="#24362E"/></LinearLayout>
    </LinearLayout>

    <TextView android:id="@+id/widgetTracker" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="9dp" android:paddingTop="7dp" android:text="Bugün namaz takibi · 0/5" android:textColor="#52665C" android:textStyle="bold" android:textSize="10sp" />
    <TextView android:id="@+id/widgetAyah" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="7dp" android:maxLines="2" android:ellipsize="end" android:textColor="#34483E" android:textSize="10sp" android:lineSpacingExtra="1dp" android:visibility="gone" />
</LinearLayout>
'''

WIDGET_INFO_XML = r'''<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="250dp" android:minHeight="170dp" android:updatePeriodMillis="1800000"
    android:initialLayout="@layout/prayer_widget" android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen" android:description="@string/widget_description" />
'''


def upgrade_html(html: str) -> str:
    if 'market-polish-v1' not in html:
        html = html.replace('</style>', POLISH_CSS + '</style>', 1)
    html = re.sub(r'<section id="home" class="screen active">.*?</section>', HOME_SECTION.strip(), html, count=1, flags=re.S)
    if 'id="namazHocasi"' not in html:
        marker = '<section id="knowledge" class="screen">'
        html = html.replace(marker, GUIDE_SECTION + marker, 1)
    hocasi_button = '<button class="menu-card" onclick="show(\'namazHocasi\')"><i>☼</i><b>Namaz Hocası</b><span>Abdest, namaz ve fıkıh rehberi</span></button>'
    if hocasi_button not in html:
        marker = '<button class="menu-card" onclick="show(\'knowledge\')"><i>☼</i><b>İslami Bilgiler</b><span>Kaynaklı sosyal hayat rehberi</span></button>'
        html = html.replace(marker, hocasi_button + marker, 1)
    return html


def upgrade_js(js: str) -> str:
    old = "AndroidBridge.updateWidget(JSON.stringify({name:next.name,time:next.time,epoch:next.date.getTime()}))"
    payload = "AndroidBridge.updateWidget(JSON.stringify({name:next.name,time:next.time,epoch:next.date.getTime(),imsak:C.cleanTime(t.Fajr),gunes:C.cleanTime(t.Sunrise),ogle:C.cleanTime(t.Dhuhr),ikindi:C.cleanTime(t.Asr),aksam:C.cleanTime(t.Maghrib),yatsi:C.cleanTime(t.Isha),city:cfg.city,hijri:(()=>{const h=prayerData.date&&prayerData.date.hijri;if(!h)return '';const m=['Muharrem','Safer','Rebiülevvel','Rebiülahir','Cemaziyelevvel','Cemaziyelahir','Recep','Şaban','Ramazan','Şevval','Zilkade','Zilhicce'];return `${h.day} ${m[(Number(h.month&&h.month.number)||1)-1]} ${h.year}`})(),tracked:(()=>{const r=readJSON('prayerRecords',{}),d=r[C.isoDateLocal(now)]||{};return C.prayerOrder.filter(p=>!!d[p]).length})()}))"
    js = js.replace(old, payload)
    js = js.replace("saveJSON('prayerRecords',r);renderTracker();renderIbadetSummary()", "saveJSON('prayerRecords',r);renderTracker();renderIbadetSummary();syncNativeSchedule()")
    return js


def apply(root: Path) -> None:
    assets = root / 'app/src/main/assets'
    html_path = assets / 'index.html'
    js_path = assets / 'app.js'
    html_path.write_text(upgrade_html(html_path.read_text(encoding='utf-8')), encoding='utf-8')
    js_path.write_text(upgrade_js(js_path.read_text(encoding='utf-8')), encoding='utf-8')
    layout = root / 'app/src/main/res/layout/prayer_widget.xml'
    info = root / 'app/src/main/res/xml/prayer_widget_info.xml'
    layout.parent.mkdir(parents=True, exist_ok=True)
    info.parent.mkdir(parents=True, exist_ok=True)
    layout.write_text(WIDGET_XML, encoding='utf-8')
    info.write_text(WIDGET_INFO_XML, encoding='utf-8')


def self_test(root: Path) -> None:
    html = (root / 'app/src/main/assets/index.html').read_text(encoding='utf-8')
    js = (root / 'app/src/main/assets/app.js').read_text(encoding='utf-8')
    xml = (root / 'app/src/main/res/layout/prayer_widget.xml').read_text(encoding='utf-8')
    required = ['market-polish-v1', 'id="namazHocasi"', 'DİB Namaz İlmihali', 'Namaza Hazırlık', 'Beş Vakit Namaz']
    for token in required:
        if token not in html:
            raise SystemExit(f'missing html token: {token}')
    for token in ['imsak:', 'gunes:', 'ogle:', 'ikindi:', 'aksam:', 'yatsi:', 'tracked:']:
        if token not in js:
            raise SystemExit(f'missing widget payload token: {token}')
    for token in ['widgetHijri', 'widgetTimes', 'widgetTracker', 'widgetAyah']:
        if token not in xml:
            raise SystemExit(f'missing widget layout token: {token}')


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument('--apply', metavar='ROOT')
    ap.add_argument('--self-test', metavar='ROOT')
    args = ap.parse_args()
    if args.apply:
        apply(Path(args.apply))
    if args.self_test:
        self_test(Path(args.self_test))
    if not args.apply and not args.self_test:
        ap.error('use --apply ROOT and/or --self-test ROOT')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
