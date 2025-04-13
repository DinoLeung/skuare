package xyz.d1n0.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.qualifier.named
import xyz.d1n0.ui.navigation.NavGraph
import xyz.d1n0.ui.navigation.navBarItems
import com.juul.kable.State as PeripheralState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavScaffold(
    onDisconnect: () -> Unit,
) {
    val viewModel = koinViewModel<NavBarViewModel>()
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

    val watchConnectionState = viewModel.watchState.collectAsState(initial = PeripheralState.Disconnected())

    LaunchedEffect(Unit) {
        viewModel.watchState.filterIsInstance<PeripheralState.Disconnected>()
            .onEach { onDisconnect() }
            .launchIn(viewModel.viewModelScope)
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Square") }
            )
        },
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
        if (watchConnectionState.value is PeripheralState.Connected)
            NavGraph(
                navHostController = navHostController,
                innerPadding = innerPadding,
            )
    }
}