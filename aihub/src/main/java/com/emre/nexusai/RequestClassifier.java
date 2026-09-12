package com.emre.nexusai;

import java.util.Locale;

public final class RequestClassifier {
    private RequestClassifier() { }

    public static RequestMode classify(String prompt) {
        String p = prompt == null ? "" : prompt.toLowerCase(new Locale("tr", "TR"));

        // Video must win over image because prompts commonly say "bu görselden video".
        if (containsAny(p, "video", "videoya", "animasyon", "hareketlendir", "klip", "text to video", "image to video")) {
            return RequestMode.VIDEO;
        }

        // Code wins over research when a user asks to research and then fix code.
        if (containsAny(p, "kodla", "kodunu", "kod yaz", "code", "bug", "hata düzelt", "compile", "derle", "test et", "kotlin", "java", "python", "javascript", "android", "api", "sql", "ilogic", "inventor")) {
            return RequestMode.CODE;
        }

        if (containsAny(p, "araştır", "güncel kaynak", "kaynakları", "web'de", "internette", "research", "haber", "son gelişme")) {
            return RequestMode.RESEARCH;
        }

        if (containsAny(p, "görsel", "resim", "fotoğraf", "image", "çiz", "tasarla", "render", "illüstrasyon", "logo oluştur")) {
            return RequestMode.IMAGE;
        }

        if (containsAny(p, "sesli oku", "seslendir", "konuş", "text to speech", "tts", "ses üret")) {
            return RequestMode.AUDIO;
        }

        return RequestMode.CHAT;
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) return true;
        }
        return false;
    }
}
