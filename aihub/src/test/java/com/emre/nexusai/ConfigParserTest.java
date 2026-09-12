package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ConfigParserTest {
    @Test public void parsesHttpsEndpointsInOrder() throws Exception {
        String json = "{\"textEndpoints\":[" +
                "{\"name\":\"Kilo\",\"url\":\"https://api.kilo.ai/api/openrouter/chat/completions\",\"model\":\"kilo-auto/free\"}," +
                "{\"name\":\"Cehpoint\",\"url\":\"https://ai-api.cehpoint.co.in/v1/chat/completions\",\"model\":\"cehpoint-ai\"}]," +
                "\"imageEndpoint\":\"https://quillly.com/api/image-generator/generate\"}";
        NexusConfig config = ConfigParser.parse(json);
        assertEquals(2, config.textEndpoints.size());
        assertEquals("Kilo", config.textEndpoints.get(0).name);
        assertEquals("Cehpoint", config.textEndpoints.get(1).name);
    }

    @Test public void rejectsCleartextEndpoint() {
        String json = "{\"textEndpoints\":[{\"name\":\"Bad\",\"url\":\"http://example.com/chat\",\"model\":\"x\"}]}";
        assertThrows(IllegalArgumentException.class, () -> ConfigParser.parse(json));
    }

    @Test public void rejectsEmptyTextEndpointList() {
        assertThrows(IllegalArgumentException.class, () -> ConfigParser.parse("{\"textEndpoints\":[]}"));
    }
}
