package com.emre.nexusai;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class DuckDuckGoSearch {
    private DuckDuckGoSearch() { }

    public static List<WebSearchResult> search(String query, int maxResults) throws Exception {
        int limit = Math.max(1, Math.min(maxResults, 10));
        String encoded = URLEncoder.encode(query == null ? "" : query.trim(), StandardCharsets.UTF_8.name());
        String[] endpoints = new String[] {
                "https://html.duckduckgo.com/html/?q=" + encoded + "&kl=tr-tr",
                "https://lite.duckduckgo.com/lite/?q=" + encoded
        };
        Exception last = null;
        for (String endpoint : endpoints) {
            try {
                HttpUtil.Response response = HttpUtil.get(endpoint, 10_000, 20_000);
                if (!response.isSuccessful()) continue;
                List<WebSearchResult> results = parseResults(response.body, limit);
                if (!results.isEmpty()) return results;
            } catch (Exception ex) {
                last = ex;
            }
        }
        if (last != null) throw last;
        return new ArrayList<>();
    }

    public static List<WebSearchResult> parseResults(String html, int maxResults) {
        List<WebSearchResult> results = new ArrayList<>();
        if (html == null || html.trim().isEmpty() || maxResults <= 0) return results;
        Document doc = Jsoup.parse(html);
        Elements links = doc.select("a.result__a, a.result-link");
        for (Element link : links) {
            if (results.size() >= maxResults) break;
            String href = unwrap(link.attr("href"));
            if (!href.startsWith("https://")) continue;
            Element container = link.closest(".result");
            if (container == null) container = link.parent();
            Element snippetEl = container == null ? null : container.selectFirst(".result__snippet, .result-snippet");
            String snippet = snippetEl == null ? "" : snippetEl.text();
            String title = link.text();
            if (title.isEmpty()) continue;
            results.add(new WebSearchResult(title, href, snippet));
        }
        return results;
    }

    private static String unwrap(String href) {
        if (href == null) return "";
        String value = href.trim();
        if (value.startsWith("//")) value = "https:" + value;
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            if (host != null && host.endsWith("duckduckgo.com") && uri.getRawQuery() != null) {
                for (String part : uri.getRawQuery().split("&")) {
                    int eq = part.indexOf('=');
                    if (eq <= 0) continue;
                    if ("uddg".equals(part.substring(0, eq))) {
                        return URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8.name());
                    }
                }
            }
        } catch (Exception ignored) { }
        return value;
    }
}
