package app.namaz.tr.v8

import app.namaz.tr.v8.learn.LearningCatalog
import app.namaz.tr.v8.quran.QuranSourceInfo
import app.namaz.tr.v8.worship.WorshipCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FullContentContractTest {
    @Test
    fun `learning academy ships a sourced beginner path with madhhab differences`() {
        assertTrue(LearningCatalog.lessons.size >= 10)
        assertTrue(LearningCatalog.lessons.all { it.source.isNotBlank() })
        assertTrue(LearningCatalog.lessons.any { !it.shafiiNote.isNullOrBlank() })
        assertTrue(LearningCatalog.lessons.any { it.category == "Namaz" })
        assertTrue(LearningCatalog.lessons.any { it.category == "Elif-Bâ" })
    }

    @Test
    fun `worship hub never ships unsourced dua or hadith entries`() {
        assertTrue(WorshipCatalog.duas.size >= 6)
        assertTrue(WorshipCatalog.duas.all { it.source.isNotBlank() })
        assertTrue(WorshipCatalog.hadiths.size >= 5)
        assertTrue(WorshipCatalog.hadiths.all { it.source.isNotBlank() })
        assertTrue(WorshipCatalog.esma.size == 99)
    }

    @Test
    fun `quran data attribution is explicit and share alike`() {
        assertEquals("quran-json 3.1.2", QuranSourceInfo.dataset)
        assertEquals("CC-BY-SA-4.0", QuranSourceInfo.license)
        assertTrue(QuranSourceInfo.translation.contains("Diyanet"))
        assertTrue(QuranSourceInfo.arabicSource.contains("Tanzil"))
    }
}
