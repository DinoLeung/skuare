package xyz.d1n0.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.koin.compose.viewmodel.koinViewModel
import xyz.d1n0.ui.navigation.NavGraph
import xyz.d1n0.ui.navigation.navBarItems
import com.juul.kable.State as PeripheralState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavBarScaffold(
	onDisconnect: () -> Unit,
) {
	val viewModel = koinViewModel<NavBarViewModel>()

	val navHostController = rememberNavController()
	val navBackStackEntry by navHostController.currentBackStackEntryAsState()
	val currentBottomNavBarRoute by remember(navBackStackEntry) {
		derivedStateOf {
			navBackStackEntry?.destination?.route?.let { route ->
				navBarItems.find { it.route.route == route }?.route
			}
		}
	}

	val watchConnectionState =
		viewModel.watchState.collectAsState(initial = PeripheralState.Disconnected())

	LaunchedEffect(viewModel.disconnectEvents) {
		viewModel.disconnectEvents.collect { onDisconnect() }
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(currentBottomNavBarRoute?.route ?: "Skuare") })
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
		},
	) { scaffoldPadding ->
		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(scaffoldPadding)
				.consumeWindowInsets(scaffoldPadding)
				.imePadding()
		) {
			if (watchConnectionState.value is PeripheralState.Connected)
				NavGraph(navHostController = navHostController)
		}
	}
}