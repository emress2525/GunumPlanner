package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertThrows;

public class ConfigModelRequiredTest {
    @Test public void rejectsMissingModel() {
        String json = "{\"textEndpoints\":[{\"name\":\"A\",\"url\":\"https://a.example\"}]}";
        assertThrows(IllegalArgumentException.class, () -> ConfigParser.parse(json));
    }
}
