package com.emre.nexusai;

import java.util.List;

public final class ResearchGateway {
    public static final int MAX_RESULTS = 6;
    private final TextGateway textGateway;

    public ResearchGateway(TextGateway textGateway) {
        this.textGateway = textGateway;
    }

    public NexusResult research(NexusConfig config, String query) {
        try {
            List<WebSearchResult> results = DuckDuckGoSearch.search(query, MAX_RESULTS);
            if (results.isEmpty()) {
                return NexusResult.error("Web araştırma sonuçları alınamadı. Biraz sonra tekrar dene.");
            }
            String evidencePrompt = ResearchContextBuilder.build(query, results);
            return textGateway.complete(config, RequestMode.RESEARCH, evidencePrompt);
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (message == null || message.trim().isEmpty()) message = ex.getClass().getSimpleName();
            return NexusResult.error("Web araştırması yapılamadı: " + message);
        }
    }
}
