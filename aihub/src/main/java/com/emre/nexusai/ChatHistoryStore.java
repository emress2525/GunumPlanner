package com.emre.nexusai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class ChatHistoryStore {
    private static final String KEY = "nexus_chat_history_v1";

    public interface Storage {
        String get(String key, String fallback);
        void put(String key, String value);
        void remove(String key);
    }

    private final Storage storage;

    public ChatHistoryStore(Storage storage) {
        this.storage = storage;
    }

    public List<ChatHistoryEntry> load() {
        List<ChatHistoryEntry> result = new ArrayList<>();
        if (storage == null) return result;
        String raw = storage.get(KEY, "");
        if (raw == null || raw.trim().isEmpty()) return result;
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) continue;
                String kindValue = item.optString("kind", ChatHistoryEntry.Kind.TEXT.name());
                ChatHistoryEntry.Kind kind;
                try {
                    kind = ChatHistoryEntry.Kind.valueOf(kindValue);
                } catch (Exception ignored) {
                    kind = ChatHistoryEntry.Kind.TEXT;
                }
                result.add(new ChatHistoryEntry(
                        kind,
                        item.optString("label", ""),
                        item.optString("content", ""),
                        item.optString("mode", "")
                ));
            }
        } catch (Exception ignored) {
            // Corrupt local history must never prevent the app from opening.
        }
        return result;
    }

    public void append(ChatHistoryEntry entry) {
        if (entry == null || storage == null) return;
        List<ChatHistoryEntry> entries = load();
        entries.add(entry);
        save(entries);
    }

    public void clear() {
        if (storage != null) storage.remove(KEY);
    }

    private void save(List<ChatHistoryEntry> entries) {
        JSONArray array = new JSONArray();
        for (ChatHistoryEntry entry : entries) {
            try {
                JSONObject item = new JSONObject();
                item.put("kind", entry.kind.name());
                item.put("label", entry.label);
                item.put("content", entry.content);
                item.put("mode", entry.mode);
                array.put(item);
            } catch (Exception ignored) {
                // Skip only the malformed item, keep the rest of the conversation.
            }
        }
        storage.put(KEY, array.toString());
    }
}
