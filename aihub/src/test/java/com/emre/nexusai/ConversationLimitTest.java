package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class ConversationLimitTest {
    @Test public void conversationHistoryIsBounded() {
        assertTrue(NexusAiEngine.MAX_HISTORY_MESSAGES >= 4);
        assertTrue(NexusAiEngine.MAX_HISTORY_MESSAGES <= 40);
    }
}
