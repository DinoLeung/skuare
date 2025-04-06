package xyz.d1n0.navigation

sealed class NavRoute(val route: String) {
    data object Scan : NavRoute("scan")
    data object Watch : NavRoute("watch")
}