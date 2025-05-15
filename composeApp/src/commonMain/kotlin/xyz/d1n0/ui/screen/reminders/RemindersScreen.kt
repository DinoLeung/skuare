package xyz.d1n0.ui.screen.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import xyz.d1n0.ui.component.CardView
import xyz.d1n0.ui.component.SaveScreenScaffold
import xyz.d1n0.ui.component.SwitchField

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
		LazyColumn(
			state = rememberLazyListState(),
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(8.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp),
		) {
			state.reminders.forEachIndexed { index, reminder ->
				item {
					CardView(modifier = Modifier.fillMaxWidth()) {
						SwitchField(
							title = {
								// TODO: title error help text
								OutlinedTextField(
									modifier = Modifier.fillMaxWidth(),
									value = reminder.title.value,
									label = { Text("Title") },
									onValueChange = {
										viewModel.onEvent(
											RemindersUiEvent.ReminderTitleChange(
												index = index,
												title = it
											)
										)
									},
									enabled = state.isTitlesInitialized && state.isLoading == false,
								)
							},
							check = reminder.config.enable,
							onCheckedChange = {
								viewModel.onEvent(
									RemindersUiEvent.ReminderToggle(
										index = index,
										enable = it
									)
								)
							},
							enabled = state.isConfigsInitialized && state.isLoading == false,
							modifier = Modifier.fillMaxWidth()
						)
						// TODO: other configs
						// TODO: config error help text
					}
				}
			}
		}
	}
}