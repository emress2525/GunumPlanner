package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class RemoteConfigUrlTest {
    @Test public void configComesFromProjectRepository() {
        assertTrue(RemoteConfigRepository.REMOTE_CONFIG_URL.contains("emress2525/GunumPlanner"));
        assertTrue(RemoteConfigRepository.REMOTE_CONFIG_URL.contains("ai-nexus-apk"));
    }
}
