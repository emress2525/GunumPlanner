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
            String url = "https://html.duckduckgo.com/html/?q=" + encoded + "&kl=tr-tr";
            HttpUtil.Response response = HttpUtil.getHtml(url, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
            if (!response.isSuccessful()) return Collections.emptyList();
            return DuckDuckGoSearchParser.parse(response.body, MAX_RESULTS);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }
}
