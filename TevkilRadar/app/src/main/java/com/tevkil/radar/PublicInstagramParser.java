package com.tevkil.radar;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conservative parser of URLs and public Open Graph metadata, with no login or bypass. */
public final class PublicInstagramParser {
    private static final Pattern POST_URL = Pattern.compile(
        "(?i)(?:https?://(?:www\\.)?instagram\\.com)?/(p|reel)/([A-Za-z0-9_-]{5,24})(?:[/\"'?&#\\s]|$)");
    private static final Pattern SHORTCODE = Pattern.compile(
        "(?i)[\"']shortcode[\"']\\s*:\\s*[\"']([A-Za-z0-9_-]{5,24})[\"']");
    private static final Pattern META = Pattern.compile("(?is)<meta\\s+[^>]*>");
    private static final Pattern ATTRIBUTE = Pattern.compile(
        "(?is)([a-zA-Z:_-]+)\\s*=\\s*([\"'])(.*?)\\2");
    private static final Pattern NUM_ENTITY = Pattern.compile("&#(x[0-9a-fA-F]+|[0-9]+);");

    public static final class Link {
        public final String code, url;
        Link(String code, String url) { this.code=code; this.url=url; }
    }

    private PublicInstagramParser() {}

    public static List<Link> links(String html) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (html == null) return new ArrayList<>();
        String clean = html.replace("\\u002F", "/").replace("\\u002f", "/")
                .replace("\\/", "/").replace("&sol;", "/").replace("&#47;", "/");
        Matcher m = POST_URL.matcher(clean);
        while (m.find() && ids.size() < 6) ids.add(m.group(1).toLowerCase() + "/" + m.group(2));
        if (ids.isEmpty()) {
            Matcher fallback = SHORTCODE.matcher(clean);
            while (fallback.find() && ids.size() < 6) ids.add("p/" + fallback.group(1));
        }
        List<Link> result = new ArrayList<>();
        for (String id : ids) {
            String code = id.substring(id.indexOf('/') + 1);
            result.add(new Link(code, "https://www.instagram.com/" + id + "/"));
        }
        return result;
    }

    public static String meta(String html, String name) {
        if (html == null || name == null) return "";
        Matcher tags = META.matcher(html);
        while (tags.find()) {
            String tag=tags.group();
            Matcher attrs=ATTRIBUTE.matcher(tag);
            String key="", content="";
            while (attrs.find()) {
                if (attrs.group(1).equalsIgnoreCase("property") || attrs.group(1).equalsIgnoreCase("name"))
                    key=attrs.group(3);
                if (attrs.group(1).equalsIgnoreCase("content")) content=attrs.group(3);
            }
            if (name.equalsIgnoreCase(key)) return decode(content);
        }
        return "";
    }

    public static String caption(String html) {
        String value = meta(html, "og:description");
        if (value.isEmpty()) value=meta(html,"description");
        if (value.startsWith("Instagram photos and videos") ||
                value.startsWith("See Instagram photos")) return "";
        return value.length()>2000 ? value.substring(0,2000) : value;
    }

    public static String image(String html) {
        String value = meta(html, "og:image");
        if (value.isEmpty()) value=meta(html,"twitter:image");
        return value.startsWith("https://") ? value : "";
    }

    private static String decode(String raw) {
        String s=raw.replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&apos;", "'").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&amp;", "&");
        Matcher m=NUM_ENTITY.matcher(s);
        StringBuffer sb=new StringBuffer();
        while (m.find()) {
            try {
                String v=m.group(1);
                int n=v.startsWith("x") ? Integer.parseInt(v.substring(1),16) : Integer.parseInt(v);
                m.appendReplacement(sb, Matcher.quoteReplacement(new String(Character.toChars(n))));
            } catch (Exception ex) { m.appendReplacement(sb,Matcher.quoteReplacement(m.group(0))); }
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
