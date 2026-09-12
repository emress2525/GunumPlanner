package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ModeLabelTest {
    @Test public void labelsAreTurkishAndStable() {
        assertEquals("Sohbet", RequestMode.CHAT.label);
        assertEquals("Kodlama", RequestMode.CODE.label);
        assertEquals("Araştırma", RequestMode.RESEARCH.label);
        assertEquals("Görsel", RequestMode.IMAGE.label);
        assertEquals("Video", RequestMode.VIDEO.label);
        assertEquals("Ses", RequestMode.AUDIO.label);
    }
}
