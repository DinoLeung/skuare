package xyz.d1n0.ui.screen.reminders

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.lib.model.RemindersSettings
import xyz.d1n0.lib.model.Watch
import xyz.d1n0.lib.model.requestReminderConfigs
import xyz.d1n0.lib.model.requestReminderTitles

class RemindersScreenViewModel: ViewModel(), KoinComponent {
    private val watch: Watch by inject()

    val reminders: RemindersSettings get() = watch.reminders

    val isTitlesInitialized: StateFlow<Boolean> get() = watch.reminders.isTitlesInitialized
    val isConfigsInitialized: StateFlow<Boolean> get() = watch.reminders.isConfigsInitialized

    fun requestTitles() = watch.scope.launch { watch.requestReminderTitles() }
    fun requestConfigs() = watch.scope.launch { watch.requestReminderConfigs() }
}