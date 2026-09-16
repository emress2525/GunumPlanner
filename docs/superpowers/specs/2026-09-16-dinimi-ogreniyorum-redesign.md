# Dinimi Öğreniyorum Redesign Spec

## Goal
Transform the verified Namaz V2 RC1 into **Dinimi Öğreniyorum**, a Turkish red-and-white native Android learning app whose controls remain visible above Android system bars and whose Islamic learning content is substantially deeper and more structured.

## Approved product direction
- App display name: **Dinimi Öğreniyorum**.
- Visual identity: Turkish-flag-inspired red/white palette and a white crescent/star icon on red.
- Keep the application native Android; do not introduce WebView/CSS for the main UI.
- Preserve prayer alarms, Quran, qibla, worship tracking, hadith download, backup, widget, and Billing features.
- Fix bottom navigation overlap on Android 15/16 using real system-bar insets rather than fixed spacing.
- Make touch targets at least 48dp and avoid fixed-screen assumptions.
- Make learning the primary product identity: structured beginner path plus a broader searchable reference catalog.
- Keep religious-source transparency and madhhab notes. Do not present differing jurisprudential views as if there were no differences.
- Do not copy proprietary full-text Diyanet/TDV books; use source references and openly licensed data already used by the app.

## UI design
- Primary: Turkish red `#E30A17`.
- Dark red: `#B10814`.
- Background: `#F7F7F8`.
- Surfaces: white.
- Text: charcoal `#202124`; secondary `#667085`.
- Header is red with white brand text.
- Bottom navigation is white with red labels/icons and receives dynamic bottom system inset padding.
- Hero/prayer summary uses red and white; content cards stay white with subtle red borders/accents.
- App icon uses red background, white crescent, and white five-point star.

## Learning content
- Increase searchable learning coverage to at least 150 topics including aqidah, Quran literacy, hadith literacy, purification, prayer, fasting, zakat, hajj/umrah, family/social ethics, daily adab, Islamic history, and source literacy.
- Expand the step-by-step worship guide catalog to at least 30 guides.
- Expand sirah timeline to at least 35 milestones.
- Add a structured beginner learning path with at least 18 lessons. Each lesson must contain an objective, explanation, practical actions, source references, and an optional madhhab note.
- Keep at least 20 Quran duas and 99 Esma entries.

## Verification
- Core catalog contract tests enforce minimum content counts and search coverage.
- Android unit tests pass.
- Android lint passes with abort-on-error.
- Debug APK and release AAB candidate build.
- APK signature verifies.
- API 35 emulator installs and cold-starts the APK, executes 200 Monkey UI events, process remains alive, and logcat has no app FATAL EXCEPTION or ANR.
