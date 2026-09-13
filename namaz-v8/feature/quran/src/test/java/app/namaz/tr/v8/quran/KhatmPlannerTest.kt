package app.namaz.tr.v8.quran

import org.junit.Assert.assertEquals
import org.junit.Test

class KhatmPlannerTest {
    @Test fun redistributes_remaining_verses() {
        assertEquals(208, KhatmPlanner.dailyTarget(totalVerses = 6236, completed = 0, daysRemaining = 30))
        assertEquals(100, KhatmPlanner.dailyTarget(totalVerses = 1000, completed = 500, daysRemaining = 5))
        assertEquals(0, KhatmPlanner.dailyTarget(totalVerses = 100, completed = 100, daysRemaining = 1))
    }

    @Test fun surah_catalog_has_all_114_names() {
        assertEquals(114, SurahNames.all.size)
        assertEquals("Fâtiha", SurahNames.name(1))
        assertEquals("Nâs", SurahNames.name(114))
    }
}
