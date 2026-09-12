package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertFalse;

public class EngineModeTest {
    @Test public void audioModeDoesNotRequireCloudTextRequestByItself() {
        assertFalse(NexusAiEngine.requiresCloudGeneration(RequestMode.AUDIO));
    }
}
