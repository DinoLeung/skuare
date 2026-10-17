package xyz.d1n0.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import xyz.d1n0.ui.screen.pairing.PairingScreen

@Composable
fun RootNavGraph() {
	val rootNavHostController = rememberNavController()

	val onConnect = {
		rootNavHostController.navigate(RootNavRoute.Watch.route) {
			popUpTo(RootNavRoute.Pairing.route) { inclusive = true }
		}
	}

	val onDisconnect = {
		rootNavHostController.navigate(RootNavRoute.Pairing.route) {
			popUpTo(RootNavRoute.Watch.route) { inclusive = true }
		}
	}

	NavHost(
		navController = rootNavHostController,
		startDestination = RootNavRoute.Pairing.route,
	) {
		composable(RootNavRoute.Pairing.route) {
			PairingScreen(onConnect = onConnect)
		}
		composable(RootNavRoute.Watch.route) {
			NavBarScaffold(onDisconnect = onDisconnect)
		}
	}
}

