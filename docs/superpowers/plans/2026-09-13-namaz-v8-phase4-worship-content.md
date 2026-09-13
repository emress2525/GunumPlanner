# Namaz V8 Faz 4 — İbadet ve Güvenilir Bilgi Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dua, zikir, Esma, hadis, Ramazan, Hac/Umre, kadın kayıtları, seyahat bilgisi, takvim, zekât yardımcısı ve kaynaklı yerel dini aramayı tek native İbadet merkezinde sunmak.

**Architecture:** `feature:worship` kaynaklı içerik kataloğu ve araç ekranlarından oluşur. Dini asistan bir üretken model değil, yerel kaynak kataloğunda eşleşen maddeleri döndüren güvenli arama yardımcısıdır; kaynak yoksa cevap üretmez. Hassas kayıtlar yalnız cihazda tutulur.

**Tech Stack:** Kotlin, Compose, DataStore/SharedPreferences, Android ICU.

**Spec:** `docs/superpowers/specs/2026-09-13-namaz-v8-islamic-life-platform-design.md`

## Global Constraints
- Dua/hadis/ilmihal içeriklerinde kaynak görünür.
- Kaynak bulunamazsa dini asistan “doğrulanmış kaynak bulamadım” diyerek durur.
- Kadınlara özel kayıtlar kullanıcı kontrollü ve yereldir; varsayılan dışa aktarmaya girmez.
- Uygulama kullanıcıyı otomatik seferî ilan etmez.

---

### Task 1: Sourced worship catalog
**Files:** Create `feature/worship/build.gradle.kts`, `WorshipModels.kt`, `WorshipCatalog.kt`, tests.
- [ ] Dua, hadis, Esma ve ilmihal maddesinde source alanını zorunlu kılan test yaz.
- [ ] Sabah/akşam, namaz sonrası, yolculuk, sıkıntı, şükür ve temel günlük duaları kaynaklı ekle.
- [ ] 99 isim için ad/anlam/kısa açıklama alanlarını ekle.

### Task 2: Dhikr + local history
**Files:** Create `DhikrStore.kt`, `DhikrScreen.kt`.
- [ ] Hedef, sayaç, titreşim tercihi ve günlük geçmiş hesabı testini yaz.
- [ ] Hazır ve özel zikir ekleme/sıfırlama/geçmiş akışını uygula.

### Task 3: Ramadan/Hajj/Umrah/Calendar
**Files:** Create `RamadanScreen.kt`, `HajjUmrahScreen.kt`, `IslamicCalendarScreen.kt`.
- [ ] Oruç günlüğü, sahur/iftar notu, mukabele hedefi ve temel fitre/fidye rehberini kaynaklı göster.
- [ ] Hac/Umre adımlarını checklist + offline dua kartları olarak ekle.
- [ ] Hicrî/Gregoryen tarih ve önemli dini günleri Android ICU üzerinden göster; tarihlerin yerel otoriteye göre farklılaşabileceğini not et.

### Task 4: Women/private + travel + zakat
**Files:** Create `PrivateWorshipStore.kt`, `WomenModeScreen.kt`, `TravelGuideScreen.kt`, `ZakatCalculatorScreen.kt`.
- [ ] Namaz takibinde duraklatma ve oruç/kaza sayacını yerel kayıtla ekle.
- [ ] Seyahat ekranında kriterleri kaynaklı öğret; otomatik hüküm verme.
- [ ] Zekât hesap yardımcısında nakit/altın/ticari mal/borç girdileri ve açıklayıcı sonuç ekle; fetva yerine yaklaşık hesap etiketi kullan.

### Task 5: Source-grounded assistant
**Files:** Create `KnowledgeAssistant.kt`, `KnowledgeScreen.kt`, tests.
- [ ] Sorgu eşleşmesi varsa yalnız kaynaklı maddeleri döndürme testini yaz.
- [ ] Eşleşme yoksa boş/uyarı sonucu döndürme testini yaz.
- [ ] Mezhep farkı ve “kişisel fetva gerekebilir” etiketlerini UI’da göster.

### Task 6: Integration
**Files:** Create `WorshipScreen.kt`; modify app navigation.
- [ ] İbadet ana sayfasını araç kartları ve alt ekranlarla bağla.
- [ ] İbadet placeholder’ını kaldır.
- [ ] Unit/lint/assemble ve “kaynaksız içerik yok” sözleşmesini CI’da doğrula.
