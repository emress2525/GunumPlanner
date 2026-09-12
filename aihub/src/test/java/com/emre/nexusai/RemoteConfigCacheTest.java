package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class RemoteConfigCacheTest {
    @Test public void cacheFilenameIsStable() {
        assertEquals("nexus-config-cache.json", RemoteConfigRepository.CACHE_FILE_NAME);
    }
}
