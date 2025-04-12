package xyz.d1n0.ui.screen.clocks

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.lib.model.*


class ClocksScreenViewModel: ViewModel(), KoinComponent {
    private val watch: Watch by inject()

    val isInitialized: StateFlow<Boolean> get() = watch.clocks.isInitialized
    val clocks: ClocksSettings get() = watch.clocks

    fun requestClocks() = watch.scope.launch { watch.requestClocks() }

    fun writeClocks() = watch.scope.launch {
        watch.writeClocks()
        watch.writeTimeZoneConfigs()
        watch.writeTimeZoneCoordinatesAndRadioId()
        watch.writeTimeZoneNames()
    }
}