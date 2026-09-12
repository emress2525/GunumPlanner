package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertFalse;

public class NoSecretKeyTest {
    @Test public void configDoesNotNeedAuthorizationHeader() {
        NexusConfig.TextEndpoint endpoint = new NexusConfig.TextEndpoint("A", "https://a.example", "m");
        assertFalse(endpoint.requiresAuthorization);
    }
}
