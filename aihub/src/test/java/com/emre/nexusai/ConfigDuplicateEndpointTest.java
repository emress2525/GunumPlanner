package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ConfigDuplicateEndpointTest {
    @Test public void parserPreservesDeclaredEntries() throws Exception {
        String json = "{\"textEndpoints\":[" +
                "{\"name\":\"A\",\"url\":\"https://a.example\",\"model\":\"m\"}," +
                "{\"name\":\"A2\",\"url\":\"https://a.example\",\"model\":\"m\"}]}";
        assertEquals(2, ConfigParser.parse(json).textEndpoints.size());
    }
}
