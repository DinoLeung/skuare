package xyz.d1n0.lib.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import xyz.d1n0.lib.constant.ConnectReason
import xyz.d1n0.lib.constant.ConnectionTimeout
import xyz.d1n0.lib.constant.OpCode

class WatchInfo {
    private val _name = MutableStateFlow<WatchName?>(null)
    val name: StateFlow<WatchName?> get() = _name.asStateFlow()

    private val _connectReason = MutableStateFlow<ConnectReason?>(null)
    val connectReason: StateFlow<ConnectReason?> get() = _connectReason.asStateFlow()

    private val _watchSettings = MutableStateFlow<WatchSettings?>(null)
    val watchSettings: StateFlow<WatchSettings?> get() = _watchSettings.asStateFlow()

    private val _connectionSettings = MutableStateFlow<ConnectionSettings?>(null)
    val connectionSettings: StateFlow<ConnectionSettings?> get() = _connectionSettings.asStateFlow()

    fun parseNamePacket(packet: ByteArray) =
        _name.update { WatchName.fromPacket(packet) }

    @OptIn(ExperimentalStdlibApi::class)
    fun parseConnectReasonPacket(packet: ByteArray) {
        // TODO: figure out meanings of rest of the bytes
        require(packet.first() == OpCode.CONNECT_REASON.byte) {
            "Settings packet must starts with command code ${OpCode.WATCH_SETTINGS.byte.toHexString(HexFormat.UpperCase)}"
        }
        require(packet.size == 19) {
            "Settings packet must be exactly 15 bytes long, e.g. 10 26 94 50 90 70 D8 7F 01 03 0F FF FF FF FF 24 00 00 00"
        }
        _connectReason.update { ConnectReason.fromByte(packet[8]) }
    }

	fun parseWatchSettingsPacket(packet: ByteArray) =
		_watchSettings.update { WatchSettings.fromPacket(packet) }

    fun parseConnectionSettingsPacket(packet: ByteArray) =
        _connectionSettings.update { ConnectionSettings.fromPacket(packet) }
}


