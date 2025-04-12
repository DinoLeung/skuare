package xyz.d1n0.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import xyz.d1n0.ui.screen.alarms.AlarmsScreen
import xyz.d1n0.ui.screen.clocks.ClocksScreen
import xyz.d1n0.ui.screen.reminders.RemindersScreen
import xyz.d1n0.ui.screen.settings.SettingsScreen
import xyz.d1n0.ui.screen.timer.TimerScreen

@Composable
fun NavGraph(
    navHostController: NavHostController,
    innerPadding: PaddingValues,
) {
    NavHost(
        navController = navHostController,
        startDestination = NavBarRoute.Clocks.route,
    ) {
        composable(route = NavBarRoute.Clocks.route) {
            ClocksScreen(innerPadding = innerPadding)
        }
        composable(route = NavBarRoute.Alarms.route) {
            AlarmsScreen(innerPadding = innerPadding)
        }
        composable(route = NavBarRoute.Timer.route) {
            TimerScreen(innerPadding = innerPadding)
        }
        composable(route = NavBarRoute.Reminders.route) {
            RemindersScreen(innerPadding = innerPadding)
        }
        composable(route = NavBarRoute.Settings.route) {
            SettingsScreen(innerPadding = innerPadding)
        }
    }
}