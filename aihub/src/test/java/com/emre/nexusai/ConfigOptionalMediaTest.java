package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ConfigOptionalMediaTest {
    @Test public void mediaEndpointsAreOptional() throws Exception {
        NexusConfig config = ConfigParser.parse("{\"textEndpoints\":[{\"name\":\"A\",\"url\":\"https://a.example\",\"model\":\"m\"}]}");
        assertEquals(1, config.textEndpoints.size());
        assertNull(config.imageEndpoint);
        assertNull(config.videoEndpoint);
    }
}
