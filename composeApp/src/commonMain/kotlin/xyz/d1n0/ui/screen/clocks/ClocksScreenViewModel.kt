package xyz.d1n0.ui.screen.clocks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.lib.model.*


class ClocksScreenViewModel: ViewModel(), KoinComponent {
    private val watch: Watch by inject()

    private val clocks: StateFlow<ClocksSettings> = watch.clocks

    val homeClock: StateFlow<HomeClock?> = clocks.map { it.homeClock }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = null
        )

    val worldClocks: StateFlow<List<WorldClock?>> = clocks.map { it.worldClocks }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = List(5) { null }
        )

    val isInitialized: StateFlow<Boolean> = clocks.map { it.isInitialized }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = false
        )

    fun requestClocks() = watch.scope.launch { watch.requestClocks() }

    fun writeClocks() = watch.scope.launch {
        clocks.value.let {
            watch.writeClocks(it)
            watch.writeTimeZoneConfigs(it)
            watch.writeTimeZoneCoordinatesAndRadioId(it)
            watch.writeTimeZoneNames(it)
        }
    }
}