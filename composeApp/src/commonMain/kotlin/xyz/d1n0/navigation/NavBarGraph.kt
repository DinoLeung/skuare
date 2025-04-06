package xyz.d1n0.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import xyz.d1n0.screen.alarms.AlarmsScreen
import xyz.d1n0.screen.clocks.ClocksScreen
import xyz.d1n0.screen.reminders.RemindersScreen
import xyz.d1n0.screen.settings.SettingsScreen
import xyz.d1n0.screen.timer.TimerScreen

@Composable
fun NavBarGraph(
    navController: NavHostController,
    innerPadding: PaddingValues,
) {
    NavHost(navController, startDestination = NavBarRoute.Clocks.route) {
        composable(route = NavBarRoute.Clocks.route) {
            ClocksScreen()
        }
        composable(route = NavBarRoute.Alarms.route) {
            AlarmsScreen()
        }
        composable(route = NavBarRoute.Timer.route) {
            TimerScreen()
        }
        composable(route = NavBarRoute.Reminders.route) {
            RemindersScreen()
        }
        composable(route = NavBarRoute.Settings.route) {
            SettingsScreen()
        }
    }
}
