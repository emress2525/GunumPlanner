package com.emre.nexusai;

import java.util.List;

public final class ResearchContextBuilder {
    private ResearchContextBuilder() { }

    public static String build(String originalQuery, List<WebSearchResult> results) {
        StringBuilder out = new StringBuilder();
        out.append("USER RESEARCH REQUEST:\n").append(originalQuery == null ? "" : originalQuery.trim());
        out.append("\n\nLIVE WEB SEARCH RESULTS:\n");
        if (results == null || results.isEmpty()) {
            out.append("No search results were available.\n");
            return out.toString();
        }
        int index = 1;
        for (WebSearchResult result : results) {
            if (result == null || result.url.isEmpty()) continue;
            out.append("\n[").append(index++).append("] ").append(result.title).append("\n");
            out.append("URL: ").append(result.url).append("\n");
            if (!result.snippet.isEmpty()) out.append("Snippet: ").append(result.snippet).append("\n");
        }
        out.append("\nSynthesize the answer from these results. Compare alternatives, call out uncertainty, and cite only the URLs above.");
        return out.toString();
    }
}
