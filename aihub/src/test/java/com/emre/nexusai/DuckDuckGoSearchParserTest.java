package com.emre.nexusai;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class DuckDuckGoSearchParserTest {
    @Test
    public void parsesOrganicResultsAndUnwrapsDuckDuckGoRedirect() {
        String html = "<div class=\"result\">" +
                "<a class=\"result__a\" href=\"//duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.com%2Fbest%3Fa%3D1\">Best &amp; Complete Prompt Apps</a>" +
                "<a class=\"result__snippet\">Compare prompt tools, features and pricing.</a>" +
                "</div>" +
                "<div class=\"result\">" +
                "<a class=\"result__a\" href=\"https://second.example.org/guide\">Prompt engineering guide</a>" +
                "<a class=\"result__snippet\">A practical guide.</a>" +
                "</div>";

        List<WebSearchResult> results = DuckDuckGoSearchParser.parse(html, 5);
        assertEquals(2, results.size());
        assertEquals("Best & Complete Prompt Apps", results.get(0).title);
        assertEquals("https://example.com/best?a=1", results.get(0).url);
        assertTrue(results.get(0).snippet.contains("features"));
        assertEquals("https://second.example.org/guide", results.get(1).url);
    }
}
