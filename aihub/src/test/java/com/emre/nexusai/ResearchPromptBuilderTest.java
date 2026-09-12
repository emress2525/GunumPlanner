package com.emre.nexusai;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class ResearchPromptBuilderTest {
    @Test
    public void buildsGroundedTurkishResearchPromptWithSourceUrls() {
        String prompt = ResearchPromptBuilder.build(
                "Prompt yazıcı uygulamalarını araştır ve en iyisini tasarla",
                Arrays.asList(
                        new WebSearchResult("Tool A", "https://a.example/", "Feature comparison"),
                        new WebSearchResult("Tool B", "https://b.example/", "User workflow")
                ));

        assertTrue(prompt.contains("Prompt yazıcı uygulamalarını araştır"));
        assertTrue(prompt.contains("https://a.example/"));
        assertTrue(prompt.contains("https://b.example/"));
        assertTrue(prompt.toLowerCase().contains("türkçe"));
        assertTrue(prompt.toLowerCase().contains("kaynak"));
    }
}
