package com.danieljm.delijn.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.danieljm.delijn.ui.screens.home.HomeScreen
import com.danieljm.delijn.ui.screens.plan.PlanScreen
import com.danieljm.delijn.ui.screens.settings.SettingsScreen
import com.danieljm.delijn.ui.screens.stops.StopsScreen
import com.danieljm.delijn.ui.screens.stopdetailscreen.StopDetailScreen
import org.koin.androidx.compose.koinViewModel

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

        composable(Screen.Plan.route) {
            PlanScreen(viewModel = koinViewModel())
        }

        composable(Screen.Stops.route) {
            StopsScreen(navController = navController)
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }

        composable(
            "stopDetail/{stopId}/{stopName}",
            arguments = listOf(
                navArgument("stopId") { type = NavType.StringType },
                navArgument("stopName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val stopId = backStackEntry.arguments?.getString("stopId") ?: ""
            val stopName = backStackEntry.arguments?.getString("stopName") ?: ""
            StopDetailScreen(stopId = stopId, stopName = stopName)
        }
    }
}
