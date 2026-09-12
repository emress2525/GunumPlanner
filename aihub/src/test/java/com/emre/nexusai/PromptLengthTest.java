package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class PromptLengthTest {
    @Test public void promptLimitIsBounded() {
        assertTrue(NexusAiEngine.MAX_PROMPT_CHARS >= 4000);
        assertTrue(NexusAiEngine.MAX_PROMPT_CHARS <= 50000);
    }
}
