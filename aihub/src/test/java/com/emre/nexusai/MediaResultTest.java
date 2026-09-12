package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class MediaResultTest {
    @Test public void createsImageResult() {
        NexusResult result = NexusResult.image("https://example.com/a.png");
        assertEquals(NexusResult.Kind.IMAGE, result.kind);
    }

    @Test public void createsVideoResult() {
        NexusResult result = NexusResult.video("https://example.com/a.mp4");
        assertEquals(NexusResult.Kind.VIDEO, result.kind);
    }
}
