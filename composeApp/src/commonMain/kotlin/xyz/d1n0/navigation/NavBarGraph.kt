package xyz.d1n0.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import xyz.d1n0.model.Watch
import xyz.d1n0.screen.alarms.AlarmsScreen
import xyz.d1n0.screen.clocks.ClocksScreen
import xyz.d1n0.screen.reminders.RemindersScreen
import xyz.d1n0.screen.settings.SettingsScreen
import xyz.d1n0.screen.timer.TimerScreen

@Composable
fun NavGraph(
    navHostController: NavHostController,
    innerPadding: PaddingValues,
) {
    val rootNavHostController = koinInject<NavHostController>(named("rootNavHostController"))
    val watch = koinInject<Watch>()
    val scope: CoroutineScope = rememberCoroutineScope()

    val onDisconnect = {
        rootNavHostController.navigate(RootNavRoute.Scan.route) {
            popUpTo(RootNavRoute.Watch.route) { inclusive = true }
        }
    }

    LaunchedEffect(Unit) {
        watch.connect()
            .invokeOnCompletion {
                scope.launch(Dispatchers.Main) { onDisconnect() }
            }
    }

    NavHost(
        navController = navHostController,
        startDestination = NavBarRoute.Clocks.route,
    ) {
        composable(route = NavBarRoute.Clocks.route) {
            ClocksScreen(innerPadding = innerPadding)
        }
        composable(route = NavBarRoute.Alarms.route) {
            AlarmsScreen(innerPadding = innerPadding)
        }
        composable(route = NavBarRoute.Timer.route) {
            TimerScreen(innerPadding = innerPadding)
        }
        composable(route = NavBarRoute.Reminders.route) {
            RemindersScreen(innerPadding = innerPadding)
        }
        composable(route = NavBarRoute.Settings.route) {
            SettingsScreen(innerPadding = innerPadding)
        }
    }
}