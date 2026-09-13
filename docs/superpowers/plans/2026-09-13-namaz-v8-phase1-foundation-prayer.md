# Namaz V8 Phase 1 — Native Foundation + Prayer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tamamen native Kotlin/Compose Namaz V8 temelini kurmak ve onboarding, Bugün, Namaz, namaz takibi, ezan/alarm ve Bildirim Sağlık Merkezi ile çalışan ilk V8 APK'yı üretmek.

**Architecture:** V8, repo içinde bağımsız `namaz-v8/` Gradle projesidir; V7'nin WebView/patch kodlarına çalışma zamanında bağımlı değildir. Faz 1'de `app`, `core:model`, `core:settings`, `core:prayer`, `feature:today`, `feature:prayer`, `feature:health` modülleri kurulur. PrayerRepository tek gerçek namaz veri kaynağıdır; Compose ekranları, alarm katmanı ve ileride widget/Wear yüzeyleri aynı repository kontratını kullanır.

**Tech Stack:** compileSdk/targetSdk 35, minSdk 26, Java 17, Gradle 8.9, Android Gradle Plugin 8.7.3, Kotlin 2.0.21, Compose Material 3, Navigation Compose, Room 2.6.1, DataStore 1.1.1, WorkManager 2.10.0, `com.batoulapps.adhan:adhan:1.2.1`, JUnit4, AndroidX test.

**Spec:** `docs/superpowers/specs/2026-09-13-namaz-v8-islamic-life-platform-design.md`

## Global Constraints

- Package: `app.namaz.tr.v8`; app label: `Namaz V8`; versionCode 8; versionName 8.0.0.
- V7 dosyaları değiştirilmez veya silinmez.
- V8 reklam SDK'sı, analytics SDK'sı veya zorunlu oturum açma içermez.
- Namaz verisi yerel cache ile internet kesilince çalışmaya devam eder.
- Türkiye varsayılan hesap yöntemi Adhan `CalculationMethod.TURKEY`; arayüz bunu “Diyanet'e yaklaşık hesap” olarak açıkça etiketler. Resmî Diyanet verisi ileride ayrı doğrulanmış kaynak olarak eklenmedikçe “resmî Diyanet saati” denmez.
- Hanefî/Şafiî ikindi seçimi kullanıcı ayarıdır.
- Manual minute adjustments, alarm ve UI aynı PrayerSchedule verisine uygulanır.
- Exact alarm izni yoksa uygulama teşhis ekranında açıkça gösterir; çalışıyormuş gibi davranmaz.
- Her task failing test → minimal implementation → passing test → commit döngüsüyle yürütülür.

---

## File Map

- `namaz-v8/settings.gradle.kts`: V8 modülleri ve repository yönetimi.
- `namaz-v8/build.gradle.kts`: Android/Kotlin plugin sürümleri.
- `namaz-v8/app/`: application, MainActivity, navigation, theme, manifest.
- `namaz-v8/core/model/`: saf domain modelleri; Android UI bağımlılığı yok.
- `namaz-v8/core/settings/`: DataStore tabanlı kullanıcı tercihleri/onboarding.
- `namaz-v8/core/prayer/`: Adhan hesaplama, Room cache, repository, alarm schedule kontratı.
- `namaz-v8/feature/today/`: Bugün Compose ekranı.
- `namaz-v8/feature/prayer/`: Namaz detay ve takip ekranı.
- `namaz-v8/feature/health/`: izin/alarm/pil sağlık ekranı.
- `.github/workflows/namaz-v8-build.yml`: unit/lint/build/signature/package artifact doğrulaması.

---

### Task 1: Native V8 Gradle project and Compose shell

**Files:**
- Create: `namaz-v8/settings.gradle.kts`
- Create: `namaz-v8/build.gradle.kts`
- Create: `namaz-v8/gradle.properties`
- Create: `namaz-v8/app/build.gradle.kts`
- Create: `namaz-v8/app/src/main/AndroidManifest.xml`
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/MainActivity.kt`
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/ui/NamazApp.kt`
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/ui/theme/NamazTheme.kt`
- Test: `namaz-v8/app/src/test/java/app/namaz/tr/v8/AppIdentityTest.kt`

**Interfaces:**
- Produces: Android application `app.namaz.tr.v8`; root composable `@Composable fun NamazApp()`.

- [ ] **Step 1: Write the failing identity test**

```kotlin
class AppIdentityTest {
    @Test fun expectedIdentityIsStable() {
        assertEquals("app.namaz.tr.v8", BuildConfig.APPLICATION_ID)
        assertEquals(8, BuildConfig.VERSION_CODE)
        assertEquals("8.0.0", BuildConfig.VERSION_NAME)
    }
}
```

- [ ] **Step 2: Run the test and confirm it fails because the V8 project does not exist**

```bash
gradle -p namaz-v8 :app:testDebugUnitTest --tests '*AppIdentityTest' --stacktrace
```

Expected: FAIL before project/module creation.

- [ ] **Step 3: Create Gradle/module configuration**

`namaz-v8/settings.gradle.kts` must include:

```kotlin
pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "NamazV8"
include(":app")
```

`namaz-v8/build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("com.android.library") version "8.7.3" apply false
    kotlin("android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
```

`app/build.gradle.kts` must set compileSdk 35, minSdk 26, targetSdk 35, namespace/applicationId `app.namaz.tr.v8`, Java/Kotlin JVM 17, Compose enabled, versionCode 8 and versionName `8.0.0`.

- [ ] **Step 4: Create minimal Compose activity**

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NamazTheme { NamazApp() } }
    }
}

@Composable
fun NamazApp() {
    Surface(Modifier.fillMaxSize()) { Text("Namaz V8") }
}
```

- [ ] **Step 5: Run unit test and assemble**

```bash
gradle -p namaz-v8 :app:testDebugUnitTest :app:assembleDebug --stacktrace
```

Expected: PASS and `namaz-v8/app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 6: Commit**

```bash
git add namaz-v8
git commit -m "feat: scaffold native Namaz V8 app"
```

---

### Task 2: Modular core, onboarding, Basit/Tam mode and five-tab navigation

**Files:**
- Modify: `namaz-v8/settings.gradle.kts`
- Create: `namaz-v8/core/model/build.gradle.kts`
- Create: `namaz-v8/core/settings/build.gradle.kts`
- Create: `namaz-v8/core/model/src/main/java/app/namaz/tr/v8/model/UserProfile.kt`
- Create: `namaz-v8/core/settings/src/main/java/app/namaz/tr/v8/settings/UserSettingsRepository.kt`
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/ui/navigation/AppDestination.kt`
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/ui/onboarding/OnboardingScreen.kt`
- Modify: `namaz-v8/app/src/main/java/app/namaz/tr/v8/ui/NamazApp.kt`
- Test: `namaz-v8/core/settings/src/test/java/app/namaz/tr/v8/settings/UserProfileRulesTest.kt`

**Interfaces:**
- Produces: `enum class UserProfile`, `enum class AppMode`, `interface UserSettingsRepository { val settings: Flow<UserSettings>; suspend fun setProfile(...); suspend fun setMode(...) }`.

- [ ] **Step 1: Write profile-rule tests**

```kotlin
@Test fun beginnerProfileStartsInSimpleMode() {
    assertEquals(AppMode.SIMPLE, defaultModeFor(UserProfile.RELIGION_FROM_ZERO))
}
@Test fun everyProfileKeepsAllDestinationsAvailable() {
    UserProfile.entries.forEach { assertTrue(availableDestinations(it).containsAll(AppDestination.entries)) }
}
```

- [ ] **Step 2: Run and verify failure**

```bash
gradle -p namaz-v8 :core:settings:testDebugUnitTest --tests '*UserProfileRulesTest*'
```

Expected: FAIL because profile/settings types do not exist.

- [ ] **Step 3: Implement domain enums and settings contract**

```kotlin
enum class UserProfile { RELIGION_FROM_ZERO, NEW_TO_PRAYER, LEARN_QURAN, DAILY_WORSHIP }
enum class AppMode { SIMPLE, FULL }
data class UserSettings(val profile: UserProfile?, val mode: AppMode, val onboardingDone: Boolean)
fun defaultModeFor(profile: UserProfile) = AppMode.SIMPLE
```

DataStore keys must be `profile`, `app_mode`, `onboarding_done`; no account/user identifier is created.

- [ ] **Step 4: Implement onboarding and root navigation**

```kotlin
enum class AppDestination(val title: String) {
    TODAY("Bugün"), QURAN("Kur’an"), LEARN("Öğren"), WORSHIP("İbadet"), MORE("Daha Fazla")
}
```

The onboarding screen must show exactly four approved profile choices and persist selection. Simple mode visually prioritizes TODAY/QURAN and exposes Prayer/Qibla from Today/More, but must not delete or lock feature routes.

- [ ] **Step 5: Run tests and app build**

```bash
gradle -p namaz-v8 :core:settings:testDebugUnitTest :app:assembleDebug
```

- [ ] **Step 6: Commit**

```bash
git add namaz-v8
git commit -m "feat: add V8 onboarding and navigation shell"
```

---

### Task 3: Prayer domain model, Room cache and single repository contract

**Files:**
- Modify: `namaz-v8/settings.gradle.kts`
- Create: `namaz-v8/core/prayer/build.gradle.kts`
- Create: `namaz-v8/core/model/src/main/java/app/namaz/tr/v8/model/Prayer.kt`
- Create: `namaz-v8/core/prayer/src/main/java/app/namaz/tr/v8/prayer/db/PrayerDayEntity.kt`
- Create: `namaz-v8/core/prayer/src/main/java/app/namaz/tr/v8/prayer/db/PrayerDayDao.kt`
- Create: `namaz-v8/core/prayer/src/main/java/app/namaz/tr/v8/prayer/PrayerRepository.kt`
- Create: `namaz-v8/core/prayer/src/test/java/app/namaz/tr/v8/prayer/PrayerScheduleTest.kt`

**Interfaces:**
- Produces: `PrayerName`, `PrayerInstant`, `PrayerSchedule`, `PrayerLocation`, `PrayerMethod`, `PrayerRepository`.
- `PrayerRepository` signature:

```kotlin
interface PrayerRepository {
    fun observeDay(date: LocalDate): Flow<PrayerSchedule?>
    suspend fun schedule(date: LocalDate): PrayerSchedule
    suspend fun refresh(range: ClosedRange<LocalDate>): Result<Unit>
    suspend fun setCompleted(date: LocalDate, prayer: PrayerName, completed: Boolean)
}
```

- [ ] **Step 1: Write schedule ordering/adjustment tests**

```kotlin
@Test fun manualAdjustmentChangesOnlySelectedPrayer() {
    val base = sampleSchedule()
    val adjusted = base.withAdjustments(mapOf(PrayerName.ASR to 3))
    assertEquals(base.asr.instant.plusSeconds(180), adjusted.asr.instant)
    assertEquals(base.maghrib.instant, adjusted.maghrib.instant)
}

@Test fun nextPrayerSkipsSunriseAsTrackablePrayer() {
    assertEquals(PrayerName.DHUHR, sampleSchedule().nextPrayer(after = instantBetweenSunriseAndDhuhr()).name)
}
```

- [ ] **Step 2: Run and verify failure**

```bash
gradle -p namaz-v8 :core:prayer:testDebugUnitTest --tests '*PrayerScheduleTest*'
```

- [ ] **Step 3: Implement models**

```kotlin
enum class PrayerName { FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA }
data class PrayerInstant(val name: PrayerName, val instant: Instant)
data class PrayerSchedule(
    val date: LocalDate,
    val zoneId: String,
    val location: PrayerLocation,
    val methodLabel: String,
    val prayers: List<PrayerInstant>
)
```

Tracking must treat `SUNRISE` as information only, never as one of the five obligatory prayer completion records.

- [ ] **Step 4: Implement Room cache**

Use one `PrayerDayEntity` row per local date/location/method with epoch-millis columns for six times and a `sourceLabel`. DAO exposes range upsert, day flow and stale-row deletion. Persist manual adjustments separately in DataStore; never mutate raw calculated values in DB.

- [ ] **Step 5: Implement repository skeleton with injected calculator/cache/settings**

```kotlin
class DefaultPrayerRepository(
    private val dao: PrayerDayDao,
    private val calculator: PrayerCalculator,
    private val settings: PrayerSettingsStore,
    private val clock: Clock,
) : PrayerRepository { /* day cache -> calculate -> persist -> adjusted domain */ }
```

`schedule(date)` must return cache first when valid; if no cache, calculate locally so first-run offline still has prayer times.

- [ ] **Step 6: Run tests and commit**

```bash
gradle -p namaz-v8 :core:prayer:testDebugUnitTest
git add namaz-v8 && git commit -m "feat: add prayer repository and offline cache"
```

---

### Task 4: Offline Turkey-compatible calculation, madhab and manual adjustments

**Files:**
- Modify: `namaz-v8/core/prayer/build.gradle.kts`
- Create: `namaz-v8/core/prayer/src/main/java/app/namaz/tr/v8/prayer/AdhanPrayerCalculator.kt`
- Create: `namaz-v8/core/prayer/src/main/java/app/namaz/tr/v8/prayer/PrayerSettingsStore.kt`
- Test: `namaz-v8/core/prayer/src/test/java/app/namaz/tr/v8/prayer/AdhanPrayerCalculatorTest.kt`

**Interfaces:**
- Consumes: `PrayerSchedule`, `PrayerLocation` from Task 3.
- Produces:

```kotlin
interface PrayerCalculator {
    fun calculate(date: LocalDate, location: PrayerLocation, config: PrayerCalculationConfig): PrayerSchedule
}
```

- [ ] **Step 1: Add failing Diyanet-reference regression test**

Use the upstream Adhan Turkey regression fixture for Istanbul, 2020-04-16:

```kotlin
@Test fun turkeyMethodMatchesKnownIstanbulFixtureWithinOneMinute() {
    val result = calculator.calculate(
        LocalDate.of(2020, 4, 16),
        PrayerLocation("İstanbul", 41.005616, 28.976380, "Europe/Istanbul"),
        PrayerCalculationConfig(PrayerMethod.TURKEY_APPROX, MadhabChoice.HANAFI)
    )
    assertTime(result, PrayerName.FAJR, "04:44", toleranceMinutes = 1)
    assertTime(result, PrayerName.DHUHR, "13:09", toleranceMinutes = 1)
    assertTime(result, PrayerName.MAGHRIB, "19:52", toleranceMinutes = 1)
}
```

- [ ] **Step 2: Run and verify failure**

```bash
gradle -p namaz-v8 :core:prayer:testDebugUnitTest --tests '*AdhanPrayerCalculatorTest*'
```

- [ ] **Step 3: Add calculation dependency and implementation**

```kotlin
dependencies { implementation("com.batoulapps.adhan:adhan:1.2.1") }
```

Implementation mapping:

```kotlin
val coordinates = Coordinates(location.latitude, location.longitude)
val params = CalculationMethod.TURKEY.parameters
params.madhab = if (config.madhab == MadhabChoice.HANAFI) Madhab.HANAFI else Madhab.SHAFI
val times = PrayerTimes(coordinates, DateComponents(date.year, date.monthValue, date.dayOfMonth), params)
```

Convert returned `Date` instants using `location.zoneId`. Apply user minute adjustments only after calculation. Method label returned to UI must be `Diyanet’e yaklaşık hesap (Adhan Turkey)`; never `Resmî Diyanet`.

- [ ] **Step 4: Implement settings persistence**

Persist: manual city/lat/lon/zone, method, madhab, six minute offsets. Default location may be Ankara only as onboarding fallback until user chooses/permits location; the UI must display the active city clearly.

- [ ] **Step 5: Run regression tests**

```bash
gradle -p namaz-v8 :core:prayer:testDebugUnitTest
```

Expected: Istanbul fixture within specified tolerance; manual adjustment tests pass.

- [ ] **Step 6: Commit**

```bash
git add namaz-v8 && git commit -m "feat: add offline Turkey prayer calculation"
```

---

### Task 5: Bugün screen and prayer detail UI

**Files:**
- Modify: `namaz-v8/settings.gradle.kts`
- Create: `namaz-v8/feature/today/build.gradle.kts`
- Create: `namaz-v8/feature/prayer/build.gradle.kts`
- Create: `namaz-v8/feature/today/src/main/java/app/namaz/tr/v8/today/TodayViewModel.kt`
- Create: `namaz-v8/feature/today/src/main/java/app/namaz/tr/v8/today/TodayScreen.kt`
- Create: `namaz-v8/feature/prayer/src/main/java/app/namaz/tr/v8/prayerui/PrayerScreen.kt`
- Test: `namaz-v8/feature/today/src/test/java/app/namaz/tr/v8/today/TodayViewModelTest.kt`

**Interfaces:**
- Consumes: `PrayerRepository.observeDay`, `PrayerSchedule.nextPrayer`.
- Produces: `TodayUiState` and Compose Today/Prayer screens.

- [ ] **Step 1: Write state-mapping test**

```kotlin
@Test fun todayStateShowsNextPrayerFiveTrackablePrayersAndSunriseInfo() = runTest {
    val state = mapper.map(sampleSchedule(), now = instantBeforeAsr())
    assertEquals("İkindi", state.nextPrayer.name)
    assertEquals(5, state.trackablePrayers.size)
    assertNotNull(state.sunrise)
    assertTrue(state.methodLabel.contains("yaklaşık"))
}
```

- [ ] **Step 2: Run and verify failure**

```bash
gradle -p namaz-v8 :feature:today:testDebugUnitTest --tests '*TodayViewModelTest*'
```

- [ ] **Step 3: Implement UI state and mapper**

```kotlin
data class TodayUiState(
    val city: String,
    val methodLabel: String,
    val nextPrayer: NextPrayerUi,
    val trackablePrayers: List<PrayerRowUi>,
    val sunrise: PrayerRowUi?,
    val hijriPlaceholder: String? = null,
)
```

Phase 1 must not fake ayah/hadith/dua content. Their cards remain absent until validated content is added in later phases.

- [ ] **Step 4: Implement Compose cards**

Today must render: large next-prayer card, remaining duration, city/method label, five prayer strip, separate sunrise info, prayer completion actions. Prayer detail screen exposes calculation method, madhab and manual adjustment controls.

- [ ] **Step 5: Run tests/build and commit**

```bash
gradle -p namaz-v8 :feature:today:testDebugUnitTest :app:assembleDebug
git add namaz-v8 && git commit -m "feat: add native today and prayer screens"
```

---

### Task 6: Prayer tracking and user-entered qaza counts

**Files:**
- Create: `namaz-v8/core/prayer/src/main/java/app/namaz/tr/v8/prayer/db/PrayerRecordEntity.kt`
- Create: `namaz-v8/core/prayer/src/main/java/app/namaz/tr/v8/prayer/db/PrayerRecordDao.kt`
- Modify: `namaz-v8/core/prayer/src/main/java/app/namaz/tr/v8/prayer/PrayerRepository.kt`
- Create: `namaz-v8/feature/prayer/src/main/java/app/namaz/tr/v8/prayerui/PrayerTrackingViewModel.kt`
- Test: `namaz-v8/core/prayer/src/test/java/app/namaz/tr/v8/prayer/PrayerTrackingRulesTest.kt`

**Interfaces:**
- Produces: `PrayerCompletion(completed, onTime, congregation, qaza)`; qaza balance is only explicit user input.

- [ ] **Step 1: Write rules tests**

```kotlin
@Test fun sunriseCannotBeTrackedAsObligatoryPrayer() {
    assertFalse(PrayerName.SUNRISE.isTrackable)
}
@Test fun appNeverDerivesHistoricQazaDebt() {
    val state = QazaState(userEntered = 120)
    assertEquals(120, state.total)
}
```

- [ ] **Step 2: Implement Room entity/DAO and repository methods**

Use compound key `(localDate, prayerName)`. Do not create records for SUNRISE. Detailed fields are nullable/optional so simple mode stores only completion.

- [ ] **Step 3: Implement UI**

Simple mode: `Kıldım` toggle. Detailed mode optional sheet: `Vaktinde`, `Cemaatle`, `Kazaya kaldı`. Qaza count screen has explicit `Başlangıç sayısını ben giriyorum` wording.

- [ ] **Step 4: Run tests and commit**

```bash
gradle -p namaz-v8 :core:prayer:testDebugUnitTest :feature:prayer:testDebugUnitTest
git add namaz-v8 && git commit -m "feat: add prayer and qaza tracking"
```

---

### Task 7: Exact-alarm scheduling, boot recovery and adhan notification/service

**Files:**
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/alarm/PrayerAlarmScheduler.kt`
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/alarm/PrayerAlarmReceiver.kt`
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/alarm/BootReceiver.kt`
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/adhan/AdhanService.kt`
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/adhan/AdhanNotificationFactory.kt`
- Modify: `namaz-v8/app/src/main/AndroidManifest.xml`
- Test: `namaz-v8/app/src/test/java/app/namaz/tr/v8/alarm/PrayerAlarmPlannerTest.kt`

**Interfaces:**
- Consumes: `PrayerSchedule` from core:prayer.
- Produces: `PrayerAlarmPlanner.plan(schedule, preferences): List<PlannedAlarm>` and Android scheduler implementation.

- [ ] **Step 1: Write pure planner tests**

```kotlin
@Test fun plannerCreatesPrayerAndEnabledPreReminderOnly() {
    val alarms = planner.plan(sampleSchedule(), prefs(preReminderMinutes = 10, ishaMode = OFF))
    assertTrue(alarms.any { it.prayer == PrayerName.MAGHRIB && it.kind == ADHAN })
    assertTrue(alarms.any { it.prayer == PrayerName.MAGHRIB && it.kind == PRE_REMINDER })
    assertFalse(alarms.any { it.prayer == PrayerName.ISHA })
}
```

- [ ] **Step 2: Implement planner and Android scheduler**

Use stable request code derived from `date + prayer + kind`. On API 31+, call exact alarm only when `alarmManager.canScheduleExactAlarms()`; otherwise schedule best-effort alarm and mark health state red/yellow rather than silently claiming exact delivery.

```kotlin
if (Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()) {
    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
} else {
    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
}
```

- [ ] **Step 3: Implement receiver/service and notification actions**

Notification actions: `Durdur`, `5 dk sonra hatırlat`, `Bildirim ayarları`. Full adhan mode starts foreground MediaPlayer/Media3 playback from bundled licensed/verified resource; notification-only mode never starts audio. Keep service isolated from Compose process state.

- [ ] **Step 4: Implement boot/timezone recovery**

Manifest receiver listens to `BOOT_COMPLETED`, `TIMEZONE_CHANGED`, `TIME_SET`. Receiver enqueues a small WorkManager job that reads repository/settings and reschedules upcoming prayers; no network is required for rescheduling cached/local-calculated times.

- [ ] **Step 5: Add required manifest permissions**

Declare `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, and audio foreground-service type where required. Do not request microphone/location in Phase 1 startup.

- [ ] **Step 6: Run tests/build and commit**

```bash
gradle -p namaz-v8 :app:testDebugUnitTest :app:assembleDebug
git add namaz-v8 && git commit -m "feat: add V8 prayer alarm and adhan engine"
```

---

### Task 8: Notification Health Center and diagnostic tests

**Files:**
- Modify: `namaz-v8/settings.gradle.kts`
- Create: `namaz-v8/feature/health/build.gradle.kts`
- Create: `namaz-v8/feature/health/src/main/java/app/namaz/tr/v8/health/HealthCheck.kt`
- Create: `namaz-v8/feature/health/src/main/java/app/namaz/tr/v8/health/NotificationHealthViewModel.kt`
- Create: `namaz-v8/feature/health/src/main/java/app/namaz/tr/v8/health/NotificationHealthScreen.kt`
- Test: `namaz-v8/feature/health/src/test/java/app/namaz/tr/v8/health/HealthStatusReducerTest.kt`

**Interfaces:**
- Produces: `HealthItem(id, title, status, explanation, action)` and `HealthStatus { GREEN, YELLOW, RED }`.

- [ ] **Step 1: Write reducer test**

```kotlin
@Test fun missingNotificationPermissionIsRedOnApi33Plus() {
    val result = reducer.reduce(api = 35, notificationGranted = false, exactAlarm = true, batteryRestricted = false)
    assertEquals(HealthStatus.RED, result.first { it.id == "notification" }.status)
}
```

- [ ] **Step 2: Implement checks**

Checks: notification permission, exact alarm capability, battery optimization status, background restriction, DND policy access. OEM auto-start cannot be reliably introspected; render it as YELLOW informational item with manufacturer-specific settings shortcut where resolvable, never as a fake green check.

- [ ] **Step 3: Implement test notification and one-minute test alarm**

```kotlin
fun scheduleDiagnosticAlarm(nowMillis: Long) = scheduler.scheduleDiagnostic(nowMillis + 60_000L)
```

Diagnostic alarms use a separate request-code namespace and never alter real prayer alarms.

- [ ] **Step 4: Wire screen under Daha Fazla**

Rows show status icon + plain explanation + action such as `İzni aç`, `Pil ayarına git`, `Test bildirimi gönder`, `1 dk sonra test alarmı`.

- [ ] **Step 5: Run tests and commit**

```bash
gradle -p namaz-v8 :feature:health:testDebugUnitTest :app:assembleDebug
git add namaz-v8 && git commit -m "feat: add notification health diagnostics"
```

---

### Task 9: CI, APK verification and Phase 1 artifact

**Files:**
- Create: `.github/workflows/namaz-v8-build.yml`
- Create: `namaz-v8/app/src/test/java/app/namaz/tr/v8/Phase1ManifestContractTest.kt`

**Interfaces:**
- Produces: GitHub Actions artifact `Namaz-V8-Phase1-APK` containing `Namaz-V8-Phase1.apk`.

- [ ] **Step 1: Add failing manifest contract test before workflow**

Test must assert expected alarm receiver/service classes and package identity constants. Then run:

```bash
gradle -p namaz-v8 testDebugUnitTest
```

- [ ] **Step 2: Create workflow**

Workflow triggers on `namaz-clean-rebuild` changes to `namaz-v8/**` and itself. Steps: checkout, Java 17, Android SDK 35/build-tools 35.0.0, Gradle 8.9, unit tests, lint, assembleDebug, signature verification, aapt badging, manifest dump, artifact upload.

Core verification shell:

```bash
APK=namaz-v8/app/build/outputs/apk/debug/app-debug.apk
cp "$APK" Namaz-V8-Phase1.apk
${ANDROID_HOME}/build-tools/35.0.0/apksigner verify --verbose Namaz-V8-Phase1.apk
unzip -t Namaz-V8-Phase1.apk >/dev/null
${ANDROID_HOME}/build-tools/35.0.0/aapt dump badging Namaz-V8-Phase1.apk | tee /tmp/badging.txt
grep -q "package: name='app.namaz.tr.v8' versionCode='8' versionName='8.0.0'" /tmp/badging.txt
grep -q "application-label:'Namaz V8'" /tmp/badging.txt
```

Manifest verification must grep `PrayerAlarmReceiver`, `BootReceiver`, and `AdhanService`.

- [ ] **Step 3: Run the full local/CI-equivalent gate**

```bash
gradle -p namaz-v8 testDebugUnitTest --stacktrace
gradle -p namaz-v8 lintDebug --stacktrace
gradle -p namaz-v8 assembleDebug --stacktrace
```

Expected: all green, APK present.

- [ ] **Step 4: Verify V7 regression suite remains green**

```bash
PYTHONPATH=namaz-native python3 -m unittest discover -s namaz-native -p 'test_*.py' -v
```

Expected: existing V7 tests remain PASS; no V7 file modifications are required by Phase 1.

- [ ] **Step 5: Commit**

```bash
git add .github/workflows/namaz-v8-build.yml namaz-v8
git commit -m "ci: build and verify Namaz V8 phase 1 APK"
```

---

## Phase 1 completion gate

Phase 1 is complete only when all of the following are evidenced by fresh test/build output:

- Native Compose app opens with no WebView dependency in V8.
- Onboarding has the four approved profiles; Basit/Tam mode persists.
- Five-tab navigation exists and back navigation does not jump between unrelated roots.
- Prayer calculation works offline with Turkey approximation and transparent method label.
- Hanefî/Şafiî Asr selection and manual per-prayer adjustment are covered by tests.
- Today shows next prayer, countdown-ready instant, five obligatory rows and separate sunrise.
- Prayer tracking never counts sunrise; qaza count comes only from user input.
- Alarm planner supports per-prayer mode and pre-reminder; boot/time/timezone recovery is registered.
- Notification Health Center diagnoses permission/exact alarm/battery/DND states and can run diagnostics.
- `testDebugUnitTest`, `lintDebug`, `assembleDebug`, APK signature/package/manifest checks pass.
- Existing V7 regression tests remain green.

## Self-review record

Spec coverage for Phase 1: sections 2.1, 2.2, 4, 5, 6, 7, 24 architecture foundation, offline prayer baseline, privacy/no-account baseline, and Phase-1-relevant acceptance criteria are mapped above. Kur'an, learning content, worship content, widgets/Wear and release backup/accessibility requirements are intentionally assigned to roadmap Phases 2–6 rather than left unowned. No TBD/TODO/“implement later” placeholders are part of this executable phase plan.
