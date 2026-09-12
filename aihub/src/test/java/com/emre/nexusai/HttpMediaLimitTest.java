package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class HttpMediaLimitTest {
    @Test public void mediaSourceLimitMatchesGateway() {
        assertEquals(3_500_000, MediaGateway.MAX_SOURCE_IMAGE_BYTES);
    }
}
