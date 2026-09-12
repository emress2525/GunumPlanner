package com.emre.nexusai;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.assertEquals;

public class RetryCountTest {
    @Test public void eachConfiguredEndpointIsTriedOnce() {
        NexusConfig.TextEndpoint a = new NexusConfig.TextEndpoint("A", "https://a.example", "a");
        NexusConfig.TextEndpoint b = new NexusConfig.TextEndpoint("B", "https://b.example", "b");
        assertEquals(2, FailoverPolicy.ordered(Arrays.asList(a,b)).size());
    }
}
