package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class RemoteConfigTimeoutTest {
    @Test public void remoteConfigTimeoutIsShort() {
        assertTrue(RemoteConfigRepository.TIMEOUT_MS > 0);
        assertTrue(RemoteConfigRepository.TIMEOUT_MS <= 10000);
    }
}
