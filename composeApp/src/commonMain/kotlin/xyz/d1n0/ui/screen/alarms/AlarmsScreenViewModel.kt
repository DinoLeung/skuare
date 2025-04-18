package xyz.d1n0.ui.screen.alarms

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.lib.model.Alarm
import xyz.d1n0.lib.model.HourlySignal
import xyz.d1n0.lib.model.Watch
import xyz.d1n0.lib.model.requestAlarms
import xyz.d1n0.lib.model.writeAlarms

class AlarmsScreenViewModel: ViewModel(), KoinComponent {
    private val watch: Watch by inject()

    val isInitialized: StateFlow<Boolean> get() = watch.alarms.isInitialized
    val hourlySignal: StateFlow<HourlySignal?> get() = watch.alarms.hourlySignal
    val alarms: StateFlow<List<Alarm?>> get() = watch.alarms.alarms
    val snoozeAlarm: StateFlow<Alarm?> get() = watch.alarms.snoozeAlarm

    fun requestAlarms() = watch.scope.launch { watch.requestAlarms() }

    fun writeAlarms() = watch.scope.launch { watch.writeAlarms() }
}