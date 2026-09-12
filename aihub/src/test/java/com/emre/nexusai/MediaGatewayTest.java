package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class MediaGatewayTest {
    @Test public void mediaEndpointsMustBeHttps() {
        NexusConfig config = new NexusConfig();
        config.imageEndpoint = "https://quillly.com/api/image-generator/generate";
        config.videoEndpoint = "https://www.ahm7xmakki.com/api/ptv";
        assertTrue(MediaGateway.hasSafeImageEndpoint(config));
        assertTrue(MediaGateway.hasSafeVideoEndpoint(config));
    }
}
