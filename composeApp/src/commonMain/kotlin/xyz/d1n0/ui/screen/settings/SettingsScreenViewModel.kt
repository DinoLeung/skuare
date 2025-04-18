package xyz.d1n0.ui.screen.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.lib.model.*

class SettingsScreenViewModel: ViewModel(), KoinComponent {
    private val watch: Watch by inject()

    private val _watchInfo = MutableStateFlow<WatchInfo>(watch.info)

    val name: StateFlow<WatchName?> get() = watch.info.name
    val watchSettings: StateFlow<WatchSettings?> get() = watch.info.watchSettings
    val connectionSettings: StateFlow<ConnectionSettings?> get() = watch.info.connectionSettings

    fun requestName() = watch.scope.launch { watch.requestName() }
    fun writeName() = watch.scope.launch {
//        watch.info.name.value?.let { it.value = "SILVER" }
//        watch.write(WatchName(_value = "#SILVER!").packet)
        watch.writeName()
        watch.requestName()
    }

    fun requestWatchSettings() = watch.scope.launch { watch.requestWatchSettings() }

    fun requestConnectionSettings() = watch.scope.launch { watch.requestConnectionSettings() }
}