package com.emre.gunumplanner;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class TopicEngine {
    private static final Set<String> STOP = new HashSet<>(Arrays.asList(
            "ve","ile","bir","bu","şu","o","da","de","için","ama","çok","daha","gibi","olan","olarak",
            "mi","mı","mu","mü","ben","sen","biz","siz","not","notu","bugün","yarın","sonra","önce",
            "the","and","for","with","this","that","from","into","about","note"
    ));

    private TopicEngine() {}

    public static long findOrCreateTopic(Db db, String title, String body) {
        String text = ((title == null ? "" : title) + " " + (body == null ? "" : body)).trim();
        Set<String> incoming = tokens(text);
        if (incoming.isEmpty()) return db.createTopic(suggestTitle(text));

        Db.Topic best = null;
        double bestScore = 0.0;
        for (Db.Topic topic : db.getTopics()) {
            if (!topic.autoGroup) continue;
            Set<String> corpus = tokens(db.getTopicCorpus(topic.id));
            double score = similarity(incoming, corpus);
            if (score > bestScore) {
                bestScore = score;
                best = topic;
            }
        }

        long topicId;
        if (best != null && bestScore >= 0.28) {
            topicId = best.id;
        } else {
            topicId = db.createTopic(suggestTitle(text));
        }
        return topicId;
    }

    public static void refreshAutoTitle(Db db, long topicId) {
        Db.Topic topic = db.getTopic(topicId);
        if (topic == null || topic.locked) return;
        if (db.countNotesInTopic(topicId) < 2) return;
        String corpus = db.getTopicCorpus(topicId);
        String suggestion = suggestTitleFromFrequency(corpus);
        if (!suggestion.trim().isEmpty()) db.renameTopic(topicId, suggestion, false);
    }

    public static String suggestTitle(String text) {
        List<String> ts = new ArrayList<>(tokens(text));
        if (ts.isEmpty()) return "Yeni Konu";
        ts.sort(Comparator.comparingInt(String::length).reversed());
        List<String> chosen = ts.subList(0, Math.min(3, ts.size()));
        return titleCase(String.join(" ", chosen));
    }

    private static String suggestTitleFromFrequency(String text) {
        Map<String, Integer> freq = new HashMap<>();
        for (String token : tokensKeepingDuplicates(text)) {
            freq.put(token, freq.getOrDefault(token, 0) + 1);
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(freq.entrySet());
        entries.sort((a, b) -> {
            int cmp = Integer.compare(b.getValue(), a.getValue());
            if (cmp != 0) return cmp;
            return Integer.compare(b.getKey().length(), a.getKey().length());
        });
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : entries) {
            if (e.getValue() < 2 && !out.isEmpty()) continue;
            out.add(e.getKey());
            if (out.size() == 3) break;
        }
        if (out.isEmpty()) return "";
        return titleCase(String.join(" ", out));
    }

    private static double similarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        double jaccard = union.isEmpty() ? 0 : (double) inter.size() / union.size();

        int strongMatches = 0;
        for (String x : a) if (b.contains(x) && x.length() >= 5) strongMatches++;
        double strongBoost = Math.min(0.25, strongMatches * 0.08);
        return jaccard + strongBoost;
    }

    private static Set<String> tokens(String text) {
        return new HashSet<>(tokensKeepingDuplicates(text));
    }

    private static List<String> tokensKeepingDuplicates(String text) {
        if (text == null) return Collections.emptyList();
        String normalized = Normalizer.normalize(text.toLowerCase(new Locale("tr", "TR")), Normalizer.Form.NFKC)
                .replaceAll("[^\\p{L}\\p{N}]+", " ");
        List<String> out = new ArrayList<>();
        for (String raw : normalized.split("\\s+")) {
            String t = raw.trim();
            if (t.length() < 3 || STOP.contains(t) || t.matches("\\d+")) continue;
            out.add(t);
        }
        return out;
    }

    private static String titleCase(String s) {
        String[] parts = s.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        Locale tr = new Locale("tr", "TR");
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(p.substring(0, 1).toUpperCase(tr));
            if (p.length() > 1) sb.append(p.substring(1));
        }
        return sb.toString();
    }
}
