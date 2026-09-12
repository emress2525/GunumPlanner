package com.emre.nexusai;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.assertEquals;

public class FailoverPolicyTest {
    @Test public void preservesConfiguredOrder() {
        NexusConfig.TextEndpoint a = new NexusConfig.TextEndpoint("A", "https://a.example/chat", "m1");
        NexusConfig.TextEndpoint b = new NexusConfig.TextEndpoint("B", "https://b.example/chat", "m2");
        assertEquals(Arrays.asList(a, b), FailoverPolicy.ordered(Arrays.asList(a, b)));
    }
}
