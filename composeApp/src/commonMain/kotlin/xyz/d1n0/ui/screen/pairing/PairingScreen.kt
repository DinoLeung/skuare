package xyz.d1n0.ui.screen.pairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import xyz.d1n0.data.database.PairedWatch
import xyz.d1n0.ui.component.PairedWatchCard
import xyz.d1n0.ui.component.appVersion
import kotlin.time.Instant
import xyz.d1n0.ui.theme.SkuareTheme
import org.jetbrains.compose.resources.stringResource
import xyz.d1n0.resources.*

@Composable
fun PairingScreen(
	onConnect: () -> Unit,
	onOpenAppSettings: () -> Unit,
	onOpenLocations: () -> Unit,
) {
	val permissionsControllerFactory = rememberPermissionsControllerFactory()
	val permissionsController =
		remember(permissionsControllerFactory) { permissionsControllerFactory.createPermissionsController() }
	val viewModel = koinViewModel<PairingScreenViewModel>(
		parameters = { parametersOf(onConnect, permissionsController) }
	)
	val state by viewModel.state.collectAsState()
	val watchPicker = rememberWatchPicker { viewModel.onEvent(PairingUiEvent.Connect(it)) }

	BindEffect(viewModel.permissionsController)
	LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
		viewModel.onEvent(PairingUiEvent.RequestPermissions)
	}

	PairingScreenContent(
		state = state,
		onEvent = { event ->
			// platforms with a watch picker show it instead of searching
			if (event == PairingUiEvent.Search && watchPicker != null) watchPicker()
			else viewModel.onEvent(event)
		},
		onOpenSettings = viewModel.permissionsController::openAppSettings,
		onOpenAppSettings = onOpenAppSettings,
		onOpenLocations = onOpenLocations,
	)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PairingScreenContent(
	state: PairingUiState,
	onEvent: (PairingUiEvent) -> Unit,
	onOpenSettings: () -> Unit,
	onOpenAppSettings: () -> Unit,
	onOpenLocations: () -> Unit,
) {
	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(Res.string.app_name)) },
				actions = {
					IconButton(onClick = onOpenLocations) {
						Icon(Icons.Default.Place, contentDescription = stringResource(Res.string.locations_title))
					}
					IconButton(onClick = onOpenAppSettings) {
						Icon(Icons.Default.Settings, contentDescription = stringResource(Res.string.app_settings_title))
					}
					var menuExpanded by remember { mutableStateOf(false) }
					// the box anchors the menu to the button
					Box {
						IconButton(onClick = { menuExpanded = true }) {
							Icon(Icons.Default.MoreVert, contentDescription = stringResource(Res.string.pairing_more_options))
						}
						DropdownMenu(
							expanded = menuExpanded,
							onDismissRequest = { menuExpanded = false },
						) {
							// needs no permissions, the demo watch uses no Bluetooth
							DropdownMenuItem(
								text = { Text(stringResource(Res.string.pairing_connect_demo_watch)) },
								onClick = {
									menuExpanded = false
									onEvent(PairingUiEvent.StartDemo)
								},
							)
						}
					}
				},
			)
		},
		// the FAB sits above the bottom bar, so the version stays visible in the corner
		bottomBar = {
			Text(
				text = appVersion(),
				style = MaterialTheme.typography.labelSmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				textAlign = TextAlign.End,
				modifier = Modifier
					.fillMaxWidth()
					.navigationBarsPadding()
					.padding(horizontal = 16.dp, vertical = 4.dp),
			)
		},
		floatingActionButton = {
			when {
				!state.isPermissionGranted -> Unit
				state.isSearching -> FloatingActionButton(onClick = { onEvent(PairingUiEvent.StopSearching) }) {
					Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.pairing_stop_searching))
				}

				else -> FloatingActionButton(onClick = { onEvent(PairingUiEvent.Search) }) {
					Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.pairing_pair_new_watch))
				}
			}
		},
	) { scaffoldPadding ->
		if (!state.isPermissionGranted) {
			Column(
				modifier = Modifier
					.fillMaxSize()
					.padding(scaffoldPadding),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.Center,
			) {
				Button(onClick = onOpenSettings) {
					Text(stringResource(Res.string.pairing_grant_permissions))
				}
			}
		} else if (state.pairedWatches.isEmpty()) {
			Column(
				modifier = Modifier
					.fillMaxSize()
					.padding(scaffoldPadding)
					.padding(32.dp),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
			) {
				Text(text = stringResource(Res.string.pairing_no_watches), style = MaterialTheme.typography.titleMedium)
				Text(
					text = stringResource(Res.string.pairing_no_watches_hint),
					style = MaterialTheme.typography.bodyMedium,
					textAlign = TextAlign.Center,
				)
			}
		} else {
			LazyColumn(
				modifier = Modifier
					.fillMaxSize()
					.padding(scaffoldPadding),
				// extra bottom padding, so the FAB doesn't cover the last card's forget button
				contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 88.dp),
				// a single card sits in the middle of the screen; longer lists start at the top and scroll
				verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
			) {
				items(state.pairedWatches, key = { it.address }) { watch ->
					PairedWatchCard(
						watch = watch,
						locationCount = state.locationCounts[watch.address] ?: 0,
						onForget = { deleteLocations -> onEvent(PairingUiEvent.Forget(watch.address, deleteLocations)) },
						modifier = Modifier.fillMaxWidth(),
					)
				}
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun PairingScreenPreview() {
	SkuareTheme {
		PairingScreenContent(
			state = PairingUiState(
				isSearching = false,
				pairedWatches = listOf(
					PairedWatch(
						address = "AA:BB:CC:DD:EE:FF",
						advertisedName = "CASIO GW-B5600#",
						displayName = "CASIO GW-B5600#",
						pairedAt = Instant.fromEpochMilliseconds(1792220000000),
						lastSyncAt = Instant.fromEpochMilliseconds(1792231646000),
					),
					PairedWatch(
						address = "A1:B2:C3:D4:E5:F6",
						advertisedName = "CASIO GW-B5000#",
						displayName = "CASIO GW-B5000#",
						pairedAt = Instant.fromEpochMilliseconds(1792220000000),
						lastSyncAt = Instant.fromEpochMilliseconds(1792231646000),
					),
				),
				scanPermissionState = PermissionState.Granted,
				connectPermissionState = PermissionState.Granted,
			),
			onEvent = {},
			onOpenSettings = {},
			onOpenAppSettings = {},
			onOpenLocations = {},
		)
	}
}
@Preview(showBackground = true)
@Composable
private fun PairingScreenEmptyPreview() {
	SkuareTheme {
		PairingScreenContent(
			state = PairingUiState(
				scanPermissionState = PermissionState.Granted,
				connectPermissionState = PermissionState.Granted,
			),
			onEvent = {},
			onOpenSettings = {},
			onOpenAppSettings = {},
			onOpenLocations = {},
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun PairingScreenNoPermissionPreview() {
	SkuareTheme {
		PairingScreenContent(
			state = PairingUiState(),
			onEvent = {},
			onOpenSettings = {},
			onOpenAppSettings = {},
			onOpenLocations = {},
		)
	}
}
