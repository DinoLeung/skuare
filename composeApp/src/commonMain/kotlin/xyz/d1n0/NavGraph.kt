package xyz.d1n0

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform.getKoin
import xyz.d1n0.model.Watch
import xyz.d1n0.view.ScanScreen
import xyz.d1n0.view.WatchScreen
import xyz.d1n0.viewModel.ScanScreenViewModel
import xyz.d1n0.viewModel.WatchScreenViewModel

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController, startDestination = Route.Scan.route) {
        composable(route = Route.Scan.route) {
            ScanScreen(navToWatch = {
                navController.navigate(Route.Watch.route) {
                    popUpTo(Route.Scan.route) { inclusive = true }
                }
            })
        }
        composable(route = Route.Watch.route) {
            WatchScreen(navBack = { navController.popBackStack() })
        }
    }
}

sealed class Route(val route: String) {
    data object Scan : Route("scan")
    data object Watch : Route("watch")
}