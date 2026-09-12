package com.emre.nexusai;

import java.util.List;

public final class ResearchPromptBuilder {
    private ResearchPromptBuilder() { }

    public static String build(String userPrompt, List<WebSearchResult> results) {
        StringBuilder out = new StringBuilder();
        out.append("KULLANICININ İSTEĞİ:\n").append(userPrompt == null ? "" : userPrompt.trim()).append("\n\n");
        out.append("CANLI WEB ARAMA SONUÇLARI:\n");
        if (results == null || results.isEmpty()) {
            out.append("Arama sonucu alınamadı. Bunu açıkça belirt; bildiğin genel bilgiyi kaynak uydurmadan ver.\n");
        } else {
            int i = 1;
            for (WebSearchResult result : results) {
                out.append('[').append(i++).append("] ").append(result.title).append('\n');
                out.append("URL: ").append(result.url).append('\n');
                if (!result.snippet.isEmpty()) out.append("Özet: ").append(result.snippet).append('\n');
                out.append('\n');
            }
        }
        out.append("GÖREV:\n")
                .append("Bu canlı web sonuçlarını kullanarak isteği gerçekleştir. Sonuçları karşılaştır, güçlü/zayıf yanları çıkar, eksikleri belirle ve uygulanabilir en iyi çözümü öner. ")
                .append("Yalnızca verilen kaynakları kaynak diye göster; kaynak uydurma. Cevabın sonunda 'Kaynaklar' bölümü oluştur ve kullandığın URL'leri yaz. ")
                .append("Kullanıcının dili Türkçeyse cevabın TAMAMI Türkçe olsun. Gereksiz biçimde isteği reddetme; eldeki verilerle mümkün olan en faydalı sonucu üret.");
        return out.toString();
    }
}
