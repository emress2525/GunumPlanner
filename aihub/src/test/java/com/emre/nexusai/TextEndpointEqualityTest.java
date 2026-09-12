package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TextEndpointEqualityTest {
    @Test public void endpointValueObjectsCompareByFields() {
        NexusConfig.TextEndpoint a = new NexusConfig.TextEndpoint("A", "https://a.example", "m");
        NexusConfig.TextEndpoint b = new NexusConfig.TextEndpoint("A", "https://a.example", "m");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
