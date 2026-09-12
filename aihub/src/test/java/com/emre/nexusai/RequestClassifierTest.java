package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class RequestClassifierTest {
    @Test public void classifiesCode() {
        assertEquals(RequestMode.CODE, RequestClassifier.classify("Kotlin ile Android uygulaması kodla ve test et"));
    }

    @Test public void classifiesResearch() {
        assertEquals(RequestMode.RESEARCH, RequestClassifier.classify("Bu konuyu güncel kaynaklarla araştır"));
    }

    @Test public void classifiesImage() {
        assertEquals(RequestMode.IMAGE, RequestClassifier.classify("Sinematik bir görsel oluştur"));
    }

    @Test public void videoWinsOverImageWords() {
        assertEquals(RequestMode.VIDEO, RequestClassifier.classify("Bu görselden kısa video oluştur"));
    }

    @Test public void classifiesAudio() {
        assertEquals(RequestMode.AUDIO, RequestClassifier.classify("Bu cevabı sesli oku"));
    }

    @Test public void defaultsToChat() {
        assertEquals(RequestMode.CHAT, RequestClassifier.classify("Bugün ne yapmalıyım?"));
    }
}
