package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HttpsUrlValidatorTest {
    @Test public void acceptsHttpsOnly() {
        assertTrue(HttpsUrlValidator.isSafe("https://example.com/path"));
        assertFalse(HttpsUrlValidator.isSafe("http://example.com/path"));
        assertFalse(HttpsUrlValidator.isSafe("javascript:alert(1)"));
        assertFalse(HttpsUrlValidator.isSafe(""));
    }
}
