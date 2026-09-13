package app.namaz.tr.v8.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    TODAY("today", "Bugün", Icons.Outlined.Home),
    QURAN("quran", "Kur’an", Icons.Outlined.Book),
    LEARN("learn", "Öğren", Icons.Outlined.School),
    WORSHIP("worship", "İbadet", Icons.Outlined.SelfImprovement),
    MORE("more", "Daha Fazla", Icons.Outlined.Menu)
}
