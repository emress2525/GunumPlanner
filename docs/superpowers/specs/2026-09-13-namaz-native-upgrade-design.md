# Namaz Native Upgrade Design

## Scope
Upgrade the current Namaz Android app from a mostly-WebView shell into a hybrid/native Android app where device-dependent features are implemented natively and the existing Quran/meal UI is preserved where practical.

## Goals
1. Fix bottom-navigation overlap so Quran text, meal text and transient messages never sit under the navigation bar.
2. Replace the current visual/static qibla display with real sensor-driven qibla guidance using accelerometer + magnetometer and the user’s location, with calibration/accuracy feedback.
3. Add reliable Quran audio controls: play verse, play surah, pause, resume, stop, playback progress, and graceful fallback when audio is unavailable.
4. Add scheduled prayer-time notifications with optional adhan audio that can fire while the app is backgrounded/closed, with user-selectable modes: full adhan, short tone, notification-only, off.
5. Add an Android home-screen widget that shows next prayer, remaining time, and optionally a daily ayah; tapping opens the app.
6. Add a sourced “İslami Bilgiler” area focused on practical social life (family, parents, spouse, neighbours, work, trade, debt, rights, social media, privacy, anger, disputes, friendship, hospitality, illness, travel, cleanliness, daily manners). Each item must show its source and avoid presenting disputed rulings as universally settled.
7. Preserve privacy-first behaviour: no forced account, no ads, no analytics SDK, and store personal tracking data locally.

## Architecture
### Native Android layer
- MainActivity hosts the primary navigation and app screens.
- QiblaService/Controller reads SensorManager data and computes bearing to the Kaaba from current/selected coordinates.
- PrayerScheduler uses AlarmManager for exact prayer-time alarms when permitted; falls back to WorkManager/inexact scheduling where needed by Android restrictions.
- AdhanPlayer uses MediaPlayer/ExoPlayer-equivalent native playback with stop/pause handling and foreground notification controls when audio is active.
- AppWidgetProvider renders next prayer, countdown snapshot and optional daily ayah.
- Notification channel(s) separate adhan audio from silent prayer reminders so users can control sound at OS level.

### Content/UI layer
- Existing HTML/CSS/JS Quran/meal UI may remain embedded temporarily, but all device capabilities are bridged through explicit native interfaces instead of browser capability detection.
- Bottom navigation layout must reserve safe-area/inset space using WindowInsets and content padding; no overlaying controls without compensating content inset.
- Audio errors appear as non-blocking inline status/snackbar above the navigation bar.

## Qibla computation
- Request coarse/fine location only when qibla is opened or automatic city/location mode is selected.
- If permission is denied, allow manual city selection and compute bearing from stored coordinates.
- Compute great-circle initial bearing to Kaaba coordinates (21.4225, 39.8262).
- Combine magnetic heading with current bearing; display deviation in degrees and directional arrow.
- Show sensor accuracy state and a calibration hint when SensorManager reports unreliable/low accuracy.
- Do not claim precision when a device lacks required sensors; fall back to map/bearing-only mode.

## Quran audio
- Each ayah card gets play/stop status that does not collide with fixed navigation.
- Surah-level controls: play, pause/resume, stop.
- Only one audio stream may play at a time.
- Audio state must survive screen redraws and expose a persistent mini-player above bottom navigation while playing.
- If a remote recitation source fails, show a clear retry message rather than “device unsupported” unless the device truly lacks required playback capability.

## Prayer alarms and adhan
- Each prayer can be enabled/disabled independently.
- User chooses full adhan, short tone, silent notification, or off.
- A visible notification contains Stop and, where appropriate, Snooze/Remind-later controls.
- Re-schedule alarms after reboot, timezone change, date change, and prayer-time refresh.
- Respect Do Not Disturb / OS notification permissions; never bypass system policy.
- Android 12+ exact-alarm restrictions must be handled transparently; if exact scheduling is unavailable, inform the user and use best-effort timing.

## Widget
- Native Android AppWidget with at least one compact layout.
- Shows next prayer, time and remaining duration; optional daily ayah line.
- Refresh at prayer transitions and periodically within OS limits.
- Tapping opens the main screen; a secondary action may open Quran or settings if layout permits.

## Islamic knowledge quality rules
- No unsourced generated religious rulings are stored as authoritative content.
- Quran references show surah/ayah.
- Hadith references show collection and identifier where available.
- Jurisprudential differences are labelled as differences rather than flattened into one claim.
- Social-life guidance is phrased as practical guidance and distinguished from binding legal/religious rulings.
- Content source metadata is visible to the user.

## Testing
- Unit tests: qibla bearing calculation, prayer scheduling calculations, next-prayer selection, audio state machine.
- UI tests: bottom navigation never covers Quran/meal text or snackbars; mini-player remains above navigation.
- Instrumentation tests: launch, permission denial paths, missing-sensor fallback, notification permission denial, stop-adhan action.
- Build verification: Gradle assemble, Android apksigner verification, ZIP integrity.
- Manual device checklist: install/update, app relaunch, background alarm, reboot reschedule, qibla rotation response, Quran audio controls, widget refresh.

## Acceptance criteria
- No Quran or snackbar text is hidden under bottom navigation on the user’s device class.
- Qibla arrow responds to phone rotation and uses live sensor data; if unavailable, the UI explicitly enters fallback mode.
- Quran audio can be started, paused/resumed and stopped without the current “device unsupported” false error.
- Enabled adhan/reminder fires while the app is backgrounded, subject to Android permissions and battery policies, and can be stopped from a notification/action.
- Home-screen widget is addable and shows prayer information.
- Islamic social-life content is source-labelled and avoids unsupported certainty.
- APK is built from source in one Gradle build and passes apksigner verification.
