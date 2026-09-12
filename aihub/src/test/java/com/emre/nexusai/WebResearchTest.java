package com.emre.nexusai;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class WebResearchTest {
    @Test
    public void parsesDuckDuckGoResultsAndUnwrapsRealUrls() {
        String html = "<div class='result'><a class='result__a' href='//duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.com%2Fbest-ai-apps'>Best AI Apps</a>" +
                "<a class='result__snippet'>Compare leading AI tools and features.</a></div>";
        List<WebSearchResult> results = DuckDuckGoSearchParser.parse(html, 5);
        assertEquals(1, results.size());
        assertEquals("Best AI Apps", results.get(0).title);
        assertEquals("https://example.com/best-ai-apps", results.get(0).url);
        assertTrue(results.get(0).snippet.contains("Compare leading AI tools"));
    }

    @Test
    public void researchPromptContainsLiveEvidenceAndSourceUrls() {
        WebSearchResult result = new WebSearchResult("Prompt apps", "https://example.org/prompts", "Feature comparison");
        String context = ResearchPromptBuilder.build("en iyi prompt yazıcı uygulamalarını araştır", java.util.Collections.singletonList(result));
        assertTrue(context.contains("CANLI WEB ARAMA SONUÇLARI"));
        assertTrue(context.contains("https://example.org/prompts"));
        assertTrue(context.contains("Feature comparison"));
        assertTrue(context.contains("en iyi prompt yazıcı"));
    }
}
