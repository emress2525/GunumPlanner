package app.namaz.tr.v8.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningCatalogTest {
    @Test fun every_lesson_has_sources_and_madhab_metadata() {
        assertTrue(LearningCatalog.validate().joinToString(), LearningCatalog.validate().isEmpty())
    }
    @Test fun core_learning_route_is_present() {
        val ids = LearningCatalog.lessons.map { it.id }.toSet()
        listOf("iman","abdest","sabah","ogle","ikindi","aksam","yatsi","vitir","elifba","oruç","zekat","hac").forEach { assertTrue(it, it in ids) }
        assertTrue(LearningCatalog.lessons.size >= 20)
        assertEquals("Fâtiha", LearningCatalog.recitations[1].title)
    }
}
