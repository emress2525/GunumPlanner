package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class NexusResultTest {
    @Test public void createsTextResult() {
        NexusResult result = NexusResult.text("hello");
        assertEquals(NexusResult.Kind.TEXT, result.kind);
        assertEquals("hello", result.content);
    }

    @Test public void createsErrorResult() {
        NexusResult result = NexusResult.error("offline");
        assertEquals(NexusResult.Kind.ERROR, result.kind);
        assertEquals("offline", result.content);
    }
}
