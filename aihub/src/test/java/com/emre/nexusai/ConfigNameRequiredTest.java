package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertThrows;

public class ConfigNameRequiredTest {
    @Test public void rejectsMissingName() {
        String json = "{\"textEndpoints\":[{\"url\":\"https://a.example\",\"model\":\"m\"}]}";
        assertThrows(IllegalArgumentException.class, () -> ConfigParser.parse(json));
    }
}
