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

	val isInitialized = viewModel.isInitialized.collectAsState()
	val timer = viewModel.timer.collectAsState()
	val hasUpdates = viewModel.hasUpdates.collectAsState()
	val waitingUpdates = viewModel.waitingUpdates.collectAsState()
	val error = viewModel.error.collectAsState()

	LaunchedEffect(Unit) {
		if (isInitialized.value == false)
			viewModel.requestTimer()
	}

	Column(
		modifier = Modifier.fillMaxSize().padding(innerPadding),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Top,

		) {
		Box(
			modifier = Modifier.fillMaxWidth()
				.height(5.dp)
				.background(if (waitingUpdates.value) Color.Red else Color.Green)
		)
		TimerCard(
			timer = timer.value,
			onValueChange = { viewModel.updatePendingTimer(it) },
			saveButtonEnabled = hasUpdates.value && error.value == null,
			saveButtonOnClick = { viewModel.writeTimer() },
			isError = error.value != null,
			supportingText = {
				error.value?.let { Text(it.message ?: "Unknown errors") }
			}
		)
	}
}