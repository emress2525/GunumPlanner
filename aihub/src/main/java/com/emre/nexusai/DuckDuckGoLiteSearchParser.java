package com.emre.nexusai;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DuckDuckGoLiteSearchParser {
    private static final Pattern ANCHOR = Pattern.compile("(?is)<a\\b([^>]*)>(.*?)</a>");
    private static final Pattern HREF = Pattern.compile("(?is)href\\s*=\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern SNIPPET = Pattern.compile("(?is)<(?:td|div)\\b[^>]*class\\s*=\\s*['\"][^'\"]*result-snippet[^'\"]*['\"][^>]*>(.*?)</(?:td|div)>");
    private static final Pattern TAG = Pattern.compile("(?is)<[^>]+>");

    private DuckDuckGoLiteSearchParser() { }

    public static List<WebSearchResult> parse(String html, int maxResults) {
        List<WebSearchResult> out = new ArrayList<>();
        if (html == null || html.isEmpty() || maxResults <= 0) return out;

        Matcher anchors = ANCHOR.matcher(html);
        List<Integer> starts = new ArrayList<>();
        List<Integer> ends = new ArrayList<>();
        List<String[]> data = new ArrayList<>();
        while (anchors.find()) {
            String attrs = anchors.group(1);
            if (!attrs.toLowerCase().contains("result-link")) continue;
            Matcher hrefMatcher = HREF.matcher(attrs);
            if (!hrefMatcher.find()) continue;
            starts.add(anchors.start());
            ends.add(anchors.end());
            data.add(new String[]{unwrap(hrefMatcher.group(1)), cleanText(anchors.group(2))});
        }

        for (int i = 0; i < data.size() && out.size() < maxResults; i++) {
            String url = data.get(i)[0];
            if (!url.startsWith("https://") && !url.startsWith("http://")) continue;
            int start = ends.get(i);
            int end = i + 1 < starts.size() ? starts.get(i + 1) : Math.min(html.length(), start + 2500);
            if (end < start) end = Math.min(html.length(), start + 2500);
            Matcher snippetMatcher = SNIPPET.matcher(html.substring(start, end));
            String snippet = snippetMatcher.find() ? cleanText(snippetMatcher.group(1)) : "";
            String title = data.get(i)[1].isEmpty() ? url : data.get(i)[1];
            out.add(new WebSearchResult(title, url, snippet));
        }
        return out;
    }

    private static String unwrap(String href) {
        String value = decodeEntities(href == null ? "" : href.trim());
        if (value.startsWith("//")) value = "https:" + value;
        int marker = value.indexOf("uddg=");
        if (marker >= 0) {
            String encoded = value.substring(marker + 5);
            int amp = encoded.indexOf('&');
            if (amp >= 0) encoded = encoded.substring(0, amp);
            try {
                return URLDecoder.decode(encoded, StandardCharsets.UTF_8.name());
            } catch (Exception ignored) { }
        }
        return value;
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
