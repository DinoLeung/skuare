package xyz.d1n0

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import xyz.d1n0.navigation.NavBarScaffold
import xyz.d1n0.screen.alarms.TempScreen

@Composable
fun NavGraph(
    rootNavController: NavHostController,
) {
    NavHost(rootNavController, startDestination = RootRoute.Scan.route) {
        composable(route = RootRoute.Scan.route) {
//            ScanScreen(navToWatch = {
//                navController.navigate(Route.Watch.route)
////                { popUpTo(Route.Scan.route) { inclusive = true } }
//            })
            TempScreen(onConnect = {
                rootNavController.navigate(RootRoute.Watch.route) {
                    popUpTo(RootRoute.Scan.route) { inclusive = true }
                }
            })
        }

        composable(route = RootRoute.Watch.route) {
//            WatchScreen(navBack = { navController.popBackStack() })
            NavBarScaffold(rootNavController = rootNavController)
        }
    }
}

