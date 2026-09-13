package app.namaz.tr.v8.learn

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class QuizQuestion(val question: String, val options: List<String>, val correctIndex: Int, val explanation: String)
data class Lesson(
    val id: String,
    val category: String,
    val title: String,
    val summary: String,
    val steps: List<String>,
    val hanafi: String,
    val shafii: String,
    val sources: List<String>,
    val quiz: QuizQuestion,
)

data class Recitation(val title: String, val arabic: String, val pronunciation: String, val meaning: String, val source: String)

object LearningCatalog {
    private const val ILMIHAL = "Diyanet İşleri Başkanlığı, İlmihal I — İman ve İbadetler"
    private fun lesson(id: String, category: String, title: String, summary: String, steps: List<String>, hanafi: String = "Hanefî anlatım: ana uygulama metni bu çerçevededir.", shafii: String = "Şafiî uygulamada ayrıntılar değişebilir; mezhep farkı ilgili başlıkta ayrıca gösterilir.", sources: List<String> = listOf(ILMIHAL), q: QuizQuestion) = Lesson(id, category, title, summary, steps, hanafi, shafii, sources, q)

    val lessons: List<Lesson> = listOf(
        lesson("iman", "Başlangıç", "İmanın Temelleri", "İman; Allah'a, meleklere, kitaplara, peygamberlere, ahiret gününe ve kadere iman başlıklarıyla öğrenilir.", listOf("İman esaslarını sırayla öğren.", "Her başlığın anlamını kısa cümlelerle tekrar et.", "Bilmediğin kavramı sözlükten aç."), sources = listOf("Kur’an 2:285", ILMIHAL), q = QuizQuestion("İman esasları içinde hangisi vardır?", listOf("Ahiret gününe iman", "Sadece gelenek", "Sadece tarih"), 0, "Ahiret gününe iman temel iman esaslarındandır.")),
        lesson("taharet", "Temizlik", "Temizlik ve Taharet", "İbadete hazırlıkta beden, elbise ve namaz yerinin temizliği temel konudur.", listOf("Necaseti fark et ve temizle.", "Su kullanımının mümkün olup olmadığını değerlendir.", "Abdest/gusül gerektiren hâlleri öğren."), q = QuizQuestion("Namaza hazırlıkta hangisi önemlidir?", listOf("Temizlik", "Telefon sesi", "Ayakkabı markası"), 0, "Taharet namazın hazırlık şartlarındandır.")),
        lesson("abdest", "Temizlik", "Abdest", "Abdest; belirli organları usulüne uygun yıkama ve mesh etme ibadetidir.", listOf("Niyet et ve besmele çek.", "Eller, ağız ve burnu temizle.", "Yüzü yıka.", "Kolları dirseklerle birlikte yıka.", "Başı mesh et.", "Ayakları topuklarla birlikte yıka."), hanafi = "Hanefî mezhebinde abdestin farzları: yüzü yıkamak, kolları dirseklerle yıkamak, başın en az dörtte birini mesh etmek ve ayakları topuklarla yıkamaktır.", shafii = "Şafiî mezhebinde niyet ve tertip de farzlar arasındadır; başın bir kısmını mesh etmek yeterli kabul edilir.", sources = listOf("Kur’an 5:6", ILMIHAL), q = QuizQuestion("Abdestte Kur’an 5:6'da geçen uygulamalardan biri hangisidir?", listOf("Yüzü yıkamak", "Saçı kesmek", "Elbise değiştirmek"), 0, "Ayet yüz ve kolların yıkanmasını, başın meshini ve ayaklarla ilgili hükmü bildirir.")),
        lesson("gusul", "Temizlik", "Gusül", "Gusül, bütün bedeni yıkayarak hükmî temizlik sağlamaktır.", listOf("Niyet et.", "Necaseti temizle.", "Ağız ve burnu iyice yıka.", "Bütün bedende kuru yer bırakmadan yıkan."), hanafi = "Hanefî mezhebinde ağız ve burnu yıkamak guslün farzlarındandır.", shafii = "Şafiî mezhebinde niyet ve suyun bütün bedene ulaşması farzdır; ağız-burun yıkama sünnet kabul edilir.", q = QuizQuestion("Gusülde ortak temel nedir?", listOf("Suyun bütün bedene ulaşması", "Sadece elleri yıkamak", "Sadece yüzü yıkamak"), 0, "Bütün bedenin yıkanması guslün temelidir.")),
        lesson("teyemmum", "Temizlik", "Teyemmüm", "Su bulunmadığında veya kullanımı ciddi zarar doğurduğunda temiz toprak/cinsinden yüzeyle hükmî temizliktir.", listOf("Geçerli mazereti belirle.", "Niyet et.", "Temiz toprak/cinsinden yüzeye elleri vur.", "Yüz ve elleri mezhep uygulamasına göre mesh et."), sources = listOf("Kur’an 5:6", ILMIHAL), q = QuizQuestion("Teyemmüm hangi durumda gündeme gelir?", listOf("Su bulunmadığında veya kullanılamadığında", "Her zaman", "Sadece gece"), 0, "Teyemmüm suyun yokluğu veya kullanılamaması gibi mazeretlerde ruhsattır.")),
        lesson("namaz-sart", "Namaz", "Namazın Şartları", "Namazdan önce vakit, temizlik, kıble, setr-i avret ve niyet gibi şartlar hazırlanır.", listOf("Vaktin girdiğini kontrol et.", "Abdest ve temizliği kontrol et.", "Kıbleyi belirle.", "Uygun şekilde örtün.", "Kılacağın namaza niyet et."), q = QuizQuestion("Namazdan önce hangisi kontrol edilir?", listOf("Vakit", "Telefon modeli", "Hava sıcaklığı zorunlu olarak"), 0, "Vakit namazın temel şartlarındandır.")),
        lesson("sabah", "Namaz", "Sabah Namazı", "Sabah namazı iki rekât sünnet ve iki rekât farz olarak öğretilir.", listOf("2 rekât sünnet: niyet, kıyam, kıraat, rükû, secdeler ve oturuş.", "Ardından 2 rekât farz için niyet et.", "İki rekâtı Fâtiha ve sûre/ayetlerle tamamla.", "Son oturuşta tahiyyat, salavat ve dua okuyup selam ver."), q = QuizQuestion("Sabah namazının farzı kaç rekâttır?", listOf("2", "3", "4"), 0, "Sabah namazının farzı iki rekâttır.")),
        lesson("ogle", "Namaz", "Öğle Namazı", "Öğle namazı Hanefî öğretimde dört ilk sünnet, dört farz ve iki son sünnet akışıyla öğrenilir.", listOf("4 rekât ilk sünnet.", "4 rekât farz.", "2 rekât son sünnet.", "Her bölümde niyeti ayrı yap."), q = QuizQuestion("Öğle namazının farzı kaç rekâttır?", listOf("2", "3", "4"), 2, "Öğle farzı dört rekâttır.")),
        lesson("ikindi", "Namaz", "İkindi Namazı", "İkindi dört rekât sünnet ve dört rekât farz akışıyla öğretilir.", listOf("4 rekât sünnet.", "4 rekât farz.", "Vakit başlangıcında Hanefî/Şafiî ikindi hesabı farkını uygulama ayarından görebilirsin."), hanafi = "Hanefî ikindi vakti hesabında gölge boyu için iki kat görüşü esas alınır.", shafii = "Şafiî ikindi vakti hesabında bir kat gölge görüşü esas alınır.", q = QuizQuestion("İkindi farzı kaç rekâttır?", listOf("4", "2", "3"), 0, "İkindi farzı dört rekâttır.")),
        lesson("aksam", "Namaz", "Akşam Namazı", "Akşam namazı üç rekât farz ve ardından iki rekât sünnet olarak öğretilir.", listOf("3 rekât farz.", "2 rekât sünnet.", "Üçüncü rekâtta oturuşla namazı tamamla."), q = QuizQuestion("Akşam farzı kaç rekâttır?", listOf("2", "3", "4"), 1, "Akşam farzı üç rekâttır.")),
        lesson("yatsi", "Namaz", "Yatsı Namazı", "Yatsı; dört sünnet, dört farz, iki son sünnet ve ardından vitirle tamamlanan gece ibadeti akışıdır.", listOf("4 rekât ilk sünnet.", "4 rekât farz.", "2 rekât son sünnet.", "Vitir namazını ayrı niyetle kıl."), q = QuizQuestion("Yatsı farzı kaç rekâttır?", listOf("4", "3", "2"), 0, "Yatsı farzı dört rekâttır.")),
        lesson("vitir", "Namaz", "Vitir Namazı", "Vitir gece namazlarının sonunda tek sayılı rekâtlarla kılınan ibadettir.", listOf("Mezhep uygulamana göre vitir niyetini yap.", "Kıraat ve kunut uygulamasını öğren.", "Selamla tamamla."), hanafi = "Hanefî mezhebinde vitir vacip kabul edilir ve üç rekât tek selamla, üçüncü rekâtta kunutla kılınır.", shafii = "Şafiî mezhebinde vitir sünnet-i müekkededir; bir veya daha fazla tek sayılı rekâtla kılınabilir.", q = QuizQuestion("Vitir konusunda mezhepler arasında ne vardır?", listOf("Hüküm ve uygulama ayrıntısı farkı", "Hiç fark yok", "Vitir diye namaz yok"), 0, "Hanefî ve Şafiî mezheplerinde hüküm ve kılınış ayrıntıları farklıdır.")),
        lesson("cuma", "Namaz", "Cuma Namazı", "Cuma, şartları oluştuğunda cemaatle kılınan ve hutbe içeren haftalık ibadettir.", listOf("Cuma vaktini ve cami/cemaat imkânını kontrol et.", "Hutbeyi dinle.", "İmamla iki rekât cuma farzını kıl.", "Sünnetleri yerel uygulama ve ilmihal bilgisiyle tamamla."), sources = listOf("Kur’an 62:9", ILMIHAL), q = QuizQuestion("Cuma çağrısında Kur’an 62:9 neyi vurgular?", listOf("Allah'ın zikrine yönelmeyi", "Alışverişi artırmayı", "Yolculuğa çıkmayı"), 0, "Ayet cuma çağrısıyla Allah'ın zikrine yönelmeyi emreder.")),
        lesson("bayram", "Namaz", "Bayram Namazı", "Bayram sabahı cemaatle kılınır; tekbir düzeni mezhebe göre ayrıntı gösterir.", listOf("Bayram namazına niyet et.", "İmamın tekbirlerini takip et.", "Hutbeyi ve bayram adabını takip et."), q = QuizQuestion("Bayram namazında neye uyulur?", listOf("İmamın tekbirlerine", "Rastgele sayıya", "Telefon sayacına"), 0, "Cemaat imamı takip eder.")),
        lesson("cenaze", "Namaz", "Cenaze Namazı", "Rükû ve secdesi olmayan, tekbirler ve dualardan oluşan toplu duadır.", listOf("Cenaze için niyet et.", "İmamla tekbirleri takip et.", "Sena, salavat ve cenaze duasını oku.", "Selamla tamamla."), q = QuizQuestion("Cenaze namazında hangisi yoktur?", listOf("Rükû ve secde", "Tekbir", "Dua"), 0, "Cenaze namazında rükû ve secde bulunmaz.")),
        lesson("teravih", "Namaz", "Teravih", "Ramazan gecelerinde yatsıdan sonra kılınan nafile/sünnet namazıdır.", listOf("Yatsıdan sonra teravihe niyet et.", "Cemaatle veya tek başına kılınabilir.", "Rekât düzeninde mezhep ve yerel uygulamayı takip et."), q = QuizQuestion("Teravih hangi ayla ilişkilidir?", listOf("Ramazan", "Muharrem yalnız", "Her ay zorunlu"), 0, "Teravih Ramazan gecelerinin ibadetidir.")),
        lesson("teheccud", "Namaz", "Teheccüd", "Gece kalkarak kılınan nafile namazdır.", listOf("Uykudan sonra gece vakti kalk.", "İki rekâtlar halinde nafile kıl.", "Dua ve istiğfarla tamamla."), q = QuizQuestion("Teheccüd nedir?", listOf("Gece nafile namazı", "Farz cuma", "Bayram hutbesi"), 0, "Teheccüd gece kılınan nafile namazdır.")),
        lesson("istihare", "Namaz", "İstihare", "Mubah bir konuda hayırlısını Allah'tan isteme duası ve nafile namazıdır; rüya görme şart değildir.", listOf("Konuyu meşru yollarla araştır ve istişare et.", "İki rekât nafile kıl.", "İstihare duasıyla hayırlısını iste.", "Sonucu yalnız rüyaya bağlama."), q = QuizQuestion("İstihare için rüya görmek şart mıdır?", listOf("Hayır", "Evet mutlaka", "Sadece cuma"), 0, "İstihareyi yalnız rüyaya indirgemek doğru değildir.")),
        lesson("hasta", "Namaz", "Hastalıkta Namaz", "Gücü yetmeyen kişi namazı yapabildiği şekle göre kılar; ayrıntı durum ve mezhebe göre değerlendirilir.", listOf("Ayakta durabiliyorsan ayakta kıl.", "Güç yetmiyorsa oturarak veya işaretle kılma hükümlerini öğren.", "Özel tıbbi durumda güvenilir din görevlisine danış."), q = QuizQuestion("Hastalıkta temel ilke nedir?", listOf("Güç yettiği kadar ibadet", "Namazı daima terk", "Sadece ayakta zorunlu"), 0, "İslam kolaylık ilkesini gözetir; ayrıntı kişisel duruma göre değişir.")),
        lesson("seferi", "Namaz", "Seyahat ve Seferîlik", "Yolculukta namaz hükümleri mesafe, niyet ve kalış süresine göre değişebilir; uygulama seni otomatik seferî ilan etmez.", listOf("Yolculuk şartlarını öğren.", "Mezhebine göre kasr/cem hükümlerini kontrol et.", "Kişisel durum belirsizse ehil din görevlisine danış."), q = QuizQuestion("Uygulama kullanıcıyı otomatik seferî ilan eder mi?", listOf("Hayır", "Evet", "Her şehir değişiminde"), 0, "Hüküm kişisel şartlara bağlıdır; uygulama yalnız bilgi verir.")),
        lesson("kaza", "Namaz", "Kaza Namazı", "Vaktinde kılınamayan farz namazların telafisiyle ilgili hükümler mezheplerde ayrıntılıdır.", listOf("Kaza sayını kendin gir.", "Uygulamanın geçmişten otomatik borç üretmediğini bil.", "Planlı ve sürdürülebilir bir telafi düzeni kur."), q = QuizQuestion("V8 kaza borcunu nasıl belirler?", listOf("Kullanıcının girdiği sayıyla", "Doğum tarihinden otomatik", "Tahmin ederek"), 0, "Kaza sayısını kullanıcı girer; uygulama dini borç uydurmaz.")),
        lesson("cemaat", "Namaz", "Cemaatle Namaz", "İmama uyarak toplu namaz kılmanın adabını ve temel takip kurallarını öğretir.", listOf("Saf düzenine katıl.", "İmama uyma niyeti yap.", "Hareketlerde imamı geçme.", "Geç kalan rekâtları mezhep usulüne göre tamamla."), hanafi = "Hanefî mezhebinde muktedinin imam arkasındaki kıraat uygulaması Şafiî mezhebinden farklıdır.", shafii = "Şafiî mezhebinde Fâtiha kıraati konusunda farklı uygulama vardır.", q = QuizQuestion("Cemaatte temel takip ilkesi nedir?", listOf("İmamı takip etmek", "İmamdan önce hareket etmek", "Safı terk etmek"), 0, "Muktedî imamı takip eder.")),
        lesson("elifba", "Kur’an Öğrenme", "Elif-Bâ Başlangıcı", "Arap harflerini ses, şekil ve birleşme üzerinden adım adım öğrenme rotasıdır.", listOf("Harfleri tek tek tanı.", "Başta-ortada-sonda şekilleri ayırt et.", "Üstün, esre, ötre öğren.", "Cezm ve şeddeyi çalış.", "Tenvin ve med harflerini çalış.", "Birleşik kelimelere geç.", "Kısa ayet ve surelerle pekiştir."), sources = listOf("Diyanet Elif-Bâ öğretim materyalleri; uygulama özgün özet anlatımı"), q = QuizQuestion("Elif-Bâ rotasında harekelerden sonra ne gelir?", listOf("Cezm ve şedde", "Doğrudan tefsir", "Zekât hesabı"), 0, "Öğrenme rotası aşamalıdır.")),
        lesson("oruç", "İbadet", "Oruç Temelleri", "Ramazan orucu imsakla başlayıp iftarla tamamlanan farz ibadettir; mazeret ve kaza hükümleri kişisel duruma göre değişebilir.", listOf("İmsak/iftar vaktini kontrol et.", "Niyet ve orucu bozan temel hâlleri öğren.", "Sağlık/seyahat/özel durumlarda ehil kaynağa danış."), sources = listOf("Kur’an 2:183-185", ILMIHAL), q = QuizQuestion("Ramazan orucu hangi ayetlerde açıkça ele alınır?", listOf("Bakara 2:183-185", "Fîl suresi", "Sadece hadislerde"), 0, "Bakara 183-185 oruç hükümlerinin temel ayetlerindendir.")),
        lesson("zekat", "İbadet", "Zekât Temelleri", "Belirli mal türlerinde nisap ve yıl gibi şartlarla mali ibadet doğar; hesap ayrıntıları mala göre değişir.", listOf("Zekâta tabi malı belirle.", "Borç ve nisap değerlendirmesini yap.", "Oranı ve özel mal hükümlerini güvenilir ilmihalden kontrol et."), sources = listOf("Kur’an 2:43", ILMIHAL), q = QuizQuestion("Zekât sadece matematik işlemi midir?", listOf("Hayır, şartları da vardır", "Evet sadece %2,5", "Hiç şartı yok"), 0, "Nisap, süre ve mal türü gibi şartlar dikkate alınır.")),
        lesson("hac", "İbadet", "Hac ve Umreye Giriş", "İhram, tavaf, sa'y ve hac menasiki belirli zaman ve mekânlarda yerine getirilir.", listOf("İhram ve yasaklarını öğren.", "Tavaf ve sa'y sırasını öğren.", "Arafat, Müzdelife ve Mina görevlerini hac türüne göre öğren.", "Resmî organizasyon rehberini takip et."), sources = listOf("Kur’an 2:196-203", "Diyanet Hac ve Umre Rehberi"), q = QuizQuestion("Haccın temel duraklarından biri hangisidir?", listOf("Arafat", "Herhangi bir alışveriş merkezi", "Evde kalmak"), 0, "Arafat haccın temel menasik duraklarındandır.")),
        lesson("ahlak", "Günlük Hayat", "Ahlak ve Kul Hakkı", "Doğruluk, emanet, gıybetten sakınma ve kul hakkına riayet günlük Müslüman hayatının merkezindedir.", listOf("Sözünde doğru ol.", "Emanete riayet et.", "Gıybet ve iftiradan sakın.", "Haksızlık yaptıysan hakkı iade ve helalleşme yollarını ara."), sources = listOf("Kur’an 49:11-12", "Kur’an 4:58"), q = QuizQuestion("Kul hakkı konusunda doğru yaklaşım hangisidir?", listOf("Hakkı iade etmeye çalışmak", "Önemsememek", "Gizlemek"), 0, "Hak sahibinin hakkını iade etmek esastır."))
    )

    val recitations = listOf(
        Recitation("Sübhaneke", "سُبْحَانَكَ اللّٰهُمَّ وَبِحَمْدِكَ وَتَبَارَكَ اسْمُكَ وَتَعَالَى جَدُّكَ وَلَا إِلٰهَ غَيْرُكَ", "Sübhâneke allâhümme ve bi hamdik...", "Allah'ım! Seni noksan sıfatlardan tenzih eder, sana hamd ederim...", "Diyanet namaz rehberi"),
        Recitation("Fâtiha", "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ ...", "Elhamdü lillâhi rabbil âlemîn...", "Hamd âlemlerin Rabbi Allah'a mahsustur...", "Kur’an 1:1-7"),
        Recitation("Ettehiyyatü", "اَلتَّحِيَّاتُ لِلّٰهِ وَالصَّلَوَاتُ وَالطَّيِّبَاتُ ...", "Ettehiyyâtü lillâhi vessalevâtü vettayyibât...", "Bütün hürmetler, dualar ve güzel sözler Allah'a mahsustur...", "Sahih Müslim, Salât; Diyanet namaz rehberi"),
        Recitation("Salli-Barik", "اَللّٰهُمَّ صَلِّ عَلَى مُحَمَّدٍ ...", "Allâhümme salli alâ Muhammedin...", "Allah'ım, Muhammed'e ve ailesine rahmet eyle...", "Buhârî, Enbiyâ; Diyanet namaz rehberi"),
        Recitation("Rabbenâ âtinâ", "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ", "Rabbenâ âtinâ fiddünyâ haseneten...", "Rabbimiz! Bize dünyada iyilik, ahirette de iyilik ver ve bizi ateş azabından koru.", "Kur’an 2:201")
    )

    fun validate(): List<String> = lessons.flatMap { l -> buildList {
        if (l.id.isBlank()) add("id")
        if (l.title.isBlank()) add("title")
        if (l.steps.isEmpty()) add("steps")
        if (l.sources.isEmpty() || l.sources.any { it.isBlank() }) add("sources")
        if (l.hanafi.isBlank() || l.shafii.isBlank()) add("madhab")
    }.map { "${l.id}:$it" } }
}

class LearningProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("learning_progress_v8", Context.MODE_PRIVATE)
    fun complete(id: String) { val s = prefs.getStringSet("done", emptySet()).orEmpty().toMutableSet(); s += id; prefs.edit().putStringSet("done", s).putString("last", id).apply() }
    fun isComplete(id: String) = id in prefs.getStringSet("done", emptySet()).orEmpty()
    fun completedCount() = prefs.getStringSet("done", emptySet()).orEmpty().size
    fun last() = prefs.getString("last", null)
}

@Composable
fun LearnScreen() {
    val context = LocalContext.current
    val progress = remember(context) { LearningProgressStore(context.applicationContext) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Tümü") }
    val selected = LearningCatalog.lessons.firstOrNull { it.id == selectedId }
    if (selected != null) {
        LessonScreen(selected, progress) { selectedId = null }
        return
    }
    val categories = listOf("Tümü") + LearningCatalog.lessons.map { it.category }.distinct()
    val filtered = LearningCatalog.lessons.filter { (category == "Tümü" || it.category == category) && (query.isBlank() || it.title.contains(query, true) || it.summary.contains(query, true)) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Öğren Akademisi", style = MaterialTheme.typography.headlineMedium)
        Text("Sıfırdan başla, kaynağı gör, mezhep farkını karıştırma. ${progress.completedCount()} ders tamamlandı.", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(query, { query = it }, label = { Text("Ders ara") }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
        LazyColumn {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    categories.take(5).forEach { c -> FilterChip(category == c, { category = c }, label = { Text(c) }) }
                }
            }
            item {
                Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Önerilen rota", fontWeight = FontWeight.Bold)
                        Text("İman → Temizlik → Abdest → Namaz → Okunanlar → Beş Vakit → Dua → Kur’an → Ahlak → Oruç → Zekât → Hac")
                    }
                }
            }
            items(filtered, key = { it.id }) { l ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(l.title, style = MaterialTheme.typography.titleMedium)
                        Text(l.category + if (progress.isComplete(l.id)) " • ✓ tamamlandı" else "")
                        Text(l.summary, maxLines = 2)
                        OutlinedButton(onClick = { selectedId = l.id }) { Text("Dersi aç") }
                    }
                }
            }
            item {
                Text("Namazda Okunanlar", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
                LearningCatalog.recitations.forEach { r ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Column(Modifier.padding(12.dp)) { Text(r.title, fontWeight = FontWeight.Bold); Text(r.arabic); Text(r.pronunciation); Text(r.meaning); Text("Kaynak: ${r.source}", style = MaterialTheme.typography.bodySmall) } }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun LessonScreen(lesson: Lesson, progress: LearningProgressStore, onBack: () -> Unit) {
    var answer by rememberSaveable(lesson.id) { mutableIntStateOf(-1) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            OutlinedButton(onClick = onBack) { Text("← Akademi") }
            Text(lesson.title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 12.dp))
            Text(lesson.summary, modifier = Modifier.padding(vertical = 8.dp))
            Text("Adım adım", style = MaterialTheme.typography.titleMedium)
            lesson.steps.forEachIndexed { i, s -> Text("${i + 1}. $s", modifier = Modifier.padding(vertical = 3.dp)) }
            Card(Modifier.fillMaxWidth().padding(top = 12.dp)) { Column(Modifier.padding(12.dp)) { Text("Hanefî", fontWeight = FontWeight.Bold); Text(lesson.hanafi); Text("Şafiî farkı", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)); Text(lesson.shafii) } }
            Text("Kaynaklar", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
            lesson.sources.forEach { Text("• $it") }
            Text("Mini test", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            Text(lesson.quiz.question)
            lesson.quiz.options.forEachIndexed { i, option -> OutlinedButton(onClick = { answer = i }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) { Text(option) } }
            if (answer >= 0) Text(if (answer == lesson.quiz.correctIndex) "✓ Doğru. ${lesson.quiz.explanation}" else "Tekrar kontrol et. ${lesson.quiz.explanation}", modifier = Modifier.padding(vertical = 8.dp))
            Button(onClick = { progress.complete(lesson.id) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Dersi tamamladım") }
            Text("Kişisel ve özel bir hüküm gerekiyorsa uygulama yerine ehil bir din görevlisine danış.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
            Spacer(Modifier.height(36.dp))
        }
    }
}
