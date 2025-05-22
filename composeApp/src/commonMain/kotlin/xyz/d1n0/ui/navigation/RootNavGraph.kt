package xyz.d1n0.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import xyz.d1n0.ui.screen.scan.ScanScreen

@Composable
fun RootNavGraph() {
	val rootNavHostController = koinInject<NavHostController>(named("rootNavHostController"))

	val onConnect = {
		rootNavHostController.navigate(RootNavRoute.Watch.route) {
			popUpTo(RootNavRoute.Scan.route) { inclusive = true }
		}
	}

	val onDisconnect = {
		rootNavHostController.navigate(RootNavRoute.Scan.route) {
			popUpTo(RootNavRoute.Watch.route) { inclusive = true }
		}
	}

	NavHost(
		navController = rootNavHostController,
		startDestination = RootNavRoute.Scan.route,
	) {
		composable(RootNavRoute.Scan.route) {
			ScanScreen(onConnect = onConnect)
		}
		composable(RootNavRoute.Watch.route) {
			NavBarScaffold(onDisconnect = onDisconnect)
		}
	}
}

