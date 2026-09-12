package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class SystemPromptTest {
    @Test public void codeModeRequestsVerification() {
        String prompt = SystemPrompts.forMode(RequestMode.CODE);
        assertTrue(prompt.toLowerCase().contains("test"));
        assertTrue(prompt.toLowerCase().contains("code"));
    }

    @Test public void researchModeRequestsSourceAwareness() {
        String prompt = SystemPrompts.forMode(RequestMode.RESEARCH);
        assertTrue(prompt.toLowerCase().contains("source"));
    }
}
