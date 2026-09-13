package app.namaz.tr.v8.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.namaz.tr.v8.AppGraph
import app.namaz.tr.v8.model.AppDestination
import app.namaz.tr.v8.model.UserProfile
import app.namaz.tr.v8.model.UserSettings
import app.namaz.tr.v8.prayerui.PrayerScreen
import app.namaz.tr.v8.settings.DataStoreUserSettingsRepository
import app.namaz.tr.v8.today.TodayScreen
import app.namaz.tr.v8.today.TodayViewModel
import app.namaz.tr.v8.ui.onboarding.OnboardingScreen
import kotlinx.coroutines.launch

private const val PRAYER_DETAILS_ROUTE = "prayer-details"

@Composable
fun NamazApp() {
    val context = LocalContext.current
    val userSettings = remember(context) { DataStoreUserSettingsRepository(context.applicationContext) }
    val graph = remember(context) { AppGraph(context.applicationContext) }
    val settings by userSettings.settings.collectAsState(initial = UserSettings())
    val scope = rememberCoroutineScope()

    if (!settings.onboardingDone || settings.profile == null) {
        OnboardingScreen { profile: UserProfile -> scope.launch { userSettings.setProfile(profile) } }
        return
    }
    RootNavigation(graph)
}

@Composable
private fun RootNavigation(graph: AppGraph) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val scope = rememberCoroutineScope()
    val prayerSettings by graph.prayerSettings.settings.collectAsState(initial = null)
    val todayViewModel: TodayViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            TodayViewModel(graph.prayerRepository) as T
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
                TodayScreen(
                    state = todayState,
                    onPrayerCompleted = todayViewModel::setCompleted,
                    onOpenPrayerDetails = { navController.navigate(PRAYER_DETAILS_ROUTE) },
                )
            }
            composable(PRAYER_DETAILS_ROUTE) {
                prayerSettings?.let { runtime ->
                    PrayerScreen(
                        settings = runtime,
                        onMadhabChange = { scope.launch { graph.prayerSettings.setMadhab(it); todayViewModel.refresh() } },
                        onAdjustmentChange = { prayer, minutes -> scope.launch { graph.prayerSettings.setAdjustment(prayer, minutes); todayViewModel.refresh() } },
                        onBack = { navController.popBackStack() },
                    )
                } ?: PlaceholderScreen("Namaz", "Ayarlar yükleniyor")
            }
            composable(AppDestination.QURAN.route) { PlaceholderScreen("Kur’an", "Kur’an Pro sonraki fazda burada olacak") }
            composable(AppDestination.LEARN.route) { PlaceholderScreen("Öğren", "Akademi sonraki fazda native olarak taşınacak") }
            composable(AppDestination.WORSHIP.route) { PlaceholderScreen("İbadet", "Dua, zikir ve diğer araçlar burada olacak") }
            composable(AppDestination.MORE.route) { PlaceholderScreen("Daha Fazla", "Kıble, ayarlar ve sağlık kontrolleri burada") }
        }
    }
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
