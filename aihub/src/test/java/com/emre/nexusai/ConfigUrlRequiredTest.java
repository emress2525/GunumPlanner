package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertThrows;

public class ConfigUrlRequiredTest {
    @Test public void rejectsMissingUrl() {
        String json = "{\"textEndpoints\":[{\"name\":\"A\",\"model\":\"m\"}]}";
        assertThrows(IllegalArgumentException.class, () -> ConfigParser.parse(json));
    }
}
