package xyz.d1n0.ui.screen.alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.lib.model.Alarm
import xyz.d1n0.lib.model.AlarmsSettings
import xyz.d1n0.lib.model.HourlySignal
import xyz.d1n0.lib.model.Watch
import xyz.d1n0.lib.model.requestAlarms
import xyz.d1n0.lib.model.writeAlarms
import xyz.d1n0.ui.boilerplate.updateCatching

data class AlarmsUiState(
	val isHourlySignalInitialized: Boolean = false,
	val isHourlySignalLoading: Boolean = true,
	val savedHourlySignal: HourlySignal = defaultHourlySignal,
	val pendingHourlySignal: HourlySignal = defaultHourlySignal,
	val pendingHourlySignalError: Throwable? = null,

	val isAlarmsInitialized: Boolean = false,
	val isAlarmsLoading: Boolean = true,
	val savedAlarms: List<Alarm> = defaultAlarms,
	val pendingAlarms: List<Alarm> = defaultAlarms,
	val pendingAlarmsError: List<Throwable?> = List(4) { null },

	val isSnoozeAlarmInitialized: Boolean = false,
	val savedSnoozeAlarm: Alarm = defaultSnoozeAlarm,
	val isSnoozeAlarmLoading: Boolean = true,
	val pendingSnoozeAlarm: Alarm = defaultSnoozeAlarm,
	val pendingSnoozeAlarmError: Throwable? = null,
) {
	val isLoading: Boolean
		get() = isHourlySignalLoading || isAlarmsLoading || isSnoozeAlarmLoading

	val hasUpdates: Boolean
		get() = isHourlySignalUpdated || isAlarmsUpdated || isSnoozeAlarmUpdated

	val hasErrors: Boolean
		get() = pendingHourlySignalError != null || pendingAlarmsError != null || pendingSnoozeAlarmError != null

	val isHourlySignalUpdated: Boolean
		get() = savedHourlySignal != pendingHourlySignal

	val isAlarmsUpdated: Boolean
		get() = savedAlarms != pendingAlarms

	val isSnoozeAlarmUpdated: Boolean
		get() = savedSnoozeAlarm != pendingSnoozeAlarm
}

sealed interface AlarmsUiEvent {
	object RequestAlarms : AlarmsUiEvent
	object SaveAlarms : AlarmsUiEvent

	data class HourlySignalChange(val enable: Boolean) : AlarmsUiEvent
	data class AlarmToggle(val index: Int, val enable: Boolean) : AlarmsUiEvent
	data class AlarmTimeChange(val index: Int, val time: LocalTime) : AlarmsUiEvent
	data class SnoozeAlarmChange(val alarm: Alarm) : AlarmsUiEvent
}

class AlarmsScreenViewModel : ViewModel(), KoinComponent {
	private val watch: Watch by inject()
	private val alarmsSettings: StateFlow<AlarmsSettings> = watch.alarms

	private val _uiState = MutableStateFlow(AlarmsUiState())
	val uiState: StateFlow<AlarmsUiState> = _uiState.asStateFlow()

	init {
		viewModelScope.launch {
			alarmsSettings.collect { settings ->
				_uiState.updateCatching(
					transform = {
						it.copy(
							savedHourlySignal = settings.hourlySignal ?: defaultHourlySignal,
							savedAlarms = settings.alarms.zip(defaultAlarms) { new, default ->
								new ?: default
							},
							savedSnoozeAlarm = settings.snoozeAlarm ?: defaultSnoozeAlarm,
							pendingHourlySignal = settings.hourlySignal ?: it.pendingHourlySignal,
							pendingAlarms = settings.alarms.zip(it.pendingAlarms) { new, old ->
								new ?: old
							},
							pendingSnoozeAlarm = settings.snoozeAlarm ?: it.pendingSnoozeAlarm,
						)
					},
					onSuccess = {
						it.copy(
							isHourlySignalInitialized = settings.hourlySignal != null,
							isAlarmsInitialized = settings.alarms.count { it == null } == 0,
							isSnoozeAlarmInitialized = settings.snoozeAlarm != null,
							isHourlySignalLoading = settings.hourlySignal != it.pendingHourlySignal,
							isAlarmsLoading = settings.alarms != it.pendingAlarms,
							isSnoozeAlarmLoading = settings.snoozeAlarm != it.pendingSnoozeAlarm,
						)
					},
					onFailure = { copy() },
				)
			}
		}
	}

	fun onEvent(event: AlarmsUiEvent) = when (event) {
		AlarmsUiEvent.RequestAlarms -> requestAlarms()
		AlarmsUiEvent.SaveAlarms -> writeAlarms()
		is AlarmsUiEvent.AlarmToggle -> onAlarmToggle(index = event.index, enable = event.enable)
		is AlarmsUiEvent.AlarmTimeChange -> onAlarmTimeChange(
			index = event.index,
			time = event.time
		)

		is AlarmsUiEvent.HourlySignalChange -> TODO()
		is AlarmsUiEvent.SnoozeAlarmChange -> TODO()
	}

	fun requestAlarms() = watch.scope.launch {
		_uiState.update {
			it.copy(
				isHourlySignalLoading = true,
				isAlarmsLoading = true,
				isSnoozeAlarmLoading = true,
			)
		}
		watch.requestAlarms()
	}

	fun writeAlarms() = watch.scope.launch {
		_uiState.update {
			it.copy(
				isHourlySignalLoading = true,
				isAlarmsLoading = true,
				isSnoozeAlarmLoading = true,
			)
		}
		watch.writeAlarms()
	}

	fun onAlarmToggle(index: Int, enable: Boolean) = _uiState.updateCatching(
		transform = { it.pendingAlarms[index].copy(enable = enable) },
		onSuccess = {
			val newAlarms = this.pendingAlarms.mapIndexed { i, alarm ->
				if (i == index) it else alarm
			}
			copy(
				pendingAlarms = this.pendingAlarms.mapIndexed { i, alarm ->
					if (i == index) it else alarm
				},
				pendingAlarmsError = this.pendingAlarmsError.mapIndexed { i, err ->
					if (i == index) null else err
				},
			)
		},
		onFailure = {
			copy(
				pendingAlarmsError = this.pendingAlarmsError.mapIndexed { i, err ->
					if (i == index) it else err
				}
			)
		}
	)

	fun onAlarmTimeChange(index: Int, time: LocalTime) = _uiState.updateCatching(
		transform = { it.pendingAlarms[index].copy(time = time) },
		onSuccess = {
			val newAlarms = this.pendingAlarms.mapIndexed { i, alarm ->
				if (i == index) it else alarm
			}
			copy(
				pendingAlarms = this.pendingAlarms.mapIndexed { i, alarm ->
					if (i == index) it else alarm
				},
				pendingAlarmsError = this.pendingAlarmsError.mapIndexed { i, err ->
					if (i == index) null else err
				},
			)
		},
		onFailure = {
			copy(
				pendingAlarmsError = this.pendingAlarmsError.mapIndexed { i, err ->
					if (i == index) it else err
				}
			)
		}
	)

	val hourlySignal: StateFlow<HourlySignal?> = alarmsSettings.map { it.hourlySignal }.stateIn(
		scope = viewModelScope, started = SharingStarted.Lazily, initialValue = null
	)

	val alarms: StateFlow<List<Alarm?>> = alarmsSettings.map { it.alarms }.stateIn(
		scope = viewModelScope,
		started = SharingStarted.Lazily,
		initialValue = List(4) { null })

	val snoozeAlarm: StateFlow<Alarm?> = alarmsSettings.map { it.snoozeAlarm }.stateIn(
		scope = viewModelScope, started = SharingStarted.Lazily, initialValue = null
	)

	val isInitialized: StateFlow<Boolean> = alarmsSettings.map { it.isInitialized }.stateIn(
		scope = viewModelScope, started = SharingStarted.Lazily, initialValue = false
	)
}

private val defaultHourlySignal = HourlySignal(enable = false)
private val defaultAlarms = List(4) { Alarm(enable = false, time = LocalTime.fromSecondOfDay(0)) }
private val defaultSnoozeAlarm = Alarm(enable = false, time = LocalTime.fromSecondOfDay(0))