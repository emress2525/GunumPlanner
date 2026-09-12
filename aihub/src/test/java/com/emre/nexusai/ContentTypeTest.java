package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ContentTypeTest {
    @Test public void jsonContentTypeIsStable() {
        assertEquals("application/json; charset=utf-8", HttpUtil.JSON_CONTENT_TYPE);
    }
}
