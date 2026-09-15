package app.namaz.tr.v8

import app.namaz.tr.v8.quran.QuranSearch
import app.namaz.tr.v8.quran.QuranSurah
import app.namaz.tr.v8.quran.QuranVerse
import org.junit.Assert.assertEquals
import org.junit.Test

class QuranSearchTest {
    private val sample = listOf(
        QuranSurah(1, "الفاتحة", "Al-Fatihah", "Fâtiha", "meccan", listOf(QuranVerse(1, "بسم الله", "Rahmân ve Rahîm olan Allah’ın adıyla"))),
        QuranSurah(2, "البقرة", "Al-Baqarah", "Bakara", "medinan", listOf(QuranVerse(255, "الله لا إله إلا هو", "Allah, O’ndan başka ilah yoktur"))),
    )

    @Test
    fun `search matches Turkish surah name meal and numeric surah id`() {
        assertEquals(listOf(2), QuranSearch.filter(sample, "Bakara").map { it.id })
        assertEquals(listOf(1), QuranSearch.filter(sample, "Rahîm").map { it.id })
        assertEquals(listOf(2), QuranSearch.filter(sample, "2").map { it.id })
    }

    @Test
    fun `blank search preserves source order`() {
        assertEquals(listOf(1, 2), QuranSearch.filter(sample, "").map { it.id })
    }
}
