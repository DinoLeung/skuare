package xyz.d1n0.ui.screen.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.lib.model.Timer
import xyz.d1n0.lib.model.TimerSettings
import xyz.d1n0.lib.model.Watch
import xyz.d1n0.lib.model.requestTimer

class TimerScreenViewModel: ViewModel(), KoinComponent {
    private val watch: Watch by inject()

    private val timerSettings: StateFlow<TimerSettings> = watch.timer

    val timer: StateFlow<Timer?> = timerSettings.map { it.timer }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = null
        )

    val isInitialized: StateFlow<Boolean> = timerSettings.map { it.isInitialized }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = false
        )

    fun requestTimer() = watch.scope.launch { watch.requestTimer() }

    fun writeTimer() = watch.scope.launch {
//        watch.writeTimer()
    }
}