package xyz.d1n0.navigation


import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import xyz.d1n0.screen.scan.ScanScreen

@Composable
fun RootNavGraph() {
    val rootNavHostController = koinInject<NavHostController>(named("rootNavHostController"))

    val onConnect = {
        rootNavHostController.navigate(RootNavRoute.Watch.route) {
            popUpTo(RootNavRoute.Scan.route) { inclusive = true }
        }
    }

    NavHost(
        navController = rootNavHostController,
        startDestination = RootNavRoute.Scan.route,
    ) {
        composable(RootNavRoute.Scan.route) {
            ScanScreen(onWatchFound = onConnect)
        }
        composable(RootNavRoute.Watch.route) {
            NavScaffold()
        }
    }
}

