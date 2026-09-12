package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ModePriorityTest {
    @Test public void videoHasPriorityOverImage() {
        assertEquals(RequestMode.VIDEO, RequestClassifier.classify("görseli videoya çevir"));
    }

    @Test public void codeHasPriorityOverGenericResearchWord() {
        assertEquals(RequestMode.CODE, RequestClassifier.classify("araştır ve Java kodunu düzelt test et"));
    }
}
