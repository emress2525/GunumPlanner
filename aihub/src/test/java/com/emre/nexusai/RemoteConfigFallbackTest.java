package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class RemoteConfigFallbackTest {
    @Test public void fallbackAssetNameIsStable() {
        assertEquals("nexus-config.json", RemoteConfigRepository.DEFAULT_ASSET_NAME);
    }
}
