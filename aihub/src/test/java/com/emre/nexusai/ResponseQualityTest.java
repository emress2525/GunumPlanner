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
    public void rejectsObservedApkCapabilityEvasion() {
        String prompt = "Bana promt yazıcı uygulaması yap telefon için APK ver";
        String answer = "Maalesef ben doğrudan derlenmiş APK dosyası üretemiyorum. Ancak sana çalışan kaynak kod yazabilirim ve bunu kendin derleyebilirsin.";
        assertTrue(ResponseQuality.isTaskRefusal(prompt, answer));
    }

    @Test
    public void acceptsActionableAppBuildAnswer() {
        String prompt = "Bana Android uygulaması yap";
        String answer = "Tam Android proje yapısı aşağıda. settings.gradle, build.gradle, AndroidManifest.xml ve MainActivity.kt dosyalarını eksiksiz veriyorum.";
        assertFalse(ResponseQuality.isTaskRefusal(prompt, answer));
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
