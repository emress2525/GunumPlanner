package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertThrows;

public class ConfigRejectsUnsafeMediaTest {
    @Test public void rejectsUnsafeImageUrl() {
        String json = "{\"textEndpoints\":[{\"name\":\"A\",\"url\":\"https://a.example\",\"model\":\"m\"}],\"imageEndpoint\":\"http://bad.example\"}";
        assertThrows(IllegalArgumentException.class, () -> ConfigParser.parse(json));
    }
}
