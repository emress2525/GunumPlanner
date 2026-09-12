package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ConfigVersionTest {
    @Test public void parsesVersion() throws Exception {
        NexusConfig config = ConfigParser.parse("{\"version\":2,\"textEndpoints\":[{\"name\":\"A\",\"url\":\"https://a.example/chat\",\"model\":\"m\"}]}");
        assertEquals(2, config.version);
    }
}
