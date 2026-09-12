package com.emre.nexusai;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ProviderRegistryTest {
    @Test public void everySmartRouteHasAProvider() {
        String[] keys = {"CHATGPT", "CLAUDE", "GEMINI", "GROK", "PERPLEXITY", "ELEVENLABS", "SUNO"};
        for (String key : keys) assertNotNull(key, ProviderRegistry.get(key));
    }

    @Test public void includesDeepSeekAsAnAlternative() {
        assertNotNull(ProviderRegistry.get("DEEPSEEK"));
    }

    @Test public void allProviderUrlsUseHttps() {
        for (ProviderRegistry.Provider provider : ProviderRegistry.all()) {
            assertTrue(provider.url.startsWith("https://"));
        }
    }
}
