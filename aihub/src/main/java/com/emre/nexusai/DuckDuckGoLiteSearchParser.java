package com.emre.nexusai;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DuckDuckGoLiteSearchParser {
    private static final Pattern RESULT_LINK = Pattern.compile(
            "(?is)<a[^>]*class=[\\\"'][^\\\"']*result-link[^\\\"']*[\\\"'][^>]*href=[\\\"']([^\\\"']+)[\\\"'][^>]*>(.*?)</a>");
    private static final Pattern SNIPPET = Pattern.compile(
            "(?is)<td[^>]*class=[\\\"'][^\\\"']*result-snippet[^\\\"']*[\\\"'][^>]*>(.*?)</td>");
    private static final Pattern TAG = Pattern.compile("(?is)<[^>]+>");

    private DuckDuckGoLiteSearchParser() { }

    public static List<WebSearchResult> parse(String html, int maxResults) {
        List<WebSearchResult> out = new ArrayList<>();
        if (html == null || html.isEmpty() || maxResults <= 0) return out;

        Matcher links = RESULT_LINK.matcher(html);
        List<int[]> ranges = new ArrayList<>();
        List<String[]> data = new ArrayList<>();
        while (links.find()) {
            ranges.add(new int[]{links.start(), links.end()});
            data.add(new String[]{decodeEntities(links.group(1).trim()), cleanText(links.group(2))});
        }

        for (int i = 0; i < data.size() && out.size() < maxResults; i++) {
            String url = data.get(i)[0];
            if (!url.startsWith("https://") && !url.startsWith("http://")) continue;
            int start = ranges.get(i)[1];
            int end = (i + 1 < ranges.size()) ? ranges.get(i + 1)[0] : Math.min(html.length(), start + 2500);
            String segment = html.substring(start, Math.max(start, end));
            Matcher sm = SNIPPET.matcher(segment);
            String snippet = sm.find() ? cleanText(sm.group(1)) : "";
            String title = data.get(i)[1];
            if (title.isEmpty()) title = url;
            out.add(new WebSearchResult(title, url, snippet));
        }
        return out;
    }

    private static String cleanText(String raw) {
        if (raw == null) return "";
        return decodeEntities(TAG.matcher(raw).replaceAll(" ")).replaceAll("\\s+", " ").trim();
    }

    private static String decodeEntities(String value) {
        return value
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }
}
