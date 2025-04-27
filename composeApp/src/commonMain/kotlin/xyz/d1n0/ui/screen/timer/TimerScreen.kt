package xyz.d1n0.ui.screen.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

	LaunchedEffect(Unit) {
		if (isInitialized.value == false)
			viewModel.requestTimer()
	}

	Column(
		modifier = Modifier.fillMaxSize().padding(innerPadding),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Top,

		) {
		timer.value?.let {
			TimerCard(
				timer = it,
				onValueChange = { viewModel.updateTimerInput(it) },
				saveButtonEnabled = hasUpdates.value,
				saveButtonOnClick = { viewModel.writeTimer() }
			)
		}
	}
}