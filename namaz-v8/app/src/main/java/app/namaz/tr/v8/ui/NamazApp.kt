package app.namaz.tr.v8.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.namaz.tr.v8.AppGraph
import app.namaz.tr.v8.adhan.AdhanNotificationFactory
import app.namaz.tr.v8.alarm.PrayerAlarmScheduler
import app.namaz.tr.v8.health.HealthAction
import app.namaz.tr.v8.health.NotificationHealthInspector
import app.namaz.tr.v8.health.NotificationHealthScreen
import app.namaz.tr.v8.health.NotificationHealthViewModel
import app.namaz.tr.v8.model.AppDestination
import app.namaz.tr.v8.prayerui.PrayerScreen
import app.namaz.tr.v8.today.TodayScreen
import app.namaz.tr.v8.today.TodayViewModel
import kotlinx.coroutines.launch

private const val PRAYER_DETAILS_ROUTE = "prayer-details"
private const val HEALTH_ROUTE = "notification-health"

@Composable
fun NamazApp(graph: AppGraph) {
    RootNavigation(graph)
}

@Composable
private fun RootNavigation(graph: AppGraph) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prayerSettings by graph.prayerSettings.settings.collectAsState(initial = null)
    val qazaTotal by graph.qazaStore.total.collectAsState(initial = 0)
    val todayViewModel: TodayViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = TodayViewModel(graph.prayerRepository) as T
    })
    val todayState by todayViewModel.state.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(AppDestination.TODAY.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(iconFor(destination), contentDescription = destination.title) },
                        label = { Text(destination.title) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(navController, AppDestination.TODAY.route, Modifier.padding(padding)) {
            composable(AppDestination.TODAY.route) {
                TodayScreen(todayState, todayViewModel::setCompleted) { navController.navigate(PRAYER_DETAILS_ROUTE) }
            }
            composable(PRAYER_DETAILS_ROUTE) {
                prayerSettings?.let { runtime ->
                    PrayerScreen(
                        settings = runtime,
                        qazaTotal = qazaTotal,
                        onMadhabChange = { scope.launch { graph.prayerSettings.setMadhab(it); todayViewModel.refresh() } },
                        onAdjustmentChange = { prayer, minutes -> scope.launch { graph.prayerSettings.setAdjustment(prayer, minutes); todayViewModel.refresh() } },
                        onQazaTotalChange = { scope.launch { graph.qazaStore.setTotal(it) } },
                        onBack = { navController.popBackStack() },
                    )
                } ?: PlaceholderScreen("Namaz", "Ayarlar yükleniyor")
            }
            composable(AppDestination.QURAN.route) { PlaceholderScreen("Kur’an", "Kur’an Pro sonraki fazda burada olacak") }
            composable(AppDestination.LEARN.route) { PlaceholderScreen("Öğren", "Akademi sonraki fazda native olarak taşınacak") }
            composable(AppDestination.WORSHIP.route) { PlaceholderScreen("İbadet", "Dua, zikir ve diğer araçlar burada olacak") }
            composable(AppDestination.MORE.route) {
                MoreScreen(onHealth = { navController.navigate(HEALTH_ROUTE) })
            }
            composable(HEALTH_ROUTE) {
                val healthViewModel = remember(context) { NotificationHealthViewModel(NotificationHealthInspector(context.applicationContext)) }
                val healthItems by healthViewModel.items.collectAsState()
                LaunchedEffect(Unit) { healthViewModel.refresh() }
                NotificationHealthScreen(
                    items = healthItems,
                    onAction = { action ->
                        performHealthAction(context, action)
                        healthViewModel.refresh()
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun MoreScreen(onHealth: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Daha Fazla", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Text("Kıble, widget, seyahat ve diğer araçlar sonraki fazlarda burada büyüyecek.", modifier = Modifier.padding(vertical = 14.dp))
        Button(onClick = onHealth, modifier = Modifier.fillMaxWidth()) { Text("Bildirim Sağlık Merkezi") }
    }
}

private fun performHealthAction(context: android.content.Context, action: HealthAction) {
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
    AppDestination.QURAN -> Icons.Outlined.MenuBook
    AppDestination.LEARN -> Icons.Outlined.AutoStories
    AppDestination.WORSHIP -> Icons.Outlined.SelfImprovement
    AppDestination.MORE -> Icons.Outlined.MoreHoriz
}
