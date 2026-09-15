package app.namaz.tr.v8.worship

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorshipFeatureTest {
    @Test fun sourced_catalog_has_no_source_less_entries() {
        assertTrue(WorshipCatalog.validate().joinToString(), WorshipCatalog.validate().isEmpty())
        assertEquals(99, WorshipCatalog.esma.size)
        assertTrue(WorshipCatalog.hadiths.isNotEmpty())
        assertTrue(WorshipCatalog.hadiths.all { it.source.isNotBlank() && it.body.contains("anlam", ignoreCase = true) })
    }

    @Test fun assistant_refuses_by_returning_empty_when_no_source_matches() {
        assertTrue(KnowledgeAssistant.search("bu-katalogda-yok-xyz").isEmpty())
        assertTrue(KnowledgeAssistant.search("abdest").isNotEmpty())
        assertTrue(KnowledgeAssistant.search("niyet").any { it.id == "niyet" })
    }

    @Test fun claim_checker_never_invents_a_source_when_no_match_exists() {
        val missing = ClaimVerifier.check("tamamen uydurma xyz qqq kaynak dışı söz")
        assertTrue(missing.matched.isEmpty())
        assertTrue(missing.explanation.contains("doğrula", ignoreCase = true))

        val known = ClaimVerifier.check("ameller niyetlere göre niyet amel")
        assertTrue(known.matched.any { it.id == "niyet" })
    }

    @Test fun zakat_math_respects_nisab_debt_and_categories() {
        assertEquals(2250.0, ZakatCalculator.calculate(100000.0, 10000.0, 50000.0), 0.001)
        assertEquals(0.0, ZakatCalculator.calculate(40000.0, 0.0, 50000.0), 0.001)
        assertEquals(2500.0, ZakatCalculator.calculate(40000.0, 30000.0, 30000.0, 10000.0, 10000.0, 50000.0), 0.001)
    }
}
