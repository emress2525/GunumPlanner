package app.namaz.tr.v8.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranLicensedAssetParserTest {
    @Test
    fun tanzilArabicAndQuranEncMealAreAlignedWithoutBasmalaZero() {
        val tanzil = """
            {
              "surahs": [
                {
                  "number": 2,
                  "name": "البقرة",
                  "ayahs": [
                    {"number": 0, "text": "بسم الله الرحمن الرحيم"},
                    {"number": 1, "text": "الم"},
                    {"number": 2, "text": "ذلك الكتاب لا ريب فيه"}
                  ]
                }
              ]
            }
        """.trimIndent()
        val meal = """
            {
              "result": [
                {"id": 8, "sura": 2, "aya": 1, "translation": "Elif Lâm Mîm."},
                {"id": 9, "sura": 2, "aya": 2, "translation": "İşte bu, kendisinde şüphe olmayan kitaptır."}
              ]
            }
        """.trimIndent()

        val surah = QuranLicensedAssetParser.parseSurah(tanzil, meal, 2)

        assertEquals(2, surah.number)
        assertEquals("البقرة", surah.name)
        assertEquals(2, surah.verses.size)
        assertEquals(1, surah.verses[0].number)
        assertEquals("الم", surah.verses[0].arabic)
        assertEquals("Elif Lâm Mîm.", surah.verses[0].turkish)
        assertEquals("2:1", surah.verses[0].key)
        assertTrue(surah.verses[0].audioGhamadi!!.contains("/002/001.mp3"))
        assertTrue(surah.verses[0].audioMaher!!.contains("/002/001.mp3"))
        assertFalse(surah.verses.any { it.number == 0 })
    }

    @Test
    fun parserPreservesQuranEncTranslationTextExactly() {
        val exact = "  Metindeki boşluk korunur; (açıklama) silinmez.  "
        val tanzil = """{"surahs":[{"number":1,"name":"الفاتحة","ayahs":[{"number":1,"text":"بسم الله"}]}]}"""
        val meal = """{"result":[{"id":1,"sura":1,"aya":1,"translation":"$exact"}]}"""

        val verse = QuranLicensedAssetParser.parseSurah(tanzil, meal, 1).verses.single()

        assertEquals(exact, verse.turkish)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parserRejectsMealWithMissingAyaRatherThanInventingText() {
        val tanzil = """{"surahs":[{"number":1,"name":"الفاتحة","ayahs":[{"number":1,"text":"بسم الله"},{"number":2,"text":"الحمد لله"}]}]}"""
        val meal = """{"result":[{"id":1,"sura":1,"aya":1,"translation":"Birinci"}]}"""
        QuranLicensedAssetParser.parseSurah(tanzil, meal, 1)
    }
}
