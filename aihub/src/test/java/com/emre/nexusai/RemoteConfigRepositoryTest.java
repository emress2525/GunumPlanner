package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class RemoteConfigRepositoryTest {
    @Test public void remoteConfigUsesHttps() {
        assertTrue(HttpsUrlValidator.isSafe(RemoteConfigRepository.REMOTE_CONFIG_URL));
    }
}
