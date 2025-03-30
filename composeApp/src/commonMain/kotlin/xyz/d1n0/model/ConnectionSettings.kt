package xyz.d1n0.model

import xyz.d1n0.constant.AutoSyncBitmask
import xyz.d1n0.constant.Command

data class ConnectionSettings(
	var autoSyncEnable: Boolean,
	var autoSyncOffsetMinute: Byte,
	var connectionTimeoutMinute: Byte,
) {
	companion object {
        // TODO: don't know what do these bytes mean yet
		val packetPrefix = byteArrayOf(0x0F, 0x0F, 0x0F, 0x06, 0x00, 0x50, 0x00, 0x04, 0x00, 0x01, 0x00)

		@OptIn(ExperimentalStdlibApi::class)
		fun fromPacket(packet: ByteArray): ConnectionSettings {
			require(packet.first() == Command.CONNECTION_SETTINGS.byte) {
				"Auto Sync Settings packet must starts with command code ${Command.CONNECTION_SETTINGS.byte.toHexString(HexFormat.UpperCase)}"
			}
			require(packet.size == 15) {
				"Auto Sync Settings packet must be exactly 15 bytes long, e.g. 11 0F 0F 0F 06 00 50 00 04 00 01 00 00 20 03"
			}
			packet[12].toInt() and AutoSyncBitmask.DISABLE == 0
			return ConnectionSettings(
                autoSyncEnable = packet[12].toInt() and AutoSyncBitmask.DISABLE == 0,
                autoSyncOffsetMinute = packet[13],
				connectionTimeoutMinute = packet[14],
            )
		}
	}

	// TODO: write settings to ensure positive values

	val packet: ByteArray
		get() = byteArrayOf(
			Command.CONNECTION_SETTINGS.byte,
			*packetPrefix,
			(if (autoSyncEnable) 0 else AutoSyncBitmask.DISABLE).toByte(),
			autoSyncOffsetMinute,
			connectionTimeoutMinute,
		)
}