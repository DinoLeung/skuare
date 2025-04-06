package xyz.d1n0

sealed class RootRoute(val route: String) {
    data object Scan : RootRoute("scan")
    data object Watch : RootRoute("watch")
}