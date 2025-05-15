package xyz.d1n0.ui.screen.alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.lib.helper.replaceAt
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
	val pendingAlarmsErrors: List<Throwable?> = List(4) { null },

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
		get() = pendingHourlySignalError != null ||
				pendingSnoozeAlarmError != null ||
				pendingAlarmsErrors.all { it != null }

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

	data class HourlySignalToggle(val enable: Boolean) : AlarmsUiEvent
	data class AlarmToggle(val index: Int, val enable: Boolean) : AlarmsUiEvent
	data class AlarmTimeChange(val index: Int, val time: LocalTime) : AlarmsUiEvent
	data class SnoozeAlarmToggle(val enable: Boolean) : AlarmsUiEvent
	data class SnoozeAlarmTimeChange(val time: LocalTime) : AlarmsUiEvent
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
							isAlarmsInitialized = settings.alarms.all { it != null },
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
		is AlarmsUiEvent.HourlySignalToggle -> onHourlySignalToggle(enable = event.enable)
		is AlarmsUiEvent.AlarmToggle -> onAlarmToggle(index = event.index, enable = event.enable)
		is AlarmsUiEvent.AlarmTimeChange -> onAlarmTimeChange(
			index = event.index,
			time = event.time
		)

		is AlarmsUiEvent.SnoozeAlarmToggle -> onSnoozeAlarmToggle(enable = event.enable)
		is AlarmsUiEvent.SnoozeAlarmTimeChange -> onSnoozeAlarmChange(time = event.time)
	}

	private fun requestAlarms() = watch.scope.launch {
		_uiState.update {
			it.copy(
				isHourlySignalLoading = true,
				isAlarmsLoading = true,
				isSnoozeAlarmLoading = true,
			)
		}
		watch.requestAlarms()
	}

	private fun writeAlarms() = watch.scope.launch {
		_uiState.update {
			it.copy(
				isHourlySignalLoading = _uiState.value.isHourlySignalUpdated,
				isAlarmsLoading = _uiState.value.isAlarmsUpdated,
				isSnoozeAlarmLoading = _uiState.value.isSnoozeAlarmUpdated
			)
		}
		watch.writeAlarms(
			alarmSettings = AlarmsSettings(
				hourlySignal = _uiState.value.pendingHourlySignal,
				alarms = _uiState.value.pendingAlarms,
				snoozeAlarm = _uiState.value.pendingSnoozeAlarm
			)
		)
	}

	private fun onHourlySignalToggle(enable: Boolean) = _uiState.updateCatching(
		transform = { it.pendingHourlySignal.copy(enable = enable) },
		onSuccess = { copy(pendingHourlySignal = it, pendingHourlySignalError = null) },
		onFailure = { copy(pendingHourlySignalError = it) },
	)

	private fun onAlarmToggle(index: Int, enable: Boolean) = _uiState.updateCatching(
		transform = { it.pendingAlarms[index].copy(enable = enable) },
		onSuccess = {
			copy(
				pendingAlarms = pendingAlarms.replaceAt(index, it),
				pendingAlarmsErrors = pendingAlarmsErrors.replaceAt(index, null),
			)
		},
		onFailure = {
			copy(pendingAlarmsErrors = pendingAlarmsErrors.replaceAt(index, it))
		}
	)

	private fun onAlarmTimeChange(index: Int, time: LocalTime) = _uiState.updateCatching(
		transform = { it.pendingAlarms[index].copy(time = time) },
		onSuccess = {
			copy(
				pendingAlarms = pendingAlarms.replaceAt(index, it),
				pendingAlarmsErrors = pendingAlarmsErrors.replaceAt(index, null),
			)
		},
		onFailure = {
			copy(pendingAlarmsErrors = pendingAlarmsErrors.replaceAt(index, it))
		}
	)

	private fun onSnoozeAlarmToggle(enable: Boolean) = _uiState.updateCatching(
		transform = { it.pendingSnoozeAlarm.copy(enable = enable) },
		onSuccess = { copy(pendingSnoozeAlarm = it, pendingSnoozeAlarmError = null) },
		onFailure = { copy(pendingSnoozeAlarmError = it) },
	)

	private fun onSnoozeAlarmChange(time: LocalTime) = _uiState.updateCatching(
		transform = { it.pendingSnoozeAlarm.copy(time = time) },
		onSuccess = { copy(pendingSnoozeAlarm = it, pendingSnoozeAlarmError = null) },
		onFailure = { copy(pendingSnoozeAlarmError = it) },
	)
}

private val defaultHourlySignal = HourlySignal(enable = false)
private val defaultAlarms = List(4) { Alarm(enable = false, time = LocalTime.fromSecondOfDay(0)) }
private val defaultSnoozeAlarm = Alarm(enable = false, time = LocalTime.fromSecondOfDay(0))