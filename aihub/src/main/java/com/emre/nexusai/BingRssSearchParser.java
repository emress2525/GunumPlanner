package com.emre.nexusai;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BingRssSearchParser {
    private static final Pattern ITEM = Pattern.compile("(?is)<item\\b[^>]*>(.*?)</item>");
    private static final Pattern TITLE = Pattern.compile("(?is)<title\\b[^>]*>(.*?)</title>");
    private static final Pattern LINK = Pattern.compile("(?is)<link\\b[^>]*>(.*?)</link>");
    private static final Pattern DESCRIPTION = Pattern.compile("(?is)<description\\b[^>]*>(.*?)</description>");
    private static final Pattern TAG = Pattern.compile("(?is)<[^>]+>");

    private BingRssSearchParser() { }

    public static List<WebSearchResult> parse(String xml, int maxResults) {
        List<WebSearchResult> out = new ArrayList<>();
        if (xml == null || xml.isEmpty() || maxResults <= 0) return out;

        Matcher items = ITEM.matcher(xml);
        while (items.find() && out.size() < maxResults) {
            String item = items.group(1);
            String title = clean(extract(TITLE, item));
            String url = clean(extract(LINK, item));
            String snippet = clean(extract(DESCRIPTION, item));
            if (!url.startsWith("https://") && !url.startsWith("http://")) continue;
            if (title.isEmpty()) title = url;
            out.add(new WebSearchResult(title, url, snippet));
        }
        return out;
    }

    private static String extract(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String clean(String raw) {
        if (raw == null) return "";
        String value = raw.replace("<![CDATA[", "").replace("]]>", "");
        value = decodeEntities(value);
        value = TAG.matcher(value).replaceAll(" ");
        return value.replaceAll("\\s+", " ").trim();
    }

    private static String decodeEntities(String value) {
        return value
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }
}
