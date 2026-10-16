package xyz.d1n0.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import xyz.d1n0.ui.screen.alarms.AlarmsScreen
import xyz.d1n0.ui.screen.alarms.AlarmsScreenViewModel
import xyz.d1n0.ui.screen.clocks.ClocksScreen
import xyz.d1n0.ui.screen.clocks.ClocksScreenViewModel
import xyz.d1n0.ui.screen.reminders.RemindersScreen
import xyz.d1n0.ui.screen.reminders.RemindersScreenViewModel
import xyz.d1n0.ui.screen.settings.SettingsScreen
import xyz.d1n0.ui.screen.settings.SettingsScreenViewModel
import xyz.d1n0.ui.screen.timer.TimerScreen
import xyz.d1n0.ui.screen.timer.TimerScreenViewModel

@Composable
fun NavGraph(
	navHostController: NavHostController,
	clocksViewModel: ClocksScreenViewModel,
	alarmsViewModel: AlarmsScreenViewModel,
	timerViewModel: TimerScreenViewModel,
	remindersViewModel: RemindersScreenViewModel,
	settingsViewModel: SettingsScreenViewModel,
) {
	NavHost(
		navController = navHostController,
		startDestination = NavBarRoute.Clocks.route,
	) {
		composable(route = NavBarRoute.Clocks.route) {
			ClocksScreen(viewModel = clocksViewModel)
		}
		composable(route = NavBarRoute.Alarms.route) {
			AlarmsScreen(viewModel = alarmsViewModel)
		}
		composable(route = NavBarRoute.Timer.route) {
			TimerScreen(viewModel = timerViewModel)
		}
		composable(route = NavBarRoute.Reminders.route) {
			RemindersScreen(viewModel = remindersViewModel)
		}
		composable(route = NavBarRoute.Settings.route) {
			SettingsScreen(viewModel = settingsViewModel)
		}
	}
}
