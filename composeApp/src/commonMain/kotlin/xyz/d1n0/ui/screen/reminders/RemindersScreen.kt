package xyz.d1n0.ui.screen.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import xyz.d1n0.lib.model.ReminderTitle
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

	RemindersScreenContent(
		state = state,
		onEvent = { viewModel.onEvent(it) },
	)
}

@Composable
private fun RemindersScreenContent(
	state: ReminderUiState,
	onEvent: (RemindersUiEvent) -> Unit,
) {
	SaveScreenScaffold(
		saveFabVisible = state.hasUpdates &&
				state.isLoading == false &&
				state.hasErrors == false,
		saveFabOnClick = { onEvent(RemindersUiEvent.SaveReminders) }
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
							onEvent(RemindersUiEvent.ReminderTitleChange(index = index, title = it))
						},
						toggleChange = {
							onEvent(RemindersUiEvent.ReminderToggle(index = index, enable = it))
						},
						recurrenceChange = {
							onEvent(RemindersUiEvent.ReminderRecurrenceChange(index = index, recurrence = it))
						},
						startDateChange = {
							onEvent(RemindersUiEvent.ReminderStartDateChange(index = index, date = it))
						},
						endDateChange = {
							onEvent(RemindersUiEvent.ReminderEndDateChange(index = index, date = it))
						},
						daysOfWeekChange = {
							onEvent(RemindersUiEvent.ReminderDaysChange(index = index, days = it))
						},
					)
				}
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun RemindersScreenPreview() {
	MaterialTheme {
		RemindersScreenContent(
			state = ReminderUiState(
				isTitlesInitialized = true,
				isTitlesLoading = false,
				pendingTitles = listOf(
					ReminderTitle("Doctor appointment"),
					ReminderTitle("Pay rent"),
					ReminderTitle("Water plants"),
					ReminderTitle("Call family"),
					ReminderTitle("Anniversary"),
				),
				isConfigsInitialized = true,
				isConfigsLoading = false,
			),
			onEvent = {},
		)
	}
}
