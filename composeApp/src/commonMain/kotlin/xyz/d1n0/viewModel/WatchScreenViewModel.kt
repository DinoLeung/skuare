package xyz.d1n0.viewModel

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juul.kable.State
import kotlinx.coroutines.*
import xyz.d1n0.Repo
import xyz.d1n0.model.Watch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class WatchScreenViewModel(
    private val repo: Repo
): ViewModel() {
    val watch = repo.getWatch() ?: throw IllegalStateException("Can't find watch")

    val connectionState: State = watch.state.value

    fun connect(onConnectionLost: () -> Unit) = viewModelScope.launch {
        watch.connect().invokeOnCompletion {
            viewModelScope.launch(Dispatchers.Main) { onConnectionLost() }
        }
    }

    fun disconnect(onDisconnected: () -> Unit) = viewModelScope.launch {
        watch.disconnect()
        onDisconnected()
    }

    fun getName() = watch.scope.launch { watch.requestName() }

    fun syncTime() = watch.scope.launch {
        runCatching {
            watch.requestClocks()
            withTimeoutOrNull(10.seconds) {
                while (!watch.clocksConfig.hasInitializedClocks()) {
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
