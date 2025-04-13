package xyz.d1n0.ui.screen.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.lib.model.Watch
import xyz.d1n0.lib.model.requestConnectionSettings
import xyz.d1n0.lib.model.requestName
import xyz.d1n0.lib.model.requestWatchSettings

class SettingsScreenViewModel: ViewModel(), KoinComponent {
    private val watch: Watch by inject()

    val isNameInitialized: StateFlow<Boolean> get() = watch.info.isNameInitialized
    val isWatchSettingsInitialized: StateFlow<Boolean> get() = watch.info.isWatchSettingsInitialized
    val isConnectionSettingsInitialized: StateFlow<Boolean> get() = watch.info.isConnectionSettingsInitialized

    val info = watch.info

    fun requestName() = watch.scope.launch { watch.requestName() }

    fun requestWatchSettings() = watch.scope.launch { watch.requestWatchSettings() }

    fun requestConnectionSettings() = watch.scope.launch { watch.requestConnectionSettings() }
}