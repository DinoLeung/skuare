package xyz.d1n0.ui.screen.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.lib.model.Reminder
import xyz.d1n0.lib.model.RemindersSettings
import xyz.d1n0.lib.model.Watch
import xyz.d1n0.lib.model.requestReminderConfigs
import xyz.d1n0.lib.model.requestReminderTitles

class RemindersScreenViewModel : ViewModel(), KoinComponent {
	private val watch: Watch by inject()

	private val remindersSettings: StateFlow<RemindersSettings> = watch.reminders

	val isTitlesInitialized: StateFlow<Boolean>
		get() = remindersSettings.map { it.isTitlesInitialized }.stateIn(
				scope = viewModelScope, started = SharingStarted.Lazily, initialValue = false
			)
	val isConfigsInitialized: StateFlow<Boolean>
		get() = remindersSettings.map { it.isConfigsInitialized }.stateIn(
				scope = viewModelScope, started = SharingStarted.Lazily, initialValue = false
			)

	val reminders: StateFlow<List<Reminder>>
		get() = remindersSettings.map { it.reminders }.stateIn(
				scope = viewModelScope,
				started = SharingStarted.Lazily,
				initialValue = List(5) { Reminder() })

	fun requestTitles() = watch.scope.launch { watch.requestReminderTitles() }
	fun requestConfigs() = watch.scope.launch { watch.requestReminderConfigs() }
}