package xyz.d1n0.ui.screen.pairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import kotlin.time.Instant

@Composable
fun PairingScreen(
	onConnect: () -> Unit,
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
	)
}

@Composable
private fun PairingScreenContent(
	state: PairingUiState,
	onEvent: (PairingUiEvent) -> Unit,
	onOpenSettings: () -> Unit,
) {
	Scaffold(
		floatingActionButton = {
			when {
				!state.isPermissionGranted -> Unit
				state.isSearching -> FloatingActionButton(onClick = { onEvent(PairingUiEvent.StopSearching) }) {
					Icon(Icons.Default.Close, contentDescription = "Stop searching")
				}

				else -> FloatingActionButton(onClick = { onEvent(PairingUiEvent.Search) }) {
					Icon(Icons.Default.Add, contentDescription = "Pair a new watch")
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
					Text("Grant permissions from app settings")
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
				Text(text = "No paired watches", style = MaterialTheme.typography.titleMedium)
				Text(
					text = "Press and hold the MODE button (lower-left) on your watch, then tap + to pair it.",
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
				verticalArrangement = Arrangement.spacedBy(8.dp),
			) {
				items(state.pairedWatches, key = { it.address }) { watch ->
					PairedWatchCard(
						watch = watch,
						onForget = { onEvent(PairingUiEvent.Forget(watch.address)) },
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
	MaterialTheme {
		PairingScreenContent(
			state = PairingUiState(
				isSearching = false,
				pairedWatches = listOf(
					PairedWatch(
						address = "D8:70:90:50:94:26",
						advertisedName = "CASIO GW-B5600#",
						displayName = "CASIO GW-B5600#",
						pairedAt = Instant.fromEpochMilliseconds(1792220000000),
						lastSyncAt = Instant.fromEpochMilliseconds(1792231646000),
					),
				),
				scanPermissionState = PermissionState.Granted,
				connectPermissionState = PermissionState.Granted,
			),
			onEvent = {},
			onOpenSettings = {},
		)
	}
}
@Preview(showBackground = true)
@Composable
private fun PairingScreenEmptyPreview() {
	MaterialTheme {
		PairingScreenContent(
			state = PairingUiState(
				scanPermissionState = PermissionState.Granted,
				connectPermissionState = PermissionState.Granted,
			),
			onEvent = {},
			onOpenSettings = {},
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun PairingScreenNoPermissionPreview() {
	MaterialTheme {
		PairingScreenContent(
			state = PairingUiState(),
			onEvent = {},
			onOpenSettings = {},
		)
	}
}
