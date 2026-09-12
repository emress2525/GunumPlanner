package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class LocalTtsContractTest {
    @Test public void speechIsLocalAndroidFeature() {
        assertTrue(NexusArchitecture.USES_ANDROID_TTS);
    }
}
