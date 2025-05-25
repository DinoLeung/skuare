package xyz.d1n0.ui.screen.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import xyz.d1n0.ui.component.ReminderCard
import xyz.d1n0.ui.component.SaveScreenScaffold

@Composable
fun RemindersScreen() {
	val viewModel = koinViewModel<RemindersScreenViewModel>()
	val state by viewModel.uiState.collectAsState()

	LaunchedEffect(Unit) {
		if (state.isTitlesInitialized == false || state.isConfigsInitialized == false) viewModel.onEvent(
			RemindersUiEvent.RequestReminders
		)
	}

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
					ReminderCard(
						modifier = Modifier.fillMaxWidth(),
						reminder = reminder,
						titleEnabled = state.isTitlesInitialized && state.isLoading == false,
						titleIsError = state.pendingTitleErrors[index] != null,
						titleSupportingText = { state.pendingTitleErrors[index]?.message?.let { Text(it) } },
						configEnabled = state.isConfigsInitialized && state.isLoading == false,
						configError = state.pendingConfigErrors[index],
						onTitleChange = {
							viewModel.onEvent(RemindersUiEvent.ReminderTitleChange(index = index, title = it))
						},
						toggleChange = {
							viewModel.onEvent(RemindersUiEvent.ReminderToggle(index = index, enable = it))
						},
						recurrenceChange = {
							viewModel.onEvent(RemindersUiEvent.ReminderRecurrenceChange(index = index, recurrence = it))
						},
						startDateChange = {
							viewModel.onEvent(RemindersUiEvent.ReminderStartDateChange(index = index, date = it))
						},
						endDateChange = {
							viewModel.onEvent(RemindersUiEvent.ReminderEndDateChange(index = index, date = it))
						},
						daysOfWeekChange = {
							viewModel.onEvent(RemindersUiEvent.ReminderDaysChange(index = index, days = it))
						},
					)
				}
			}
		}
	}
}