package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ResponseTrimTest {
    @Test public void trimsTextContent() throws Exception {
        String json = "{\"choices\":[{\"message\":{\"content\":\"  answer  \"}}]}";
        assertEquals("answer", TextResponseParser.parseOpenAiResponse(json));
    }
}
