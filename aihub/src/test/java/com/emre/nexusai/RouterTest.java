package com.emre.nexusai;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class RouterTest {
    @Test public void routesCodingToClaude() {
        assertEquals("CLAUDE", Router.route("Kotlin ile Android uygulaması kodla ve test et"));
    }

    @Test public void routesResearchToPerplexity() {
        assertEquals("PERPLEXITY", Router.route("Bu konuyu araştır, güncel kaynakları göster"));
    }

    @Test public void routesImageToGrok() {
        assertEquals("GROK", Router.route("Bana sinematik bir görsel oluştur"));
    }

    @Test public void routesVideoToGemini() {
        assertEquals("GEMINI", Router.route("Bu görselden kısa bir video üret"));
    }

    @Test public void routesVoiceToElevenLabs() {
        assertEquals("ELEVENLABS", Router.route("Bu metni doğal bir sesle seslendir"));
    }

    @Test public void routesMusicToSuno() {
        assertEquals("SUNO", Router.route("Enerjik bir şarkı ve müzik üret"));
    }

    @Test public void routesGeneralToChatGPT() {
        assertEquals("CHATGPT", Router.route("Bugün için yaratıcı fikirler ver"));
    }
}
