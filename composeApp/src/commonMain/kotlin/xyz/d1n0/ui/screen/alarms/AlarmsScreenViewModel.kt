package xyz.d1n0.ui.screen.alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.lib.model.*

class AlarmsScreenViewModel: ViewModel(), KoinComponent {
    private val watch: Watch by inject()

    private val alarmsSettings: StateFlow<AlarmsSettings> = watch.alarms

    val hourlySignal: StateFlow<HourlySignal?> = alarmsSettings.map { it.hourlySignal }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = null
        )

    val alarms: StateFlow<List<Alarm?>> = alarmsSettings.map { it.alarms }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = List(4) { null }
        )

    val snoozeAlarm: StateFlow<Alarm?> = alarmsSettings.map { it.snoozeAlarm }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = null
        )

    val isInitialized: StateFlow<Boolean> = alarmsSettings.map { it.isInitialized }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = false
        )

    fun requestAlarms() = watch.scope.launch { watch.requestAlarms() }

    fun writeAlarms() = watch.scope.launch { watch.writeAlarms() }
}