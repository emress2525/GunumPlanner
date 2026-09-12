package com.emre.nexusai;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class DuckDuckGoLiteSearchParserTest {
    @Test
    public void parsesLiteResultLinksAndSnippets() {
        String html = "<table>" +
                "<tr><td><a rel=\"nofollow\" class='result-link' href=\"https://example.com/a\">Prompt Tool A</a></td></tr>" +
                "<tr><td class='result-snippet'>Templates &amp; testing features.</td></tr>" +
                "<tr><td><a rel=\"nofollow\" class='result-link' href=\"https://example.org/b\">Prompt Tool B</a></td></tr>" +
                "<tr><td class='result-snippet'>Sharing and versioning.</td></tr>" +
                "</table>";

        List<WebSearchResult> results = DuckDuckGoLiteSearchParser.parse(html, 5);
        assertEquals(2, results.size());
        assertEquals("Prompt Tool A", results.get(0).title);
        assertEquals("https://example.com/a", results.get(0).url);
        assertEquals("Templates & testing features.", results.get(0).snippet);
        assertEquals("https://example.org/b", results.get(1).url);
    }
}
