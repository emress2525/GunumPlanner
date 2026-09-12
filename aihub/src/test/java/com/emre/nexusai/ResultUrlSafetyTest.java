package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertThrows;

public class ResultUrlSafetyTest {
    @Test public void imageResultRejectsCleartextUrl() {
        assertThrows(IllegalArgumentException.class, () -> NexusResult.image("http://example.com/a.png"));
    }

    @Test public void videoResultRejectsJavascriptUrl() {
        assertThrows(IllegalArgumentException.class, () -> NexusResult.video("javascript:alert(1)"));
    }
}
