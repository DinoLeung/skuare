package xyz.d1n0.ui.screen.timer

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.lib.model.TimerSettings
import xyz.d1n0.lib.model.Watch
import xyz.d1n0.lib.model.requestTimer
import xyz.d1n0.lib.model.writeTimer

class TimerScreenViewModel: ViewModel(), KoinComponent {
    private val watch: Watch by inject()

    val isInitialized: StateFlow<Boolean> get() = watch.timer.isInitialized
    val timer: TimerSettings get() = watch.timer

    fun requestTimer() = watch.scope.launch { watch.requestTimer() }

    fun writeTimer() = watch.scope.launch { watch.writeTimer() }
}