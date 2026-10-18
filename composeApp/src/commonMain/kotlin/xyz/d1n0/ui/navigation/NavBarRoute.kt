package xyz.d1n0.ui.navigation

sealed class NavBarRoute(val route: String) {
	data object Timezones : NavBarRoute("Timezones")
	data object Alarms : NavBarRoute("Alarms")
	data object Timer : NavBarRoute("Timer")
	data object Reminders : NavBarRoute("Reminders")
	data object Settings : NavBarRoute("Settings")
}