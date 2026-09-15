package app.namaz.tr.v8.quran

data class QuranContentRightsEvidence(
    val sourceId: String,
    val sourceUrl: String,
    val bundledLayers: Set<String>,
    val rightsScope: String,
    val rightsEvidenceUrl: String?,
    val official: Boolean,
    val allowsOfflineRedistribution: Boolean,
)

object QuranRightsPolicy {
    fun mayBundleMeal(evidence: QuranContentRightsEvidence): Boolean =
        evidence.allowsOfflineRedistribution &&
            !evidence.rightsEvidenceUrl.isNullOrBlank() &&
            "meal" in evidence.bundledLayers

    fun mayBundleTafsir(evidence: QuranContentRightsEvidence): Boolean =
        evidence.allowsOfflineRedistribution &&
            !evidence.rightsEvidenceUrl.isNullOrBlank() &&
            "tafsir" in evidence.bundledLayers
}
