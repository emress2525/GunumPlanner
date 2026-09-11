package com.promptnet.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class PromptEngineTest {
    @Test
    public void expandsShortRequestIntoStructuredPrompt() {
        PromptEngine.Result result = PromptEngine.compile("Bana kitap okuma uygulaması yap");
        assertTrue(result.prompt.contains("# FINAL PROMPT"));
        assertTrue(result.prompt.contains("## ACCEPTANCE CRITERIA"));
        assertTrue(result.requirementCount >= 8);
    }

    @Test
    public void codingRequestRequiresRealEnvironmentAndNoPassNoDelivery() {
        PromptEngine.Result result = PromptEngine.compile("Inventor için iLogic kodu yaz");
        assertTrue(result.prompt.contains("NO PASS → NO DELIVERY"));
        assertTrue(result.prompt.contains("gerçek hedef Inventor"));
        assertTrue(result.prompt.contains("Build/compile"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyRequest() {
        PromptEngine.compile("   ");
    }
}
