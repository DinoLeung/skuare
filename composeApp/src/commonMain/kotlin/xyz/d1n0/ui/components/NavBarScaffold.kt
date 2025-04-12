package xyz.d1n0.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import xyz.d1n0.ui.navigation.NavGraph
import xyz.d1n0.ui.navigation.RootNavRoute
import xyz.d1n0.ui.navigation.navBarItems
import com.juul.kable.State as PeripheralState

@Composable
fun NavScaffold() {
    val viewModel = viewModel<NavBarViewModel>()
    val rootNavHostController = koinInject<NavHostController>(named("rootNavHostController"))
    val navHostController = rememberNavController()
    val navBackStackEntry by navHostController.currentBackStackEntryAsState()
    val currentBottomNavBarRoute by remember(navBackStackEntry) {
        derivedStateOf {
            navBackStackEntry?.destination?.route?.let { route ->
                navBarItems
                    .find { it.route.route == route }
                    ?.route
            }
        }
    }

    val watchState = viewModel.watch.state.collectAsState(initial = PeripheralState.Disconnected())
    val onDisconnect = {
        rootNavHostController.navigate(RootNavRoute.Scan.route) {
            popUpTo(RootNavRoute.Watch.route) { inclusive = true }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.connect(onConnectionLost = onDisconnect)
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = currentBottomNavBarRoute != null,
                enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight }),
                exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight })
            ) {
                NavBar(
                    items = navBarItems,
                    currentRoute = currentBottomNavBarRoute,
                    onItemClick = { item ->
                        navHostController.navigate(item.route.route) {
                            navHostController.graph.startDestinationRoute?.let {
                                popUpTo(it) { saveState = false }
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }
    ) { innerPadding ->
        if (watchState.value is PeripheralState.Connected)
            NavGraph(
                navHostController = navHostController,
                innerPadding = innerPadding,
            )
    }
}