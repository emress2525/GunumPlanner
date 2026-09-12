package com.emre.nexusai;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DuckDuckGoSearchParser {
    private static final Pattern RESULT_LINK = Pattern.compile(
            "(?is)<a[^>]*class=[\\\"'][^\\\"']*result__a[^\\\"']*[\\\"'][^>]*href=[\\\"']([^\\\"']+)[\\\"'][^>]*>(.*?)</a>");
    private static final Pattern SNIPPET = Pattern.compile(
            "(?is)<(?:a|div)[^>]*class=[\\\"'][^\\\"']*result__snippet[^\\\"']*[\\\"'][^>]*>(.*?)</(?:a|div)>");
    private static final Pattern TAG = Pattern.compile("(?is)<[^>]+>");

    private DuckDuckGoSearchParser() { }

    public static List<WebSearchResult> parse(String html, int maxResults) {
        List<WebSearchResult> out = new ArrayList<>();
        if (html == null || html.isEmpty() || maxResults <= 0) return out;

        Matcher links = RESULT_LINK.matcher(html);
        List<int[]> ranges = new ArrayList<>();
        List<String[]> linkData = new ArrayList<>();
        while (links.find()) {
            ranges.add(new int[]{links.start(), links.end()});
            linkData.add(new String[]{links.group(1), cleanText(links.group(2))});
        }

        for (int i = 0; i < linkData.size() && out.size() < maxResults; i++) {
            String url = unwrap(linkData.get(i)[0]);
            String title = linkData.get(i)[1];
            if (!url.startsWith("https://") && !url.startsWith("http://")) continue;
            int segmentStart = ranges.get(i)[1];
            int segmentEnd = (i + 1 < ranges.size()) ? ranges.get(i + 1)[0] : Math.min(html.length(), segmentStart + 2500);
            String segment = html.substring(segmentStart, Math.max(segmentStart, segmentEnd));
            Matcher sm = SNIPPET.matcher(segment);
            String snippet = sm.find() ? cleanText(sm.group(1)) : "";
            if (title.isEmpty()) title = url;
            out.add(new WebSearchResult(title, url, snippet));
        }
        return out;
    }

    private static String unwrap(String href) {
        if (href == null) return "";
        String value = decodeEntities(href.trim());
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
        return decodeEntities(TAG.matcher(raw).replaceAll(" "))
                .replaceAll("\\s+", " ")
                .trim();
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
