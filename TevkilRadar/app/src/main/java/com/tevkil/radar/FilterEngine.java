package com.tevkil.radar;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** No Android dependencies. Deterministic matching and fee parsing. */
public final class FilterEngine {
    public static final class Rules {
        public boolean ankaraOnly = true;
        public String include = "tevkil, duruşma, keşif, talimat, icra, adliye";
        public String exclude = "";
        public String districts = "";
        public long minFee = 0;
    }

    private static final String[] ANKARA_PLACES = {
        "ankara", "çankaya", "yenimahalle", "keçiören", "mamak", "sincan", "etimesgut",
        "altındağ", "gölbaşı", "pursaklar", "polatlı", "çubuk", "akyurt", "elmadag",
        "elmadağ", "beypazarı", "kızılcahamam", "kahramankazan", "kazan", "haymana",
        "şereflikoçhisar", "nallıhan", "güdül", "ayaş", "kalecik", "bala", "evren"
    };
    private static final Pattern FEE = Pattern.compile("(?iu)(\\d{1,3}(?:[.,]\\d{3})+|\\d{3,6})\\s*(?:₺|tl|lira)(?!\\p{L})");
    private static final Pattern SIMPLE_TL = Pattern.compile("₺\\s*(\\d{3,6}(?:[.,]\\d{3})?)");

    private FilterEngine() {}

    public static String fold(String input) {
        if (input == null) return "";
        String s = input.toLowerCase(new Locale("tr", "TR"));
        s = s.replace('ı', 'i');
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    public static boolean containsTerm(String normalizedText, String rawTerm) {
        String term = fold(rawTerm).trim();
        return !term.isEmpty() && normalizedText.contains(term);
    }

    private static boolean anyTerm(String normalizedText, String csv) {
        if (csv == null) return false;
        for (String term : csv.split("[,;\n]")) {
            if (containsTerm(normalizedText, term)) return true;
        }
        return false;
    }

    public static long advertisedFee(String input) {
        if (input == null) return 0;
        long max = 0;
        Matcher m = FEE.matcher(input);
        while (m.find()) {
            long n = parseMoney(m.group(1));
            if (n > max) max = n;
        }
        Matcher s = SIMPLE_TL.matcher(input);
        while (s.find()) {
            long n = parseMoney(s.group(1));
            if (n > max) max = n;
        }
        return max;
    }

    private static long parseMoney(String raw) {
        if (raw == null) return 0;
        try { return Long.parseLong(raw.replace(".", "").replace(",", "")); }
        catch (NumberFormatException ex) { return 0; }
    }

    public static boolean matches(String title, String body, Rules rules) {
        String message = fold((title == null ? "" : title) + " " + (body == null ? "" : body));
        if (message.trim().isEmpty()) return false;
        if (!anyTerm(message, rules.include)) return false;
        if (anyTerm(message, rules.exclude)) return false;
        if (rules.ankaraOnly) {
            boolean inAnkara = false;
            for (String p : ANKARA_PLACES) if (containsTerm(message, p)) { inAnkara = true; break; }
            if (!inAnkara) return false;
        }
        if (rules.districts != null && !rules.districts.trim().isEmpty() && !anyTerm(message, rules.districts)) return false;
        if (rules.minFee > 0 && advertisedFee(body) < rules.minFee) return false;
        return true;
    }

    public static String sourceName(String packageName) {
        if (packageName == null) return null;
        switch (packageName) {
            case "com.whatsapp": return "WhatsApp";
            case "com.whatsapp.w4b": return "WhatsApp Business";
            case "org.telegram.messenger":
            case "org.telegram.messenger.web":
            case "org.thunderdog.challegram": return "Telegram";
            case "com.instagram.android": return "Instagram";
            default: return null;
        }
    }
}
