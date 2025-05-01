package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.ConnectReason
import xyz.d1n0.lib.constant.OpCode

data class WatchInfo(
	val name: WatchName? = null,
	val connectReason: ConnectReason? = null,
	val watchSettings: WatchSettings? = null,
	val connectionSettings: ConnectionSettings? = null,
) {
	fun parseNamePacket(packet: ByteArray) = this.copy(name = WatchName.fromPacket(packet))

	@OptIn(ExperimentalStdlibApi::class)
	fun parseConnectReasonPacket(packet: ByteArray): WatchInfo {
		// TODO: figure out meanings of rest of the bytes
		require(packet.first() == OpCode.CONNECT_REASON.byte) {
			"Settings packet must starts with command code ${
				OpCode.WATCH_SETTINGS.byte.toHexString(
					HexFormat.UpperCase
				)
			}"
		}
		require(packet.size == 19) {
			"Settings packet must be exactly 15 bytes long, e.g. 10 26 94 50 90 70 D8 7F 01 03 0F FF FF FF FF 24 00 00 00"
		}
		return this.copy(connectReason = ConnectReason.fromByte(packet[8]))
	}

	fun parseWatchSettingsPacket(packet: ByteArray) =
		this.copy(watchSettings = WatchSettings.fromPacket(packet))

	fun parseConnectionSettingsPacket(packet: ByteArray) =
		this.copy(connectionSettings = ConnectionSettings.fromPacket(packet))
}
