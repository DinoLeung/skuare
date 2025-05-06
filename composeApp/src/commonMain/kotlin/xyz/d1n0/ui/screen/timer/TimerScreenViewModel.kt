package xyz.d1n0.ui.screen.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.lib.constant.TimerStatus
import xyz.d1n0.lib.model.Timer
import xyz.d1n0.lib.model.TimerSettings
import xyz.d1n0.lib.model.Watch
import xyz.d1n0.lib.model.requestTimer
import xyz.d1n0.lib.model.writeTimer
import xyz.d1n0.ui.boilerplate.updateCatching
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class TimerUiState(
	val isInitialized: Boolean = false,
	val waitingUpdates: Boolean = true,
	val savedTimer: Timer = defaultTimer,
	val pendingTimer: Timer = defaultTimer,
	val pendingTimerError: Throwable? = null,
) {
	val hasUpdates: Boolean
		get() = pendingTimer != savedTimer
}

sealed interface TimerUiEvent {
	data class TimerInputChange(val duration: Duration) : TimerUiEvent
	object SaveTimer : TimerUiEvent
	object RequestTimer : TimerUiEvent
}

class TimerScreenViewModel : ViewModel(), KoinComponent {
	private val watch: Watch by inject()
	private val timerSettings: StateFlow<TimerSettings> = watch.timer

	private val _uiState = MutableStateFlow(TimerUiState())
	val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

	init {
		viewModelScope.launch {
			timerSettings.collect { settings ->
				_uiState.update {
					it.copy(
						isInitialized = settings.timer != null,
						waitingUpdates = false,
						savedTimer = settings.timer ?: defaultTimer,
					)
				}
			}
		}
	}

	fun onEvent(event: TimerUiEvent) = when (event) {
		TimerUiEvent.RequestTimer -> requestTimer()
		TimerUiEvent.SaveTimer -> saveTimer()
		is TimerUiEvent.TimerInputChange -> onTimerInputChange(duration = event.duration)
	}

	private fun requestTimer() = watch.scope.launch {
		_uiState.update { it.copy(waitingUpdates = true) }
		watch.requestTimer()
	}

	private fun saveTimer() = watch.scope.launch {
		_uiState.update { it.copy(waitingUpdates = true) }
		watch.writeTimer(timer = _uiState.value.pendingTimer)
		watch.requestTimer()
	}

	private fun onTimerInputChange(duration: Duration) = _uiState.updateCatching(
		transform = { it.pendingTimer.copy(duration = duration) },
		onSuccess = { copy(pendingTimer = it, pendingTimerError = null) },
		onFailure = { copy(pendingTimerError = it) },
	)
}

private val defaultTimer = Timer(duration = 0.seconds, status = TimerStatus.NOT_STARTED)
