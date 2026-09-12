package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class TextGatewayContractTest {
    @Test public void timeoutsAreBounded() {
        assertTrue(TextGateway.CONNECT_TIMEOUT_MS > 0);
        assertTrue(TextGateway.CONNECT_TIMEOUT_MS <= 15000);
        assertTrue(TextGateway.READ_TIMEOUT_MS > 0);
        assertTrue(TextGateway.READ_TIMEOUT_MS <= 45000);
    }
}
