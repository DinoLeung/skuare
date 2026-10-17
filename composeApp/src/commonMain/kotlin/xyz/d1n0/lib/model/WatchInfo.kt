package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.ConnectReason
import xyz.d1n0.lib.constant.OpCode

data class WatchInfo(
	val name: WatchName? = null,
	val address: String? = null,
	val connectReason: ConnectReason? = null,
	val appInfo: AppInfo? = null,
	val watchSettings: WatchSettings? = null,
	val connectionSettings: ConnectionSettings? = null,
	val watchStatus: WatchStatus? = null,
) {
	fun parseNamePacket(packet: ByteArray) = this.copy(name = WatchName.fromPacket(packet))

	/**
	 * Known layout, e.g. 10 40 A2 D5 AF A1 ED 7F 01 03 0F FF FF FF FF 27 00 00 00 (19 bytes):
	 * [1..6] BLE address of the watch, reversed (ED:A1:AF:D5:A2:40)
	 * [8] connect reason, see [ConnectReason]
	 * rest unknown
	 */
	@OptIn(ExperimentalStdlibApi::class)
	fun parseConnectReasonPacket(packet: ByteArray): WatchInfo {
		require(packet.first() == OpCode.CONNECT_REASON.byte) {
			"Connect reason packet must start with command code ${
				OpCode.CONNECT_REASON.byte.toHexString(
					HexFormat.UpperCase
				)
			}"
		}
		require(packet.size == 19) {
			"Connect reason packet must be exactly 19 bytes long, e.g. 10 26 94 50 90 70 D8 7F 01 03 0F FF FF FF FF 24 00 00 00"
		}
		return this.copy(
			address = packet.copyOfRange(1, 7).reversed()
				.joinToString(":") { it.toHexString(HexFormat.UpperCase) },
			connectReason = ConnectReason.fromByte(packet[8]),
		)
	}

	fun parseAppInfoPacket(packet: ByteArray) = this.copy(appInfo = AppInfo.fromPacket(packet))

	fun parseWatchSettingsPacket(packet: ByteArray) =
		this.copy(watchSettings = WatchSettings.fromPacket(packet))

	fun parseWatchStatusPacket(packet: ByteArray) = this.copy(watchStatus = WatchStatus.fromPacket(packet))

	fun parseConnectionSettingsPacket(packet: ByteArray) =
		this.copy(connectionSettings = ConnectionSettings.fromPacket(packet))
}
