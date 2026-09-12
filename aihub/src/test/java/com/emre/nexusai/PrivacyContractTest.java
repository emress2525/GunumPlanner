package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class PrivacyContractTest {
    @Test public void privacyNoteMentionsCloudPromptHandling() {
        String note = PrivacyNotice.CLOUD_NOTICE.toLowerCase();
        assertTrue(note.contains("cloud"));
        assertTrue(note.contains("prompt"));
    }
}
