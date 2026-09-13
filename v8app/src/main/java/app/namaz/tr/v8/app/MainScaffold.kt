package app.namaz.tr.v8.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun MainScaffold(
    mode: AppMode,
    onModeChange: (AppMode) -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            androidx.compose.material3.Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.TODAY.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(AppDestination.TODAY.route) {
                DestinationPlaceholder(
                    title = "Bugün",
                    subtitle = "Sıradaki namaz, vakitler ve günlük ibadet akışı burada olacak."
                )
            }
            composable(AppDestination.QURAN.route) {
                DestinationPlaceholder(
                    title = "Kur’an",
                    subtitle = "Mushaf, meal, tefsir, ses, ezber ve tajvid merkezi."
                )
            }
            composable(AppDestination.LEARN.route) {
                DestinationPlaceholder(
                    title = "Öğren",
                    subtitle = "Elif-Bâ, Namaz Akademisi ve kaynaklı ilmihal eğitim yolu."
                )
            }
            composable(AppDestination.WORSHIP.route) {
                DestinationPlaceholder(
                    title = "İbadet",
                    subtitle = "Namaz takibi, dua, zikir, oruç, Ramazan ve diğer ibadet araçları."
                )
            }
            composable(AppDestination.MORE.route) {
                MoreScreen(mode = mode, onModeChange = onModeChange)
            }
        }
    }
}

@Composable
private fun DestinationPlaceholder(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun MoreScreen(mode: AppMode, onModeChange: (AppMode) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Daha Fazla", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Geçerli görünüm: ${if (mode == AppMode.SIMPLE) "Basit Mod" else "Tam Mod"}")
        Button(
            onClick = {
                onModeChange(if (mode == AppMode.SIMPLE) AppMode.FULL else AppMode.SIMPLE)
            }
        ) {
            Text(if (mode == AppMode.SIMPLE) "Tam Moda geç" else "Basit Moda geç")
        }
    }
}
