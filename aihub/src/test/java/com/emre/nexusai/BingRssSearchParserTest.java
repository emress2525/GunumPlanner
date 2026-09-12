package com.emre.nexusai;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class BingRssSearchParserTest {
    @Test
    public void parsesRssItemsIntoGroundedResults() {
        String xml = "<?xml version=\"1.0\"?><rss><channel>" +
                "<item><title>Prompt &amp; AI Tools</title><link>https://example.com/tools</link><description>Compare &lt;b&gt;prompt&lt;/b&gt; apps.</description></item>" +
                "<item><title>Prompt Guide</title><link>https://example.org/guide</link><description><![CDATA[Templates and testing.]]></description></item>" +
                "</channel></rss>";

        List<WebSearchResult> results = BingRssSearchParser.parse(xml, 5);
        assertEquals(2, results.size());
        assertEquals("Prompt & AI Tools", results.get(0).title);
        assertEquals("https://example.com/tools", results.get(0).url);
        assertTrue(results.get(0).snippet.contains("Compare prompt apps"));
        assertEquals("Templates and testing.", results.get(1).snippet);
    }
}
