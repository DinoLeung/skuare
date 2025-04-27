package xyz.d1n0.ui.screen.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class TimerScreenViewModel : ViewModel(), KoinComponent {
	private val watch: Watch by inject()
	private val timerSettings: StateFlow<TimerSettings> = watch.timer

	private val defaultTimer = Timer(duration = 0.seconds, status = TimerStatus.NOT_STARTED)

	val timer: StateFlow<Timer> = timerSettings.map { it.timer ?: defaultTimer }.stateIn(
		scope = viewModelScope, started = SharingStarted.Lazily, initialValue = defaultTimer
	)
	val isInitialized: StateFlow<Boolean> = timerSettings.map { it.isInitialized }.stateIn(
		scope = viewModelScope, started = SharingStarted.Lazily, initialValue = false
	)

	private val timerLocal = MutableStateFlow<Timer>(timer.value)

	fun updateTimerInput(duration: Duration) = timerLocal.update { it.copy(duration = duration) }

	fun requestTimer() = watch.scope.launch { watch.requestTimer() }

	fun writeTimer() = watch.scope.launch {
		_waitingUpdates.update { true }
		watch.writeTimer(timer = timerLocal.value)
		watch.requestTimer()
	}

	private val _waitingUpdates = MutableStateFlow(true)
	val waitingUpdates: StateFlow<Boolean> = _waitingUpdates.asStateFlow()

	init {
		viewModelScope.launch {
			timerSettings.collect {
				_waitingUpdates.update { false }
			}
		}
	}

	val hasUpdates = combine(timerSettings, timerLocal) { timerSettings, inputTimer ->
		timerSettings.timer?.duration != inputTimer.duration
	}.stateIn(scope = viewModelScope, started = SharingStarted.Lazily, initialValue = false)
}