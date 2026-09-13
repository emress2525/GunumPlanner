# Namaz V8 Faz 3 — Öğren Akademisi Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dini bilgisi olmayan kullanıcıdan ileri ilmihale uzanan, kaynaklı ve mezhep farklarını görünür tutan tamamen native öğrenme akademisi kurmak.

**Architecture:** `feature:learn` içinde kaynaklı `Lesson` veri modeli, yerel ders kataloğu, arama, ilerleme ve quiz motoru bulunur. Her yayınlanabilir ders `sources` ve `madhab` metadata’sına sahip olmak zorundadır. İlerleme yerel saklanır.

**Tech Stack:** Kotlin, Jetpack Compose, local JSON/Kotlin seed content, DataStore.

**Spec:** `docs/superpowers/specs/2026-09-13-namaz-v8-islamic-life-platform-design.md`

## Global Constraints
- Hanefî ana anlatım; Şafiî farkları ayrı etiket.
- Kaynaksız doğrulanmış dini içerik yayınlanmaz.
- Kullanıcıya kişisel fetva yerine öğretici bilgi verilir.
- Ders → alt konu → test → kaynak → geri → kaldığın yer zinciri korunur.

---

### Task 1: Learning domain
**Files:** Create `feature/learn/build.gradle.kts`, `LearningModels.kt`, `LearningCatalog.kt`, tests.
- [ ] `Lesson` için id/title/category/summary/steps/hanafi/shafii/sources/quiz alanlarını zorunlu tutan test yaz.
- [ ] Başlangıç rotası sırasını test et: iman → temizlik → abdest → namaz → okunanlar → beş vakit → dua → Kur’an → ahlak → oruç → zekât → hac → ileri ilmihal.
- [ ] Katalog ve doğrulama fonksiyonlarını uygula.

### Task 2: Namaz Öğreniyorum
**Files:** Create `PrayerLearningContent.kt`, `LessonDetailScreen.kt`.
- [ ] Beş vakit için rekât bazlı hareket/okunanlar/tam rehber verisini ekle.
- [ ] Vitir, cuma, bayram, cenaze, teravih, teheccüd, istihare, hacet, şükür, seferî, hasta, kaza ve cemaat derslerini katalogda görünür yap.
- [ ] Her konuda kaynak ve mezhep farkı kartı göster.

### Task 3: Recitations + Elif-Bâ
**Files:** Create `RecitationContent.kt`, `ElifBaContent.kt`, `ElifBaScreen.kt`.
- [ ] Sübhaneke, Fâtiha, kısa sureler, Ettehiyyatü, Salli-Barik, Rabbena, kunut ve tesbihat için Arapça/okunuş/anlam/kaynak alanları ekle.
- [ ] Elif-Bâ sırasını harf → şekiller → harekeler → cezm → şedde → tenvin → med → birleşik okuma → kelime → ayet → kısa sure olarak uygula.
- [ ] Büyük yazı ve tek görev odaklı başlangıç ekranını ekle.

### Task 4: Quiz + progress
**Files:** Create `LearningProgressStore.kt`, `QuizEngine.kt`, tests.
- [ ] Quiz puanı ve ders tamamlama testlerini yaz.
- [ ] Son ders, tamamlanan dersler ve tekrar önerilerini yerel sakla.
- [ ] Profil seçimine göre önerilen ilk ders değişsin; özellik kilitlenmesin.

### Task 5: UI + integration
**Files:** Create `LearnScreen.kt`; modify app navigation.
- [ ] Kategori, arama, kaldığın yer, günlük ders ve kaynak ekranlarını bağla.
- [ ] Öğren placeholder’ını kaldır.
- [ ] Unit/lint/assemble ve kaynak-metadata sözleşmesini CI’da doğrula.
