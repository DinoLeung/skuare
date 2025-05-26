package xyz.d1n0.ui.screen.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.lib.constant.ReminderDayOfWeek
import xyz.d1n0.lib.constant.ReminderRecurrence
import xyz.d1n0.lib.helper.replaceAt
import xyz.d1n0.lib.model.*
import xyz.d1n0.ui.boilerplate.updateCatching

data class ReminderUiState(
	val isTitlesInitialized: Boolean = false,
	val isTitlesLoading: Boolean = true,
	val savedTitles: List<ReminderTitle> = defaultReminderTitles,
	val pendingTitles: List<ReminderTitle> = defaultReminderTitles,
	val pendingTitleErrors: List<Throwable?> = List(5) { null },

	val isConfigsInitialized: Boolean = false,
	val isConfigsLoading: Boolean = true,
	val savedConfigs: List<ReminderConfig> = defaultReminderConfigs,
	val pendingConfigs: List<ReminderConfig> = defaultReminderConfigs,
	val pendingConfigErrors: List<Throwable?> = List(5) { null },
) {
	val reminders: List<Reminder>
		get() = pendingTitles.zip(pendingConfigs).map { (title, config) ->
			Reminder(title = title, config = config)
		}

	val hasUpdates: Boolean
		get() = isReminderTitlesUpdated || isReminderConfigsUpdated

	val isLoading: Boolean
		get() = isTitlesLoading || isConfigsLoading

	val hasErrors: Boolean
		get() = pendingTitleErrors.all { it != null } || pendingConfigErrors.all { it != null }

	val isReminderTitlesUpdated: Boolean
		get() = savedTitles != pendingTitles

	val isReminderConfigsUpdated: Boolean
		get() = savedConfigs != pendingConfigs
}

sealed interface RemindersUiEvent {
	object RequestReminders : RemindersUiEvent
	object SaveReminders : RemindersUiEvent

	data class ReminderToggle(val index: Int, val enable: Boolean) : RemindersUiEvent
	data class ReminderTitleChange(val index: Int, val title: String) : RemindersUiEvent
	data class ReminderStartDateChange(val index: Int, val date: LocalDate) : RemindersUiEvent
	data class ReminderEndDateChange(val index: Int, val date: LocalDate) : RemindersUiEvent
	data class ReminderRecurrenceChange(val index: Int, val recurrence: ReminderRecurrence) :
		RemindersUiEvent

	data class ReminderDaysChange(val index: Int, val days: Set<ReminderDayOfWeek>) :
		RemindersUiEvent
}

class RemindersScreenViewModel : ViewModel(), KoinComponent {
	private val watch: Watch by inject()
	private val remindersSettings: StateFlow<RemindersSettings> = watch.reminders

	private val _uiState = MutableStateFlow(ReminderUiState())
	val uiState: StateFlow<ReminderUiState> = _uiState.asStateFlow()

	init {
		viewModelScope.launch {
			remindersSettings.collect { settings ->
				_uiState.updateCatching(
					transform = {
						it.copy(
							savedTitles = settings.reminderTitles.zip(
								defaultReminderTitles
							) { new, old -> new ?: old },
							savedConfigs = settings.reminderConfigs.zip(
								defaultReminderConfigs
							) { new, old -> new ?: old },
							pendingTitles = settings.reminderTitles.zip(
								it.pendingTitles
							) { new, old -> new ?: old },
							pendingConfigs = settings.reminderConfigs.zip(
								it.pendingConfigs
							) { new, old -> new ?: old },
						)
					},
					onSuccess = {
						it.copy(
							isTitlesInitialized = settings.reminderTitles.all { it != null },
							isConfigsInitialized = settings.reminderConfigs.all { it != null },
							isTitlesLoading = settings.reminderTitles != it.pendingTitles,
							isConfigsLoading = settings.reminderConfigs != it.pendingConfigs,
						)
					},
					onFailure = { copy() },
				)
			}
		}
	}

	fun onEvent(event: RemindersUiEvent) = when (event) {
		RemindersUiEvent.RequestReminders -> requestReminders()
		RemindersUiEvent.SaveReminders -> writeReminders()
		is RemindersUiEvent.ReminderToggle -> onReminderToggle(
			index = event.index,
			enable = event.enable
		)

		is RemindersUiEvent.ReminderTitleChange -> onReminderTitleChange(
			index = event.index,
			title = event.title
		)

		is RemindersUiEvent.ReminderDaysChange -> onReminderDatsChange(
			index = event.index,
			days = event.days
		)

		is RemindersUiEvent.ReminderRecurrenceChange -> onReminderRecurrenceChange(
			index = event.index,
			recurrence = event.recurrence
		)

		is RemindersUiEvent.ReminderStartDateChange -> onReminderStartDateChange(
			index = event.index,
			date = event.date
		)

		is RemindersUiEvent.ReminderEndDateChange -> onReminderEndDateChange(
			index = event.index,
			date = event.date
		)
	}

	private fun requestReminders() = watch.scope.launch {
		_uiState.update {
			it.copy(
				isTitlesLoading = true,
				isConfigsLoading = true,
			)
		}
		watch.requestReminderTitles()
		watch.requestReminderConfigs()
	}

	//	TODO: try writing only updated items
	private fun writeReminders() = watch.scope.launch {
		_uiState.update {
			it.copy(
				isTitlesLoading = _uiState.value.isReminderTitlesUpdated,
				isConfigsLoading = _uiState.value.isReminderConfigsUpdated,
			)
		}
		RemindersSettings(
			reminderTitles = _uiState.value.pendingTitles,
			reminderConfigs = _uiState.value.pendingConfigs,
		).let {
			if (_uiState.value.isReminderTitlesUpdated)
				watch.writeReminderTitles(it)
			if (_uiState.value.isReminderConfigsUpdated)
				watch.writeReminderConfigs(it)
		}
	}

	private fun onReminderToggle(index: Int, enable: Boolean) = _uiState.updateCatching(
		transform = { it.pendingConfigs[index].copy(enable = enable) },
		onSuccess = {
			copy(
				pendingConfigs = pendingConfigs.replaceAt(index, it),
				pendingConfigErrors = pendingConfigErrors.replaceAt(index, null),
			)
		},
		onFailure = {
			copy(pendingConfigErrors = pendingConfigErrors.replaceAt(index, it))
		}
	)

	private fun onReminderTitleChange(index: Int, title: String) = _uiState.updateCatching(
		transform = { it.pendingTitles[index].copy(value = title) },
		onSuccess = {
			copy(
				pendingTitles = pendingTitles.replaceAt(index, it),
				pendingTitleErrors = pendingTitleErrors.replaceAt(index, null),
			)
		},
		onFailure = {
			copy(pendingTitleErrors = pendingTitleErrors.replaceAt(index, it))
		}
	)

	private fun onReminderDatsChange(index: Int, days: Set<ReminderDayOfWeek>) =
		_uiState.updateCatching(
			transform = { it.pendingConfigs[index].copy(daysOfWeek = days) },
			onSuccess = {
				copy(
					pendingConfigs = pendingConfigs.replaceAt(index, it),
					pendingConfigErrors = pendingConfigErrors.replaceAt(index, null)
				)
			},
			onFailure = {
				copy(pendingConfigErrors = pendingConfigErrors.replaceAt(index, it))
			}
		)

	private fun onReminderRecurrenceChange(index: Int, recurrence: ReminderRecurrence) =
		_uiState.updateCatching(
			transform = { it.pendingConfigs[index].copy(recurrence = recurrence) },
			onSuccess = {
				copy(
					pendingConfigs = pendingConfigs.replaceAt(index, it),
					pendingConfigErrors = pendingConfigErrors.replaceAt(index, null)
				)
			},
			onFailure = {
				copy(pendingConfigErrors = pendingConfigErrors.replaceAt(index, it))
			}
		)

	private fun onReminderStartDateChange(index: Int, date: LocalDate) = _uiState.updateCatching(
		transform = { it.pendingConfigs[index].copy(startDate = date) },
		onSuccess = {
			copy(
				pendingConfigs = pendingConfigs.replaceAt(index, it),
				pendingConfigErrors = pendingConfigErrors.replaceAt(index, null)
			)
		},
		onFailure = {
			copy(pendingConfigErrors = pendingConfigErrors.replaceAt(index, it))
		}
	)

	private fun onReminderEndDateChange(index: Int, date: LocalDate) = _uiState.updateCatching(
		transform = { it.pendingConfigs[index].copy(endDate = date) },
		onSuccess = {
			copy(
				pendingConfigs = pendingConfigs.replaceAt(index, it),
				pendingConfigErrors = pendingConfigErrors.replaceAt(index, null)
			)
		},
		onFailure = {
			copy(pendingConfigErrors = pendingConfigErrors.replaceAt(index, it))
		}
	)
}

private val defaultReminderTitles = List(5) { ReminderTitle("") }
private val defaultReminderConfigs = List(5) {
	ReminderConfig(
		enable = false,
		recurrence = ReminderRecurrence.ONCE,
		startDate = LocalDate(year = 2000, monthNumber = 1, dayOfMonth = 1),
		endDate = LocalDate(year = 2000, monthNumber = 1, dayOfMonth = 1),
		daysOfWeek = setOf()
	)
}
