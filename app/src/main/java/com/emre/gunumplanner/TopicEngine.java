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
            "görev","gorev","yap","et","bak","ara","al","ver","the","and","for","with","this","that","from","into","about","note"
    ));

    private TopicEngine() {}

    public static long findOrCreateTopic(Db db, String title, String body) {
        String text = ((title == null ? "" : title) + " " + (body == null ? "" : body)).trim();
        long match = findMatchingTopic(db, title, body, 0.28);
        return match > 0 ? match : db.createTopic(suggestTitle(text));
    }

    public static long findMatchingTopic(Db db, String title, String body) { return findMatchingTopic(db, title, body, 0.30); }

    public static long findMatchingTopic(Db db, String title, String body, double threshold) {
        String text = ((title == null ? "" : title) + " " + (body == null ? "" : body)).trim();
        Set<String> incoming = tokens(text);
        if (incoming.isEmpty()) return 0;
        Db.Topic best = null; double bestScore = 0.0;
        for (Db.Topic topic : db.getTopics()) {
            if (!topic.autoGroup) continue;
            double score = similarity(incoming, tokens(db.getTopicCorpus(topic.id)));
            if (score > bestScore) { bestScore = score; best = topic; }
        }
        return best != null && bestScore >= threshold ? best.id : 0;
    }

    public static List<TopicMatch> similarTopics(Db db, long sourceTopicId) {
        List<TopicMatch> out = new ArrayList<>(); Set<String> source = tokens(db.getTopicCorpus(sourceTopicId));
        if (source.isEmpty()) return out;
        for (Db.Topic t : db.getTopics()) {
            if (t.id == sourceTopicId) continue;
            double score = similarity(source, tokens(db.getTopicCorpus(t.id)));
            if (score >= 0.18) { TopicMatch m = new TopicMatch(); m.topic = t; m.score = score; out.add(m); }
        }
        out.sort((a,b)->Double.compare(b.score,a.score));
        return out.size()>5 ? new ArrayList<>(out.subList(0,5)) : out;
    }

    public static void refreshAutoTitle(Db db,long topicId){Db.Topic topic=db.getTopic(topicId);if(topic==null||topic.locked||db.countItemsInTopic(topicId)<2)return;String suggestion=suggestTitleFromFrequency(db.getTopicCorpus(topicId));if(!suggestion.trim().isEmpty())db.renameTopic(topicId,suggestion,false);}
    public static String suggestTitle(String text){List<String> ts=new ArrayList<>(tokens(text));if(ts.isEmpty())return "Yeni Konu";ts.sort(Comparator.comparingInt(String::length).reversed());return titleCase(String.join(" ",ts.subList(0,Math.min(3,ts.size()))));}

    private static String suggestTitleFromFrequency(String text){Map<String,Integer> freq=new HashMap<>();for(String token:tokensKeepingDuplicates(text))freq.put(token,freq.getOrDefault(token,0)+1);List<Map.Entry<String,Integer>> entries=new ArrayList<>(freq.entrySet());entries.sort((a,b)->{int cmp=Integer.compare(b.getValue(),a.getValue());return cmp!=0?cmp:Integer.compare(b.getKey().length(),a.getKey().length());});List<String> out=new ArrayList<>();for(Map.Entry<String,Integer> e:entries){if(e.getValue()<2&&!out.isEmpty())continue;out.add(e.getKey());if(out.size()==3)break;}return out.isEmpty()?"":titleCase(String.join(" ",out));}
    private static double similarity(Set<String>a,Set<String>b){if(a.isEmpty()||b.isEmpty())return 0.0;Set<String> inter=new HashSet<>(a);inter.retainAll(b);Set<String> union=new HashSet<>(a);union.addAll(b);double j=union.isEmpty()?0:(double)inter.size()/union.size();int strong=0;double prefix=0;for(String x:a){if(b.contains(x)&&x.length()>=5)strong++;for(String y:b){if(x.length()>=5&&y.length()>=5&&commonPrefix(x,y)>=Math.min(5,Math.min(x.length(),y.length())-1)){prefix+=.015;break;}}}return j+Math.min(.25,strong*.08)+Math.min(.10,prefix);}
    private static int commonPrefix(String a,String b){int n=Math.min(a.length(),b.length()),i=0;while(i<n&&a.charAt(i)==b.charAt(i))i++;return i;}
    private static Set<String> tokens(String text){return new HashSet<>(tokensKeepingDuplicates(text));}
    private static List<String> tokensKeepingDuplicates(String text){if(text==null)return Collections.emptyList();String normalized=Normalizer.normalize(text.toLowerCase(new Locale("tr","TR")),Normalizer.Form.NFKC).replaceAll("[^\\p{L}\\p{N}]+"," ");List<String> out=new ArrayList<>();for(String raw:normalized.split("\\s+")){String t=raw.trim();if(t.length()<3||STOP.contains(t)||t.matches("\\d+"))continue;out.add(simpleStem(t));}return out;}
    private static String simpleStem(String t){String[] suffixes={"lardan","lerden","ları","leri","lar","ler","dan","den","dır","dir","dur","dür","lik","lık","luk","lük","yi","yı","yu","yü"};for(String s:suffixes)if(t.length()>s.length()+4&&t.endsWith(s))return t.substring(0,t.length()-s.length());return t;}
    private static String titleCase(String s){String[] parts=s.trim().split("\\s+");StringBuilder sb=new StringBuilder();Locale tr=new Locale("tr","TR");for(String p:parts){if(p.isEmpty())continue;if(sb.length()>0)sb.append(' ');sb.append(p.substring(0,1).toUpperCase(tr));if(p.length()>1)sb.append(p.substring(1));}return sb.toString();}
    public static class TopicMatch { public Db.Topic topic; public double score; }
}
