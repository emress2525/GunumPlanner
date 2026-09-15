package app.namaz.tr.v8.quran

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranRightsPolicyTest {
    @Test
    fun mealBundlingRequiresExplicitOfflineRedistributionEvidence() {
        val repositoryOnly = QuranContentRightsEvidence(
            sourceId = "repository-mit",
            sourceUrl = "https://example.invalid/repository",
            bundledLayers = setOf("arabic", "meal"),
            rightsScope = "Repository license only",
            rightsEvidenceUrl = null,
            official = false,
            allowsOfflineRedistribution = false,
        )
        assertFalse(QuranRightsPolicy.mayBundleMeal(repositoryOnly))

        val quranEnc = QuranContentRightsEvidence(
            sourceId = "quranenc-turkish-rwwad",
            sourceUrl = "https://quranenc.com/tr/browse/turkish_rwwad",
            bundledLayers = setOf("meal"),
            rightsScope = "QuranEnc translation republication terms",
            rightsEvidenceUrl = "https://quranenc.com/en/home/usage",
            official = true,
            allowsOfflineRedistribution = true,
        )
        assertTrue(QuranRightsPolicy.mayBundleMeal(quranEnc))
    }

    @Test
    fun tafsirBundlingRequiresTafsirLayerInGrant() {
        val mealOnly = QuranContentRightsEvidence(
            sourceId = "quranenc-meal",
            sourceUrl = "https://quranenc.com",
            bundledLayers = setOf("meal"),
            rightsScope = "QuranEnc republication terms",
            rightsEvidenceUrl = "https://quranenc.com/en/home/usage",
            official = true,
            allowsOfflineRedistribution = true,
        )
        assertFalse(QuranRightsPolicy.mayBundleTafsir(mealOnly))
        assertTrue(QuranRightsPolicy.mayBundleTafsir(mealOnly.copy(bundledLayers = setOf("meal", "tafsir"))))
    }
}
