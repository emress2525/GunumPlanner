package com.tevkil.radar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure-Java helper. A watched profile is a notification filter, NOT remote profile access. */
public final class InstagramSources {
    public static final String DEFAULT = "tevkiliniz";
    private static final Pattern URL = Pattern.compile(
        "^(?:https?://)?(?:www\\.|m\\.)?instagram\\.com/([A-Za-z0-9._]{1,30})(?:[/?#].*)?$",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern USER = Pattern.compile("^[A-Za-z0-9._]{1,30}$");

    private InstagramSources() {}

    /** Accepts a profile URL, @username or username, but rejects post/story/reel links. */
    public static String parse(String input) {
        if (input == null) return null;
        String value = input.trim();
        if (value.startsWith("@")) value = value.substring(1);
        Matcher m = URL.matcher(value);
        if (m.matches()) value = m.group(1);
        else if (value.contains("://") || value.contains("/") || value.contains("?")) return null;
        if (!USER.matcher(value).matches() || value.startsWith(".") || value.endsWith(".")) return null;
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.equals("p") || normalized.equals("reel") || normalized.equals("reels") ||
            normalized.equals("stories") || normalized.equals("explore") || normalized.equals("accounts") ||
            normalized.equals("direct") || normalized.equals("about")) return null;
        return normalized;
    }

    public static String profileUrl(String account) {
        String parsed = parse(account);
        return parsed == null ? "" : "https://www.instagram.com/" + parsed + "/";
    }

    public static List<String> accounts(String encoded) {
        if (encoded == null || encoded.trim().isEmpty()) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (String value : encoded.split("[,;\\n]")) {
            String account = parse(value);
            if (account != null && !result.contains(account)) result.add(account);
        }
        return result;
    }

    public static String add(String encoded, String input) {
        String account = parse(input);
        if (account == null) throw new IllegalArgumentException("Geçerli bir Instagram profil adresi gir.");
        List<String> current = new ArrayList<>(accounts(encoded));
        if (!current.contains(account)) {
            if (current.size() >= 50) throw new IllegalArgumentException("En fazla 50 hesap eklenebilir.");
            current.add(account);
        }
        return String.join(",", current);
    }

    public static String remove(String encoded, String account) {
        List<String> current = new ArrayList<>(accounts(encoded));
        current.remove(parse(account));
        return String.join(",", current);
    }

    /** Conservative match: username must be explicitly visible in title/body. */
    public static boolean matchesNotification(String title, String body, String encoded) {
        String sourceText = (title == null ? "" : title) + " " + (body == null ? "" : body);
        for (String account : accounts(encoded)) {
            Pattern p = Pattern.compile("(?<![A-Za-z0-9._])@?" + Pattern.quote(account) + "(?![A-Za-z0-9._])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            if (p.matcher(sourceText).find()) return true;
        }
        return false;
    }
}
