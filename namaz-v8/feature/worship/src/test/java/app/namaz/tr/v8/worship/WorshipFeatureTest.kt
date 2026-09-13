package app.namaz.tr.v8.worship

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorshipFeatureTest {
    @Test fun sourced_catalog_has_no_source_less_entries() {
        assertTrue(WorshipCatalog.validate().joinToString(), WorshipCatalog.validate().isEmpty())
        assertEquals(99, WorshipCatalog.esma.size)
    }
    @Test fun assistant_refuses_by_returning_empty_when_no_source_matches() {
        assertTrue(KnowledgeAssistant.search("bu-katalogda-yok-xyz").isEmpty())
        assertTrue(KnowledgeAssistant.search("abdest").isNotEmpty())
    }
    @Test fun zakat_math_respects_nisab_and_debt() {
        assertEquals(2250.0, ZakatCalculator.calculate(100000.0, 10000.0, 50000.0), 0.001)
        assertEquals(0.0, ZakatCalculator.calculate(40000.0, 0.0, 50000.0), 0.001)
    }
}
