package xyz.d1n0.lib.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WatchInfo {
    lateinit var name: WatchName
    lateinit var watchSettings: WatchSettings
	lateinit var connectionSettings: ConnectionSettings

    val isNameInitialized: StateFlow<Boolean> get() = _isNameInitializedFlow
    private val _isNameInitializedFlow = MutableStateFlow(false)
    private fun updateNameInitializedState() {
        _isNameInitializedFlow.value = ::name.isInitialized
    }

    fun parseNamePacket(packet: ByteArray) {
        name = WatchName.fromPacket(packet)
    }

    val isWatchSettingsInitialized: StateFlow<Boolean> get() = _isWatchSettingsInitializedFlow
    private val _isWatchSettingsInitializedFlow = MutableStateFlow(false)
    private fun updateWatchSettingsInitializedState() {
        _isWatchSettingsInitializedFlow.value = ::watchSettings.isInitialized
    }

	fun parseWatchSettingsPacket(packet: ByteArray) {
		watchSettings = WatchSettings.fromPacket(packet)
	}

    val isConnectionSettingsInitialized: StateFlow<Boolean> get() = _isConnectionSettingsInitializedFlow
    private val _isConnectionSettingsInitializedFlow = MutableStateFlow(false)
    private fun updateConnectionSettingsInitializedState() {
        _isConnectionSettingsInitializedFlow.value = ::connectionSettings.isInitialized
    }

    fun parseConnectionSettingsPacket(packet: ByteArray) {
        connectionSettings = ConnectionSettings.fromPacket(packet)
    }

}


