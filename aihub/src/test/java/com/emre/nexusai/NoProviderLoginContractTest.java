package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertFalse;

public class NoProviderLoginContractTest {
    @Test public void architectureDoesNotUseProviderWebFlow() {
        assertFalse(NexusArchitecture.USES_PROVIDER_WEBVIEW);
        assertFalse(NexusArchitecture.REQUIRES_PROVIDER_LOGIN);
    }
}
