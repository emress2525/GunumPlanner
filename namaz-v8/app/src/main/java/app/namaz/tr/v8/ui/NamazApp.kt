package app.namaz.tr.v8.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewmodel.compose.viewModel
import app.namaz.tr.v8.AppGraph
import app.namaz.tr.v8.adhan.AdhanNotificationFactory
import app.namaz.tr.v8.alarm.AdhanMode
import app.namaz.tr.v8.alarm.AlarmPreferences
import app.namaz.tr.v8.alarm.PrayerAlarmCoordinator
import app.namaz.tr.v8.alarm.PrayerAlarmScheduler
import app.namaz.tr.v8.health.HealthAction
import app.namaz.tr.v8.health.NotificationHealthInspector
import app.namaz.tr.v8.health.NotificationHealthScreen
import app.namaz.tr.v8.health.NotificationHealthViewModel
import app.namaz.tr.v8.learn.LearnScreen
import app.namaz.tr.v8.model.AppDestination
import app.namaz.tr.v8.model.AppMode
import app.namaz.tr.v8.model.UserProfile
import app.namaz.tr.v8.model.UserSettings
import app.namaz.tr.v8.prayerui.PrayerScreen
import app.namaz.tr.v8.quran.QuranScreen
import app.namaz.tr.v8.settings.DataStoreUserSettingsRepository
import app.namaz.tr.v8.today.TodayScreen
import app.namaz.tr.v8.today.TodayViewModel
import app.namaz.tr.v8.ui.onboarding.OnboardingScreen
import app.namaz.tr.v8.worship.WorshipScreen
import kotlinx.coroutines.launch

internal enum class RootDetail {
    PRAYER_DETAILS,
    HEALTH,
}

internal data class RootTabState(
    val selected: AppDestination = AppDestination.TODAY,
    val detail: RootDetail? = null,
) {
    fun select(destination: AppDestination): RootTabState = copy(selected = destination, detail = null)
    fun open(next: RootDetail): RootTabState = copy(detail = next)
    fun closeDetail(): RootTabState = copy(detail = null)
}

@Composable
fun NamazApp(graph: AppGraph) {
    val context = LocalContext.current
    val userSettingsRepository = remember(context) { DataStoreUserSettingsRepository(context.applicationContext) }
    val userSettings by userSettingsRepository.settings.collectAsState(initial = UserSettings())
    val scope = rememberCoroutineScope()

    if (!userSettings.onboardingDone || userSettings.profile == null) {
        OnboardingScreen { profile: UserProfile ->
            scope.launch { userSettingsRepository.setProfile(profile) }
        }
        return
    }

    RootNavigation(
        graph = graph,
        appMode = userSettings.mode,
        onModeChange = { mode -> scope.launch { userSettingsRepository.setMode(mode) } },
    )
}

@Composable
private fun RootNavigation(
    graph: AppGraph,
    appMode: AppMode,
    onModeChange: (AppMode) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val baseDensity = LocalDensity.current
    val uiPrefs = remember(context) { context.getSharedPreferences("ui_prefs_v8", Context.MODE_PRIVATE) }
    var fontScale by rememberSaveable {
        mutableFloatStateOf(uiPrefs.getFloat("font_scale", 1f).coerceIn(1f, 1.3f))
    }
    var selectedRootName by rememberSaveable { mutableStateOf(AppDestination.TODAY.name) }
    var detailName by rememberSaveable { mutableStateOf<String?>(null) }
    val navigation = RootTabState(
        selected = runCatching { AppDestination.valueOf(selectedRootName) }.getOrDefault(AppDestination.TODAY),
        detail = detailName?.let { name -> runCatching { RootDetail.valueOf(name) }.getOrNull() },
    )
    val applyNavigation: (RootTabState) -> Unit = { next ->
        selectedRootName = next.selected.name
        detailName = next.detail?.name
    }

    val prayerSettings by graph.prayerSettings.settings.collectAsState(initial = null)
    val qazaTotal by graph.qazaStore.total.collectAsState(initial = 0)
    var alarmPreferences by remember { mutableStateOf<AlarmPreferences?>(null) }
    val alarmCoordinator = remember(context, graph) { PrayerAlarmCoordinator(context.applicationContext, graph) }
    LaunchedEffect(Unit) { alarmPreferences = graph.alarmPreferences.current() }

    val todayViewModel: TodayViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = TodayViewModel(graph.prayerRepository) as T
    })
    val todayState by todayViewModel.state.collectAsState()

    BackHandler(enabled = navigation.detail != null) {
        applyNavigation(navigation.closeDetail())
    }

    CompositionLocalProvider(
        LocalDensity provides Density(baseDensity.density, baseDensity.fontScale * fontScale),
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    AppDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = navigation.selected == destination,
                            onClick = { applyNavigation(navigation.select(destination)) },
                            icon = { Icon(iconFor(destination), contentDescription = destination.title) },
                            label = { Text(destination.title) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (navigation.detail) {
                    RootDetail.PRAYER_DETAILS -> {
                        prayerSettings?.let { runtime ->
                            val modes = alarmPreferences?.perPrayer?.mapValues { it.value.mode.name }.orEmpty()
                            val reminders = alarmPreferences?.perPrayer?.mapValues { it.value.preReminderMinutes }.orEmpty()
                            PrayerScreen(
                                settings = runtime,
                                qazaTotal = qazaTotal,
                                alarmModeByPrayer = modes,
                                preReminderByPrayer = reminders,
                                onLocationChange = { location ->
                                    scope.launch {
                                        graph.prayerSettings.setLocation(location)
                                        todayViewModel.refresh()
                                        alarmCoordinator.rescheduleUpcoming()
                                    }
                                },
                                onMadhabChange = {
                                    scope.launch {
                                        graph.prayerSettings.setMadhab(it)
                                        todayViewModel.refresh()
                                        alarmCoordinator.rescheduleUpcoming()
                                    }
                                },
                                onAdjustmentChange = { prayer, minutes ->
                                    scope.launch {
                                        graph.prayerSettings.setAdjustment(prayer, minutes)
                                        todayViewModel.refresh()
                                        alarmCoordinator.rescheduleUpcoming()
                                    }
                                },
                                onAlarmModeChange = { prayer, modeName ->
                                    scope.launch {
                                        val mode = runCatching { AdhanMode.valueOf(modeName) }.getOrDefault(AdhanMode.FULL)
                                        graph.alarmPreferences.setMode(prayer, mode)
                                        alarmPreferences = graph.alarmPreferences.current()
                                        alarmCoordinator.rescheduleUpcoming()
                                    }
                                },
                                onPreReminderChange = { prayer, minutes ->
                                    scope.launch {
                                        graph.alarmPreferences.setPreReminder(prayer, minutes)
                                        alarmPreferences = graph.alarmPreferences.current()
                                        alarmCoordinator.rescheduleUpcoming()
                                    }
                                },
                                onQazaTotalChange = { scope.launch { graph.qazaStore.setTotal(it) } },
                                onBack = { applyNavigation(navigation.closeDetail()) },
                            )
                        } ?: PlaceholderScreen("Namaz", "Ayarlar yükleniyor")
                    }

                    RootDetail.HEALTH -> {
                        val healthViewModel = remember(context) {
                            NotificationHealthViewModel(NotificationHealthInspector(context.applicationContext))
                        }
                        val healthItems by healthViewModel.items.collectAsState()
                        LaunchedEffect(Unit) { healthViewModel.refresh() }
                        NotificationHealthScreen(
                            items = healthItems,
                            onAction = { action ->
                                performHealthAction(context, action)
                                healthViewModel.refresh()
                            },
                            onBack = { applyNavigation(navigation.closeDetail()) },
                        )
                    }

                    null -> when (navigation.selected) {
                        AppDestination.TODAY -> {
                            TodayScreen(todayState, todayViewModel::setCompleted) {
                                applyNavigation(navigation.open(RootDetail.PRAYER_DETAILS))
                            }
                        }

                        AppDestination.QURAN -> QuranScreen()
                        AppDestination.LEARN -> LearnScreen()
                        AppDestination.WORSHIP -> WorshipScreen()
                        AppDestination.MORE -> {
                            MoreHubScreen(
                                appMode = appMode,
                                onModeChange = onModeChange,
                                runtime = prayerSettings,
                                fontScale = fontScale,
                                onFontScale = { requested ->
                                    val safeScale = requested.coerceIn(1f, 1.3f)
                                    fontScale = safeScale
                                    uiPrefs.edit().putFloat("font_scale", safeScale).apply()
                                },
                                onHealth = { applyNavigation(navigation.open(RootDetail.HEALTH)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun performHealthAction(context: Context, action: HealthAction) {
    val appUri = Uri.parse("package:${context.packageName}")
    val intent = when (action) {
        HealthAction.OPEN_NOTIFICATION_SETTINGS -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        HealthAction.OPEN_EXACT_ALARM_SETTINGS -> Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, appUri)
        HealthAction.OPEN_BATTERY_SETTINGS -> Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        HealthAction.OPEN_DND_SETTINGS -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        HealthAction.OPEN_APP_SETTINGS -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, appUri)
        HealthAction.SEND_TEST_NOTIFICATION -> {
            AdhanNotificationFactory(context).postDiagnostic()
            null
        }
        HealthAction.SCHEDULE_TEST_ALARM -> {
            PrayerAlarmScheduler(context.applicationContext).scheduleDiagnostic(System.currentTimeMillis() + 60_000L)
            null
        }
    }
    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let(context::startActivity)
}

@Composable
private fun PlaceholderScreen(title: String, subtitle: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("$title\n$subtitle") }
}

private fun iconFor(destination: AppDestination): ImageVector = when (destination) {
    AppDestination.TODAY -> Icons.Outlined.Home
    AppDestination.QURAN -> Icons.AutoMirrored.Outlined.MenuBook
    AppDestination.LEARN -> Icons.Outlined.AutoStories
    AppDestination.WORSHIP -> Icons.Outlined.SelfImprovement
    AppDestination.MORE -> Icons.Outlined.MoreHoriz
}
