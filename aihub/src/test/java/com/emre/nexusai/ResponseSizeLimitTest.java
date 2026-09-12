package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class ResponseSizeLimitTest {
    @Test public void responseLimitIsFinite() {
        assertTrue(HttpUtil.MAX_RESPONSE_BYTES >= 65536);
        assertTrue(HttpUtil.MAX_RESPONSE_BYTES <= 2_000_000);
    }
}
