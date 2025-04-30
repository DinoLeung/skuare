package xyz.d1n0.ui.screen.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import xyz.d1n0.ui.component.TimerCard

@Composable
fun TimerScreen(
	innerPadding: PaddingValues,
) {
	val viewModel = koinViewModel<TimerScreenViewModel>()
	val state by viewModel.uiState.collectAsState()

	LaunchedEffect(Unit) {
		if (state.isInitialized == false)
			viewModel.onEvent(TimerUiEvent.Refresh)
	}

	Column(
		modifier = Modifier.fillMaxSize().padding(innerPadding),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Top,

		) {
		Box(
			modifier = Modifier.fillMaxWidth()
				.height(5.dp)
				.background(if (state.waitingUpdates) Color.Red else Color.Green)
		)
		TimerCard(
			timer = state.savedTimer,
			onValueChange = { viewModel.onEvent(TimerUiEvent.TimerInputChange(it)) },
			saveButtonEnabled = state.hasUpdates && state.pendingTimerError == null,
			saveButtonOnClick = { viewModel.onEvent(TimerUiEvent.Submit) },
			isError = state.pendingTimerError != null,
			supportingText = {
				state.pendingTimerError?.let { Text(it.message ?: "Unknown errors") }
			}
		)
	}
}