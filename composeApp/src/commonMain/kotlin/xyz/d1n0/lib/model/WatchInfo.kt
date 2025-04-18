package xyz.d1n0.lib.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import xyz.d1n0.lib.constant.ConnectionTimeout

class WatchInfo {
    private val _name = MutableStateFlow<WatchName?>(null)
    val name: StateFlow<WatchName?> get() = _name.asStateFlow()

    private val _watchSettings = MutableStateFlow<WatchSettings?>(null)
    val watchSettings: StateFlow<WatchSettings?> get() = _watchSettings.asStateFlow()

    private val _connectionSettings = MutableStateFlow<ConnectionSettings?>(null)
    val connectionSettings: StateFlow<ConnectionSettings?> get() = _connectionSettings.asStateFlow()


    fun parseNamePacket(packet: ByteArray) =
        _name.update { WatchName.fromPacket(packet) }

	fun parseWatchSettingsPacket(packet: ByteArray) =
		_watchSettings.update { WatchSettings.fromPacket(packet) }

    fun parseConnectionSettingsPacket(packet: ByteArray) =
        _connectionSettings.update { ConnectionSettings.fromPacket(packet) }
}


