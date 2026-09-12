package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertFalse;

public class NoWebViewClassTest {
    @Test public void providerWebViewIsDisabledByDesign() {
        assertFalse(NexusArchitecture.USES_PROVIDER_WEBVIEW);
    }
}
