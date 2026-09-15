package app.namaz.tr.v8

import app.namaz.tr.v8.quran.HatimPlan
import app.namaz.tr.v8.tools.QiblaMath
import app.namaz.tr.v8.worship.GroundedEntry
import app.namaz.tr.v8.worship.SourceGroundedSearch
import app.namaz.tr.v8.worship.ZakatCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FullProductRulesTest {
    @Test
    fun `30 day hatim distributes 604 pages without undercounting`() {
        assertEquals(21, HatimPlan.pagesPerDay(totalPages = 604, days = 30))
    }

    @Test
    fun `zakat helper only performs transparent percentage math`() {
        assertEquals(2500.0, ZakatCalculator.amount(100000.0), 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zakat helper rejects negative wealth`() {
        ZakatCalculator.amount(-1.0)
    }

    @Test
    fun `Ankara qibla bearing is physically plausible and normalized`() {
        val bearing = QiblaMath.bearing(39.9334, 32.8597)
        assertTrue("bearing=$bearing", bearing in 150.0..170.0)
        assertTrue(bearing >= 0.0 && bearing < 360.0)
    }

    @Test
    fun `religious search answers only from verified local sources`() {
        val search = SourceGroundedSearch(
            listOf(
                GroundedEntry(
                    id = "abdest-kan",
                    title = "Kan abdesti bozar mı?",
                    body = "Hanefî mezhebinde akıp yayılan kan abdesti bozar; Şafiî mezhebinde bu durum genel olarak abdesti bozmaz.",
                    source = "Diyanet İlmihal, Temizlik bölümü",
                    tags = listOf("kan", "abdest", "hanefi", "şafii"),
                )
            )
        )
        val hit = search.answer("kan abdesti")
        assertEquals("Diyanet İlmihal, Temizlik bölümü", hit?.source)
        assertNull(search.answer("Mars'ta namaz"))
    }
}
