package xyz.d1n0.ui.screen.timer

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel
import xyz.d1n0.ui.component.CardView
import xyz.d1n0.ui.component.DurationTextInput
import xyz.d1n0.ui.component.ScreenContainer

@Composable
fun TimerScreen(
	innerPadding: PaddingValues,
) {
	val viewModel = koinViewModel<TimerScreenViewModel>()
	val state by viewModel.uiState.collectAsState()

	LaunchedEffect(Unit) {
		if (state.isInitialized == false)
			viewModel.onEvent(TimerUiEvent.RequestTimer)
	}

	ScreenContainer(
		modifier = Modifier.fillMaxSize().padding(innerPadding),
		saveVisible = state.hasUpdates && state.isLoading == false && state.hasErrors == false,
		saveOnClick = { viewModel.onEvent(TimerUiEvent.SaveTimer) },
	) {
		CardView(
			modifier = Modifier.fillMaxWidth(),
			title = { Text("Timer") },
			indicator = {
				AssistChip(
					label = { Text(state.savedTimer.status.toString()) },
					onClick = { },
				)
			},
		) {
			DurationTextInput(
				duration = state.savedTimer.duration,
				onDurationChange = { viewModel.onEvent(TimerUiEvent.TimerInputChange(it)) },
				enabled = state.isLoading == false,
				isError = state.hasErrors,
				supportingText = {
					state.pendingTimerError?.let { Text(it.message ?: "Unknown errors") }
				},
				modifier = Modifier.fillMaxWidth()
			)
		}
	}
}