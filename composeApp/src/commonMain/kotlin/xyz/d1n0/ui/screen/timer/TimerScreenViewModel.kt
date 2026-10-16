package xyz.d1n0.ui.screen.timer

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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.lib.constant.TimerStatus
import xyz.d1n0.lib.model.Timer
import xyz.d1n0.lib.model.TimerSettings
import xyz.d1n0.lib.model.Watch
import xyz.d1n0.lib.model.requestTimer
import xyz.d1n0.lib.model.writeTimer
import xyz.d1n0.ui.boilerplate.updateCatching
import xyz.d1n0.ui.navigation.SaveState
import xyz.d1n0.ui.navigation.SaveableScreen
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class TimerUiState(
	val isInitialized: Boolean = false,
	val isLoading: Boolean = false,
	val savedTimer: Timer = defaultTimer,
	val pendingTimer: Timer = defaultTimer,
	val pendingTimerError: Throwable? = null,
) {
	val hasUpdates: Boolean
		get() = pendingTimer != savedTimer

	val hasErrors: Boolean
		get() = pendingTimerError != null
}

sealed interface TimerUiEvent {
	data class TimerInputChange(val duration: Duration) : TimerUiEvent
	object RequestTimer : TimerUiEvent
}

class TimerScreenViewModel : ViewModel(), KoinComponent, SaveableScreen {
	private val watch: Watch by inject()
	private val timerSettings: StateFlow<TimerSettings> = watch.timer

	private val _uiState = MutableStateFlow(TimerUiState())
	val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

	override val saveState: StateFlow<SaveState> = _uiState
		.map { SaveState(hasUpdates = it.hasUpdates, isLoading = it.isLoading, hasErrors = it.hasErrors) }
		.stateIn(viewModelScope, SharingStarted.Eagerly, SaveState())

	init {
		viewModelScope.launch {
			timerSettings.collect { settings ->
				_uiState.updateCatching(
					transform = {
						it.copy(
							savedTimer = settings.timer ?: defaultTimer,
							pendingTimer = if (it.hasUpdates) it.pendingTimer
								else settings.timer ?: defaultTimer,
						)
					},
					onSuccess = {
						it.copy(
							isInitialized = settings.timer != null,
							isLoading = it.isLoading && settings.timer != it.pendingTimer,
						)
					},
					onFailure = { copy() }
				)
			}
		}
	}

	fun onEvent(event: TimerUiEvent) = when (event) {
		TimerUiEvent.RequestTimer -> requestTimer()
		is TimerUiEvent.TimerInputChange -> onTimerInputChange(duration = event.duration)
	}

	private fun requestTimer() = watch.scope.launch {
		_uiState.update { it.copy(isLoading = true) }
		watch.requestTimer()
	}

	override suspend fun save() {
		_uiState.update { it.copy(isLoading = true) }
		watch.writeTimer(timer = _uiState.value.pendingTimer)
	}

	private fun onTimerInputChange(duration: Duration) = _uiState.updateCatching(
		transform = { it.pendingTimer.copy(duration = duration) },
		onSuccess = { copy(pendingTimer = it, pendingTimerError = null) },
		onFailure = { copy(pendingTimerError = it) },
	)
}

private val defaultTimer = Timer(duration = 0.seconds, status = TimerStatus.NOT_STARTED)
