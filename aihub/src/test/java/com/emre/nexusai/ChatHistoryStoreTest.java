package com.emre.nexusai;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ChatHistoryStoreTest {
    @Test
    public void restoresMessagesFromPersistentStorage() {
        MemoryStorage storage = new MemoryStorage();
        ChatHistoryStore first = new ChatHistoryStore(storage);

        first.append(new ChatHistoryEntry(ChatHistoryEntry.Kind.USER, "SEN", "Bana bir uygulama yap", "Kodlama"));
        first.append(new ChatHistoryEntry(ChatHistoryEntry.Kind.TEXT, "NEXUS • KODLAMA", "Tam çalışan proje yapısı...", "Kodlama"));

        ChatHistoryStore reopened = new ChatHistoryStore(storage);
        List<ChatHistoryEntry> restored = reopened.load();

        assertEquals(2, restored.size());
        assertEquals(ChatHistoryEntry.Kind.USER, restored.get(0).kind);
        assertEquals("Bana bir uygulama yap", restored.get(0).content);
        assertEquals(ChatHistoryEntry.Kind.TEXT, restored.get(1).kind);
        assertEquals("Tam çalışan proje yapısı...", restored.get(1).content);
    }

    @Test
    public void clearRemovesPersistedConversation() {
        MemoryStorage storage = new MemoryStorage();
        ChatHistoryStore store = new ChatHistoryStore(storage);
        store.append(new ChatHistoryEntry(ChatHistoryEntry.Kind.USER, "SEN", "Merhaba", "Sohbet"));

        store.clear();

        assertTrue(new ChatHistoryStore(storage).load().isEmpty());
    }

    private static final class MemoryStorage implements ChatHistoryStore.Storage {
        private final Map<String, String> values = new HashMap<>();

        @Override public String get(String key, String fallback) {
            return values.containsKey(key) ? values.get(key) : fallback;
        }

        @Override public void put(String key, String value) {
            values.put(key, value);
        }

        @Override public void remove(String key) {
            values.remove(key);
        }
    }
}
