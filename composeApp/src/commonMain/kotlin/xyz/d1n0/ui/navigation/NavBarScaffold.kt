package xyz.d1n0.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.koin.compose.viewmodel.koinViewModel
import xyz.d1n0.ui.component.SaveFab
import xyz.d1n0.ui.screen.alarms.AlarmsScreenViewModel
import xyz.d1n0.ui.screen.clocks.ClocksScreenViewModel
import xyz.d1n0.ui.screen.reminders.RemindersScreenViewModel
import xyz.d1n0.ui.screen.settings.SettingsScreenViewModel
import xyz.d1n0.ui.screen.timer.TimerScreenViewModel
import com.juul.kable.State as PeripheralState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavBarScaffold(
	onDisconnect: () -> Unit,
) {
	val viewModel = koinViewModel<NavBarViewModel>()

	// Screen view models are scoped here rather than to each tab's back stack entry,
	// so pending changes survive switching tabs and can be saved from the shared FAB
	val clocksViewModel = koinViewModel<ClocksScreenViewModel>()
	val alarmsViewModel = koinViewModel<AlarmsScreenViewModel>()
	val timerViewModel = koinViewModel<TimerScreenViewModel>()
	val remindersViewModel = koinViewModel<RemindersScreenViewModel>()
	val settingsViewModel = koinViewModel<SettingsScreenViewModel>()

	val navBarItems = viewModel.navBarItems
	// only screens with a tab, so a hidden screen is never saved
	val saveableScreens: Map<NavBarRoute, SaveableScreen> = mapOf(
		NavBarRoute.Clocks to clocksViewModel,
		NavBarRoute.Alarms to alarmsViewModel,
		NavBarRoute.Timer to timerViewModel,
		NavBarRoute.Reminders to remindersViewModel,
		NavBarRoute.Settings to settingsViewModel,
	).filterKeys { route -> navBarItems.any { it.route == route } }
	val saveStates = saveableScreens.mapValues { (_, screen) ->
		screen.saveState.collectAsState().value
	}
	val isSaving by viewModel.isSaving.collectAsState()
	val isLoading = isSaving || saveStates.values.any { it.isLoading }

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
					unsavedRoutes = saveStates.filterValues { it.hasUpdates }.keys,
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
		floatingActionButton = {
			SaveFab(
				visible = isSaving == false &&
					watchConnectionState.value is PeripheralState.Connected &&
					saveStates.values.any { it.canSave },
				onClick = { viewModel.saveAll(saveableScreens.values) },
			)
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
				NavGraph(
					navHostController = navHostController,
					clocksViewModel = clocksViewModel,
					alarmsViewModel = alarmsViewModel,
					timerViewModel = timerViewModel,
					remindersViewModel = remindersViewModel,
					settingsViewModel = settingsViewModel,
				)

			AnimatedVisibility(
				visible = isLoading,
				enter = fadeIn(),
				exit = fadeOut(),
				modifier = Modifier
					.align(Alignment.TopCenter)
					.fillMaxWidth(),
			) {
				LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
			}
		}
	}
}
