package xyz.d1n0.screen.watch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juul.kable.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.model.* // TODO: import all for now :(
import xyz.d1n0.navigation.RootNavRoute

class WatchScreenViewModel: ViewModel(), KoinComponent {
    private val watch: Watch by inject()

    val connectionState: State = watch.state.value

    fun disconnect() = viewModelScope.launch(Dispatchers.Main) { watch.disconnect() }

    fun getConnectReason() = watch.scope.launch { watch.requestConnectReason() }
    fun getConnectionSettings() = watch.scope.launch { watch.requestConnectionSettings() }
    fun getWatchSettings() = watch.scope.launch { watch.requestWatchSettings() }
    fun getInfo() = watch.scope.launch { watch.requestAppInfo() }
    fun getName() = watch.scope.launch { watch.requestName() }
    fun getWatchCondition() = watch.scope.launch { watch.requestWatchCondition() }
    fun getAlarms() = watch.scope.launch { watch.requestAlarms() }
    fun getTimer() = watch.scope.launch { watch.requestTimer() }

    fun syncTime() = watch.scope.launch { watch.adjustTime() }

    fun getGPS() = watch.scope.launch {
        watch.requestTimeZoneCoordinatesAndRadioId()
    }

    fun writeAlarms() = watch.scope.launch { watch.writeAlarms() }
    fun writeTimer() = watch.scope.launch { watch.writeTimer() }
}
