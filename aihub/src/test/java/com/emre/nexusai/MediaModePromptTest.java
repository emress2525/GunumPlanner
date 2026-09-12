package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class MediaModePromptTest {
    @Test public void engineNormalizesWhitespaceOnly() {
        assertEquals("cat in space", NexusAiEngine.normalizePrompt("  cat in space  "));
    }
}
