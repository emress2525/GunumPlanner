# Namaz v5 Learning App Design

## Goal
Build a rich Android prayer and Islamic learning application that is useful both to practicing users and to someone who does not yet know how to pray. The app must feel like a polished consumer product, not a sparse utility.

## Product Principles
- No ads, no mandatory account, no analytics SDK.
- Religious learning content must show sources and must not present school-specific differences as universal rules.
- The app should teach progressively: beginner path first, reference library second.
- Home should be calm but information-dense: next prayer, countdown, six daily times, Hijri date, daily ayah/dua/hadith, prayer tracker, continue-reading card.
- Existing native background adhan, qibla sensor, Quran audio, and widgets are preserved and surfaced more clearly.

## Information Architecture
Bottom navigation: Today, Quran, Learn, Worship, More.

### Today
- Next prayer + seconds countdown
- Six prayer times
- Daily ayah, dua, and hadith cards with source labels
- Prayer completion strip
- Continue Quran card
- Quick actions for Qibla, Adhan, Namaz Hocasi, and Tasbih

### Learn
- Beginner path: What is Islam? -> purification -> wudu -> prayer movements -> prayer words -> five daily prayers -> mosque/cemaat basics
- Namaz Hocasi library: 32 fard, iman essentials, Allah's attributes, ghusl, wudu, tayammum, prayer conditions, fard/wajib/sunnah, invalidators, sujud al-sahw, congregation, Friday, Eid, funeral, travel, illness, qada, khushu/waswasa
- Daily-life ethics: parents, spouse/family, neighbours, work, trade, debt, rights of others, gossip, anger, privacy, social media, waste, illness, death
- Each topic has source notes and school-difference labels where applicable

### Quran
Keep current surah/ayah reader, Turkish translation, audio, favorites, notes, last-read, and khatm support. Improve card hierarchy and reading controls.

### Worship
Prayer tracker, qada counter, fasting tracker, dhikr/tasbih, Ramadan, duas.

### More
Qibla, settings, source policy, privacy, accessibility, app version.

## Visual Design
- Deep emerald header, warm off-white surfaces, muted gold accents.
- Rounded 20-28dp cards, strong hierarchy, generous spacing, no emoji-heavy UI.
- Consistent iconography and compact labels.
- Arabic text given a separate large reading style.
- Learn modules use progress cards and step-by-step expandable lessons.
- Bottom navigation never overlays page content; screens reserve safe-area padding.

## Widgets
- 1x2 next prayer
- 2x2 next prayer + countdown
- 4x2 all six prayer times + Hijri date
- 4x3 prayer times + daily ayah + prayer tracker
- Existing native widget becomes information-dense with city, Hijri date, tracker, daily ayah, and six times.

## Reliability
- New package id `app.namaz.tr.v5` avoids confusion with old experimental builds.
- Visible app label `Namaz V5` and in-app version marker make the installed build unmistakable.
- CI must run patch tests, Android unit tests, assemble, apksigner verify, unzip integrity, package-id check, and asset-content checks.

## Delivery Scope
This iteration focuses on the complete visual/learning shell, rich Islamic learning content, improved widgets, preserved Quran/qibla/adhan native bridges, and a verified single APK. Advanced offline Quran dataset packaging and map-based mosque search remain outside this APK iteration because they require separate licensed datasets/provider integration.