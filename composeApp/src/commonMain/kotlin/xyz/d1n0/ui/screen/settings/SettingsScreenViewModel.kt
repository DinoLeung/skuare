package xyz.d1n0.ui.screen.settings

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

class SettingsScreenViewModel: ViewModel(), KoinComponent {
    private val watch: Watch by inject()

    private val watchInfo: StateFlow<WatchInfo> = watch.info

    val name: StateFlow<WatchName?> = watchInfo.map { it.name }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )
    val watchSettings: StateFlow<WatchSettings?> = watchInfo.map { it.watchSettings }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )
    val connectionSettings: StateFlow<ConnectionSettings?> = watchInfo.map { it.connectionSettings }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    fun requestName() = watch.scope.launch { watch.requestName() }
    fun writeName() = watch.scope.launch {
//        watch.writeName("FINCH!")
    }

    fun requestWatchSettings() = watch.scope.launch { watch.requestWatchSettings() }

    fun requestConnectionSettings() = watch.scope.launch { watch.requestConnectionSettings() }
}