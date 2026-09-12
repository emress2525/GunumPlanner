package com.emre.nexusai;

import java.util.Locale;

public final class Router {
    private Router() {}

    public static String route(String prompt) {
        String p = prompt == null ? "" : prompt.toLowerCase(Locale.forLanguageTag("tr-TR"));

        if (containsAny(p, "video", "animasyon", "hareketlendir", "image to video", "text to video", "görselden video")) {
            return "GEMINI";
        }
        if (containsAny(p, "müzik", "şarkı", "beste", "melodi", "beat", "song", "music")) {
            return "SUNO";
        }
        if (containsAny(p, "seslendir", "dublaj", "voice", "text to speech", "tts", "ses üret", "konuştur")) {
            return "ELEVENLABS";
        }
        if (containsAny(p, "görsel", "resim", "fotoğraf", "image", "çizim", "logo", "poster", "illüstrasyon")) {
            return "GROK";
        }
        if (containsAny(p, "araştır", "kaynak", "güncel", "webde ara", "internette ara", "research", "haberleri bul")) {
            return "PERPLEXITY";
        }
        if (containsAny(p, "kod", "kotlin", "java", "python", "javascript", "typescript", "swift", "c#", "c++", "debug", "hata düzelt", "uygulama geliştir")) {
            return "CLAUDE";
        }
        return "CHATGPT";
    }

    private static boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) return true;
        }
        return false;
    }
}
