package xyz.d1n0.ui.navigation

sealed class RootNavRoute(val route: String) {
	data object Scan : RootNavRoute("scan")
	data object Watch : RootNavRoute("watch")
}