package com.danieljm.delijn.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.danieljm.delijn.ui.screens.home.HomeScreen
import com.danieljm.delijn.ui.screens.settings.SettingsScreen
import com.danieljm.delijn.ui.screens.stops.StopsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen()
        }

        composable(Screen.Stops.route) {
            StopsScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
