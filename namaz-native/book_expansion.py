#!/usr/bin/env python3
from __future__ import annotations

import argparse
from pathlib import Path

EXTRA = r'''
<details><summary>32 Farz Öğretim Şeması</summary><div class="guide-body">
<p>Türkiye'deki geleneksel temel din eğitiminde “32 Farz”, Müslümanın önce öğrenmesi istenen farzları topluca hatırlatmak için kullanılan bir öğretim şemasıdır. Ayrı bir ibadet veya yeni bir iman şartı değildir.</p>
<ul>
<li><b>İmanın 6 esası:</b> Allah'a, meleklere, kitaplara, peygamberlere, âhiret gününe ve kadere iman.</li>
<li><b>İslâmın 5 esası:</b> Kelime-i şehadet, namaz, zekât, oruç ve hac.</li>
<li><b>Abdestin 4 farzı:</b> Yüzü yıkamak; kolları dirseklerle yıkamak; başın gerekli kısmını mesh etmek; ayakları topuklarla yıkamak.</li>
<li><b>Guslün 3 farzı (Hanefî öğretiminde):</b> Ağza su vermek, burna su vermek, bütün bedeni kuru yer kalmayacak şekilde yıkamak.</li>
<li><b>Teyemmümün 2 farzı (Hanefî öğretiminde):</b> Niyet ve temiz toprak/toprak cinsinden yüzeyle yüz ile kolları usulünce mesh etmek.</li>
<li><b>Namazın 12 farzı:</b> Altısı namazdan önceki şartlar; altısı namazın içindeki rükünler olarak öğretilir.</li>
</ul>
<div class="fiqh-note"><b>Mezhep notu:</b> Bu sayılandırma özellikle Hanefî temel öğretim geleneğinde yaygındır. Şafiî fıkhında abdest, gusül, teyemmüm ve namazın rükünlerinin tasnifi/sayımı farklı olabilir.</div>
<div class="guide-source">Kaynak kontrolü: Diyanet İşleri Başkanlığı'nın “32 farz nedir?” açıklaması ve DİB Namaz İlmihali. Bu metin uygulama için özgün özetlenmiştir.</div>
</div></details>

<details><summary>Namazın 12 Farzı: Dışındaki ve İçindeki Şartlar</summary><div class="guide-body">
<p><b>Namazdan önceki altı şart:</b> Hadesten taharet, necasetten taharet, setr-i avret, istikbal-i kıble, vakit ve niyet.</p>
<p><b>Namazın içindeki altı temel rükün (Hanefî yaygın öğretimi):</b> İftitah tekbiri, kıyam, kıraat, rükû, secde ve son oturuş.</p>
<p>Bu başlıklar “ezber listesi” olarak değil, namazı doğru hazırlayıp doğru icra etmeyi anlamak için açıklanır. Hastalık, yolculuk ve zaruret hâllerinde bazı uygulama biçimleri değişebilir.</p>
<div class="guide-source">Kaynak: DİB Namaz İlmihali — namazın şartları ve rükünleri.</div>
</div></details>

<details><summary>Allah’ın Sıfatları ve Temel İnanç Dili</summary><div class="guide-body">
<p>Namaz hocası geleneğinde ibadetin yanında temel inanç bilgileri de öğretilir. Allah'ın hiçbir yaratılmışa benzemediği, ezelî ve ebedî olduğu; bir olduğu; hayat, ilim, irade, kudret, işitme, görme ve kelâm gibi sıfatlarla nitelenmesinin ne anlama geldiği temel ilmihal dilinde ele alınır.</p>
<p>Kelâm ekollerinde sıfatların isimlendirilmesi ve tasnifinde terminoloji farklılıkları olabilir. Uygulama bu alanı ayrıntılı mezhep/kelâm tartışması yerine temel kavram düzeyinde sunar.</p>
<div class="guide-source">Kaynak: Diyanet İlmihal, “Allah'ın sıfatları” başlığı; Kur'an: İhlâs 112:1-4, Şûrâ 42:11.</div>
</div></details>

<details><summary>Ef‘âl-i Mükellefîn ve Dinî Hüküm Terimleri</summary><div class="guide-body">
<p>İlmihallerde davranışlar dinî hüküm bakımından bazı temel kavramlarla açıklanır:</p>
<ul>
<li><b>Farz:</b> Yapılması kesin olarak istenen yükümlülük.</li>
<li><b>Vacip:</b> Hanefî usulünde farzdan farklı delil derecesiyle zorunlu kabul edilen fiil; diğer mezheplerde aynı ayrım kullanılmayabilir.</li>
<li><b>Sünnet:</b> Hz. Peygamber'in örnekliğiyle sabit olan uygulama ve davranışlar.</li>
<li><b>Müstehap / mendup:</b> Yapılması teşvik edilen, terkinde günah hükmü verilmeyen davranışlar için kullanılan terimler.</li>
<li><b>Mubah:</b> Aslen yapılıp yapılmaması serbest olan alan.</li>
<li><b>Mekruh:</b> Terk edilmesi istenen veya hoş görülmeyen davranışlar; Hanefî terminolojisinde türleri vardır.</li>
<li><b>Haram:</b> Yapılması kesin olarak yasaklanan fiil.</li>
</ul>
<div class="fiqh-note">Bir davranışa hüküm vermek delil ve fıkıh bilgisi gerektirir. Uygulama bu kavramları öğretir; kişisel durumlar için otomatik fetva üretmez.</div>
<div class="guide-source">Kaynak: Diyanet İlmihal — dinî hükümler / ef‘âl-i mükellefîn başlıkları.</div>
</div></details>

<details><summary>Temel Dinî Kavramlar</summary><div class="guide-body">
<p><b>Rükün</b>, bir ibadetin yapısını oluşturan temel unsur; <b>şart</b>, ibadetin geçerliliği için önceden veya dışarıdan bulunması gereken unsur; <b>sahih</b>, gerekli şartları taşıyan; <b>fasit/batıl</b> gibi terimler ise fıkıh ekollerine göre ayrıntılı kullanım farkları bulunan kavramlardır.</p>
<p><b>Kaza</b>, vaktinde yerine getirilemeyen ibadetin sonradan edası; <b>eda</b>, ibadetin kendi vaktinde yapılması; <b>cemaat</b>, namazın imama uyularak topluca kılınması bağlamında kullanılır.</p>
<div class="guide-source">Kaynak: Diyanet İlmihal — temel fıkıh ve ibadet terimleri.</div>
</div></details>

<details><summary>Misvak ve Giyim Adabı</summary><div class="guide-body">
<p><b>Misvak</b>, ağız ve diş temizliğinde kullanılan geleneksel bir araçtır. Buradaki ana amaç temizlik ve ağız bakımına özen göstermektir; modern hijyen araçlarıyla ağız temizliği de ayrıca sürdürülmelidir.</p>
<p><b>Sarık ve benzeri geleneksel kıyafetler</b> tarihî ve kültürel uygulamalar olarak ele alınabilir; ancak sarık takmak namazın geçerlilik şartı değildir. Namaz açısından temel konu, örtünme şartlarının ve temizlik ölçülerinin sağlanmasıdır.</p>
<div class="guide-source">Kaynak kontrolü: DİB Namaz İlmihali'nin namazın şartları/temizlik bölümleri. Bu başlık, geleneksel namaz hocası kitaplarında yer alan konunun yanlış anlaşılmasını önlemek için açıklayıcı biçimde özetlenmiştir.</div>
</div></details>

<details><summary>Okuma Parçaları, Risaleler ve Kaynak Güvenliği</summary><div class="guide-body">
<p>Bazı <i>Tam Namaz Hocası</i> baskılarında namazın önemiyle ilgili risaleler ve farklı eserlerden seçmeler bulunur. Telifli risale metinleri birebir aktarılmaz. Uygulama bunun yerine Kur'an ayetleri, açık kaynak/izinli içerikler ve resmî ilmihal kaynaklarından hazırlanmış özgün özetler gösterir.</p>
<p>Bir metin mezhebî yorum, tasavvufî yaklaşım veya belirli bir âlimin görüşü ise bu nitelik açıkça etiketlenmelidir; “İslam'da tartışmasız tek görüş budur” şeklinde sunulmamalıdır.</p>
<div class="guide-source">Kaynak ilkesi: Kur'an referansı + Diyanet ilmihal/fetva kaynak kontrolü; üçüncü taraf telifli eserlerde yalnız konu başlığı ve izinli kısa atıf.</div>
</div></details>
'''


def expand_html(html: str) -> str:
    if '32 Farz Öğretim Şeması' in html:
        return html
    anchor = '<details open><summary>Namazın Yeri ve Önemi</summary>'
    return html.replace(anchor, EXTRA + '\n' + anchor, 1)


def apply(root: Path) -> None:
    path = root / 'app/src/main/assets/index.html'
    path.write_text(expand_html(path.read_text(encoding='utf-8')), encoding='utf-8')


def self_test(root: Path) -> None:
    html = (root / 'app/src/main/assets/index.html').read_text(encoding='utf-8')
    for token in ['32 Farz Öğretim Şeması', 'Allah’ın Sıfatları', 'Ef‘âl-i Mükellefîn', 'Misvak ve Giyim Adabı', 'Telifli risale metinleri birebir aktarılmaz']:
        if token not in html:
            raise SystemExit('missing book expansion token: ' + token)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument('--apply', metavar='ROOT')
    ap.add_argument('--self-test', metavar='ROOT')
    a = ap.parse_args()
    if a.apply:
        apply(Path(a.apply))
    if a.self_test:
        self_test(Path(a.self_test))
    if not a.apply and not a.self_test:
        ap.error('use --apply ROOT and/or --self-test ROOT')
    return 0

if __name__ == '__main__':
    raise SystemExit(main())
