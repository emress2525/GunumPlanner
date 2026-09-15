package app.namaz.tr.v8.worship

import java.util.Locale

data class GroundedEntry(
    val id: String,
    val title: String,
    val body: String,
    val source: String,
    val tags: List<String>,
    val hanafi: String? = null,
    val shafiiNote: String? = null,
)

data class GroundedAnswer(
    val id: String,
    val title: String,
    val body: String,
    val source: String,
    val hanafi: String?,
    val shafiiNote: String?,
)

class SourceGroundedSearch(private val entries: List<GroundedEntry>) {
    fun answer(query: String): GroundedAnswer? {
        val tokens = tokenize(query)
        if (tokens.isEmpty()) return null
        val ranked = entries.map { entry ->
            val titleTokens = tokenize(entry.title)
            val tagTokens = entry.tags.flatMap(::tokenize).toSet()
            val bodyTokens = tokenize(entry.body)
            val titleHits = tokens.count { it in titleTokens }
            val tagHits = tokens.count { it in tagTokens }
            val bodyHits = tokens.count { it in bodyTokens }
            val score = titleHits * 4 + tagHits * 3 + bodyHits
            entry to score
        }
        val winner = ranked.maxByOrNull { it.second } ?: return null
        if (winner.second < 3) return null
        val entry = winner.first
        return GroundedAnswer(entry.id, entry.title, entry.body, entry.source, entry.hanafi, entry.shafiiNote)
    }

    private fun tokenize(value: String): Set<String> = value
        .lowercase(Locale("tr", "TR"))
        .replace(Regex("[^a-zçğıöşü0-9]+"), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter { it.length >= 2 }
        .toSet()
}
