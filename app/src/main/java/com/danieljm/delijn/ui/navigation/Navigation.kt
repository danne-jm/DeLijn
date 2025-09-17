package com.danieljm.delijn.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.Ellipsis
import com.composables.icons.lucide.Footprints
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Lucide.House)
    object Plan : Screen("plan", "Plan", Lucide.Footprints)
    object Stops : Screen("stops", "Stops", Lucide.Search)
    object Settings : Screen("settings", "More", Lucide.Ellipsis)
    object StopDetail : Screen("stopDetail/{stopId}/{stopName}", "Stop Detail", Lucide.Search)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Plan,
    Screen.Stops,
    Screen.Settings
)
