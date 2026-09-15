package app.namaz.tr.v8.worship

data class SourcedItem(
    val title: String,
    val body: String,
    val source: String,
)

data class DuaItem(
    val title: String,
    val arabic: String,
    val pronunciation: String,
    val meaning: String,
    val source: String,
)

data class EsmaEntry(val name: String, val meaning: String)

object WorshipCatalog {
    val duas = listOf(
        DuaItem("Rabbimiz, bize iyilik ver", "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ", "Rabbenâ âtinâ fid-dünyâ haseneten ve fil-âhireti haseneten ve kınâ azâben-nâr.", "Rabbimiz, bize dünyada da ahirette de iyilik ver; bizi ateş azabından koru.", "Kur’an: Bakara 2:201"),
        DuaItem("İlim duası", "رَبِّ زِدْنِي عِلْمًا", "Rabbi zidnî ilmâ.", "Rabbim, ilmimi artır.", "Kur’an: Tâhâ 20:114"),
        DuaItem("Anne-baba için", "رَبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا", "Rabbirhamhumâ kemâ rabbeyânî sagîrâ.", "Rabbim, onlar beni küçükken yetiştirdikleri gibi sen de onlara merhamet et.", "Kur’an: İsrâ 17:24"),
        DuaItem("Kalbi doğrulukta tutma", "رَبَّنَا لَا تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا", "Rabbenâ lâ tuziğ kulûbenâ ba‘de iz hedeytenâ.", "Rabbimiz, bize hidayet verdikten sonra kalplerimizi eğriltme.", "Kur’an: Âl-i İmrân 3:8"),
        DuaItem("Sıkıntıda teslimiyet", "حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ", "Hasbunallâhu ve ni‘mel vekîl.", "Allah bize yeter; O ne güzel vekildir.", "Kur’an: Âl-i İmrân 3:173"),
        DuaItem("Bağışlanma duası", "رَبَّنَا ظَلَمْنَا أَنْفُسَنَا وَإِنْ لَمْ تَغْفِرْ لَنَا وَتَرْحَمْنَا لَنَكُونَنَّ مِنَ الْخَاسِرِينَ", "Rabbenâ zalemnâ enfusenâ ve in lem tağfir lenâ ve terhamnâ lenekûnenne minel hâsirîn.", "Rabbimiz, kendimize zulmettik; bizi bağışlamaz ve bize merhamet etmezsen kaybedenlerden oluruz.", "Kur’an: A‘râf 7:23"),
        DuaItem("Yolculuk duası özeti", "سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَذَا", "Subhânellezî sehhara lenâ hâzâ.", "Bunu hizmetimize veren Allah’ı noksanlıklardan tenzih ederiz.", "Kur’an: Zuhruf 43:13; Müslim, Hac 425"),
        DuaItem("Kolaylık duası", "رَبِّ اشْرَحْ لِي صَدْرِي وَيَسِّرْ لِي أَمْرِي", "Rabbişrah lî sadrî ve yessir lî emrî.", "Rabbim, göğsümü genişlet ve işimi kolaylaştır.", "Kur’an: Tâhâ 20:25-26"),
    )

    val hadiths = listOf(
        SourcedItem("Niyet", "Amellerin değerinin niyetle bağlantılı olduğu öğretilir; kişi yaptığı işteki niyetinden sorumludur.", "Buhârî, Bed’ü’l-vahy 1; Müslim, İmâre 155"),
        SourcedItem("Kolaylaştırmak", "Dini anlatımda insanlara kolaylık göstermek, gereksiz zorluk ve nefret oluşturmamak teşvik edilir.", "Buhârî, İlim 11; Müslim, Cihâd 6"),
        SourcedItem("Merhamet", "Merhametli davranmanın ilahî rahmetle ilişkisi vurgulanır; canlılara şefkat güzel ahlakın parçasıdır.", "Tirmizî, Birr 16"),
        SourcedItem("Komşuluk", "Komşuya iyi davranmak iman ve ahlak sorumluluğunun önemli bir parçası olarak öğretilir.", "Buhârî, Edeb 31; Müslim, Îmân 74"),
        SourcedItem("Temizlik", "Temizlik ve arınmanın iman hayatındaki değeri güçlü biçimde vurgulanır.", "Müslim, Tahâret 1"),
        SourcedItem("Güzel söz", "İnsanlara faydalı ve güzel söz söylemek, zarar vermekten kaçınmak ahlaki bir sorumluluk olarak aktarılır.", "Buhârî, Edeb 31; Müslim, Îmân 74"),
    )

    val guides = listOf(
        SourcedItem("Ramazan", "İmsak-iftar takibi, oruç kaydı, mukabele/hatim hedefi ve teravih takibi aynı merkezde yönetilir. Sağlık veya kişisel fetva gerektiren durumlarda uygulama kesin hüküm üretmez.", "Kur’an: Bakara 2:183-187; Diyanet İlmihal I, Oruç"),
        SourcedItem("Hac/Umre", "İhram hazırlığı, niyet, telbiye, tavaf, sa‘y ve ihramdan çıkış; hacda ayrıca Mina, Arafat, Müzdelife ve ilgili menasik adım adım izlenir.", "Diyanet Hac ve Umre Rehberi"),
        SourcedItem("Seyahatte ibadet", "Şehir değiştiğinde vakitler güncellenebilir. Uygulama kişiyi otomatik seferî ilan etmez; mesafe ve kalış niyeti gibi ölçüleri kaynaklı rehber olarak gösterir.", "Diyanet İlmihal I, Seferîlik"),
        SourcedItem("Kadınlara özel kayıt", "Hayız/nifas dönemleri yalnız kullanıcının isteğiyle yerel olarak kaydedilir. Bu kayıtlar namaz takibinde yanlış eksik uyarılarını önlemek içindir; uygulama kişisel fetva üretmez.", "Diyanet İlmihal I, Kadınlara Mahsus Haller"),
    )

    val esma: List<EsmaEntry> = listOf(
        "Allah|Bütün kemal sıfatları kendinde toplayan özel isim", "Er-Rahmân|Rahmeti bütün varlığı kuşatan", "Er-Rahîm|Merhameti sürekli olan", "El-Melik|Mülkün gerçek sahibi", "El-Kuddûs|Her eksiklikten uzak", "Es-Selâm|Esenliğin kaynağı", "El-Mü’min|Güven veren", "El-Müheymin|Gözetip koruyan", "El-Azîz|Mutlak üstün", "El-Cebbâr|Kudreti karşı konulmaz", "El-Mütekebbir|Büyüklükte eşsiz", "El-Hâlık|Yaratan", "El-Bâri|Kusursuzca var eden", "El-Musavvir|Şekil ve özellik veren", "El-Gaffâr|Çok bağışlayan", "El-Kahhâr|Her şeye galip", "El-Vehhâb|Karşılıksız bağışlayan", "Er-Rezzâk|Rızık veren", "El-Fettâh|Hayır kapılarını açan", "El-Alîm|Her şeyi bilen", "El-Kâbıd|Daraltan", "El-Bâsıt|Genişleten", "El-Hâfıd|Alçaltan", "Er-Râfi|Yükselten", "El-Muiz|İzzet veren", "El-Müzil|Zillete düşüren", "Es-Semî|Her şeyi işiten", "El-Basîr|Her şeyi gören", "El-Hakem|Hükmeden", "El-Adl|Mutlak adalet sahibi", "El-Latîf|Lütfu ince ve derin olan", "El-Habîr|Her şeyden haberdar", "El-Halîm|Cezada acele etmeyen", "El-Azîm|Azameti sonsuz", "El-Gafûr|Bağışlaması çok", "Eş-Şekûr|Az amele çok karşılık veren", "El-Aliyy|Çok yüce", "El-Kebîr|Büyüklüğü sonsuz", "El-Hafîz|Koruyan", "El-Mukît|Rızık ve güç veren", "El-Hasîb|Hesaba çeken ve yeten", "El-Celîl|Ululuk sahibi", "El-Kerîm|Çok ikram eden", "Er-Rakîb|Gözeten", "El-Mucîb|Dualara karşılık veren", "El-Vâsi|Rahmeti ve ilmi geniş", "El-Hakîm|Hikmet sahibi", "El-Vedûd|Çok seven ve sevilen", "El-Mecîd|Şanı yüce", "El-Bâis|Dirilten", "Eş-Şehîd|Her şeye şahit", "El-Hakk|Varlığı ve hükmü gerçek", "El-Vekîl|Kendisine dayanılan", "El-Kaviyy|Sonsuz güçlü", "El-Metîn|Kuvveti sarsılmaz", "El-Veliyy|Dost ve yardımcı", "El-Hamîd|Övgüye layık", "El-Muhsî|Her şeyi sayıp bilen", "El-Mübdi|İlk defa yaratan", "El-Muîd|Yeniden yaratan", "El-Muhyî|Hayat veren", "El-Mümît|Ölümü yaratan", "El-Hayy|Diri ve hayatın kaynağı", "El-Kayyûm|Varlığı kendinden, her şeyi ayakta tutan", "El-Vâcid|İstediğini bulan ve hiçbir şeye muhtaç olmayan", "El-Mâcid|Şanı ve keremi büyük", "El-Vâhid|Tek", "Es-Samed|Her şeyin kendisine muhtaç olduğu", "El-Kâdir|Her şeye gücü yeten", "El-Muktedir|Kudreti sonsuz", "El-Mukaddim|Öne alan", "El-Muahhir|Geri bırakan", "El-Evvel|Başlangıcı olmayan", "El-Âhir|Sonu olmayan", "Ez-Zâhir|Varlığı delilleriyle açık", "El-Bâtın|Mahiyeti duyulardan gizli", "El-Vâlî|Kâinatı yöneten", "El-Müteâlî|Her türlü eksiklikten yüce", "El-Berr|İyiliği bol", "Et-Tevvâb|Tövbeleri kabul eden", "El-Müntekim|Adaletle karşılık veren", "El-Afüvv|Günahları silen", "Er-Raûf|Çok şefkatli", "Mâlikü’l-Mülk|Mülkün gerçek sahibi", "Zü’l-Celâli ve’l-İkrâm|Ululuk ve ikram sahibi", "El-Muksit|Adaletle hükmeden", "El-Câmi|Bir araya toplayan", "El-Ganî|Hiçbir şeye muhtaç olmayan", "El-Muğnî|Zenginlik veren", "El-Mâni|Dilediğini engelleyen", "Ed-Dârr|Hikmetiyle zarar yaratabilen", "En-Nâfi|Fayda veren", "En-Nûr|Nurlandıran", "El-Hâdî|Hidayet veren", "El-Bedî|Örneksiz yaratan", "El-Bâkî|Varlığının sonu olmayan", "El-Vâris|Her şeyin gerçek varisi", "Er-Reşîd|Doğru yolu gösteren", "Es-Sabûr|Cezada acele etmeyen"
    ).map { raw -> raw.split('|', limit = 2).let { EsmaEntry(it[0], it[1]) } }
}
