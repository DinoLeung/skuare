package xyz.d1n0

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import xyz.d1n0.view.ScanScreen
import xyz.d1n0.view.WatchScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController, startDestination = Route.Scan.route) {
        composable(route = Route.Scan.route) {
            ScanScreen(navToWatch = {
                navController.navigate(Route.Watch.route)
//                { popUpTo(Route.Scan.route) { inclusive = true } }
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