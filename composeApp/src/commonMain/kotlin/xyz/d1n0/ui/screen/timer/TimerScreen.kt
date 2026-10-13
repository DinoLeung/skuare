package xyz.d1n0.ui.screen.timer

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import xyz.d1n0.lib.constant.TimerStatus
import xyz.d1n0.lib.model.Timer
import xyz.d1n0.ui.component.SaveScreenScaffold
import xyz.d1n0.ui.component.TimerCard
import kotlin.time.Duration.Companion.minutes

@Composable
fun TimerScreen() {
	val viewModel = koinViewModel<TimerScreenViewModel>()
	val state by viewModel.uiState.collectAsState()

	LaunchedEffect(Unit) {
		if (state.isInitialized == false)
			viewModel.onEvent(TimerUiEvent.RequestTimer)
	}

	TimerScreenContent(
		state = state,
		onEvent = { viewModel.onEvent(it) },
	)
}

@Composable
private fun TimerScreenContent(
	state: TimerUiState,
	onEvent: (TimerUiEvent) -> Unit,
) {
	SaveScreenScaffold(
		saveFabVisible = state.hasUpdates && state.isLoading == false && state.hasErrors == false,
		saveFabOnClick = { onEvent(TimerUiEvent.SaveTimer) },
	) {
		TimerCard(
			modifier = Modifier.fillMaxWidth(),
			timer = state.savedTimer,
			onDurationChange = { onEvent(TimerUiEvent.TimerInputChange(it)) },
			enabled = state.isLoading == false,
			isError = state.hasErrors,
			supportingText = {
				state.pendingTimerError?.let { Text(it.message ?: "Unknown errors") }
			}
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun TimerScreenPreview() {
	val timer = Timer(duration = 10.minutes, status = TimerStatus.NOT_STARTED)

	MaterialTheme {
		TimerScreenContent(
			state = TimerUiState(
				isInitialized = true,
				isLoading = false,
				savedTimer = timer,
				pendingTimer = timer,
			),
			onEvent = {},
		)
	}
}
