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

        final String encoded;
        try {
            encoded = URLEncoder.encode(clean, StandardCharsets.UTF_8.name());
        } catch (Exception ignored) {
            return Collections.emptyList();
        }

        List<WebSearchResult> results = searchBingRss(encoded);
        if (!results.isEmpty()) return results;

        results = searchDuckDuckGoLite(encoded);
        if (!results.isEmpty()) return results;

        results = searchDuckDuckGoHtml(encoded);
        return results;
    }

    private List<WebSearchResult> searchBingRss(String encoded) {
        try {
            String url = "https://www.bing.com/search?q=" + encoded + "&format=rss";
            HttpUtil.Response response = HttpUtil.getHtml(url, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
            if (!response.isSuccessful()) return Collections.emptyList();
            return BingRssSearchParser.parse(response.body, MAX_RESULTS);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private List<WebSearchResult> searchDuckDuckGoLite(String encoded) {
        try {
            String url = "https://lite.duckduckgo.com/lite/?q=" + encoded;
            HttpUtil.Response response = HttpUtil.getHtml(url, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
            if (!response.isSuccessful()) return Collections.emptyList();
            return DuckDuckGoLiteSearchParser.parse(response.body, MAX_RESULTS);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private List<WebSearchResult> searchDuckDuckGoHtml(String encoded) {
        try {
            String url = "https://html.duckduckgo.com/html/?q=" + encoded + "&kl=tr-tr";
            HttpUtil.Response response = HttpUtil.getHtml(url, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
            if (!response.isSuccessful()) return Collections.emptyList();
            return DuckDuckGoSearchParser.parse(response.body, MAX_RESULTS);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }
}
