package xyz.d1n0.ui.screen.timer

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel
import xyz.d1n0.ui.component.SaveScreenScaffold
import xyz.d1n0.ui.component.TimerCard

@Composable
fun TimerScreen() {
	val viewModel = koinViewModel<TimerScreenViewModel>()
	val state by viewModel.uiState.collectAsState()

	LaunchedEffect(Unit) {
		if (state.isInitialized == false)
			viewModel.onEvent(TimerUiEvent.RequestTimer)
	}

	SaveScreenScaffold(
		saveFabVisible = state.hasUpdates && state.isLoading == false && state.hasErrors == false,
		saveFabOnClick = { viewModel.onEvent(TimerUiEvent.SaveTimer) },
	) {
		TimerCard(
			modifier = Modifier.fillMaxWidth(),
			timer = state.savedTimer,
			onDurationChange = { viewModel.onEvent(TimerUiEvent.TimerInputChange(it)) },
			enabled = state.isLoading == false,
			isError = state.hasErrors,
			supportingText = {
				state.pendingTimerError?.let { Text(it.message ?: "Unknown errors") }
			}
		)
	}
}