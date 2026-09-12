package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.*;

public class ResponseQualityTest {
    @Test
    public void rejectsObservedResearchRefusal() {
        String response = "I cannot fulfill that request. I am instructed to be concise and to avoid fabricating sources or citations, and to state clearly when information is unavailable.";
        assertTrue(ResponseQuality.isResearchRefusal(response));
    }

    @Test
    public void acceptsUsefulResearchAnswer() {
        assertFalse(ResponseQuality.isResearchRefusal("Piyasadaki başlıca araçlar şunlar; güçlü ve zayıf yönleri..."));
    }

    @Test
    public void flagsEnglishAnswerForClearlyTurkishPrompt() {
        String prompt = "Prompt yazıcı uygulamalarını araştır ve en iyisini yap";
        String answer = "I cannot fulfill that request because the information is unavailable and I do not have web access.";
        assertTrue(ResponseQuality.isWrongLanguage(prompt, answer));
    }

    @Test
    public void doesNotFlagTurkishAnswerForTurkishPrompt() {
        String prompt = "Prompt yazıcı uygulamalarını araştır";
        String answer = "Piyasadaki araçları karşılaştırdım. En güçlü özellikler şunlar.";
        assertFalse(ResponseQuality.isWrongLanguage(prompt, answer));
    }
}
