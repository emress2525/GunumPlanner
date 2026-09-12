package com.emre.nexusai;

import java.util.Locale;

public final class ResponseQuality {
    private ResponseQuality() { }

    public static boolean isResearchRefusal(String text) {
        String t = normalize(text);
        return containsAny(t,
                "i cannot fulfill", "i can't fulfill", "unable to fulfill",
                "i cannot browse", "i can't browse", "do not have web access", "don't have web access",
                "information is unavailable", "fresh verification is unavailable",
                "bu isteği yerine getirem", "bu talebi yerine getirem",
                "internete erişimim yok", "web erişimim yok", "güncel bilgiye erişem");
    }

    public static boolean isWrongLanguage(String prompt, String answer) {
        if (!looksTurkish(prompt)) return false;
        String a = " " + normalize(answer) + " ";
        int english = countAny(a, " the ", " and ", " to ", " request ", " information ", " unavailable ", " cannot ", " with ", " that ", " because ");
        int turkish = countAny(a, " ve ", " bir ", " için ", " bu ", " ile ", " olarak ", " araştır", " uygulama", " kaynak", " özellik", " en iyi ");
        boolean hasTurkishChars = answer != null && answer.matches(".*[çğıöşüÇĞİÖŞÜ].*");
        return !hasTurkishChars && english >= 3 && english > turkish + 1;
    }

    private static boolean looksTurkish(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        if (text.matches(".*[çğıöşüÇĞİÖŞÜ].*")) return true;
        String t = " " + normalize(text) + " ";
        return countAny(t, " araştır", " uygulama", " istiyorum", " yap ", " en iyi ", " piyasada", " bana ", " eksiksiz", " nasıl ") >= 2;
    }

    private static int countAny(String text, String... needles) {
        int count = 0;
        for (String needle : needles) if (text.contains(needle)) count++;
        return count;
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) if (text.contains(needle)) return true;
        return false;
    }

    private static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
