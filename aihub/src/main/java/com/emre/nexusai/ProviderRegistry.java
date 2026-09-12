package com.emre.nexusai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProviderRegistry {
    public static final class Provider {
        public final String key;
        public final String name;
        public final String specialty;
        public final String url;
        public final String badge;

        Provider(String key, String name, String specialty, String url, String badge) {
            this.key = key;
            this.name = name;
            this.specialty = specialty;
            this.url = url;
            this.badge = badge;
        }
    }

    private static final Map<String, Provider> PROVIDERS;

    static {
        LinkedHashMap<String, Provider> map = new LinkedHashMap<>();
        add(map, "CHATGPT", "ChatGPT", "Genel zeka • görsel • dosya", "https://chatgpt.com/", "GENEL");
        add(map, "CLAUDE", "Claude", "Kodlama • uzun işler • analiz", "https://claude.ai/new", "KOD");
        add(map, "GEMINI", "Gemini", "Multimodal • video • araştırma", "https://gemini.google.com/app", "VİDEO");
        add(map, "GROK", "Grok", "Görsel üretim • düzenleme", "https://grok.com/", "GÖRSEL");
        add(map, "PERPLEXITY", "Perplexity", "Güncel web • kaynaklı araştırma", "https://www.perplexity.ai/", "ARAŞTIR");
        add(map, "DEEPSEEK", "DeepSeek", "Muhakeme • kodlama • alternatif model", "https://chat.deepseek.com/", "AKIL");
        add(map, "ELEVENLABS", "ElevenLabs", "Doğal ses • dublaj • TTS", "https://elevenlabs.io/app/speech-synthesis/text-to-speech", "SES");
        add(map, "SUNO", "Suno", "Müzik • şarkı • beste", "https://suno.com/create", "MÜZİK");
        PROVIDERS = Collections.unmodifiableMap(map);
    }

    private ProviderRegistry() {}

    private static void add(Map<String, Provider> map, String key, String name, String specialty, String url, String badge) {
        map.put(key, new Provider(key, name, specialty, url, badge));
    }

    public static Provider get(String key) {
        return PROVIDERS.get(key);
    }

    public static List<Provider> all() {
        return new ArrayList<>(PROVIDERS.values());
    }
}
