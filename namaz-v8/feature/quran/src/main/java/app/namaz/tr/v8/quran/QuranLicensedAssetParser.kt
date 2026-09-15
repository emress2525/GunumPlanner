package app.namaz.tr.v8.quran

import org.json.JSONArray
import org.json.JSONObject

object QuranLicensedAssetParser {
    fun parseSurah(tanzilJson: String, mealJson: String, surahNumber: Int): QuranSurah {
        val tanzilRoot = JSONObject(tanzilJson)
        val surahs = tanzilRoot.getJSONArray("surahs")
        var sourceSurah: JSONObject? = null
        for (index in 0 until surahs.length()) {
            val candidate = surahs.getJSONObject(index)
            if (candidate.getInt("number") == surahNumber) {
                sourceSurah = candidate
                break
            }
        }
        val surah = requireNotNull(sourceSurah) { "Sure bulunamadı: $surahNumber" }

        val mealItems = mealArray(mealJson)
        val meals = mutableMapOf<Int, Pair<Int, String>>()
        for (index in 0 until mealItems.length()) {
            val item = mealItems.getJSONObject(index)
            if (item.getInt("sura") == surahNumber) {
                val aya = item.getInt("aya")
                meals[aya] = item.optInt("id", globalId(surahNumber, aya)) to item.getString("translation")
            }
        }

        val verses = mutableListOf<QuranVerse>()
        val ayahs = surah.getJSONArray("ayahs")
        for (index in 0 until ayahs.length()) {
            val item = ayahs.getJSONObject(index)
            val aya = item.getInt("number")
            if (aya <= 0) continue
            val meal = requireNotNull(meals[aya]) { "Meal ayeti eksik: $surahNumber:$aya" }
            verses += QuranVerse(
                id = meal.first,
                number = aya,
                key = "$surahNumber:$aya",
                arabic = item.getString("text"),
                turkish = meal.second,
                audioGhamadi = audioUrl("ghamadi", surahNumber, aya),
                audioMaher = audioUrl("maher", surahNumber, aya),
            )
        }
        require(verses.isNotEmpty()) { "Sure ayetleri boş: $surahNumber" }
        return QuranSurah(surahNumber, surah.getString("name"), verses)
    }

    private fun mealArray(json: String): JSONArray {
        val trimmed = json.trimStart()
        return if (trimmed.startsWith("[")) JSONArray(json) else JSONObject(json).getJSONArray("result")
    }

    private fun audioUrl(reciter: String, surah: Int, aya: Int): String =
        "https://raw.githubusercontent.com/kurancilar/json/refs/heads/main/audio/$reciter/${surah.toString().padStart(3, '0')}/${aya.toString().padStart(3, '0')}.mp3"

    private fun globalId(surah: Int, aya: Int): Int = surah * 1000 + aya
}
