package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class BlankPromptTest {
    @Test public void nullPromptNormalizesToEmpty() {
        assertEquals("", NexusAiEngine.normalizePrompt(null));
    }
}
