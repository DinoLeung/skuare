package xyz.d1n0.ui.screen.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.compose.BindEffect
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ScanScreen(
	onConnect: () -> Unit,
) {
	val viewModel = koinViewModel<ScanScreenViewModel>(parameters = { parametersOf(onConnect) })
	val state by viewModel.state.collectAsState()

	BindEffect(viewModel.permissionsController)
	LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
		viewModel.onEvent(ScanUiEvent.RequestPermissions)
	}

	ScanScreenContent(
		state = state,
		onEvent = { viewModel.onEvent(it) },
		onOpenSettings = viewModel.permissionsController::openAppSettings,
	)
}

@Composable
private fun ScanScreenContent(
	state: ScanUiState,
	onEvent: (ScanUiEvent) -> Unit,
	onOpenSettings: () -> Unit,
) {
	Column(
		modifier = Modifier.fillMaxSize(),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center,
	) {
		if (state.scanPermissionState == PermissionState.Granted && state.connectPermissionState == PermissionState.Granted) {
			if (state.isScanning) Button(
				onClick = { onEvent(ScanUiEvent.StopScanning) },
				enabled = state.isScanning,
			) { Text("Stop!") }
			else Button(
				onClick = { onEvent(ScanUiEvent.Scan) },
				enabled = !state.isScanning,
			) { Text("Scan!") }
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
private fun ScanScreenPreview() {
	MaterialTheme {
		ScanScreenContent(
			state = ScanUiState(
				isScanning = false,
				scanPermissionState = PermissionState.Granted,
				connectPermissionState = PermissionState.Granted,
			),
			onEvent = {},
			onOpenSettings = {},
		)
	}
}
