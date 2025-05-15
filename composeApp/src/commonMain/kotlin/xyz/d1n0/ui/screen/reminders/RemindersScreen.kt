package xyz.d1n0.ui.screen.reminders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import xyz.d1n0.ui.component.SaveScreenScaffold

@Composable
fun RemindersScreen() {
	val viewModel = koinViewModel<RemindersScreenViewModel>()
	val state by viewModel.uiState.collectAsState()
	SaveScreenScaffold(
		saveFabVisible = state.hasUpdates &&
				state.isLoading == false &&
				state.hasErrors == false,
		saveFabOnClick = { viewModel.onEvent(RemindersUiEvent.SaveReminders) }
	) {

	}
}