#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
from pathlib import Path

ACADEMY_DATA = r'''window.NAMAZ_ACADEMY = {
  categories: [
    {id:"temel", title:"İman ve İslam", subtitle:"İnanç, niyet ve temel kavramlar", icon:"01"},
    {id:"temizlik", title:"Temizlik ve Hazırlık", subtitle:"Abdest, gusül, teyemmüm ve namaza hazırlık", icon:"02"},
    {id:"namaz-yapisi", title:"Namazın Yapısı", subtitle:"Şartlar, rükünler, vacipler ve hareketler", icon:"03"},
    {id:"okunanlar", title:"Namazda Okunanlar", subtitle:"Arapça, okunuş, anlam ve kullanım", icon:"04"},
    {id:"bes-vakit", title:"Beş Vakit", subtitle:"Her namaz rekât rekât uygulama", icon:"05"},
    {id:"cemaat", title:"Cemaat ve Cami", subtitle:"İmam, saf, cuma ve cami adabı", icon:"06"},
    {id:"ozel", title:"Özel Durumlar", subtitle:"Sehiv, seferîlik, hastalık ve kaza", icon:"07"},
    {id:"diger-ibadetler", title:"Diğer İbadetler", subtitle:"Oruç, zekât, sadaka, hac ve umre", icon:"08"},
    {id:"ahlak", title:"Günlük Hayat ve Ahlak", subtitle:"Aile, ticaret, kul hakkı ve sosyal hayat", icon:"09"}
  ],
  lessons: [
    {id:"islam-ve-iman", category:"temel", title:"İslam ve iman nedir?", minutes:8, keywords:["iman","islam","şehadet","inanç"]},
    {id:"imanin-alti-sarti", category:"temel", title:"İmanın 6 şartı", minutes:9, keywords:["altı şart","melek","kitap","peygamber","ahiret","kader"]},
    {id:"islamin-bes-sarti", category:"temel", title:"İslam'ın 5 şartı", minutes:8, keywords:["beş şart","namaz","oruç","zekat","hac"]},
    {id:"32-farz", category:"temel", title:"32 Farz öğretim şeması", minutes:12, keywords:["32 farz","iman","islam","abdest","gusül","teyemmüm","namaz"]},
    {id:"niyet-ve-hukumler", category:"temel", title:"Niyet; farz, vacip, sünnet ve diğer hükümler", minutes:12, keywords:["niyet","farz","vacip","sünnet","müstehap","mubah","mekruh","haram"]},

    {id:"taharet", category:"temizlik", title:"Taharet ve namaza hazırlık", minutes:10, keywords:["temizlik","necaset","elbise","namaz yeri"]},
    {id:"abdest", category:"temizlik", title:"Abdest: baştan sona", minutes:18, keywords:["abdest","abdest farzları","abdest sünnetleri","abdest bozan","mesh"]},
    {id:"gusul", category:"temizlik", title:"Gusül: farzları ve adım adım alınışı", minutes:14, keywords:["gusül","boy abdesti","cenabet"]},
    {id:"teyemmum", category:"temizlik", title:"Teyemmüm: ne zaman ve nasıl?", minutes:11, keywords:["teyemmüm","su yok","toprak"]},
    {id:"mest-mesh", category:"temizlik", title:"Mest üzerine mesh", minutes:8, keywords:["mest","mesh","abdest"]},
    {id:"setr-vakit-kible-niyet", category:"temizlik", title:"Örtünme, vakit, kıble ve niyet", minutes:13, keywords:["setr-i avret","vakit","kıble","niyet"]},

    {id:"namazin-sartlari", category:"namaz-yapisi", title:"Namazın şartları ve iç farzları", minutes:15, keywords:["namaz şartları","namaz farzları","rükün"]},
    {id:"vacip-sunnet-mekruh", category:"namaz-yapisi", title:"Vacipler, sünnetler, mekruhlar ve bozanlar", minutes:15, keywords:["vacip","sünnet","mekruh","namazı bozan"]},
    {id:"namazin-hareketleri", category:"namaz-yapisi", title:"Namazın hareketleri: tekbirden selama", minutes:20, keywords:["kıyam","rükû","secde","oturuş","selam","tadil-i erkan"]},
    {id:"husu", category:"namaz-yapisi", title:"Tâdil-i erkân ve huşû", minutes:9, keywords:["huşu","tadil-i erkan","acele"]},

    {id:"namazda-okunanlar", category:"okunanlar", title:"Namazda okunan temel sûre ve dualar", minutes:25, keywords:["Sübhaneke","Fatiha","İhlas","Kevser","Ettehiyyatü","Salli","Barik","Rabbena"]},
    {id:"kisa-sureler", category:"okunanlar", title:"Başlangıç için kısa sûreler", minutes:20, keywords:["İhlas","Kevser","Asr","Felak","Nas"]},
    {id:"zikirler", category:"okunanlar", title:"Rükû, secde ve geçiş zikirleri", minutes:10, keywords:["rükû tesbihi","secde tesbihi","semiallahu"]},

    {id:"sabah-namazi", category:"bes-vakit", title:"Sabah namazı: 4 rekât adım adım", minutes:22, keywords:["sabah","2 rekât sünnet","2 rekât farz"]},
    {id:"ogle-namazi", category:"bes-vakit", title:"Öğle namazı: 10 rekât adım adım", minutes:28, keywords:["öğle","4 rekât ilk sünnet","4 rekât farz","2 rekât son sünnet"]},
    {id:"ikindi-namazi", category:"bes-vakit", title:"İkindi namazı: 8 rekât adım adım", minutes:26, keywords:["ikindi","4 rekât sünnet","4 rekât farz"]},
    {id:"aksam-namazi", category:"bes-vakit", title:"Akşam namazı: 5 rekât adım adım", minutes:22, keywords:["akşam","3 rekât farz","2 rekât sünnet"]},
    {id:"yatsi-namazi", category:"bes-vakit", title:"Yatsı ve vitir: adım adım", minutes:32, keywords:["yatsı","4 rekât ilk sünnet","4 rekât farz","2 rekât son sünnet","Vitir"]},

    {id:"cemaat", category:"cemaat", title:"Cemaatle namaz ve imama uyma", minutes:14, keywords:["cemaat","imam","muktedî","mesbuk"]},
    {id:"cami-adabi", category:"cemaat", title:"Cami ve saf adabı", minutes:10, keywords:["cami","saf","adab"]},
    {id:"ezan-kamet", category:"cemaat", title:"Ezan ve kamet", minutes:9, keywords:["ezan","kamet"]},
    {id:"cuma", category:"cemaat", title:"Cuma namazına giriş", minutes:14, keywords:["cuma","hutbe"]},
    {id:"bayram-cenaze", category:"cemaat", title:"Bayram ve cenaze namazına giriş", minutes:12, keywords:["bayram","cenaze"]},

    {id:"sehiv-secdesi", category:"ozel", title:"Sehiv secdesi", minutes:12, keywords:["sehiv","unutma","yanılma"]},
    {id:"kaza", category:"ozel", title:"Kaza namazı ve kaçırılan namaz", minutes:11, keywords:["kaza","kaçırılan namaz"]},
    {id:"seferilik", category:"ozel", title:"Seferîlik ve yolculukta namaz", minutes:15, keywords:["seferi","yolculuk","kasr"]},
    {id:"hastalikta-namaz", category:"ozel", title:"Hastalıkta ve oturarak namaz", minutes:14, keywords:["hasta","oturarak","ima"]},
    {id:"kerahat-vakitleri", category:"ozel", title:"Kerahat vakitleri", minutes:10, keywords:["kerahat","namaz kılınmayan vakit"]},
    {id:"kadinlara-ozel", category:"ozel", title:"Kadınların ibadetine dair temel özel durumlar", minutes:12, keywords:["hayız","nifas","kadın"]},
    {id:"vesvese", category:"ozel", title:"Vesvese, şüphe ve unutma", minutes:10, keywords:["vesvese","şüphe","unutma"]},

    {id:"oruc-temel", category:"diger-ibadetler", title:"Oruç ve Ramazan: temel bilgiler", minutes:13, keywords:["oruç","ramazan","sahur","iftar"]},
    {id:"zekat-temel", category:"diger-ibadetler", title:"Zekât ve sadaka: temel çerçeve", minutes:12, keywords:["zekat","sadaka","nisap"]},
    {id:"hac-umre-temel", category:"diger-ibadetler", title:"Hac ve umre: temel kavramlar", minutes:13, keywords:["hac","umre","ihram","tavaf","say"]},

    {id:"aile", category:"ahlak", title:"Anne-baba, eşler ve aile sorumluluğu", minutes:13, keywords:["aile","anne","baba","eş","çocuk"]},
    {id:"komsuluk", category:"ahlak", title:"Komşuluk, akrabalık ve arkadaşlık", minutes:11, keywords:["komşu","akraba","arkadaş"]},
    {id:"ticaret", category:"ahlak", title:"İş ahlakı, ticaret ve dürüstlük", minutes:12, keywords:["iş","ticaret","dürüstlük","ölçü"]},
    {id:"borc-kul-hakki", category:"ahlak", title:"Borç, emanet ve kul hakkı", minutes:13, keywords:["borç","emanet","kul hakkı"]},
    {id:"giybet", category:"ahlak", title:"Gıybet, iftira ve söz sorumluluğu", minutes:11, keywords:["gıybet","iftira","dedikodu"]},
    {id:"ofke", category:"ahlak", title:"Öfke, tartışma ve affetme", minutes:10, keywords:["öfke","tartışma","affetme"]},
    {id:"israf-adalet", category:"ahlak", title:"Adalet, israf ve sorumluluk", minutes:10, keywords:["adalet","israf","sorumluluk"]},
    {id:"mahremiyet-sosyal-medya", category:"ahlak", title:"Mahremiyet ve sosyal medya adabı", minutes:12, keywords:["mahremiyet","sosyal medya","haber doğrulama"]},
    {id:"ziyaret-taziye-yolculuk", category:"ahlak", title:"Hasta ziyareti, taziye ve yolculuk adabı", minutes:10, keywords:["hasta ziyareti","taziye","yolculuk"]}
  ],
  beginnerPath:["islam-ve-iman","32-farz","abdest","gusul","teyemmum","namazin-sartlari","namazin-hareketleri","namazda-okunanlar","sabah-namazi","ogle-namazi","ikindi-namazi","aksam-namazi","yatsi-namazi","cemaat","sehiv-secdesi"]
};'''

LESSONS_HANAFI = r'''window.NAMAZ_HANAFI_LESSONS = {
"islam-ve-iman": {summary:"İslam'ın teslimiyet ve kulluk yönünü; imanın kalp, söz ve davranışla ilişkisini temel seviyede öğrenirsin.", sections:[
 {title:"İslam ne demektir?", body:"İslam, Allah'ın birliğini kabul ederek O'na kulluk etmek ve Hz. Muhammed'in tebliğ ettiği vahyin rehberliğinde yaşamaya yönelmektir. İbadet yalnız namazdan ibaret değildir; doğruluk, adalet, merhamet ve kul hakkına dikkat etmek de dinî hayatın parçasıdır."},
 {title:"İman ne demektir?", body:"İman, Allah'a ve O'nun bildirdiği iman esaslarına inanmayı ifade eder. Dini yeni öğrenen biri için ilk hedef ayrıntılı tartışmalar değil; Allah'ın birliğini, peygamberliği, vahyi, ahireti ve sorumluluk bilincini sağlam bir çerçevede kavramaktır."},
 {title:"İbadet niçin yapılır?", body:"İbadetin amacı sadece bir görev listesini tamamlamak değildir. Namaz, kulun Rabbiyle bağını düzenli biçimde canlı tutar; davranışlarını da doğruluk ve sorumluluk yönünde eğitmeyi hedefler."}],
 hanafi:"Bu ders mezhepler arası ihtilaf konusu değildir; fıkhî ayrıntılar sonraki derslerde Hanefî anlatım merkezli verilir.", mistakes:["Dini yalnız yasaklar listesi sanmak.","İbadeti ahlaktan tamamen ayırmak."], sources:["Kur'an, Bakara 2:177","Kur'an, Nahl 16:90","DİB İlmihal, İman ve İbadet bölümleri"], quiz:[{q:"İbadetin amaçlarından biri nedir?",a:["Sadece alışkanlık oluşturmak","Allah ile bağı ve sorumluluk bilincini güçlendirmek","Yalnız toplum tarafından görülmek"],correct:1}]},

"imanin-alti-sarti": {summary:"İmanın altı esasını anlamlarıyla öğrenirsin.", sections:[
 {title:"Altı esas", body:"Allah'a, meleklerine, kitaplarına, peygamberlerine, ahiret gününe, kadere; hayır ve şerrin Allah'ın bilgisi ve yaratması içinde olduğuna iman etmek klasik öğretimde imanın altı esası olarak özetlenir."},
 {title:"Sorumluluk ve kader", body:"Kader inancı insanın iradesini ve sorumluluğunu ortadan kaldıran bir mazeret değildir. İnsan tercihleri için sorumludur; Allah'ın ilmi ise zamanla sınırlı değildir."}], hanafi:"İman esasları ortak temel öğretidir.", mistakes:["Kaderi, insanın hiçbir seçimi yokmuş gibi anlamak."], sources:["Kur'an, Bakara 2:285","Kur'an, Nisâ 4:136","DİB İlmihal, İman Esasları"], quiz:[{q:"İman esaslarından biri hangisidir?",a:["Ahirete iman","Yalnız kendi gücüne inanmak","Hiç sorumluluk taşımamak"],correct:0}]},

"islamin-bes-sarti": {summary:"Kelime-i şehadet, namaz, oruç, zekât ve hacdan oluşan beş temel ibadet çerçevesini öğrenirsin.", sections:[
 {title:"Beş şart", body:"Kelime-i şehadet getirmek, namaz kılmak, Ramazan orucunu tutmak, şartları oluştuğunda zekât vermek ve gücü yeten için hacca gitmek İslam'ın beş şartı olarak öğretilir."},
 {title:"Şartların yükümlülüğü", body:"Zekât ve hac gibi ibadetlerde mali güç ve diğer şartlar önemlidir. Uygulama, kişiye özel fetva vermez; özel durumlarda güvenilir ilmihal veya yetkin din görevlisine başvurmak gerekir."}], hanafi:"Beş şart ortak temel çerçevedir; ayrıntılı fıkıh hükümleri mezheplere göre değişebilir.", mistakes:["Her ibadetin herkese her durumda aynı şartlarla farz olduğunu sanmak."], sources:["Buhârî, Îmân 1","Müslim, Îmân 19-22","DİB İlmihal, İbadetler"], quiz:[{q:"Mali şartlara bağlı ibadetlerden biri hangisidir?",a:["Zekât","Kelime-i şehadet","Günlük selamlaşma"],correct:0}]},

"32-farz": {summary:"Geleneksel 32 Farz öğretim şemasını, ezber listesinden ziyade hangi başlıklardan oluştuğunu anlayarak öğrenirsin.", sections:[
 {title:"İmanın 6 esası", body:"Allah'a, meleklerine, kitaplarına, peygamberlerine, ahiret gününe ve kadere iman."},
 {title:"İslam'ın 5 şartı", body:"Kelime-i şehadet, namaz, oruç, zekât ve hac."},
 {title:"Abdestin 4 farzı", body:"Yüzü yıkamak; dirseklerle birlikte kolları yıkamak; başın bir kısmını mesh etmek; topuklarla birlikte ayakları yıkamak."},
 {title:"Guslün 3 farzı", body:"Hanefî öğretimde ağza su vermek, burna su vermek ve bütün bedeni kuru yer kalmayacak şekilde yıkamak."},
 {title:"Teyemmümün 2 farzı", body:"Niyet etmek ve temiz toprak/cinsinden bir yüzeye elleri vurup yüz ile kolları usulüne uygun mesh etmek şeklinde öğretilir."},
 {title:"Namazın 12 farzı", body:"Altı dış şart: hadesten taharet, necasetten taharet, setr-i avret, istikbal-i kıble, vakit, niyet. Altı iç rükün: iftitah tekbiri, kıyam, kıraat, rükû, secde ve son oturuş."}],
 hanafi:"32 Farz, özellikle Türkiye'deki Hanefî temel din eğitimi geleneğinde kullanılan bir öğretim şemasıdır. Tek başına bütün ilmihalin yerine geçmez.", mistakes:["32 Farz listesini dinin bütün hükümleri sanmak.","Ezberleyip ne anlama geldiğini öğrenmemek."], sources:["DİB İlmihal, Temizlik ve Namaz bölümleri","DİB Namaz İlmihali, Namazın farzları"], quiz:[{q:"Namazın 12 farzı nasıl gruplanır?",a:["6 dış şart + 6 iç rükün","12 kısa sure","4 abdest + 8 gusül"],correct:0}]},

"niyet-ve-hukumler": {summary:"Niyet ile farz, vacip, sünnet, müstehap, mubah, mekruh ve haram terimlerinin ne anlama geldiğini öğrenirsin.", sections:[
 {title:"Niyet", body:"Niyet, yapılacak ibadeti bilinçli olarak kastetmektir. Kalbin yönelişi esastır; niyetin sırf dilde söylenmesi niyetin özü değildir."},
 {title:"Farz ve vacip", body:"Hanefî usulünde farz kesin delille sabit yükümlülüğü, vacip ise bağlayıcılığı güçlü olmakla birlikte delil yönünden farzdan farklı kategoriyi ifade eder. Diğer mezheplerde farz-vacip ayrımı aynı teknik anlamda kullanılmayabilir."},
 {title:"Sünnet, müstehap, mubah, mekruh, haram", body:"Sünnet, Hz. Peygamber'in örnekliğiyle sabit uygulamaları; müstehap yapılması güzel görülen fiilleri; mubah dinen serbest alanı; mekruh kaçınılması istenen fiilleri; haram ise kesin biçimde yasaklanan fiilleri ifade eder. Ayrıntılı derecelendirmeler fıkıh kitaplarında açıklanır."}],
 hanafi:"Hanefî mezhebinde farz-vacip teknik ayrımı belirgindir.", mistakes:["Her sünneti farz gibi, her mekruhu haram gibi ifade etmek."], sources:["DİB İlmihal, Fıkıh ve İbadet Terminolojisi","Buhârî, Bed'ü'l-vahy 1"], quiz:[{q:"Niyetin özü nedir?",a:["Kalbin bilinçli yönelişi","Mutlaka yüksek sesle söylemek","Başkalarına duyurmak"],correct:0}]},

"taharet": {summary:"Namazdan önce beden, elbise, namaz yeri ve hükmî temizlik kavramlarını öğrenirsin.", sections:[
 {title:"İki yönlü temizlik", body:"Namaza hazırlıkta hadesten taharet; abdestsizlik/cünüplük gibi hükmî kirliliği abdest veya gusülle gidermeyi, necasetten taharet ise beden, elbise ve namaz yerindeki dinen pis sayılan maddeleri temizlemeyi ifade eder."},
 {title:"Temizlikte ölçü", body:"Şüphe ve vesvese ile hayatı zorlaştırmak yerine kesin bilgiye göre hareket etmek esastır. Her leke otomatik olarak necaset sayılmaz."}], hanafi:"Necaset miktarı ve ayrıntıları fıkıh mezheplerinde teknik farklılıklar gösterebilir; bu ders başlangıç çerçevesidir.", mistakes:["Her şüpheli şeyi necis kabul etmek.","Temizlikte vesveseye kapılıp ibadeti zorlaştırmak."], sources:["Kur'an, Mâide 5:6","DİB İlmihal, Temizlik bölümü"], quiz:[{q:"Necasetten taharet neyi anlatır?",a:["Beden/elbise/namaz yerindeki maddî pisliği gidermeyi","Sadece niyet etmeyi","Kıbleyi bulmayı"],correct:0}]},

"abdest": {summary:"Abdestin Hanefî mezhebine göre dört farzını, sünnetlerini, adım adım alınışını, bozan durumları ve sık hataları öğrenirsin.", sections:[
 {title:"Abdestin 4 farzı", body:"1) Yüzü bir defa yıkamak. 2) Elleri ve kolları dirseklerle birlikte bir defa yıkamak. 3) Başın en az dörtte birini mesh etmek. 4) Ayakları topuklarla birlikte bir defa yıkamak. Bunlar Hanefî mezhebinde abdestin geçerliliği için temel farzlardır."},
 {title:"Adım 1 — niyet ve başlangıç", body:"Allah rızası için abdest almaya kalben niyet et. Besmele ile başlamak ve elleri bileklere kadar yıkamak sünnet/adab kapsamındadır. Parmak aralarını temizle."},
 {title:"Adım 2 — ağız ve burun", body:"Ağza ve buruna üçer defa su vermek Hanefî abdestinde sünnettir. Oruçluyken buruna su çekerken aşırıya kaçma."},
 {title:"Adım 3 — yüz", body:"Alın saç bitiminden çene altına ve iki kulak yumuşağı arasına kadar yüz bölgesini su ulaşacak biçimde yıka."},
 {title:"Adım 4 — kollar", body:"Önce sağ, sonra sol kolu dirsekler dahil olmak üzere yıka. Saat, dar bileklik veya suyun deriye ulaşmasını kesin engelleyen bir tabaka varsa altına su ulaşmasına dikkat et."},
 {title:"Adım 5 — başı mesh", body:"Islak elle başın en az dörtte birini mesh etmek Hanefî mezhebinde farzdır. Başın tamamını mesh etmek sünnet uygulamadır."},
 {title:"Adım 6 — kulaklar ve ayaklar", body:"Kulakları mesh ettikten sonra ayakları topuklarla birlikte, parmak aralarına su ulaştırarak yıka. Suyun ulaşmadığı kuru bir bölge bırakma."},
 {title:"Abdesti bozan temel durumlar", body:"Ön ve arka yoldan çıkan şeyler, ağız dolusu kusma, bayılma/aklın gitmesi ve Hanefî fıkhında kan/irin gibi akıcı necasetin çıktığı yerden çevreye yayılması başlıca örneklerdendir. Uyku hükmü kişinin uyuma biçimine göre ayrıntılanır."},
 {title:"Sık yapılan hatalar", body:"Dirsek veya topuğu kuru bırakmak; su geçirmeyen maddeyi fark etmemek; sadece hızlıca ıslatıp yıkama gerçekleşmeden geçmek; abdestin sünnetleriyle farzlarını birbirine karıştırmak."}],
 hanafi:"Hanefî mezhebinde niyet abdestin sünnetidir; dört farz Mâide 5:6 ayetindeki yıkama/mesh emirleri çerçevesinde sayılır. Başın en az dörtte biri mesh edilir.", mistakes:["Dirsekleri ve topukları yıkama sınırının dışında sanmak.","Niyeti Hanefî abdestinde farz diye öğretmek.","Su ulaşmasını engelleyen kalın tabakayı görmezden gelmek."], sources:["Kur'an, Mâide 5:6","DİB İlmihal, Temizlik — Abdest","Din İşleri Yüksek Kurulu, Abdestin farzları ve mezhep farklılıkları"], quiz:[{q:"Hanefî mezhebinde abdestin farzı kaçtır?",a:["3","4","6"],correct:1},{q:"Dirseklerin hükmü nedir?",a:["Kollarla birlikte yıkanır","Hiç yıkanmaz","Sadece mesh edilir"],correct:0}]},

"gusul": {summary:"Guslü gerektiren temel durumları, Hanefî mezhebinde guslün üç farzını ve baştan sona alınışını öğrenirsin.", sections:[
 {title:"Gusül nedir?", body:"Gusül, belirli hükmî kirlilik hâllerinden sonra bütün bedeni usulüne uygun yıkayarak ibadet için gerekli temizliği sağlamaktır."},
 {title:"Hanefî mezhebinde 3 farz", body:"1) Ağzın içini suyla yıkamak. 2) Burnun içine su vermek. 3) Bütün bedeni, kuru yer kalmayacak şekilde yıkamak."},
 {title:"Adım adım gusül", body:"Kalben niyet et; besmele çek; elleri ve varsa maddî pisliği temizle. Namaz abdesti gibi abdest al. Ağza ve buruna iyice su ver. Sonra baştan başlayarak bütün bedeni, saç dipleri ve kıvrımlar dahil su ulaşacak şekilde yıka. Ayakları başta yıkamadıysan sonda yıka."},
 {title:"Dikkat edilecek noktalar", body:"Su geçirmeyen kalın tabaka varsa giderilir. Küpe deliği gibi suyun normal şekilde ulaşması gereken dış bölgelerde suyun ulaşmasına dikkat edilir. Sağlık açısından su kullanımı ciddi zarar verecekse kişisel durum için güvenilir dinî ve tıbbî danışmanlık alınmalıdır."}],
 hanafi:"Hanefî mezhebinde ağız ve burnun yıkanması guslün farzlarındandır.", mistakes:["Sadece duş almayı, ağız-burun ve bütün bedene su ulaştırma şartlarını düşünmeden otomatik olarak gusül saymak.","Saç dipleri veya beden kıvrımlarında kuru yer bırakmak."], sources:["Kur'an, Mâide 5:6","DİB İlmihal, Temizlik — Gusül","Din İşleri Yüksek Kurulu, Guslün farzları"], quiz:[{q:"Hanefî guslünde hangisi farzdır?",a:["Ağza su vermek","Özel bir havlu kullanmak","Mutlaka uzun dua okumak"],correct:0}]},

"teyemmum": {summary:"Su bulunmadığında veya suyu kullanmanın mümkün olmadığı geçerli durumlarda teyemmümün ne anlama geldiğini ve nasıl yapıldığını öğrenirsin.", sections:[
 {title:"Ne zaman başvurulur?", body:"Su bulunmaması veya suyu kullanmanın sağlık bakımından ciddi zarar doğurması gibi fıkhen geçerli mazeretlerde teyemmüm, abdest veya gusül yerine geçici temizlik yolu olur. Su varken sırf kolaylık için tercih edilmez."},
 {title:"Hanefî öğretimde iki temel farz", body:"Niyet etmek ve temiz toprak/toprak cinsinden bir yüzeye elleri vurup yüz ile kolları usulüne uygun mesh etmektir."},
 {title:"Adım adım", body:"Kalben abdest/gusül yerine teyemmüme niyet et. Elleri temiz toprak veya toprak cinsinden yüzeye hafifçe vur; fazlalığı silkele; yüzün tamamını mesh et. Yeniden vurup sağ ve sol kolu dirseklere kadar mesh et."},
 {title:"Ne zaman sona erer?", body:"Teyemmümü bozan normal abdest bozucu durumlara ek olarak, suya ulaşma veya suyu kullanma engelinin kalkması gibi hâller teyemmüm hükmünü sona erdirir."}],
 hanafi:"Hanefî uygulamada yüz ve kolların mesh edilmesi esas alınır; ayrıntılar ilmihalde açıklanır.", mistakes:["Su varken ve kullanmaya engel yokken kolaylık için teyemmüm yapmak.","Niyetsiz yapıp sadece tozlanmayı teyemmüm sanmak."], sources:["Kur'an, Nisâ 4:43","Kur'an, Mâide 5:6","DİB İlmihal, Temizlik — Teyemmüm"], quiz:[{q:"Teyemmüm hangi durumda meşru olabilir?",a:["Su bulunmuyor veya kullanımı ciddi zarar verecekse","Her zaman daha hızlı olduğu için","Sadece gece"],correct:0}]},

"mest-mesh": {summary:"Abdestli giyilen uygun mest üzerine mesh ruhsatının temel çerçevesini öğrenirsin.", sections:[
 {title:"Temel fikir", body:"Fıkıhta belirli şartları taşıyan mestler abdestli olarak giyildiğinde, sonraki abdestlerde ayakları yeniden yıkamak yerine mestlerin üst kısmını mesh etme ruhsatı bulunur."},
 {title:"Şartları neden önemli?", body:"Mestin niteliği, abdestli giyilmesi, süre ve mestin çıkarılması gibi ayrıntılar vardır. Günümüzde çorap üzerine mesh tartışmaları ürünün niteliğine göre değişebildiği için tereddütte ayrıntılı DİB/ilmihal açıklamasına bakılmalıdır."}], hanafi:"Mukim ve yolcu için mesh süresi gibi teknik hükümler Hanefî ilmihalinde ayrıca düzenlenir.", mistakes:["Her ince çorabı otomatik olarak klasik mest hükmünde saymak."], sources:["DİB İlmihal, Temizlik — Mest üzerine mesh"], quiz:[{q:"Mest ruhsatında önemli ön şartlardan biri nedir?",a:["Mesti abdestli giymek","Mestin renginin siyah olması","Yalnız kışın kullanmak"],correct:0}]},

"setr-vakit-kible-niyet": {summary:"Namazın dış şartlarından örtünme, vakit, kıble ve niyeti öğrenirsin.", sections:[
 {title:"Setr-i avret", body:"Namazda dinen örtülmesi gereken yerlerin örtülü olmasıdır. Ayrıntılı sınırlar kadın/erkek ve mezheplere göre fıkıh kitaplarında açıklanır; kıyafet temiz, ibadete uygun ve beden hareketlerini engellemeyecek şekilde olmalıdır."},
 {title:"Vakit", body:"Her farz namaz kendi vakti içinde kılınır. Vaktin girmesi namazın şartıdır; uygulamadaki vakit ekranı bu nedenle önemlidir."},
 {title:"Kıble", body:"Namazda Kâbe yönüne yönelmek şarttır. Kıbleyi bilmeyen kişi imkânı ölçüsünde araştırır; uygulamadaki sensörlü kıble yardımcı araçtır ve manyetik sapma ihtimali nedeniyle gerektiğinde çevresel işaretlerle kontrol edilmelidir."},
 {title:"Niyet", body:"Hangi namazı kıldığını kalben belirlemek niyettir. Kalbin kastı esastır; sözle söylemek niyetin özü değildir."}], hanafi:"Farz/vacip namazlarda hangi namaza niyet edildiğinin belirlenmesi önemlidir.", mistakes:["Namaz vaktini kontrol etmeden kılmak.","Kıble uygulamasını metal etkisi altında mutlak doğru sanmak."], sources:["Kur'an, Bakara 2:144","Kur'an, Nisâ 4:103","DİB Namaz İlmihali, Namazın şartları"], quiz:[{q:"Niyetin temel yeri neresidir?",a:["Kalp/kasıt","Sadece ses","Telefon ekranı"],correct:0}]},

"namazin-sartlari": {summary:"Namazın dışındaki altı şartı ve içindeki altı temel farz/rüknü sistemli biçimde öğrenirsin.", sections:[
 {title:"Dış şartlar", body:"Hadesten taharet, necasetten taharet, setr-i avret, istikbal-i kıble, vakit ve niyet."},
 {title:"İç farzlar/rükünler", body:"İftitah tekbiri, kıyam, kıraat, rükû, secde ve son oturuş (ka'de-i ahîre) temel rükünler olarak öğretilir."},
 {title:"Neden ayrım var?", body:"Dış şartlar namaz başlamadan önce de bulunması gereken koşullardır; iç rükünler ise namazın yapısını oluşturan temel fiillerdir."}], hanafi:"Bu sınıflandırma Hanefî temel ilmihal anlatımıdır.", mistakes:["Vacip ve sünnetleri farzlarla karıştırmak.","Son oturuşu sadece sünnet zannetmek."], sources:["DİB Namaz İlmihali, Namazın farzları","DİB İlmihal, Namaz bölümü"], quiz:[{q:"Hangisi namazın dış şartıdır?",a:["Vakit","Rükû","Secde"],correct:0}]},

"vacip-sunnet-mekruh": {summary:"Hanefî mezhebinde namazın farz dışındaki bağlayıcı ve tamamlayıcı hükümlerini ana çerçevede öğrenirsin.", sections:[
 {title:"Vacip", body:"Hanefî mezhebinde Fâtiha'yı okumak, farz namazların ilk iki rekâtında zamm-ı sûre/ayet okumak ve ilk oturuş gibi bazı fiiller vacip sayılır. Bir vacibin unutularak terkinde sehiv secdesi gündeme gelebilir."},
 {title:"Sünnet", body:"Ellerin bağlanması, tekbirlerde ellerin kaldırılması gibi birçok uygulama sünnet kapsamında ele alınır. Sünnetler namazın Hz. Peygamber'den öğrenilen güzel icrasını tamamlar."},
 {title:"Mekruh ve bozan fiil farkı", body:"Mekruh bir davranış namazın sevabını/uygunluğunu zedelerken her mekruh namazı otomatik olarak bozmaz. Konuşmak, yemek-içmek gibi namazla bağdaşmayan fiiller ise namazı bozabilir."}], hanafi:"Farz-vacip ayrımı Hanefî teknik terminolojisinde önemlidir.", mistakes:["Her mekruhu namazı bozan fiil sanmak.","Vacip unutulduğunda her durumda namazı baştan almak gerektiğini düşünmek."], sources:["DİB Namaz İlmihali, Namazın vacipleri ve sünnetleri","DİB İlmihal, Namazı bozan ve mekruh olan şeyler"], quiz:[{q:"Hanefî mezhebinde unutulan vacip için hangi konu gündeme gelebilir?",a:["Sehiv secdesi","Hac","Zekât"],correct:0}]},

"namazin-hareketleri": {summary:"İftitah tekbirinden selama kadar temel hareketleri, her hareketin sakin ve düzgün yapılması gerektiğini öğrenirsin.", sections:[
 {title:"1 — İftitah tekbiri ve kıyam", body:"Kıbleye dön, niyet et ve 'Allahu ekber' diyerek namaza başla. Ayakta durabilen kişi farz namazda kıyam hâlinde durur. Eller bağlandıktan sonra okunacaklara geçilir.", posture:"qiyam"},
 {title:"2 — Kıraat", body:"Kıyamda başlangıç duaları ve kıraat yapılır. Farz namazın ilk iki rekâtında Fâtiha ve ardından sûre/ayet okumak Hanefî uygulamanın temel düzenidir."},
 {title:"3 — Rükû", body:"Tekbirle rükûya eğil. Beden imkânın ölçüsünde dengeli ve sakin olsun; rükû tesbihini oku. Acele edip doğrulmadan secdeye geçme.", posture:"ruku"},
 {title:"4 — Rükûdan doğrulma", body:"Rükûdan kalkarken 'Semiallahu limen hamideh', doğrulunca 'Rabbenâ lekel hamd' denir. Doğrulma hâlinin yerleşmesine dikkat edilir."},
 {title:"5 — Secde", body:"Tekbirle secdeye in. Alın ve burun, eller, dizler ve ayak parmakları secde düzeninde yere yerleşir. Tesbih oku; sonra oturup ikinci secdeyi yap.", posture:"sujud"},
 {title:"6 — Oturuş", body:"İkinci rekât sonunda ilk oturuş; namazın son rekâtında son oturuş yapılır. Ettehiyyâtü okunur; son oturuşta salavat ve dua eklenir.", posture:"sitting"},
 {title:"7 — Selam", body:"Namaz sonunda önce sağa sonra sola selam verilir. Bu, namazdan çıkışın normal uygulamasıdır."}],
 hanafi:"Kavme (rükûdan doğrulma) ve celse (iki secde arası oturuş) Hanefî mezhebinde kuvvetli görüşe göre vaciptir; tâdil-i erkânı korumak gerekir.", mistakes:["Rükûdan tam doğrulmadan secdeye inmek.","İki secde arasında hiç oturmadan ikinci secdeye geçmek.","Hareketleri yarışır gibi yapmak."], sources:["Kur'an, Hac 22:77","DİB Namaz İlmihali, Namazın kılınışı","Din İşleri Yüksek Kurulu, Kavme ve celsenin hükmü"], quiz:[{q:"Rükûdan sonra ne yapılır?",a:["Doğrulup kısa bir sükûnetle durulur","Doğrudan namaz bitirilir","Kıble değiştirilir"],correct:0}]},

"husu": {summary:"Namazı aceleye getirmeden, hareketleri yerli yerinde ve kalben dikkatle yapma alışkanlığı kazanırsın.", sections:[
 {title:"Tâdil-i erkân", body:"Rükû, doğrulma, secde ve oturuş gibi rükünlerde bedenin yerleşmesine fırsat vermek, namazı aşırı aceleyle geçiştirmemektir."},
 {title:"Huşû", body:"Huşû, namazda Allah'ın huzurunda olduğunun bilincini taşımak, anlamını bildiğin okumalarla dikkati toplamaya çalışmaktır. Zihnin dağılması insanîdir; fark ettiğinde sakin biçimde namaza dön."}], hanafi:"Tâdil-i erkân Hanefî mezhebinde vacip kabul edilen önemli bir namaz disiplinidir.", mistakes:["Zihne düşünce geldi diye namazın kesin bozulduğunu sanmak.","Acele ederek hareketlerin hakkını vermemek."], sources:["Kur'an, Mü'minûn 23:1-2","DİB Namaz İlmihali, Tâdil-i erkân ve huşû"], quiz:[{q:"Huşû için en gerçekçi yaklaşım nedir?",a:["Dikkat dağılınca sakince namaza dönmek","Namazı bırakmak","Düşünce gelmesini günah sayıp paniğe kapılmak"],correct:0}]},

"namazda-okunanlar": {summary:"Namazın temel dua ve sûrelerini Arapça metin, okunuş, anlam ve nerede okunduğu bilgisiyle çalışırsın.", sections:[
 {title:"Nasıl çalışmalı?", body:"Önce kısa parçalar hâlinde doğru telaffuza yaklaş; ardından anlamını öğren. Arapça okuma konusunda güvenilir bir hocadan yüz yüze/işitsel düzeltme almak en sağlıklı yoldur."}],
 arabicBlocks:[
  {name:"Sübhaneke", when:"İftitah tekbirinden sonra başlangıçta", arabic:"سُبْحَانَكَ اللَّهُمَّ وَبِحَمْدِكَ وَتَبَارَكَ اسْمُكَ وَتَعَالَى جَدُّكَ وَلَا إِلٰهَ غَيْرُكَ", pronunciation:"Sübhâneke allâhümme ve bi hamdik ve tebârekesmük ve teâlâ ceddük ve lâ ilâhe ğayrük.", meaning:"Allah'ım! Seni eksikliklerden uzak tutar, sana hamd ederim. Adın mübarektir, şanın yücedir; senden başka ilah yoktur."},
  {name:"Fâtiha", when:"Kıyamda kıraat", arabic:"بِسْمِ اللَّهِ الرَّحْمٰنِ الرَّحِيمِ ۝ الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ ۝ الرَّحْمٰنِ الرَّحِيمِ ۝ مَالِكِ يَوْمِ الدِّينِ ۝ إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ ۝ اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ ۝ صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ", pronunciation:"Bismillâhirrahmânirrahîm. Elhamdü lillâhi rabbil âlemîn. Errahmânirrahîm. Mâliki yevmiddîn. İyyâke na'büdü ve iyyâke nestaîn. İhdinessırâtal müstakîm. Sırâtallezîne en'amte aleyhim ğayril mağdûbi aleyhim ve leddâllîn.", meaning:"Rahmân ve Rahîm olan Allah'ın adıyla. Hamd âlemlerin Rabbi Allah'a mahsustur... Bizi dosdoğru yola ilet."},
  {name:"Ettehiyyâtü", when:"İlk ve son oturuşta", arabic:"التَّحِيَّاتُ لِلَّهِ وَالصَّلَوَاتُ وَالطَّيِّبَاتُ السَّلَامُ عَلَيْكَ أَيُّهَا النَّبِيُّ وَرَحْمَةُ اللَّهِ وَبَرَكَاتُهُ السَّلَامُ عَلَيْنَا وَعَلَى عِبَادِ اللَّهِ الصَّالِحِينَ أَشْهَدُ أَنْ لَا إِلٰهَ إِلَّا اللَّهُ وَأَشْهَدُ أَنَّ مُحَمَّدًا عَبْدُهُ وَرَسُولُهُ", pronunciation:"Ettehiyyâtü lillâhi vessalevâtü vettayyibât... Eşhedü en lâ ilâhe illallah ve eşhedü enne Muhammeden abdühû ve rasûlüh.", meaning:"Bütün hürmetler, ibadetler ve güzel sözler Allah'a mahsustur... Allah'tan başka ilah olmadığına ve Muhammed'in O'nun kulu ve elçisi olduğuna şahitlik ederim."},
  {name:"Allahümme Salli", when:"Son oturuşta Ettehiyyâtü'den sonra", arabic:"اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ كَمَا صَلَّيْتَ عَلَى إِبْرَاهِيمَ وَعَلَى آلِ إِبْرَاهِيمَ إِنَّكَ حَمِيدٌ مَجِيدٌ", pronunciation:"Allâhümme salli alâ Muhammedin ve alâ âli Muhammed, kemâ salleyte alâ İbrâhîme ve alâ âli İbrâhîm, inneke hamîdün mecîd.", meaning:"Allah'ım! İbrahim'e ve ailesine rahmet ettiğin gibi Muhammed'e ve ailesine de rahmet eyle. Şüphesiz sen övülmeye layık ve yücesin."},
  {name:"Allahümme Bârik", when:"Son oturuşta salavatta", arabic:"اللَّهُمَّ بَارِكْ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ كَمَا بَارَكْتَ عَلَى إِبْرَاهِيمَ وَعَلَى آلِ إِبْرَاهِيمَ إِنَّكَ حَمِيدٌ مَجِيدٌ", pronunciation:"Allâhümme bârik alâ Muhammedin ve alâ âli Muhammed, kemâ bârekte alâ İbrâhîme ve alâ âli İbrâhîm, inneke hamîdün mecîd.", meaning:"Allah'ım! İbrahim'e ve ailesine bereket verdiğin gibi Muhammed'e ve ailesine de bereket ver."},
  {name:"Rabbenâ Âtinâ", when:"Son oturuşta dua", arabic:"رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ", pronunciation:"Rabbenâ âtinâ fid-dünyâ haseneten ve fil-âhireti haseneten ve kınâ azâben-nâr.", meaning:"Rabbimiz! Bize dünyada iyilik, ahirette de iyilik ver ve bizi ateş azabından koru."}
 ], hanafi:"Hanefî uygulamada Fâtiha farz namazların ilk iki rekâtında ve sünnet/nafile namazların her rekâtında okunur; Fâtiha'dan sonra sûre/ayet eklemek ilgili yerlerde vaciptir.", mistakes:["Sadece Latin harfli okunuşa güvenip telaffuz hatasını hiç düzeltmemek.","Anlamı öğrenmenin gereksiz olduğunu düşünmek."], sources:["Kur'an, Fâtiha 1:1-7","Kur'an, Bakara 2:201","DİB Namaz İlmihali, Namazda okunan sûre ve dualar"], quiz:[{q:"Ettehiyyâtü nerede okunur?",a:["Oturuşlarda","Sadece rükûda","Ezan sırasında"],correct:0}]},

"kisa-sureler": {summary:"Namazda Fâtiha'dan sonra okuyabileceğin başlangıç kısa sûrelerini anlamlarıyla çalışırsın.", sections:[{title:"Ezber yöntemi",body:"Bir sûreyi ayet ayet dinleyip tekrar etmek, ardından namaz dışında ezberden okumak ve en sonunda anlamını tekrar etmek kalıcılığı artırır."}], arabicBlocks:[
 {name:"İhlâs",when:"Fâtiha'dan sonra zamm-ı sûre olarak",arabic:"قُلْ هُوَ اللَّهُ أَحَدٌ ۝ اللَّهُ الصَّمَدُ ۝ لَمْ يَلِدْ وَلَمْ يُولَدْ ۝ وَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ",pronunciation:"Kul hüvallâhü ehad. Allâhüssamed. Lem yelid ve lem yûled. Ve lem yekün lehû küfüven ehad.",meaning:"De ki: O Allah birdir. Allah Samed'dir... Hiçbir şey O'na denk değildir."},
 {name:"Kevser",when:"Fâtiha'dan sonra zamm-ı sûre olarak",arabic:"إِنَّا أَعْطَيْنَاكَ الْكَوْثَرَ ۝ فَصَلِّ لِرَبِّكَ وَانْحَرْ ۝ إِنَّ شَانِئَكَ هُوَ الْأَبْتَرُ",pronunciation:"İnnâ a'taynâkel kevser. Fesalli li rabbike venhar. İnne şânieke hüvel ebter.",meaning:"Şüphesiz biz sana Kevser'i verdik. O hâlde Rabbin için namaz kıl ve kurban kes..."},
 {name:"Asr",when:"Fâtiha'dan sonra zamm-ı sûre olarak",arabic:"وَالْعَصْرِ ۝ إِنَّ الْإِنْسَانَ لَفِي خُسْرٍ ۝ إِلَّا الَّذِينَ آمَنُوا وَعَمِلُوا الصَّالِحَاتِ وَتَوَاصَوْا بِالْحَقِّ وَتَوَاصَوْا بِالصَّبْرِ",pronunciation:"Vel asr. İnnel insâne lefî husr. İllellezîne âmenû ve amilussâlihâti ve tevâsav bil hakkı ve tevâsav bissabr.",meaning:"Asra yemin olsun ki insan gerçekten ziyandadır; ancak iman edip iyi işler yapanlar, hakkı ve sabrı tavsiye edenler başka."},
 {name:"Felak",when:"Fâtiha'dan sonra zamm-ı sûre olarak",arabic:"قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ۝ مِنْ شَرِّ مَا خَلَقَ ۝ وَمِنْ شَرِّ غَاسِقٍ إِذَا وَقَبَ ۝ وَمِنْ شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ ۝ وَمِنْ شَرِّ حَاسِدٍ إِذَا حَسَدَ",pronunciation:"Kul eûzü bi rabbil felak...",meaning:"De ki: Yarattığı şeylerin şerrinden sabah aydınlığının Rabbine sığınırım..."},
 {name:"Nâs",when:"Fâtiha'dan sonra zamm-ı sûre olarak",arabic:"قُلْ أَعُوذُ بِرَبِّ النَّاسِ ۝ مَلِكِ النَّاسِ ۝ إِلٰهِ النَّاسِ ۝ مِنْ شَرِّ الْوَسْوَاسِ الْخَنَّاسِ ۝ الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ ۝ مِنَ الْجِنَّةِ وَالنَّاسِ",pronunciation:"Kul eûzü bi rabbin nâs. Melikin nâs. İlâhin nâs...",meaning:"De ki: İnsanların Rabbine, insanların hükümdarına, insanların ilahına sığınırım..."}
 ],hanafi:"Zamm-ı sûre için yalnız bu sûreler şart değildir; Kur'an'dan uygun ayet/sûreler okunabilir.",mistakes:["Kısa sûreleri tek nefeste ve kelimeleri bozacak hızda okumak."],sources:["Kur'an, İhlâs 112","Kur'an, Kevser 108","Kur'an, Asr 103","Kur'an, Felak 113","Kur'an, Nâs 114","DİB Kur'an Yolu meali"],quiz:[{q:"Zamm-ı sûre sadece İhlâs olmak zorunda mıdır?",a:["Hayır","Evet","Sadece cuma günü hayır"],correct:0}]},

"zikirler": {summary:"Rükû, doğrulma ve secdede söylenen temel tesbihleri öğrenirsin.",sections:[
 {title:"Rükû",body:"Rükûda 'Sübhâne rabbiyel azîm' denir; yaygın öğretimde üç defa söylemek sünnet uygulamadır."},
 {title:"Doğrulma",body:"İmam veya tek başına kılan rükûdan kalkarken 'Semiallahu limen hamideh', doğrulunca 'Rabbenâ lekel hamd' der."},
 {title:"Secde",body:"Secdede 'Sübhâne rabbiyel a'lâ' denir; yaygın öğretimde üç defa söylemek sünnet uygulamadır."}],hanafi:"Tesbih sayısı, hareketin tâdil-i erkânını bozacak bir aceleye dönüştürülmemelidir.",mistakes:["Tesbihi tamamlamak için hareketi bozacak kadar acele etmek."],sources:["DİB Namaz İlmihali, Namazın kılınışı"],quiz:[{q:"Secde tesbihi hangisidir?",a:["Sübhâne rabbiyel a'lâ","Semiallahu limen hamideh","Allahu ekber yalnız"],correct:0}]},

"sabah-namazi": {summary:"Sabah namazının 2 rekât sünnet ve 2 rekât farzını, her rekâtta ne yapacağını görerek öğrenirsin.",sections:[
 {title:"Yapı",body:"Sabah namazı: 2 rekât sünnet + 2 rekât farz. Önce sünnet, sonra farz kılınır."},
 {title:"Sünnet — 1. rekât",body:"Niyet → iftitah tekbiri → elleri bağla → Sübhaneke → Eûzü-Besmele → Fâtiha → kısa sûre/ayet → rükû → doğrulma → iki secde."},
 {title:"Sünnet — 2. rekât",body:"Besmele → Fâtiha → kısa sûre/ayet → rükû → doğrulma → iki secde → son oturuş: Ettehiyyâtü, Salli, Bârik, dua → sağa ve sola selam."},
 {title:"Farz — 1. rekât",body:"Farza niyet et → tekbir → Sübhaneke → Eûzü-Besmele → Fâtiha → kısa sûre/ayet → rükû → doğrulma → iki secde."},
 {title:"Farz — 2. rekât",body:"Besmele → Fâtiha → kısa sûre/ayet → rükû → doğrulma → iki secde → son oturuş ve selam."},
 {title:"Şimdi ne yapıyorum?",body:"Her rekâtta üç kontrol sorusu kullan: Ayakta mıyım ve ne okuyorum? Rükûdan tam doğruldum mu? İki secdeyi ve aradaki oturuşu sakin yaptım mı?"}],hanafi:"Sabah sünneti kuvvetli sünnettir. Farz namaz cemaatle kılınıyorsa imama uyma kuralları ayrıca cemaat dersinde açıklanır.",mistakes:["Sünnet ile farza aynı niyetle devam etmek; farz için ayrıca niyet gerekir.","İkinci rekâtta son oturuş dualarını atlamak."],sources:["DİB Namaz İlmihali, Sabah namazının kılınışı","DİB İlmihal, Beş vakit namaz"],quiz:[{q:"Sabah namazının farzı kaç rekâttır?",a:["2","3","4"],correct:0}]},

"ogle-namazi": {summary:"Öğle namazının 4 rekât ilk sünnet, 4 rekât farz ve 2 rekât son sünnet düzenini rekât rekât öğrenirsin.",sections:[
 {title:"Yapı",body:"Öğle namazı: 4 rekât ilk sünnet + 4 rekât farz + 2 rekât son sünnet."},
 {title:"İlk sünnet — 1 ve 2",body:"1. rekât: Sübhaneke, Eûzü-Besmele, Fâtiha, zamm-ı sûre; rükû ve secdeler. 2. rekât: Besmele, Fâtiha, zamm-ı sûre; rükû ve secdeler; ilk oturuşta Ettehiyyâtü okunur."},
 {title:"İlk sünnet — 3 ve 4",body:"3. rekâta kalk; Besmele, Fâtiha ve zamm-ı sûre oku; rükû/secde. 4. rekâtta aynı düzen; son oturuşta Ettehiyyâtü, Salli, Bârik ve dua; selam."},
 {title:"Farz — 1 ve 2",body:"Farza niyet. İlk iki rekâtta Fâtiha ve zamm-ı sûre; 2. rekât sonunda ilk oturuşta Ettehiyyâtü."},
 {title:"Farz — 3 ve 4",body:"Hanefî uygulamada farzın 3. ve 4. rekâtında Fâtiha okumak sünnettir; zamm-ı sûre eklenmez. 4. rekât sonunda son oturuş ve selam."},
 {title:"Son sünnet",body:"2 rekâttır; sabah sünnetinin iki rekâtlık düzeni gibi kılınır."},
 {title:"Şimdi ne yapıyorum?",body:"Özellikle farzın 3-4. rekâtında 'zamm-ı sûre eklemiyorum' notunu hatırla; ilk ve son oturuşu ayır."}],hanafi:"Dört rekâtlı farzların üçüncü-dördüncü rekâtında sadece Fâtiha okumak tercih edilen sünnet uygulamadır.",mistakes:["Farzın 2. rekâtındaki ilk oturuşu son oturuş gibi uzatmak.","Farzın 3-4. rekâtında zamm-ı sûreyi zorunlu sanmak."],sources:["DİB Namaz İlmihali, Öğle namazının kılınışı","DİB İlmihal, Beş vakit namaz"],quiz:[{q:"Öğle farzı kaç rekâttır?",a:["2","3","4"],correct:2}]},

"ikindi-namazi": {summary:"İkindi namazının 4 rekât sünnet ve 4 rekât farz düzenini öğrenirsin.",sections:[
 {title:"Yapı",body:"İkindi namazı: 4 rekât sünnet + 4 rekât farz."},
 {title:"Sünnetin ilk oturuş farkı",body:"Hanefî uygulamada ikindinin gayr-i müekked 4 rekât sünnetinde 2. rekât oturuşunda Ettehiyyâtü ile birlikte Salli-Bârik okunur; 3. rekâta kalkınca Sübhaneke ve Eûzü-Besmele ile kıraate başlanır."},
 {title:"Farz — ilk iki rekât",body:"Niyet → tekbir → Sübhaneke → Eûzü-Besmele → Fâtiha + zamm-ı sûre → rükû/secde. 2. rekât sonunda Ettehiyyâtü ile ilk oturuş."},
 {title:"Farz — son iki rekât",body:"3 ve 4. rekâtta Hanefî uygulamada Fâtiha okunur; son rekâtta son oturuş duaları ve selam."},
 {title:"Şimdi ne yapıyorum?",body:"Sünnet ile farz arasındaki niyeti ve ikindi sünnetinin ilk oturuş/3. rekât başlangıç farkını özellikle takip et."}],hanafi:"İkindi öncesindeki 4 rekât sünnet gayr-i müekked sünnettir.",mistakes:["İkindi sünnetini öğlenin ilk sünnetiyle tamamen aynı sanmak."],sources:["DİB Namaz İlmihali, İkindi namazının kılınışı","DİB İlmihal, Beş vakit namaz"],quiz:[{q:"İkindi farzı kaç rekâttır?",a:["4","3","2"],correct:0}]},

"aksam-namazi": {summary:"Akşam namazının önce 3 rekât farz, sonra 2 rekât sünnet düzenini öğrenirsin.",sections:[
 {title:"Yapı",body:"Akşam namazı: 3 rekât farz + 2 rekât sünnet."},
 {title:"Farz — 1 ve 2",body:"İlk iki rekâtta Fâtiha + zamm-ı sûre okunur. 2. rekât sonunda Ettehiyyâtü ile ilk oturuş yapılır."},
 {title:"Farz — 3",body:"3. rekâta kalkınca Besmele ve Fâtiha okunur; rükû ve iki secde; son oturuşta Ettehiyyâtü, Salli, Bârik ve dua; selam."},
 {title:"Sünnet",body:"2 rekât sünnet, iki rekâtlık normal sünnet düzeniyle kılınır."},
 {title:"Şimdi ne yapıyorum?",body:"Akşam farzının üç rekât olduğunu ve 2. rekât sonundaki oturuşun ilk oturuş olduğunu hatırla."}],hanafi:"Akşam farzının üçüncü rekâtında zamm-ı sûre okunması gerekli değildir.",mistakes:["2. rekâtta selam vererek farzı yanlışlıkla iki rekâtta bitirmek."],sources:["DİB Namaz İlmihali, Akşam namazının kılınışı","DİB İlmihal, Beş vakit namaz"],quiz:[{q:"Akşam farzı kaç rekâttır?",a:["2","3","4"],correct:1}]},

"yatsi-namazi": {summary:"Yatsının sünnet ve farzlarını; ardından Hanefî mezhebinde vacip olan 3 rekât vitri öğrenirsin.",sections:[
 {title:"Yapı",body:"Yatsı: 4 rekât ilk sünnet + 4 rekât farz + 2 rekât son sünnet + Vitir. Vitir Hanefî mezhebinde 3 rekât ve vaciptir."},
 {title:"İlk sünnet",body:"İkindinin 4 rekât sünneti gibi gayr-i müekked 4 rekât sünnet düzenindedir; ilk oturuşta salavatlar okunur, üçüncü rekâta Sübhaneke ile başlanır."},
 {title:"Farz",body:"4 rekâttır. İlk iki rekâtta Fâtiha + zamm-ı sûre; 2. rekât ilk oturuş; 3-4. rekâtta Fâtiha; 4. rekâtta son oturuş ve selam."},
 {title:"Son sünnet",body:"2 rekâttır; iki rekâtlık normal sünnet düzeniyle kılınır."},
 {title:"Vitir",body:"3 rekât kılınır. İlk iki rekât normal şekilde; 2. rekât sonunda oturup Ettehiyyâtü okunur ve 3. rekâta kalkılır. 3. rekâtta Fâtiha ve zamm-ı sûreden sonra eller kaldırılarak tekbir alınır, eller bağlanır ve kunut duaları okunur; sonra rükûya gidilir."},
 {title:"Şimdi ne yapıyorum?",body:"Yatsı farzını bitirdikten sonra son sünnet ve vitrin ayrı niyetler olduğunu unutma. Vitirde kunut tekbiri 3. rekâttadır."}],hanafi:"Vitir Hanefî mezhebinde vaciptir; diğer mezheplerde hükmü farklı sınıflandırılabilir.",mistakes:["Vitirde kunut tekbirini rükû tekbiriyle karıştırmak.","Yatsı farzı ile vitri tek namaz gibi düşünmek."],sources:["DİB Namaz İlmihali, Yatsı ve vitir namazı","DİB İlmihal, Vitir namazı"],quiz:[{q:"Hanefî mezhebinde vitir kaç rekâttır?",a:["2","3","4"],correct:1}]},

"cemaat": {summary:"Cemaatle namazda imama nasıl uyulduğunu ve geç kalanın temel davranışını öğrenirsin.",sections:[
 {title:"İmama uyma",body:"Cemaatle namazda kişi imama uymaya niyet eder; hareketlerde imamın önüne geçmez. İmam tekbirle rükûya giderken cemaat de onu takip eder."},
 {title:"Kıraat",body:"Hanefî mezhebinde imama uyan kişi kıraatte imamı dinler; ayrıntılı mezhep farkı aşağıdaki notta gösterilir."},
 {title:"Geç kalan (mesbuk)",body:"İmama yetiştiği yerden uyar. İmam selam verdikten sonra kaçırdığı rekâtları tamamlar. Kaç rekâtı nasıl tamamlayacağı namazın türü ve yetiştiği yere göre ayrıntılıdır; uygulamada ayrıca örnekli rehber sunulur."}],hanafi:"Beş vakit farz namazı cemaatle kılmak erkekler için kuvvetli sünnet olarak değerlendirilir; imama uyanın kıraat hükmü Hanefî usulünde ayrıca düzenlenir.",mistakes:["İmamdan önce rükû veya secdeye gitmek.","Geç kalınca cemaate hiç katılmamak gerektiğini sanmak."],sources:["Kur'an, Bakara 2:43","DİB Namaz İlmihali, Cemaatle namaz","Din İşleri Yüksek Kurulu, Cemaatle namazın hükmü"],quiz:[{q:"Cemaatte temel hareket kuralı nedir?",a:["İmamı takip etmek, önüne geçmemek","İmamdan önce hareket etmek","Herkes ayrı ritimde kılmak"],correct:0}]},

"cami-adabi": {summary:"Camiye girişten safta durmaya kadar temel adabı öğrenirsin.",sections:[
 {title:"Camiye hazırlık",body:"Temiz kıyafet, beden ve ağız temizliği; başkalarını rahatsız edecek ağır koku ve gürültüden kaçınmak cami adabının parçasıdır."},
 {title:"Saf",body:"Saflarda gereksiz boşluk bırakmamak, imamı rahatça takip edecek düzeni korumak ve başkalarını itip rahatsız etmemek gerekir."},
 {title:"Telefon ve konuşma",body:"Camiye girerken telefonu sessize almak; ibadet edenleri meşgul edecek konuşma, bildirim sesi ve görüntü kaydından kaçınmak çağdaş cami adabının açık bir uygulamasıdır."}],hanafi:"Saf ve cemaat hükümlerinin teknik ayrıntıları cemaat bölümünde ele alınır.",mistakes:["Cami içinde başkalarının namazını bozacak ses çıkarmak."],sources:["Kur'an, A'râf 7:31","DİB İlmihal, Cami ve cemaat adabı"],quiz:[{q:"Camiye girerken telefon için uygun davranış nedir?",a:["Sessize almak","Sesi sonuna kadar açmak","Rastgele video oynatmak"],correct:0}]},

"ezan-kamet": {summary:"Ezanın namaz vaktini duyuran ibadet çağrısı, kametin ise farz namaza başlama ilanı olduğunu öğrenirsin.",sections:[
 {title:"Ezan",body:"Ezan, farz namaz vakitlerini ilan eden sünnet ve İslam'ın önemli şiarlarındandır. Müezzinin sözlerine uygun biçimde karşılık vermek ve ezan sonrası dua etmek tavsiye edilmiştir."},
 {title:"Kamet",body:"Farz namaza başlanacağı zaman ezana benzeyen ifadelerle kamet getirilir. Tek başına kılan erkek için de kamet sünnet kapsamında değerlendirilir."}],hanafi:"Kametin lafız ve tekrar sayıları mezheplerde küçük farklılıklar gösterebilir.",mistakes:["Ezanı sadece alarm sesi olarak görmek; anlamını hiç öğrenmemek."],sources:["Buhârî, Ezân bölümü","DİB İlmihal, Ezan ve kamet"],quiz:[{q:"Kamet neyi bildirir?",a:["Farz namaza başlanacağını","İftar vaktini her zaman","Hac yolculuğunu"],correct:0}]},

"cuma": {summary:"Cuma namazının haftalık toplu ibadet niteliğini, hutbe ve temel şartlarını öğrenirsin.",sections:[
 {title:"Cumanın yeri",body:"Cuma günü öğle vaktinde cemaatle kılınan cuma namazı, Kur'an'da özel olarak emredilen toplu ibadettir. Hutbe namazın temel unsurlarındandır."},
 {title:"Kimler için?",body:"Yükümlülük şartları cinsiyet, yolculuk, sağlık ve yerleşiklik gibi durumlara göre fıkıhta ayrıntılıdır. Kişisel mazeret konusunda güvenilir fetva kaynağına başvur."}],hanafi:"Cuma namazının şartları Hanefî fıkhında ayrıntılı biçimde düzenlenmiştir.",mistakes:["Cuma hutbesi sırasında yüksek sesle konuşmak.","Kişisel özel durumları genel bir cümleden çıkarımla çözmeye çalışmak."],sources:["Kur'an, Cuma 62:9","DİB İlmihal, Cuma namazı"],quiz:[{q:"Cuma namazıyla bağlantılı temel unsur nedir?",a:["Hutbe","Tavaf","Sa'y"],correct:0}]},

"bayram-cenaze": {summary:"Bayram ve cenaze namazlarının günlük beş vakit namazdan farklı yapısını temel seviyede tanırsın.",sections:[
 {title:"Bayram namazı",body:"Ramazan ve Kurban bayramlarında cemaatle kılınır. İlave tekbirleri vardır; ayrıntılı uygulama mezheplere göre küçük farklılık gösterebilir."},
 {title:"Cenaze namazı",body:"Rükû ve secdesi olmayan, ayakta tekbirler ve dualarla kılınan dua niteliğinde farz-ı kifâye bir namazdır."}],hanafi:"Hanefî mezhebinde bayram namazının hükmü vacip olarak değerlendirilir.",mistakes:["Cenaze namazında rükû ve secde olduğunu sanmak."],sources:["DİB İlmihal, Bayram namazı","DİB İlmihal, Cenaze namazı"],quiz:[{q:"Cenaze namazında hangisi yoktur?",a:["Rükû ve secde","Tekbir","Dua"],correct:0}]},

"sehiv-secdesi": {summary:"Namazda unutma veya yanılma nedeniyle bazı vaciplerin gecikmesi/terk edilmesi durumunda sehiv secdesinin temel mantığını öğrenirsin.",sections:[
 {title:"Ne için yapılır?",body:"Hanefî mezhebinde namazın bir vacibinin unutularak terk edilmesi veya geciktirilmesi gibi hâllerde sehiv secdesi gerekir. Kasıtlı terk ile unutma aynı değildir."},
 {title:"Temel uygulama",body:"Hanefî yaygın uygulamada son oturuşta Ettehiyyâtü okunduktan sonra bir tarafa selam verilip iki secde yapılır; tekrar oturulup Ettehiyyâtü, salavat ve dualar okunarak namaz tamamlanır. Ayrıntılı durumlarda ilmihal hükmü kontrol edilmelidir."},
 {title:"Şüphe",body:"Rekât sayısında ara sıra oluşan şüphede kişi kanaatine göre hareket eder; sık vesvese yaşayan kişinin her şüpheyi gerçek hata saymaması gerekir. Detaylar ayrı vesvese dersinde."}],hanafi:"Sehiv secdesinin sebep ve yapılış sırası Hanefî ana anlatıma göre verilmiştir.",mistakes:["Her küçük tereddütte namazı bozup baştan başlamak.","Kasten terk edilen vaciple unutularak terk edileni aynı değerlendirmek."],sources:["DİB Namaz İlmihali, Sehiv secdesi","Din İşleri Yüksek Kurulu, Sehiv secdesi açıklamaları"],quiz:[{q:"Sehiv secdesinin tipik sebebi nedir?",a:["Vacibin unutularak terk/gecikmesi","Her sünneti yapmamak","Namazdan sonra yemek yemek"],correct:0}]},

"kaza": {summary:"Vaktinde kılınamayan farz namazların kaza edilmesi konusunda temel çerçeveyi öğrenirsin.",sections:[
 {title:"Vakit ve sorumluluk",body:"Farz namazların asıl olanı vaktinde kılınmasıdır. Uyku veya unutma gibi mazeretle kaçırılan namaz hatırlanınca kılınır."},
 {title:"Kaza",body:"Vakti geçen farz namazlar kaza edilir. Uzun süreli borç, sıralama ve özel durumlar için ilmihal/fetva ayrıntısı gerekir; uygulama kişisel borç hesabına dair dinî hüküm üretmez."}],hanafi:"Kaza ve tertip hükümlerinde Hanefî mezhebine özgü ayrıntılar bulunur.",mistakes:["Kaza edilecek diye vaktindeki namazı önemsememek.","Özel durumunu sormadan kesin hüküm üretmek."],sources:["Buhârî, Mevâkît 37","Müslim, Mesâcid 314","DİB İlmihal, Kaza namazları"],quiz:[{q:"Asıl olan nedir?",a:["Namazı vaktinde kılmak","Her namazı sonra kaza etmek","Vakitleri dikkate almamak"],correct:0}]},

"seferilik": {summary:"Yolculukta namaz ruhsatlarının temel mantığını ve mesafe/süre gibi şartların ayrıntılı olduğunu öğrenirsin.",sections:[
 {title:"Seferîlik nedir?",body:"Fıkıhta belirli yolculuk şartlarını taşıyan kişi seferî sayılır ve dört rekâtlı farz namazları iki rekât kılma gibi ruhsatlardan yararlanır."},
 {title:"Şartlar",body:"Mesafe, gidilecek yerde kalma niyeti ve yolculuğun niteliği gibi ölçüler mezheplere göre farklı ayrıntılar içerir. Sadece şehir sınırını geçmek her durumda yeterli kabul edilmez."}],hanafi:"Hanefî mezhebinde sefer mesafesi ve bir yerde 15 gün veya daha fazla kalmaya niyet etmenin hükmü gibi özel ölçüler vardır.",mistakes:["Her kısa araç yolculuğunu seferîlik saymak.","Kalma süresi niyetini hiç dikkate almamak."],sources:["Kur'an, Nisâ 4:101","DİB İlmihal, Seferîlik","Din İşleri Yüksek Kurulu, Seferîlik hükümleri"],quiz:[{q:"Seferîlikte hangi ayrıntı önemlidir?",a:["Mesafe ve kalma niyeti","Aracın rengi","Valiz sayısı"],correct:0}]},

"hastalikta-namaz": {summary:"Ayakta duramayan veya hareketleri yapamayan kişinin namazı gücü yettiği şekilde kılabileceği temel ilkesini öğrenirsin.",sections:[
 {title:"Güç yetirme ilkesi",body:"Ayakta kılmak sağlığa ciddi zarar veriyorsa veya mümkün değilse kişi oturarak kılar; oturmak da mümkün değilse durumuna uygun ima ile kılma hükümleri gündeme gelir."},
 {title:"İma",body:"Rükû ve secdeyi normal yapamayan kişi başıyla ima eder; secde işareti rükûdan daha aşağı olacak şekilde yapılır. Gözle ima gibi ileri özel durumlarda mezhep farkı bulunabilir ve yetkin fetva kaynağına başvurulmalıdır."}],hanafi:"Hastalıkta namaz ayrıntıları, hareket kabiliyetine göre kademeli değerlendirilir.",mistakes:["Hastayken namazın her durumda tamamen düştüğünü sanmak.","Sağlığa zarar verecek hareketi sırf şekil uğruna zorlamak."],sources:["Kur'an, Bakara 2:286","Buhârî, Taksîrü's-salât 19","Din İşleri Yüksek Kurulu, İma ile namaz"],quiz:[{q:"Ayakta duramayan kişi için ilk temel ruhsat nedir?",a:["Oturarak kılmak","Namazı tamamen bırakmak","Sadece ezan dinlemek"],correct:0}]},

"kerahat-vakitleri": {summary:"Bazı zaman dilimlerinde nafile veya belirli namazların kılınmasının uygun görülmediğini öğrenirsin.",sections:[
 {title:"Temel çerçeve",body:"Güneşin doğduğu, tam tepe noktasında bulunduğu ve battığı anlara yakın vakitler başta olmak üzere namaz kılmanın yasak/mekruh olduğu zamanlar vardır. Ayrıntılar namaz türüne göre değişir."},
 {title:"Uygulama",body:"Uygulamadaki 'Güneş' vakti sabah farzının vakti değildir; güneş doğuş bilgisidir. Nafile/kaza gibi namazlar için kerahat ayrıntısı ayrıca kontrol edilmelidir."}],hanafi:"Kerahat vakitleri Hanefî ilmihalinde ayrıntılı sınıflandırılır.",mistakes:["Güneş saatini ayrı bir farz namaz vakti sanmak."],sources:["Buhârî, Mevâkît bölümü","DİB İlmihal, Kerahat vakitleri"],quiz:[{q:"Uygulamadaki Güneş saati nedir?",a:["Güneş doğuş bilgisi","Altıncı farz namaz","Yatsının bitişi her yerde"],correct:0}]},

"kadinlara-ozel": {summary:"Hayız ve nifas gibi durumların ibadet hükümlerinde özel başlıklar olduğunu, mahremiyete saygılı temel çerçevede öğrenirsin.",sections:[
 {title:"Hayız ve nifas",body:"Hayız ve nifas hâlindeki kadın namaz kılmaz; bu dönemlerde kılınmayan namazlar kaza edilmez. Süre ve kanama türlerinin ayrıntıları mezheplerde teknik kurallara tabidir."},
 {title:"İstihâza",body:"Hayız/nifas dışı özür kanaması farklı hükme tabidir; namaz ve abdest uygulaması devam eder fakat özür hükümleri devreye girebilir."},
 {title:"Mahremiyet",body:"Kişisel tıbbi durumlarda kendi kendine kesin dinî teşhis koymak yerine gerektiğinde hekim ve güvenilir dinî danışmana başvur."}],hanafi:"Hayız, nifas ve istihâza süre/ayırt etme hükümleri Hanefî mezhebinde ayrıntılıdır; bu ders yalnız başlangıç çerçevesidir.",mistakes:["Her kanamayı aynı hükümde kabul etmek.","Kişisel sağlık sorununda yalnız uygulama metnine dayanmak."],sources:["DİB İlmihal, Kadınlara özel hâller","Din İşleri Yüksek Kurulu, Hayız ve nifas açıklamaları"],quiz:[{q:"Hayız döneminde kılınmayan namazların hükmü nedir?",a:["Kaza edilmez","Her biri iki kat kılınır","Sadece cuma günü kaza edilir"],correct:0}]},

"vesvese": {summary:"Sürekli şüphe üretmenin ibadeti zorlaştırmasına karşı ölçülü davranmayı öğrenirsin.",sections:[
 {title:"Şüphe ile kesinlik",body:"Kesin olarak bildiğin bir şeyi sırf sonradan gelen zayıf şüpheyle bozulmuş sayma. 'Acaba abdestim bozuldu mu?' gibi delilsiz tekrarlar vesveseyi büyütebilir."},
 {title:"Namazda rekât şüphesi",body:"Nadir şüphe ile sürekli vesvese aynı değildir. Ara sıra olan gerçek şüphede ilmihal kuralları uygulanır; kronik vesvesede her şüpheyi dikkate almak doğru değildir."}],hanafi:"Vesveseyi azaltmada 'yakîn şüphe ile ortadan kalkmaz' fıkıh ilkesi yol göstericidir.",mistakes:["Abdesti veya namazı sırf ihtimal yüzünden sürekli tekrar etmek."],sources:["Müslim, Hayız 99","DİB İlmihal, Vesvese ve şüphe hükümleri"],quiz:[{q:"Delilsiz zayıf şüphede en sağlıklı yaklaşım nedir?",a:["Kesin bildiğin durumu esas almak","Her seferinde ibadeti baştan yapmak","Paniklemek"],correct:0}]},

"oruc-temel": {summary:"Ramazan orucunun amacı, sahur-iftar ve temel bozanlar hakkında başlangıç bilgisi edinirsin.",sections:[
 {title:"Oruç",body:"Ramazan ayında şartlarını taşıyan Müslümana oruç farzdır. İmsaktan güneş batımına kadar yeme, içme ve cinsel ilişkiden uzak durulur; niyet ve ibadet bilinci esastır."},
 {title:"Mazeretler",body:"Hastalık, yolculuk, gebelik/emzirme gibi durumlarda ayrıntılı ruhsat hükümleri vardır; kişisel sağlık durumunda hekim ve yetkin dinî kaynak birlikte değerlendirilmelidir."}],hanafi:"Niyet zamanı, bozma-kefaret gibi ayrıntılı hükümler Hanefî fıkhında ayrı başlıklardır.",mistakes:["Oruçta yalnız aç kalmayı amaç sanmak.","Sağlık riskini dikkate almamak."],sources:["Kur'an, Bakara 2:183-185","DİB İlmihal, Oruç bölümü"],quiz:[{q:"Ramazan orucunun vakti hangi aralıktadır?",a:["İmsaktan güneş batımına","Öğleden ikindiye","Sadece gece"],correct:0}]},

"zekat-temel": {summary:"Zekâtın sosyal ve ibadî amacını, mali yeterlilik şartı bulunduğunu öğrenirsin.",sections:[
 {title:"Zekât",body:"Belirli mal varlığı şartlarını taşıyan Müslümanın, belirlenmiş oran ve gruplara malından pay ayırdığı farz mali ibadettir."},
 {title:"Sadaka",body:"Sadaka zekâttan daha geniş gönüllü iyilik alanıdır; sadece para vermekle sınırlı değildir."}],hanafi:"Nisap, yıl şartı, altın/para/ticaret malı hesapları ayrıntılıdır; güncel hesap için DİB kaynakları kullanılmalıdır.",mistakes:["Zekâtı herkese aynı miktar düşen sabit aidat sanmak."],sources:["Kur'an, Tevbe 9:60","Kur'an, Bakara 2:43","DİB İlmihal, Zekât bölümü"],quiz:[{q:"Zekât hangi tür ibadettir?",a:["Mali ibadet","Sadece bedeni hareket","Yalnız yolculuk"],correct:0}]},

"hac-umre-temel": {summary:"İhram, tavaf, sa'y, Arafat ve temel hac/umre kavramlarını tanırsın.",sections:[
 {title:"Hac",body:"Gücü yeten Müslümanın belirli zamanda Kâbe ve çevresindeki menâsiki yerine getirdiği farz ibadettir. Arafat vakfesi haccın ayırt edici rükünlerindendir."},
 {title:"Umre",body:"İhrama girme, Kâbe'yi tavaf, Safa-Merve arasında sa'y ve tıraş/kısaltma ile tamamlanan ibadettir; hacdan zaman ve hüküm bakımından farklıdır."}],hanafi:"Hac menâsikinde farz-vacip-sünnet ayrımları ve ceza hükümleri ayrıntılıdır.",mistakes:["Hac ile umreyi tamamen aynı ibadet sanmak."],sources:["Kur'an, Âl-i İmrân 3:97","Kur'an, Bakara 2:196","DİB Hac ve Umre Rehberi"],quiz:[{q:"Haccın ayırt edici temel rükünlerinden biri nedir?",a:["Arafat vakfesi","Ezan okumak","Teravih"],correct:0}]},

"aile": {summary:"Anne-babaya iyilik, eşler arası sorumluluk ve çocuklara merhametin dinî ahlakta yerini öğrenirsin.",sections:[
 {title:"Anne-baba",body:"Kur'an, Allah'a kulluktan hemen sonra anne-babaya iyi davranmayı güçlü biçimde vurgular. Saygı; haksızlığa ortak olmak anlamına gelmez, fakat konuşma üslubunda iyilik korunur."},
 {title:"Eşler",body:"Evlilikte merhamet, güven, haklara riayet ve karşılıklı sorumluluk esastır. Şiddet, hakaret ve aşağılamayı normalleştiren bir ilişki dinî ahlakla bağdaşmaz."},
 {title:"Çocuklar",body:"Çocuğa ibadeti öğretmek, yaşına uygun örneklik ve şefkatle yapılmalıdır; korku ve aşağılama eğitim yöntemi hâline getirilmemelidir."}],hanafi:"Bu ders fıkhî aile hukuku fetvası değil, genel ahlak çerçevesidir.",mistakes:["Anne-babaya saygıyı her talebe kayıtsız şartsız itaat sanmak.","Dini öğretmeyi korkutmakla eşitlemek."],sources:["Kur'an, İsrâ 17:23-24","Kur'an, Rûm 30:21","DİB İlmihal, Ahlak ve aile"],quiz:[{q:"Aile ahlakında temel ilkelerden biri nedir?",a:["Merhamet ve haklara riayet","Hakaret","Korkutma"],correct:0}]},

"komsuluk": {summary:"Komşu, akraba ve arkadaşlarla ilişkide iyilik, sınır ve güven ilkelerini öğrenirsin.",sections:[
 {title:"Komşu",body:"Kur'an komşuya iyiliği emreder. Gürültü, ortak alan, borç-alacak ve mahremiyet konularında 'bana bir şey olmaz' değil, karşı tarafın hakkı gözetilir."},
 {title:"Akrabalık",body:"Akrabalık bağlarını korumak önemlidir; fakat zarar veren ilişkilere sınır koymak, iyilik bağını bütünüyle düşmanlığa çevirmemekle birlikte mümkün olabilir."},
 {title:"Arkadaşlık",body:"İyi arkadaşlık güven, sır saklama, doğruyu nazikçe söyleme ve kötü alışkanlığa sürüklememe üzerine kurulur."}],hanafi:"Genel ahlak dersidir.",mistakes:["Komşu hakkını yalnız yardım isteme anlarına indirgemek."],sources:["Kur'an, Nisâ 4:36","Müslim, Birr ve Sıla bölümü","DİB Hadislerle İslam, Komşuluk"],quiz:[{q:"Komşulukta hangi ilke önemlidir?",a:["Zarar vermemek ve iyilik","Gürültüyü önemsememek","Mahremiyeti ihlal etmek"],correct:0}]},

"ticaret": {summary:"İş ve ticarette doğruluk, ölçü-tartı ve sözleşme güveninin dinî sorumluluk olduğunu öğrenirsin.",sections:[
 {title:"Dürüstlük",body:"Malın kusurunu gizlemek, karşı tarafı yanıltmak veya yanlış bilgiyle satış yapmak kul hakkına yol açabilir. Ticarette rıza, açıklık ve dürüstlük esastır."},
 {title:"Ölçü ve tartı",body:"Kur'an ölçü ve tartıda hileyi ağır biçimde eleştirir. Modern iş hayatında bu ilke fatura, sözleşme, mesai, kalite ve hizmet taahhüdünü de ahlaken kapsar."}],hanafi:"Faiz, murabaha, ortaklık ve güncel finans ürünleri ayrıntılı fıkıh konularıdır; bu ders genel ahlak düzeyindedir.",mistakes:["Kusuru gizlemeyi 'pazarlama' saymak.","İş sözünü dinî sorumluluktan ayrı görmek."],sources:["Kur'an, Mutaffifîn 83:1-3","Kur'an, Nisâ 4:29","DİB İlmihal, Ticaret ahlakı"],quiz:[{q:"Ticarette temel ilke nedir?",a:["Dürüstlük ve karşılıklı rıza","Kusuru gizlemek","Yanıltıcı bilgi"],correct:0}]},

"borc-kul-hakki": {summary:"Borcu yazma, emaneti koruma ve kul hakkını ciddiye alma alışkanlığı kazanırsın.",sections:[
 {title:"Borç",body:"Kur'an vadeli borçların yazılmasını tavsiye eden ayrıntılı bir düzen verir. Borcu küçümsememek, ödeme niyeti ve ödeme planında dürüst olmak gerekir."},
 {title:"Emanet",body:"Emanet sadece fizikî eşya değildir; sır, görev, yetki ve iş sorumluluğu da emanet niteliği taşır."},
 {title:"Kul hakkı",body:"Başkasının malı, itibarı, zamanı veya hakkı ihlal edildiğinde yalnız içten pişmanlık yetmeyebilir; mümkün olduğunda hakkı iade/helalleşme boyutu da bulunur."}],hanafi:"Tazmin, helalleşme ve borç uyuşmazlıklarının ayrıntısı olaya göre değişir.",mistakes:["Küçük miktar borcu kayıt gerektirmeyecek kadar önemsiz saymak.","Emaneti izinsiz kullanmak."],sources:["Kur'an, Bakara 2:282","Kur'an, Nisâ 4:58","DİB İlmihal, Kul hakkı ve emanet"],quiz:[{q:"Vadeli borçta Kur'an'ın tavsiyelerinden biri nedir?",a:["Yazmak/kayıt altına almak","Unutmak","Kimseye söylememek"],correct:0}]},

"giybet": {summary:"Gıybet ile haklı uyarı/şikâyet arasındaki farkı ve sözün sorumluluğunu öğrenirsin.",sections:[
 {title:"Gıybet",body:"Bir kişinin arkasından, duyduğunda hoşlanmayacağı gerçek bir kusurunu gereksiz yere konuşmak gıybet kapsamına girer. Söylenen şey gerçek değilse iftira boyutu doğabilir."},
 {title:"Her olumsuz söz aynı mı?",body:"Zulme uğrayanın hakkını araması, yetkili kişiye şikâyet, zarar önlemek için gerekli ve ölçülü uyarı gibi durumlar ayrı değerlendirilir. Ama bunlar merak/dedikodu için mazeret değildir."}],hanafi:"Genel ahlak/fıkıh ilkesi; somut olaylarda niyet ve gereklilik önemlidir.",mistakes:["'Doğru söylüyorum' diyerek gıybeti meşru sanmak.","Sosyal medyada ifşa kültürünü sorgulamamak."],sources:["Kur'an, Hucurât 49:12","Müslim, Birr 70","DİB Hadislerle İslam, Dil ve gıybet"],quiz:[{q:"Söylenen kusur gerçek olsa bile gereksizce arkasından konuşmak ne olabilir?",a:["Gıybet","Her zaman nasihat","Zekât"],correct:0}]},

"ofke": {summary:"Öfkeyi inkâr etmek yerine kontrol etmeyi, tartışmada haksızlığa düşmemeyi öğrenirsin.",sections:[
 {title:"Öfke",body:"Öfke bir duygudur; sorumluluk öfke anındaki söz ve davranıştadır. Ara vermek, ortam değiştirmek, ses tonunu düşürmek ve karar vermeyi ertelemek zararı azaltabilir."},
 {title:"Güç",body:"Hadislerde gerçek güç, öfke anında kendine hâkim olabilmekle ilişkilendirilir. Fiziksel veya sözlü saldırganlık güç göstergesi değildir."}],hanafi:"Genel ahlak dersidir.",mistakes:["Öfkeliyken söylenen her sözü mazur saymak.","Şiddeti normalleştirmek."],sources:["Kur'an, Âl-i İmrân 3:134","Buhârî, Edeb 76","Müslim, Birr 107"],quiz:[{q:"Öfke anında yararlı davranış hangisidir?",a:["Kararı erteleyip sakinleşmek","Hakaret etmek","Şiddet kullanmak"],correct:0}]},

"israf-adalet": {summary:"Kaynakları boşa harcamama ve hakkaniyetli davranma ilkelerini günlük hayata uygularsın.",sections:[
 {title:"İsraf",body:"Yiyecek, su, para, enerji ve zamanı gereksiz tüketmek israf bilinciyle değerlendirilir. 'Benim param' demek sorumluluğu tamamen kaldırmaz."},
 {title:"Adalet",body:"Adalet yalnız mahkemelik mesele değildir; aile, iş, ekip ve arkadaşlıkta da kendi aleyhine olsa bile doğru ölçüyü koruma hedefidir."}],hanafi:"Genel ahlak dersidir.",mistakes:["İsrafı sadece yemek çöpe atmakla sınırlamak."],sources:["Kur'an, A'râf 7:31","Kur'an, Nisâ 4:135","DİB Ahlak kaynakları"],quiz:[{q:"Adalet nerede gereklidir?",a:["Günlük ilişkiler dahil her yerde","Sadece mahkemede","Sadece ticarette"],correct:0}]},

"mahremiyet-sosyal-medya": {summary:"Dijital ortamda haber doğrulama, özel hayat ve paylaşım sorumluluğunu öğrenirsin.",sections:[
 {title:"Haber doğrulama",body:"Bir haber veya iddia geldiğinde özellikle başkasının itibarına zarar verecekse paylaşmadan önce doğrulamak gerekir. Hızlı paylaşım, yanlış bilgiyi yayma sorumluluğunu kaldırmaz."},
 {title:"Mahremiyet",body:"İzin almadan özel mesaj, fotoğraf, ses kaydı veya aile içi bilgiyi paylaşmak mahremiyet ve kul hakkı sorunu doğurabilir."},
 {title:"Dijital gıybet",body:"Gıybet ve hakaret, ekran arkasında yapılınca mahiyet değiştirmez. Beğenmek, alaycı yorumla büyütmek veya kitleye yaymak da sorumluluk doğurabilir."}],hanafi:"Genel ahlak ilkelerinin dijital hayata uygulanmasıdır.",mistakes:["'Zaten internette vardı' diyerek her içeriği paylaşmayı serbest sanmak.","Özel konuşmayı izinsiz yayımlamak."],sources:["Kur'an, Hucurât 49:6","Kur'an, Hucurât 49:12","Kur'an, Nûr 24:27","DİB, Sosyal medya ve ahlak içerikleri"],quiz:[{q:"Şüpheli bir haber için ilk doğru adım nedir?",a:["Doğrulamak","Hemen yaymak","Başlık yeterli saymak"],correct:0}]},

"ziyaret-taziye-yolculuk": {summary:"Hasta ziyareti, taziye ve yolculukta insanı merkeze alan temel adabı öğrenirsin.",sections:[
 {title:"Hasta ziyareti",body:"Ziyareti hastayı yormayacak kadar kısa tutmak, mahremiyetine dikkat etmek ve zorlayıcı nasihatlerden kaçınmak iyilik adabına uygundur."},
 {title:"Taziye",body:"Acılı kişiye sabır dilemek, yükünü hafifletmek ve gereksiz sorgulama/uzun oturumlarla onu yormamak esastır."},
 {title:"Yolculuk",body:"Yolculukta güvenlik, namaz vakitlerini planlama, borç/emanetleri düzenleme ve birlikte yolculuk edilenlerin hakkını gözetmek önemlidir."}],hanafi:"Seferîlik fıkhı ayrı dersin konusudur.",mistakes:["Hasta ziyaretini uzun sohbet zorunluluğuna dönüştürmek."],sources:["Müslim, Birr bölümü","DİB Hadislerle İslam, Hasta ziyareti ve taziye"],quiz:[{q:"Hasta ziyaretinde iyi ölçü nedir?",a:["Hastayı yormamak","Mümkün olduğunca uzun kalmak","Özel bilgilerini sormak"],correct:0}]}
};'''

SHAFII_NOTES = r'''window.NAMAZ_SHAFII_NOTES = {
  "abdest": {title:"Şafiî farkı", text:"Şafiî mezhebinde abdestte niyet ve tertip (organları belirtilen sırayla yıkamak/mesh etmek) farzdır. Başın bir kısmının mesh edilmesi yeterli görülür; Hanefî mezhebinde ise başın en az dörtte biri mesh edilir ve niyet sünnettir.", source:"Din İşleri Yüksek Kurulu — Mezhepler arasında abdestin farzları; DİB İlmihal, Abdest"},
  "gusul": {title:"Şafiî farkı", text:"Şafiî mezhebinde guslün temel farzları niyet ve suyu bütün bedene ulaştırmaktır; ağız ve burnu yıkamak Hanefî mezhebinde farz iken Şafiî mezhebinde aynı teknik hükümde değildir.", source:"DİB İlmihal, Gusül ve mezhep farkları"},
  "teyemmum": {title:"Şafiî farkı", text:"Teyemmümün kullanılacağı yüzey, mesh sınırı ve bazı şartlarında mezhepler arasında ayrıntı farkları vardır. Şafiî uygulamada niyetin yeri ve nakil/temiz toprak şartları ayrıca önem taşır.", source:"DİB İlmihal, Teyemmüm"},
  "namazin-sartlari": {title:"Şafiî farkı", text:"Namazın rükünlerinin sayımı ve bazı fiillerin farz/rükün-vacip olarak sınıflandırılması Şafiî mezhebinde Hanefî terminolojisinden farklıdır. Kullanıcı uygulamada Hanefî ana akışı izlerken bu farkı 'hata' olarak görmemelidir.", source:"DİB Namaz İlmihali, Mezhep farklılıkları"},
  "namazin-hareketleri": {title:"Şafiî farkı", text:"Rükûdan doğrulma (kavme) ve iki secde arasındaki oturuş (celse), Şafiî mezhebinde namazın rükünleri arasında değerlendirilir. Hanefî mezhebinde kuvvetli görüş bunları vacip sayar.", source:"Din İşleri Yüksek Kurulu — Kavme ve celsenin hükmü"},
  "vacip-sunnet-mekruh": {title:"Şafiî farkı", text:"Şafiî fıkhında Hanefî mezhebindeki teknik 'farz-vacip' ayrımı aynı biçimde kullanılmaz. Bu nedenle bir fiilin Hanefîde 'vacip', Şafiîde 'rükün/farz' veya 'sünnet' olarak adlandırılması mümkündür.", source:"DİB İlmihal, Fıkıh terminolojisi"},
  "cemaat": {title:"Şafiî farkı", text:"Beş vakit namazın cemaatle kılınmasının hükmü mezheplerde farklı sınıflandırılmıştır. Şafiî mezhebinde erkekler bakımından farz-ı kifâye görüşü öne çıkarken Hanefî mezhebinde kuvvetli sünnet olarak anlatılır.", source:"Din İşleri Yüksek Kurulu — Cemaatle namazın hükmü"},
  "seferilik": {title:"Şafiî farkı", text:"Sefer mesafesi ve gidilen yerde kalma niyetinin süre sınırı Şafiî mezhebinde Hanefî mezhebinden farklı ayrıntılara sahiptir. Yolculuk planında kendi mezhebinin ölçüsünü güvenilir kaynakla kontrol et.", source:"DİB İlmihal, Seferîlik"},
  "hastalikta-namaz": {title:"Şafiî farkı", text:"İma, oturarak kılma ve ileri derecede hareket kısıtlılığında mezhepler arasında bazı uygulama farklılıkları bulunur. Özellikle gözle ima gibi uç durumlarda kişisel fetva için yetkin bir din görevlisine danışılmalıdır.", source:"Din İşleri Yüksek Kurulu — İma ile namaz"},
  "yatsi-namazi": {title:"Şafiî farkı", text:"Vitir namazı Şafiî mezhebinde sünnet-i müekkede kabul edilir ve rekât uygulamasında seçenekler bulunur; Hanefî mezhebinde ise vitir 3 rekât vacip olarak kılınır.", source:"DİB İlmihal, Vitir namazı"}
};'''

ACADEMY_JS = r'''(function(){
"use strict";
let academyHistory=[];
let academyState={view:"home",category:null,lesson:null,anchor:null};
const A=()=>window.NAMAZ_ACADEMY||{categories:[],lessons:[],beginnerPath:[]};
const H=()=>window.NAMAZ_HANAFI_LESSONS||{};
const S=()=>window.NAMAZ_SHAFII_NOTES||{};
function byId(id){return document.getElementById(id)}
function readJson(k,fallback){try{const v=localStorage.getItem(k);return v?JSON.parse(v):fallback}catch(e){return fallback}}
function writeJson(k,v){try{localStorage.setItem(k,JSON.stringify(v))}catch(e){}}
function esc(s){return String(s==null?"":s).replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]))}
function lessonMeta(id){return A().lessons.find(x=>x.id===id)}
function completed(){const v=readJson("academy.completed",[]);return Array.isArray(v)?v:[]}
function setScrollKey(){if(academyState.lesson) writeJson("academy.scroll."+academyState.lesson,window.scrollY||0);writeJson("academy.scroll",{lesson:academyState.lesson,y:window.scrollY||0})}
function pushState(){academyHistory.push(JSON.parse(JSON.stringify(academyState)));if(academyHistory.length>30)academyHistory.shift()}
function screenActive(){const e=byId("learn");return !!(e&&e.classList.contains("active"))}
function root(){return byId("academyRoot")}
function categoryTitle(id){const c=A().categories.find(x=>x.id===id);return c?c.title:"Dersler"}
function sourceList(sources){return '<div class="academy-sources"><div class="academy-callout-title">Kaynak</div><ul>'+sources.map(x=>'<li>'+esc(x)+'</li>').join('')+'</ul><p>Bu uygulama kişisel fetva üretmez. Özel durumlarda Diyanet/Din İşleri Yüksek Kurulu veya yetkin bir din görevlisine danış.</p></div>'}
function postureSvg(type){const label={qiyam:"Kıyam",ruku:"Rükû",sujud:"Secde",sitting:"Oturuş"}[type]||"Duruş";let body="";
 if(type==="qiyam")body='<circle cx="60" cy="25" r="12"/><path d="M60 37v58M39 56l21 16 21-16M60 95l-15 46M60 95l15 46"/>';
 if(type==="ruku")body='<circle cx="42" cy="43" r="12"/><path d="M51 50l52 18M101 68l24 2M82 61l-8 43M74 104l-13 39M74 104l12 39"/>';
 if(type==="sujud")body='<circle cx="102" cy="101" r="11"/><path d="M91 96L62 75 35 100M62 75l10-34M35 100l-15 28M35 100l30 26M65 126l38-7"/>';
 if(type==="sitting")body='<circle cx="60" cy="36" r="12"/><path d="M60 48v51M60 70l28 15M60 99l-32 28M28 127h48M60 99l30 31"/>';
 return '<figure class="academy-posture"><svg viewBox="0 0 150 160" aria-label="'+label+' şeması"><g>'+body+'</g></svg><figcaption>'+label+' · Şematik anlatım</figcaption></figure>'}
function progressHtml(){const done=completed().length,total=A().lessons.length,pct=total?Math.round(done*100/total):0;return '<div class="academy-progress-card"><div><span class="academy-kicker">İLERLEMEN</span><strong>'+done+' / '+total+' ders</strong><small>Hanefî ana anlatım · Şafiî farkları görünür</small></div><div class="academy-progress-number">%'+pct+'</div><div class="academy-progress-track"><i style="width:'+pct+'%"></i></div></div>'}
function lessonButton(m,index){const done=completed().includes(m.id);return '<button class="academy-lesson-card '+(done?'is-done':'')+'" onclick="academyOpenLesson(\''+m.id+'\')"><span class="academy-lesson-no">'+(done?'✓':String(index+1).padStart(2,'0'))+'</span><span class="academy-lesson-copy"><b>'+esc(m.title)+'</b><small>'+esc(categoryTitle(m.category))+' · '+m.minutes+' dk</small></span><span class="academy-chevron">›</span></button>'}
function homeHtml(){const first=A().beginnerPath[0];const last=localStorage.getItem("academy.lastLesson");const categories=A().categories.map(c=>'<button class="academy-category" onclick="academyOpenCategory(\''+c.id+'\')"><span>'+c.icon+'</span><b>'+esc(c.title)+'</b><small>'+esc(c.subtitle)+'</small></button>').join('');const path=A().beginnerPath.slice(0,8).map((id,i)=>lessonButton(lessonMeta(id),i)).join('');return '<div class="academy-page academy-home">'+progressHtml()+'<div class="academy-search"><span>⌕</span><input id="academySearchInput" type="search" placeholder="Abdest bozan, ikindi kaç rekât, gıybet…" oninput="academySearch(this.value)"></div><div id="academySearchResults"></div>'+(last&&lessonMeta(last)?'<button class="academy-continue" onclick="academyOpenLesson(\''+last+'\')"><span><small>KALDIĞIN YER</small><b>'+esc(lessonMeta(last).title)+'</b></span><i>Devam et →</i></button>':'')+'<div class="academy-section-title"><div><span>SIFIRDAN BAŞLA</span><h2>Önerilen öğrenme yolu</h2></div><button onclick="academyOpenLesson(\''+first+'\')">Başla</button></div><div class="academy-path">'+path+'</div><div class="academy-section-title"><div><span>KONUYA GÖRE</span><h2>Akademi kütüphanesi</h2></div></div><div class="academy-category-grid">'+categories+'</div><div class="academy-policy"><b>Kaynak ve mezhep ilkesi</b><p>Ana uygulama Hanefî anlatımı izler. Şafiî farklılığı bulunan yerde ayrı mavi kutu görünür. Kaynaksız kesin hüküm yazılmaz; telifli kitap metinleri kopyalanmaz.</p></div></div>'}
function render(){const r=root();if(!r)return;if(academyState.view==="home")r.innerHTML=homeHtml();else if(academyState.view==="category")renderCategory();else if(academyState.view==="lesson")renderLesson();}
function academyHome(noPush){if(!noPush&&academyState.view!=="home")pushState();academyState={view:"home",category:null,lesson:null,anchor:null};render();window.scrollTo(0,0)}
function academyOpenCategory(categoryId){if(!A().categories.some(c=>c.id===categoryId)){academyToast("Kategori bulunamadı");return academyHome(true)}pushState();academyState={view:"category",category:categoryId,lesson:null,anchor:null};render();window.scrollTo(0,0)}
function renderCategory(){const list=A().lessons.filter(x=>x.category===academyState.category);root().innerHTML='<div class="academy-page"><button class="academy-back" onclick="academyBack()">← Akademi</button><div class="academy-category-hero"><span>KATEGORİ</span><h2>'+esc(categoryTitle(academyState.category))+'</h2><p>'+list.length+' ayrıntılı ders · istediğin sırayla açabilirsin</p></div><div class="academy-path">'+list.map(lessonButton).join('')+'</div></div>'}
function academyOpenLesson(lessonId,anchor){const meta=lessonMeta(lessonId),lesson=H()[lessonId];if(!meta||!lesson){academyToast("Ders bulunamadı");return academyHome(true)}setScrollKey();pushState();academyState={view:"lesson",category:meta.category,lesson:lessonId,anchor:anchor||null};localStorage.setItem("academy.lastLesson",lessonId);render();const y=readJson("academy.scroll."+lessonId,0);setTimeout(()=>{if(anchor){const e=byId(anchor);if(e)e.scrollIntoView({block:"start"})}else if(Number.isFinite(y)&&y>0)window.scrollTo(0,y);else window.scrollTo(0,0)},0)}
function renderLesson(){const meta=lessonMeta(academyState.lesson),l=H()[academyState.lesson],sh=S()[academyState.lesson];if(!meta||!l)return academyHome(true);const list=A().lessons,idx=list.findIndex(x=>x.id===meta.id);const sections=(l.sections||[]).map((s,i)=>'<section class="academy-step" id="sec-'+i+'"><div class="academy-step-index">'+String(i+1).padStart(2,'0')+'</div><div class="academy-step-body"><h3>'+esc(s.title)+'</h3><p>'+esc(s.body)+'</p>'+(s.posture?postureSvg(s.posture):'')+'</div></section>').join('');const arabic=(l.arabicBlocks||[]).map((b,i)=>'<section class="academy-arabic"><div class="academy-arabic-head"><b>'+esc(b.name)+'</b><small>'+esc(b.when||'')+'</small></div><div class="academy-arabic-text" dir="rtl">'+esc(b.arabic)+'</div><div class="academy-pron"><b>Okunuş</b><p>'+esc(b.pronunciation)+'</p></div><div class="academy-meaning"><b>Anlam</b><p>'+esc(b.meaning)+'</p></div></section>').join('');const mistakes=(l.mistakes||[]).length?'<div class="academy-mistake"><div class="academy-callout-title">Sık yapılan hata</div><ul>'+l.mistakes.map(x=>'<li>'+esc(x)+'</li>').join('')+'</ul></div>':'';const hanafi='<div class="academy-hanafi"><div class="academy-callout-title">Hanefî ana anlatım</div><p>'+esc(l.hanafi||'Bu ders Hanefî ana anlatıma göre hazırlanmıştır.')+'</p></div>';const shafii=sh?'<div class="academy-shafii"><div class="academy-callout-title">'+esc(sh.title)+'</div><p>'+esc(sh.text)+'</p><small>Kaynak: '+esc(sh.source)+'</small></div>':'';const quiz=quizHtml(meta.id,l.quiz||[]);const done=completed().includes(meta.id);root().innerHTML='<article class="academy-page academy-detail"><button class="academy-back" onclick="academyBack()">← '+esc(categoryTitle(meta.category))+'</button><header class="academy-lesson-hero"><span>'+esc(categoryTitle(meta.category))+' · '+meta.minutes+' dk</span><h1>'+esc(meta.title)+'</h1><p>'+esc(l.summary)+'</p><div class="academy-lesson-progress"><i style="width:'+Math.round((idx+1)*100/list.length)+'%"></i></div></header><div class="academy-learnbox"><b>Bu derste ne öğreneceksin?</b><p>'+esc(l.summary)+'</p></div>'+sections+arabic+mistakes+hanafi+shafii+sourceList(l.sources||[])+quiz+'<button class="academy-complete '+(done?'done':'')+'" onclick="academyToggleComplete(\''+meta.id+'\')">'+(done?'✓ Ders tamamlandı':'Dersi tamamla')+'</button><div class="academy-prevnext"><button '+(idx<=0?'disabled':'')+' onclick="academyPreviousLesson()">← Önceki</button><button '+(idx>=list.length-1?'disabled':'')+' onclick="academyNextLesson()">Sonraki →</button></div></article>'}
function quizHtml(id,quiz){if(!quiz.length)return '';const state=readJson("academy.quiz",{});return '<section class="academy-quiz"><div class="academy-callout-title">Kendini kontrol et</div>'+quiz.map((q,qi)=>'<div class="academy-question"><b>'+(qi+1)+'. '+esc(q.q)+'</b>'+q.a.map((a,ai)=>'<button class="'+(state[id+":"+qi]===ai?'selected':'')+'" onclick="academyAnswer(\''+id+'\','+qi+','+ai+','+q.correct+')">'+esc(a)+'</button>').join('')+'</div>').join('')+'</section>'}
function academyAnswer(id,qi,ai,correct){const state=readJson("academy.quiz",{});state[id+":"+qi]=ai;writeJson("academy.quiz",state);academyToast(ai===correct?"Doğru ✓":"Tekrar gözden geçir");renderLesson()}
function academyToggleComplete(id){let list=completed();if(list.includes(id))list=list.filter(x=>x!==id);else list.push(id);writeJson("academy.completed",list);renderLesson()}
function academyBack(){setScrollKey();const prev=academyHistory.pop();if(!prev){return academyHome(true)}academyState=prev;render();window.scrollTo(0,0)}
function academyNextLesson(){const list=A().lessons,idx=list.findIndex(x=>x.id===academyState.lesson);if(idx>=0&&idx<list.length-1)academyOpenLesson(list[idx+1].id)}
function academyPreviousLesson(){const list=A().lessons,idx=list.findIndex(x=>x.id===academyState.lesson);if(idx>0)academyOpenLesson(list[idx-1].id)}
function academySearch(query){const box=byId("academySearchResults");if(!box)return;const q=String(query||"").trim().toLocaleLowerCase("tr-TR");if(q.length<2){box.innerHTML="";return}const hits=A().lessons.filter(m=>{const l=H()[m.id]||{};const text=[m.title,(m.keywords||[]).join(' '),l.summary,(l.sections||[]).map(x=>x.title+' '+x.body).join(' ')].join(' ').toLocaleLowerCase("tr-TR");return text.includes(q)}).slice(0,12);box.innerHTML='<div class="academy-search-results">'+(hits.length?hits.map((m,i)=>lessonButton(m,i)).join(''):'<p>Sonuç bulunamadı. Daha kısa bir kelime dene.</p>')+'</div>'}
function academyToggleReview(id){let x=readJson("academy.review",[]);x=x.includes(id)?x.filter(v=>v!==id):x.concat(id);writeJson("academy.review",x)}
function academyToast(msg){if(typeof window.toast==="function")window.toast(msg)}
function academyHandleAndroidBack(){if(!screenActive())return false;if(academyHistory.length){academyBack();return true}if(academyState.view!=="home"){academyHome(true);return true}if(typeof window._academyBaseShow==="function"){window._academyBaseShow("home");return true}return false}
function academyResume(){const r=root();if(!r)return;render()}
window.academyHome=academyHome;window.academyOpenCategory=academyOpenCategory;window.academyOpenLesson=academyOpenLesson;window.academyBack=academyBack;window.academyNextLesson=academyNextLesson;window.academyPreviousLesson=academyPreviousLesson;window.academySearch=academySearch;window.academyToggleComplete=academyToggleComplete;window.academyAnswer=academyAnswer;window.academyToggleReview=academyToggleReview;window.academyHandleAndroidBack=academyHandleAndroidBack;
function hookShow(){if(typeof window.show!=="function")return setTimeout(hookShow,30);if(window._academyBaseShow)return;window._academyBaseShow=window.show;window.show=function(id){window._academyBaseShow(id);if(id==="learn")academyResume();};academyResume()}
if(document.readyState==="loading")document.addEventListener("DOMContentLoaded",hookShow);else hookShow();
window.addEventListener("scroll",()=>{if(screenActive()&&academyState.lesson)setScrollKey()},{passive:true});
})();'''

ACADEMY_CSS = r'''/* Namaz V6 Academy */
:root{--ac-emerald:#0b5f49;--ac-emerald-2:#0f765b;--ac-deep:#083e32;--ac-cream:#f7f3e8;--ac-gold:#c39a51;--ac-ink:#172a24;--ac-muted:#65756d;--ac-border:#dce5df;--ac-blue:#2d628b;--ac-blue-bg:#eef6fb;--ac-warn:#8a562b;--ac-warn-bg:#fff6e9}
#learn{background:linear-gradient(180deg,#eef6f1 0,#f8f6ef 310px,var(--bg) 620px);padding-bottom:calc(148px + env(safe-area-inset-bottom))!important;min-height:100vh}
[data-theme="dark"] #learn,.theme-dark #learn{--ac-cream:#18221f;--ac-ink:#eef6f1;--ac-muted:#a8b6af;--ac-border:#33453d;--ac-blue-bg:#172936;--ac-warn-bg:#30251d;background:#101916;color:#eef6f1}
.academy-shell{min-height:100vh}.academy-top{padding:24px 18px 22px;background:linear-gradient(145deg,var(--ac-deep),var(--ac-emerald-2));color:#fff;border-radius:0 0 34px 34px;position:relative;overflow:hidden}.academy-top:after{content:"";position:absolute;width:190px;height:190px;border:1px solid rgba(255,255,255,.12);border-radius:50%;right:-80px;top:-95px}.academy-top .academy-brand{font-size:11px;letter-spacing:.16em;font-weight:900;opacity:.72}.academy-top h1{margin:6px 0 6px;font-size:31px;letter-spacing:-1px}.academy-top p{margin:0;max-width:34em;font-size:12.5px;line-height:1.55;opacity:.82}.academy-badge{display:inline-flex;margin-top:12px;padding:7px 10px;border:1px solid rgba(255,255,255,.18);border-radius:999px;background:rgba(255,255,255,.1);font-size:10px;font-weight:800}
#academyRoot{padding:14px 14px calc(148px + env(safe-area-inset-bottom));color:var(--ac-ink)}.academy-page{max-width:760px;margin:0 auto}.academy-progress-card{display:grid;grid-template-columns:1fr auto;gap:10px;background:linear-gradient(145deg,#fff9ea,#f4ead2);border:1px solid #eadab8;border-radius:24px;padding:18px;box-shadow:0 13px 34px rgba(34,70,57,.07);color:#213a31}.academy-kicker{display:block;font-size:9px;letter-spacing:.14em;font-weight:900;color:#82683a}.academy-progress-card strong{display:block;font-size:20px;margin-top:4px}.academy-progress-card small{display:block;margin-top:5px;color:#63746b;line-height:1.4}.academy-progress-number{font-size:25px;font-weight:950;color:var(--ac-emerald)}.academy-progress-track{grid-column:1/-1;height:9px;background:rgba(12,95,73,.11);border-radius:999px;overflow:hidden}.academy-progress-track i,.academy-lesson-progress i{display:block;height:100%;background:linear-gradient(90deg,var(--ac-emerald),#21a27c);border-radius:inherit}
.academy-search{height:50px;display:flex;align-items:center;gap:9px;margin:12px 0;background:var(--card);border:1px solid var(--ac-border);border-radius:17px;padding:0 13px}.academy-search span{font-size:20px;color:var(--ac-emerald)}.academy-search input{width:100%;height:100%;border:0;outline:0;background:transparent;color:var(--text);font-size:14px}.academy-search-results{background:var(--card);border:1px solid var(--ac-border);border-radius:20px;padding:10px;margin:-4px 0 12px}.academy-continue{width:100%;min-height:66px;border:0;border-radius:20px;background:linear-gradient(135deg,var(--ac-emerald),var(--ac-emerald-2));color:#fff;padding:14px 16px;display:flex;align-items:center;justify-content:space-between;text-align:left}.academy-continue span small,.academy-continue span b{display:block}.academy-continue span small{font-size:9px;letter-spacing:.12em;opacity:.7}.academy-continue span b{font-size:14px;margin-top:3px}.academy-continue i{font-style:normal;font-size:11px;font-weight:900}
.academy-section-title{display:flex;justify-content:space-between;align-items:flex-end;gap:10px;margin:22px 2px 10px}.academy-section-title span,.academy-category-hero>span,.academy-lesson-hero>span{font-size:9px;letter-spacing:.13em;font-weight:900;color:var(--ac-emerald)}.academy-section-title h2{font-size:19px;margin:4px 0 0}.academy-section-title button{border:0;background:transparent;color:var(--ac-emerald);font-weight:900;min-height:44px}.academy-path{display:grid;gap:8px}.academy-lesson-card{width:100%;min-height:72px;border:1px solid var(--ac-border);border-radius:19px;background:var(--card);padding:11px 12px;display:flex;align-items:center;gap:11px;color:var(--text);text-align:left}.academy-lesson-card:active{transform:scale(.992)}.academy-lesson-no{width:39px;height:39px;border-radius:13px;background:color-mix(in srgb,var(--ac-emerald) 10%,var(--card));color:var(--ac-emerald);display:grid;place-items:center;font-size:11px;font-weight:950;flex:0 0 auto}.academy-lesson-card.is-done .academy-lesson-no{background:var(--ac-emerald);color:#fff}.academy-lesson-copy{flex:1;min-width:0}.academy-lesson-copy b,.academy-lesson-copy small{display:block}.academy-lesson-copy b{font-size:13px;line-height:1.35}.academy-lesson-copy small{font-size:10px;color:var(--muted);margin-top:4px}.academy-chevron{font-size:22px;color:var(--muted)}
.academy-category-grid{display:grid;grid-template-columns:1fr 1fr;gap:8px}.academy-category{min-height:125px;border:1px solid var(--ac-border);border-radius:21px;background:var(--card);color:var(--text);padding:14px;text-align:left}.academy-category span{display:grid;width:35px;height:35px;border-radius:12px;place-items:center;background:var(--soft);color:var(--ac-emerald);font-size:10px;font-weight:950}.academy-category b,.academy-category small{display:block}.academy-category b{font-size:14px;margin-top:13px}.academy-category small{font-size:10px;color:var(--muted);margin-top:5px;line-height:1.35}.academy-policy{margin-top:14px;padding:15px;border:1px solid color-mix(in srgb,var(--ac-emerald) 24%,var(--ac-border));background:color-mix(in srgb,var(--ac-emerald) 6%,var(--card));border-radius:19px}.academy-policy b{font-size:12px;color:var(--ac-emerald)}.academy-policy p{font-size:11px;line-height:1.6;color:var(--muted);margin:6px 0 0}
.academy-back{min-height:44px;border:0;background:transparent;color:var(--ac-emerald);font-weight:900;padding:4px 2px;margin-bottom:5px}.academy-category-hero{background:var(--card);border:1px solid var(--ac-border);border-radius:24px;padding:18px;margin-bottom:12px}.academy-category-hero h2{font-size:25px;margin:5px 0}.academy-category-hero p{font-size:11px;color:var(--muted);margin:0}.academy-lesson-hero{background:linear-gradient(145deg,var(--ac-deep),var(--ac-emerald));border-radius:26px;padding:21px;color:#fff}.academy-lesson-hero>span{color:#d1e7df}.academy-lesson-hero h1{font-size:27px;line-height:1.12;letter-spacing:-.6px;margin:7px 0 9px}.academy-lesson-hero p{font-size:12px;line-height:1.58;opacity:.84;margin:0}.academy-lesson-progress{height:6px;background:rgba(255,255,255,.15);border-radius:99px;margin-top:16px;overflow:hidden}.academy-lesson-progress i{background:linear-gradient(90deg,#e3c174,#fff0b2)}.academy-learnbox{margin:10px 0;padding:15px;border:1px solid #e6d7b6;background:#fff8e9;border-radius:19px;color:#3f3a2b}.academy-learnbox b{font-size:11px;color:#866728}.academy-learnbox p{font-size:12px;line-height:1.6;margin:5px 0 0}
.academy-step{display:grid;grid-template-columns:37px 1fr;gap:10px;padding:15px 0;border-bottom:1px solid var(--ac-border)}.academy-step-index{width:34px;height:34px;border-radius:11px;display:grid;place-items:center;background:var(--soft);color:var(--ac-emerald);font-size:10px;font-weight:950}.academy-step-body h3{font-size:16px;margin:4px 0 7px}.academy-step-body p{font-size:13px;line-height:1.72;color:var(--text);margin:0}.academy-posture{margin:14px 0 0;padding:12px;background:var(--card);border:1px solid var(--ac-border);border-radius:18px;text-align:center}.academy-posture svg{width:140px;max-width:60%;height:145px}.academy-posture g{fill:none;stroke:var(--ac-emerald);stroke-width:6;stroke-linecap:round;stroke-linejoin:round}.academy-posture figcaption{font-size:10px;color:var(--muted);font-weight:800}
.academy-arabic{margin:12px 0;border:1px solid var(--ac-border);border-radius:22px;background:var(--card);overflow:hidden}.academy-arabic-head{padding:13px 15px;background:var(--soft)}.academy-arabic-head b,.academy-arabic-head small{display:block}.academy-arabic-head b{font-size:14px}.academy-arabic-head small{font-size:9px;color:var(--muted);margin-top:3px}.academy-arabic-text{font-family:"Noto Naskh Arabic","Amiri",serif;font-size:27px;line-height:2;text-align:right;padding:17px 16px;border-bottom:1px solid var(--ac-border)}.academy-pron,.academy-meaning{padding:12px 15px}.academy-pron{border-bottom:1px solid var(--ac-border)}.academy-pron b,.academy-meaning b{font-size:9px;letter-spacing:.08em;color:var(--ac-emerald)}.academy-pron p,.academy-meaning p{font-size:12px;line-height:1.65;margin:5px 0 0}
.academy-mistake,.academy-hanafi,.academy-shafii,.academy-sources,.academy-quiz{margin:12px 0;padding:15px;border-radius:19px}.academy-callout-title{font-size:10px;font-weight:950;letter-spacing:.08em;margin-bottom:6px}.academy-mistake{background:var(--ac-warn-bg);border:1px solid #ead1af;color:var(--text)}.academy-mistake .academy-callout-title{color:var(--ac-warn)}.academy-mistake ul,.academy-sources ul{margin:7px 0 0;padding-left:18px}.academy-mistake li,.academy-sources li{font-size:11.5px;line-height:1.55;margin:4px 0}.academy-hanafi{background:color-mix(in srgb,var(--ac-emerald) 7%,var(--card));border:1px solid color-mix(in srgb,var(--ac-emerald) 25%,var(--ac-border))}.academy-hanafi .academy-callout-title{color:var(--ac-emerald)}.academy-hanafi p,.academy-shafii p{font-size:12px;line-height:1.65;margin:0}.academy-shafii{background:var(--ac-blue-bg);border:1px solid color-mix(in srgb,var(--ac-blue) 28%,var(--ac-border))}.academy-shafii .academy-callout-title{color:var(--ac-blue)}.academy-shafii small{display:block;margin-top:8px;color:var(--muted);font-size:9px;line-height:1.45}.academy-sources{background:var(--card);border:1px solid var(--ac-border)}.academy-sources .academy-callout-title{color:var(--ac-gold)}.academy-sources p{font-size:10px;color:var(--muted);line-height:1.5;margin:9px 0 0}
.academy-quiz{background:var(--card);border:1px solid var(--ac-border)}.academy-question{padding:11px 0;border-bottom:1px solid var(--ac-border)}.academy-question:last-child{border:0}.academy-question>b{display:block;font-size:12px;margin-bottom:7px}.academy-question button{width:100%;min-height:44px;border:1px solid var(--ac-border);border-radius:13px;background:var(--soft);color:var(--text);text-align:left;padding:9px 11px;margin:4px 0;font-size:11px}.academy-question button.selected{border-color:var(--ac-emerald);box-shadow:0 0 0 1px var(--ac-emerald)}.academy-complete{width:100%;min-height:52px;border:0;border-radius:17px;background:var(--ac-emerald);color:#fff;font-weight:900;margin-top:5px}.academy-complete.done{background:#365b50}.academy-prevnext{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:9px}.academy-prevnext button{min-height:48px;border:1px solid var(--ac-border);border-radius:15px;background:var(--card);color:var(--text);font-weight:800}.academy-prevnext button:disabled{opacity:.35}
[data-theme="dark"] .academy-progress-card,.theme-dark .academy-progress-card{background:#23251d;border-color:#4a4633;color:#f1f4eb}[data-theme="dark"] .academy-learnbox,.theme-dark .academy-learnbox{background:#2d281d;border-color:#574b2d;color:#f4eddc}
@media(max-width:390px){#academyRoot{padding-left:11px;padding-right:11px}.academy-top{padding-left:15px;padding-right:15px}.academy-top h1{font-size:27px}.academy-category-grid{grid-template-columns:1fr}.academy-lesson-hero h1{font-size:23px}.academy-arabic-text{font-size:24px}.academy-step{grid-template-columns:32px 1fr}.academy-step-index{width:30px;height:30px}.academy-prevnext{grid-template-columns:1fr}}
body.elder #learn .academy-step-body p,body.elder #learn .academy-pron p,body.elder #learn .academy-meaning p{font-size:17px;line-height:1.75}body.elder #learn .academy-category-grid{grid-template-columns:1fr}body.elder #learn .academy-lesson-card{min-height:86px}
'''

LEARN_SHELL = r'''<section id="learn" class="screen">
  <div class="academy-shell">
    <header class="academy-top">
      <div class="academy-brand">NAMAZ V6 · ÖĞRENME MERKEZİ</div>
      <h1>Namaz Akademisi</h1>
      <p>Dini hiç bilmeyen biri için temelden başlayan, gerçek derslerle ilerleyen öğrenme yolu. Dersler uygulamanın içinde kalır; başka menülere atmaz.</p>
      <div class="academy-badge">Hanefî ana anlatım · Şafiî farkları ayrı gösterilir</div>
    </header>
    <main id="academyRoot"><div class="academy-policy"><b>Akademi hazırlanıyor…</b><p>Ders verileri yerel olarak yükleniyor.</p></div></main>
  </div>
</section>'''


def asset_files() -> dict[str, str]:
    return {
        "academy/academy-data.js": ACADEMY_DATA,
        "academy/lessons-hanafi.js": LESSONS_HANAFI,
        "academy/lessons-shafii-notes.js": SHAFII_NOTES,
        "academy/academy.js": ACADEMY_JS,
        "academy/academy.css": ACADEMY_CSS,
    }


def learn_section(html: str) -> str:
    m = re.search(r'<section id="learn" class="screen[^>]*>.*?</section>', html, re.S)
    return m.group(0) if m else ""


def upgrade_html(html: str) -> str:
    if 'id="learn"' in html:
        html = re.sub(r'<section id="learn" class="screen[^>]*>.*?</section>', LEARN_SHELL.strip(), html, count=1, flags=re.S)
    else:
        anchor = '<section id="ibadet" class="screen">'
        if anchor in html:
            html = html.replace(anchor, LEARN_SHELL.strip() + "\n" + anchor, 1)
        else:
            html = html.replace('</div>\n<nav class="nav">', LEARN_SHELL.strip() + '\n</div>\n<nav class="nav">', 1)
    if 'academy/academy.css' not in html:
        html = html.replace('</head>', '<link rel="stylesheet" href="academy/academy.css">\n</head>', 1)
    if 'academy/academy.js' not in html:
        scripts = '\n'.join([
            '<script src="academy/academy-data.js"></script>',
            '<script src="academy/lessons-hanafi.js"></script>',
            '<script src="academy/lessons-shafii-notes.js"></script>',
            '<script src="academy/academy.js"></script>',
        ])
        html = html.replace('</body>', scripts + '\n</body>', 1)
    return html


def validate_assets(files: dict[str, str]) -> None:
    expected = {
        "academy/academy-data.js",
        "academy/lessons-hanafi.js",
        "academy/lessons-shafii-notes.js",
        "academy/academy.js",
        "academy/academy.css",
    }
    if set(files) != expected:
        raise AssertionError(f"academy asset set mismatch: {sorted(files)}")
    joined = "\n".join(files.values())
    must = [
        'temel', 'temizlik', 'namaz-yapisi', 'okunanlar', 'bes-vakit', 'cemaat', 'ozel', 'diger-ibadetler', 'ahlak',
        'islam-ve-iman', '32-farz', 'abdest', 'gusul', 'teyemmum', 'namazin-sartlari', 'namazin-hareketleri', 'namazda-okunanlar',
        'sabah-namazi', 'ogle-namazi', 'ikindi-namazi', 'aksam-namazi', 'yatsi-namazi',
        'sehiv-secdesi', 'seferilik', 'hastalikta-namaz', 'aile', 'ticaret', 'borc-kul-hakki', 'giybet', 'ofke', 'mahremiyet-sosyal-medya',
        'Şafiî', 'Kaynak', 'academy.completed', 'academy.lastLesson', 'academy.quiz', 'academy.review', 'academy.scroll',
        'function academyHome(', 'function academyOpenCategory(', 'function academyOpenLesson(', 'function academyBack(', 'function academyHandleAndroidBack(',
        'env(safe-area-inset-bottom)', '@media(max-width:390px)', '[data-theme="dark"]', '.academy-shafii', '.academy-arabic',
        '2 rekât sünnet', '2 rekât farz', '4 rekât ilk sünnet', '4 rekât farz', '2 rekât son sünnet', '3 rekât farz', 'Vitir', 'Şimdi ne yapıyorum?',
        'Sübhaneke', 'Fâtiha', 'İhlâs', 'Kevser', 'Ettehiyyâtü', 'Allahümme Salli', 'Allahümme Bârik', 'Rabbenâ Âtinâ',
        'arabic:', 'pronunciation:', 'meaning:'
    ]
    missing = [x for x in must if x not in joined]
    if missing:
        raise AssertionError("missing academy tokens: " + ", ".join(missing))
    js = files["academy/academy.js"]
    for bad in ["show('knowledge')", "show('duas')", "show('namazHocasi')"]:
        if bad in js:
            raise AssertionError(f"legacy route in academy controller: {bad}")
    # Every lesson object in the Hanefi file must have a source list.
    lesson_count = len(re.findall(r'^"[a-z0-9-]+"\s*:\s*\{', files["academy/lessons-hanafi.js"], re.M))
    source_count = files["academy/lessons-hanafi.js"].count('sources:[')
    if lesson_count < 30 or source_count != lesson_count:
        raise AssertionError(f"insufficient sourced lessons: lessons={lesson_count}, sources={source_count}")


def apply(root: Path) -> None:
    asset_root = root / 'app/src/main/assets'
    index = asset_root / 'index.html'
    if not index.exists():
        raise FileNotFoundError(index)
    index.write_text(upgrade_html(index.read_text(encoding='utf-8')), encoding='utf-8')
    files = asset_files()
    validate_assets(files)
    for rel, text in files.items():
        p = asset_root / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(text, encoding='utf-8')


def self_test(root: Path) -> None:
    asset_root = root / 'app/src/main/assets'
    index = (asset_root / 'index.html').read_text(encoding='utf-8')
    section = learn_section(index)
    if 'id="academyRoot"' not in section:
        raise AssertionError('academy root missing from Learn screen')
    for bad in ["show('knowledge')", "show('duas')", "show('namazHocasi')"]:
        if bad in section:
            raise AssertionError(f'legacy route remains in Learn screen: {bad}')
    files = {}
    for rel in [
        'academy/academy-data.js','academy/lessons-hanafi.js','academy/lessons-shafii-notes.js','academy/academy.js','academy/academy.css'
    ]:
        p = asset_root / rel
        if not p.is_file():
            raise AssertionError(f'missing generated asset: {rel}')
        files[rel] = p.read_text(encoding='utf-8')
    validate_assets(files)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument('--apply')
    ap.add_argument('--self-test')
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
