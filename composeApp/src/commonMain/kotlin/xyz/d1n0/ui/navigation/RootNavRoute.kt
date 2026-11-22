package xyz.d1n0.ui.navigation

sealed class RootNavRoute(val route: String) {
	data object Pairing : RootNavRoute("pairing")
	data object Watch : RootNavRoute("watch")
	data object AppSettings : RootNavRoute("app_settings")
	data object Locations : RootNavRoute("locations")
}
