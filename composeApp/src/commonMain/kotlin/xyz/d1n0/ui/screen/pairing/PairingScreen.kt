package xyz.d1n0.ui.screen.pairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

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
	Column(
		modifier = Modifier.fillMaxSize(),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center,
	) {
		if (state.scanPermissionState == PermissionState.Granted && state.connectPermissionState == PermissionState.Granted) {
			if (state.isSearching) Button(
				onClick = { onEvent(PairingUiEvent.StopSearching) },
				enabled = state.isSearching,
			) { Text("Stop!") }
			else Button(
				onClick = { onEvent(PairingUiEvent.Search) },
				enabled = !state.isSearching,
			) { Text("Pair!") }
		} else {
			Button(
				onClick = onOpenSettings,
			) {
				Text("Grant permissions from app settings")
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
				scanPermissionState = PermissionState.Granted,
				connectPermissionState = PermissionState.Granted,
			),
			onEvent = {},
			onOpenSettings = {},
		)
	}
}
