package com.emre.nexusai;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public final class WebResearchGateway {
    private static final int CONNECT_TIMEOUT_MS = 12_000;
    private static final int READ_TIMEOUT_MS = 25_000;
    private static final int MAX_RESULTS = 6;

    public List<WebSearchResult> search(String query) {
        String clean = query == null ? "" : query.trim();
        if (clean.isEmpty()) return Collections.emptyList();
        try {
            String encoded = URLEncoder.encode(clean, StandardCharsets.UTF_8.name());

            String htmlUrl = "https://html.duckduckgo.com/html/?q=" + encoded + "&kl=tr-tr";
            HttpUtil.Response htmlResponse = HttpUtil.getHtml(htmlUrl, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
            if (htmlResponse.isSuccessful()) {
                List<WebSearchResult> results = DuckDuckGoSearchParser.parse(htmlResponse.body, MAX_RESULTS);
                if (!results.isEmpty()) return results;
            }

            String liteUrl = "https://lite.duckduckgo.com/lite/?q=" + encoded;
            HttpUtil.Response liteResponse = HttpUtil.getHtml(liteUrl, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
            if (liteResponse.isSuccessful()) {
                List<WebSearchResult> results = DuckDuckGoLiteSearchParser.parse(liteResponse.body, MAX_RESULTS);
                if (!results.isEmpty()) return results;
            }
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }
}
