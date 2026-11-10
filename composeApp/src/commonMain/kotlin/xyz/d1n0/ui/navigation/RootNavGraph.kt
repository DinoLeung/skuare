package xyz.d1n0.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import xyz.d1n0.ui.screen.pairing.PairingScreen

@Composable
fun RootNavGraph() {
	val rootNavHostController = rememberNavController()

	// the tab to open the watch screens on, the one the user was on before a reload
	var watchStartRoute by rememberSaveable { mutableStateOf<String?>(null) }

	val onConnect = {
		watchStartRoute = null
		rootNavHostController.navigate(RootNavRoute.Watch.route) {
			popUpTo(RootNavRoute.Pairing.route) { inclusive = true }
		}
	}

	val onDisconnect = {
		rootNavHostController.navigate(RootNavRoute.Pairing.route) {
			popUpTo(RootNavRoute.Watch.route) { inclusive = true }
		}
	}

	// a new back stack entry, so every watch screen view model is created again for the
	// watch's new features, e.g. after its custom model changed. Opens on the tab the user was on.
	val onReload = { currentRoute: NavBarRoute? ->
		watchStartRoute = currentRoute?.route
		rootNavHostController.navigate(RootNavRoute.Watch.route) {
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
			NavBarScaffold(
				startRoute = watchStartRoute,
				onDisconnect = onDisconnect,
				onReload = onReload,
			)
		}
	}
}

