package xyz.d1n0.ui.screen.alarms

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.lib.model.AlarmsSettings
import xyz.d1n0.lib.model.Watch
import xyz.d1n0.lib.model.requestAlarms
import xyz.d1n0.lib.model.writeAlarms

class AlarmsScreenViewModel: ViewModel(), KoinComponent {
    private val watch: Watch by inject()

    val isInitialized: StateFlow<Boolean> get() = watch.alarms.isInitialized
    val alarms: AlarmsSettings = watch.alarms

    fun requestAlarms() = watch.scope.launch { watch.requestAlarms() }

    fun writeAlarms() = watch.scope.launch { watch.writeAlarms() }
}