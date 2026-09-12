package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertFalse;

public class ModeSystemPromptCoverageTest {
    @Test public void everyModeHasSystemPrompt() {
        for (RequestMode mode : RequestMode.values()) {
            assertFalse(SystemPrompts.forMode(mode).trim().isEmpty());
        }
    }
}
