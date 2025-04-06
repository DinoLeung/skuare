package xyz.d1n0.navigation

sealed class NavBarRoute(val route: String) {
    data object Clocks : NavBarRoute("clocks")
    data object Alarms : NavBarRoute("alarms")
    data object Timer : NavBarRoute("timer")
    data object Reminders : NavBarRoute("reminders")
    data object Settings : NavBarRoute("settings")
}