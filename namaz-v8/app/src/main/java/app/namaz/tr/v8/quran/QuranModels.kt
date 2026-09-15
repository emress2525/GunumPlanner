package app.namaz.tr.v8.quran

data class QuranVerse(
    val id: Int,
    val text: String,
    val translation: String,
)

data class QuranSurah(
    val id: Int,
    val name: String,
    val transliteration: String,
    val translation: String,
    val type: String,
    val verses: List<QuranVerse>,
)

object QuranSourceInfo {
    const val dataset = "quran-json 3.1.2"
    const val license = "CC-BY-SA-4.0"
    const val arabicSource = "Tanzil Uthmani Kur’an metni"
    const val translation = "Türkiye Diyanet İşleri Başkanlığı Türkçe meali; quran-json/Tanzil aktarımı"
    const val notice = "Kur’an metni ve meal ayrı kaynak katmanlarıdır. Meal, ayetin kendisi değildir. Veri paketi sürümü sabitlenmiş ve kaynak bilgisi uygulamada görünür tutulmuştur."
}
