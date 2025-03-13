package xyz.d1n0.viewModel

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juul.kable.State
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import xyz.d1n0.model.Watch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class WatchScreenViewModel(private val watch: Watch): ViewModel() {

//    val connectionState: State by watch.state.collectAsState()
    val connectionState: State = watch.state.value

    fun disconnect() = viewModelScope.launch {
        watch.disconnect()
        // TODO: navigate back
    }

    fun getName() = watch.scope.launch { watch.requestName() }

    fun syncTime() = watch.scope.launch {
        runCatching {
            watch.requestClocks()
            withTimeoutOrNull(10.seconds) {
                while (watch.clocksConfig.hasInitializedClocks()) {
                    delay(100.milliseconds)
                }
            } ?: throw IllegalStateException("Timeout waiting for clocks initialization")
            watch.writeClocks()
            watch.writeTimeZoneConfigs()
            watch.writeTimeZoneNames()
            watch.writeTime()
        }.onSuccess {
            println("Time sync completed")
        }.onFailure { error ->
            println("Error syncing time: ${error.message}")
        }
    }
}
