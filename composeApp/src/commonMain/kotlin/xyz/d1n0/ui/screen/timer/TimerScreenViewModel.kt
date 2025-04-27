package xyz.d1n0.ui.screen.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.lib.model.Timer
import xyz.d1n0.lib.model.TimerSettings
import xyz.d1n0.lib.model.Watch
import xyz.d1n0.lib.model.requestTimer
import xyz.d1n0.lib.model.writeTimer
import kotlin.time.Duration

class TimerScreenViewModel : ViewModel(), KoinComponent {
	private val watch: Watch by inject()

	private val timerSettings: StateFlow<TimerSettings> = watch.timer

	val timer: StateFlow<Timer?> = timerSettings.map { it.timer }.stateIn(
		scope = viewModelScope, started = SharingStarted.Lazily, initialValue = Timer()
	)

	val isInitialized: StateFlow<Boolean> = timerSettings.map { it.isInitialized }.stateIn(
		scope = viewModelScope, started = SharingStarted.Lazily, initialValue = false
	)

	private val timerInput = MutableStateFlow<Timer>(Timer())

	val hasUpdates = combine(timer, timerInput) { watchTimer, inputTimer ->
		watchTimer?.duration != inputTimer.duration
	}.stateIn(scope = viewModelScope, started = SharingStarted.Lazily, initialValue = false)

	fun updateTimerInput(duration: Duration) = timerInput.update { it.copy(duration = duration) }

	fun requestTimer() = watch.scope.launch { watch.requestTimer() }

	fun writeTimer() = watch.scope.launch {
		watch.writeTimer(timer = timerInput.value)
		watch.requestTimer()
	}
}