package com.emre.gunumplanner;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NaturalLanguageParser {
    public static final String NONE = "";
    public static final String DAILY = "DAILY";
    public static final String WEEKDAYS = "WEEKDAYS";
    public static final String WEEKLY = "WEEKLY";
    public static final String MONTHLY = "MONTHLY";

    private static final Locale TR = new Locale("tr", "TR");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Pattern TIME = Pattern.compile("(?i)(?:saat\\s*)?\\b([01]?\\d|2[0-3])[:.]([0-5]\\d)\\b");
    private static final Pattern PART_OF_DAY_TIME = Pattern.compile("(?i)\\b(sabah|öğlen|ogle|akşam|aksam|gece)\\s+([0-9]{1,2})(?:[:.]([0-5]\\d))?\\b");
    private static final Pattern DURATION_MIN = Pattern.compile("(?i)\\b(\\d{1,3})\\s*(dk|dakika)\\b");
    private static final Pattern DURATION_HOUR = Pattern.compile("(?i)\\b(\\d{1,2})(?:[,.](\\d))?\\s*(saat|sa)\\b");
    private static final Pattern FULL_DATE = Pattern.compile("\\b([0-3]?\\d)[./-]([01]?\\d)[./-](20\\d{2})\\b");
    private static final Pattern SHORT_DATE = Pattern.compile("\\b([0-3]?\\d)[./]([01]?\\d)\\b");

    private static final Map<String, DayOfWeek> DAYS = new HashMap<>();
    static {
        DAYS.put("pazartesi", DayOfWeek.MONDAY);
        DAYS.put("salı", DayOfWeek.TUESDAY);
        DAYS.put("sali", DayOfWeek.TUESDAY);
        DAYS.put("çarşamba", DayOfWeek.WEDNESDAY);
        DAYS.put("carsamba", DayOfWeek.WEDNESDAY);
        DAYS.put("perşembe", DayOfWeek.THURSDAY);
        DAYS.put("persembe", DayOfWeek.THURSDAY);
        DAYS.put("cuma", DayOfWeek.FRIDAY);
        DAYS.put("cumartesi", DayOfWeek.SATURDAY);
        DAYS.put("pazar", DayOfWeek.SUNDAY);
    }

    private NaturalLanguageParser() {}

    public static ParsedTask parse(String raw, LocalDate baseDate) {
        if (baseDate == null) baseDate = LocalDate.now();
        String original = raw == null ? "" : raw.trim();
        String work = original;
        String normalized = normalize(original);

        ParsedTask out = new ParsedTask();
        out.original = original;
        out.dayKey = baseDate.format(DAY_FMT);
        out.durationMinutes = 0;
        out.priority = 2;
        out.recurrence = NONE;

        if (contains(normalized, "her gun")) {
            out.recurrence = DAILY;
            work = removePhrase(work, "her gün", "her gun");
        } else if (contains(normalized, "hafta ici") || contains(normalized, "her hafta ici")) {
            out.recurrence = WEEKDAYS;
            work = removePhrase(work, "her hafta içi", "hafta içi", "hafta ici", "her hafta ici");
        } else if (contains(normalized, "her hafta") || contains(normalized, "haftalik")) {
            out.recurrence = WEEKLY;
            work = removePhrase(work, "her hafta", "haftalık", "haftalik");
        } else if (contains(normalized, "her ay") || contains(normalized, "aylik")) {
            out.recurrence = MONTHLY;
            work = removePhrase(work, "her ay", "aylık", "aylik");
        }

        if (contains(normalized, "acil")) {
            out.priority = 0;
            work = removePhrase(work, "acil");
        } else if (contains(normalized, "onemli") || contains(normalized, "önemli")) {
            out.priority = 1;
            work = removePhrase(work, "önemli", "onemli");
        }

        Matcher dm = DURATION_MIN.matcher(work);
        if (dm.find()) {
            out.durationMinutes = safeInt(dm.group(1), 0);
            work = dm.replaceFirst(" ");
        } else {
            Matcher dh = DURATION_HOUR.matcher(work);
            if (dh.find()) {
                int h = safeInt(dh.group(1), 0);
                int tenth = safeInt(dh.group(2), 0);
                out.durationMinutes = h * 60 + tenth * 6;
                work = dh.replaceFirst(" ");
            }
        }

        LocalDate date = baseDate;
        Matcher fd = FULL_DATE.matcher(work);
        if (fd.find()) {
            try {
                date = LocalDate.of(safeInt(fd.group(3), baseDate.getYear()), safeInt(fd.group(2), 1), safeInt(fd.group(1), 1));
                work = fd.replaceFirst(" ");
            } catch (Exception ignored) {}
        } else {
            Matcher sd = SHORT_DATE.matcher(work);
            if (sd.find()) {
                try {
                    int d = safeInt(sd.group(1), baseDate.getDayOfMonth());
                    int m = safeInt(sd.group(2), baseDate.getMonthValue());
                    date = LocalDate.of(baseDate.getYear(), m, d);
                    if (date.isBefore(baseDate.minusDays(1))) date = date.plusYears(1);
                    work = sd.replaceFirst(" ");
                } catch (Exception ignored) {}
            } else if (contains(normalized, "obur gun") || contains(normalized, "öbür gün")) {
                date = baseDate.plusDays(2);
                work = removePhrase(work, "öbür gün", "obur gun");
            } else if (contains(normalized, "yarin")) {
                date = baseDate.plusDays(1);
                work = removePhrase(work, "yarın", "yarin");
            } else if (contains(normalized, "bugun")) {
                date = baseDate;
                work = removePhrase(work, "bugün", "bugun");
            } else {
                String normWork = normalize(work);
                for (Map.Entry<String, DayOfWeek> e : DAYS.entrySet()) {
                    if (contains(normWork, e.getKey())) {
                        date = nextOrSame(baseDate, e.getValue());
                        if (out.recurrence.isEmpty() && contains(normWork, "her " + e.getKey())) out.recurrence = WEEKLY;
                        work = removePhrase(work, "her " + e.getKey(), e.getKey());
                        break;
                    }
                }
            }
        }
        out.dayKey = date.format(DAY_FMT);

        LocalTime time = null;
        Matcher pt = PART_OF_DAY_TIME.matcher(work);
        if (pt.find()) {
            String part = normalize(pt.group(1));
            int hour = safeInt(pt.group(2), 9);
            int minute = safeInt(pt.group(3), 0);
            if ("aksam".equals(part) || "gece".equals(part)) {
                if (hour < 12) hour += 12;
            } else if ("oglen".equals(part) || "ogle".equals(part)) {
                if (hour < 7) hour += 12;
            }
            if (hour <= 23) time = LocalTime.of(hour, Math.min(59, minute));
            work = pt.replaceFirst(" ");
        } else {
            Matcher tm = TIME.matcher(work);
            if (tm.find()) {
                int hour = safeInt(tm.group(1), 9);
                int minute = safeInt(tm.group(2), 0);
                if (hour <= 23) time = LocalTime.of(hour, minute);
                work = tm.replaceFirst(" ");
            }
        }

        out.dueAt = time == null ? 0L : LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        out.title = cleanTitle(work);
        if (out.title.isEmpty()) out.title = original.isEmpty() ? "Yeni görev" : original;
        return out;
    }

    public static String recurrenceLabel(String rule) {
        if (DAILY.equals(rule)) return "Her gün";
        if (WEEKDAYS.equals(rule)) return "Hafta içi";
        if (WEEKLY.equals(rule)) return "Her hafta";
        if (MONTHLY.equals(rule)) return "Her ay";
        return "Tekrar yok";
    }

    private static LocalDate nextOrSame(LocalDate base, DayOfWeek target) {
        int delta = target.getValue() - base.getDayOfWeek().getValue();
        if (delta < 0) delta += 7;
        return base.plusDays(delta);
    }

    private static String cleanTitle(String s) {
        if (s == null) return "";
        return s.replaceAll("(?i)\\b(saat|tarih|hatırlat|hatirlat)\\b", " ")
                .replaceAll("\\s+", " ")
                .replaceAll("^[,;:\\-–— ]+|[,;:\\-–— ]+$", "")
                .trim();
    }

    private static boolean contains(String normalized, String phrase) {
        return (" " + normalized + " ").contains(" " + normalize(phrase) + " ");
    }

    private static String removePhrase(String s, String... phrases) {
        String out = s == null ? "" : s;
        for (String p : phrases) out = out.replaceAll("(?iu)\\b" + Pattern.quote(p) + "\\b", " ");
        return out.replaceAll("\\s+", " ").trim();
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String x = s.toLowerCase(TR)
                .replace('ı', 'i').replace('ğ', 'g').replace('ü', 'u')
                .replace('ş', 's').replace('ö', 'o').replace('ç', 'c');
        x = Normalizer.normalize(x, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return x.replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static int safeInt(String s, int fallback) {
        try { return s == null ? fallback : Integer.parseInt(s); } catch (Exception e) { return fallback; }
    }

    public static class ParsedTask {
        public String original;
        public String title;
        public String dayKey;
        public long dueAt;
        public int durationMinutes;
        public int priority;
        public String recurrence;
    }
}
