package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class ErrorMessageTest {
    @Test public void offlineErrorExplainsFailure() {
        String error = TextGateway.ALL_ENDPOINTS_FAILED_MESSAGE.toLowerCase();
        assertTrue(error.contains("bağlant"));
    }
}
