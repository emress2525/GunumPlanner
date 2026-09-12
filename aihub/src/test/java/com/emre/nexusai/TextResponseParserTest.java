package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TextResponseParserTest {
    @Test public void parsesOpenAiCompatibleContent() throws Exception {
        String json = "{\"choices\":[{\"message\":{\"content\":\"Merhaba abi\"}}]}";
        assertEquals("Merhaba abi", TextResponseParser.parseOpenAiResponse(json));
    }

    @Test public void rejectsMissingContent() {
        assertThrows(IllegalArgumentException.class, () -> TextResponseParser.parseOpenAiResponse("{\"choices\":[]}"));
    }
}
