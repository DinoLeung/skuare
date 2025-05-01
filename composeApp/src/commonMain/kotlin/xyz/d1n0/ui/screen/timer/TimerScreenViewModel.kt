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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class TimerUiState(
	val isInitialized: Boolean = false,
	val waitingUpdates: Boolean = true,
	val hasUpdates: Boolean = false,
	val savedTimer: Timer = Timer(duration = 0.seconds, status = TimerStatus.NOT_STARTED),
	val pendingTimer: Timer = Timer(duration = 0.seconds, status = TimerStatus.NOT_STARTED),
	val pendingTimerError: Throwable? = null,
)

sealed interface TimerUiEvent {
	data class TimerInputChange(val duration: Duration) : TimerUiEvent
	object Submit : TimerUiEvent
	object Refresh : TimerUiEvent
}

class TimerScreenViewModel : ViewModel(), KoinComponent {
	private val watch: Watch by inject()
	private val timerSettings: StateFlow<TimerSettings> = watch.timer

	private val _uiState = MutableStateFlow(TimerUiState())
	val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

	fun onEvent(event: TimerUiEvent) = when (event) {
		TimerUiEvent.Refresh -> watch.scope.launch {
			_uiState.update { it.copy(waitingUpdates = true) }
			watch.requestTimer()
		}

		TimerUiEvent.Submit -> watch.scope.launch {
			_uiState.update { it.copy(waitingUpdates = true) }
			watch.writeTimer(timer = _uiState.value.pendingTimer)
			watch.requestTimer()
		}

		is TimerUiEvent.TimerInputChange -> runCatching {
			_uiState.value.pendingTimer.copy(
				duration = event.duration
			)
		}.onSuccess { newTimer ->
			_uiState.update {
				it.copy(
					pendingTimer = newTimer,
					pendingTimerError = null
				)
			}
		}.onFailure { e -> _uiState.update { it.copy(pendingTimerError = e) } }
	}

	init {
		viewModelScope.launch {
			timerSettings.collect { settings ->
				_uiState.update {
					it.copy(
						isInitialized = settings.isInitialized,
						savedTimer = settings.timer ?: Timer(
							duration = 0.seconds,
							status = TimerStatus.NOT_STARTED
						),
						waitingUpdates = false,
						hasUpdates = settings.timer?.duration != it.pendingTimer.duration,
					)
				}
			}
		}
	}
}