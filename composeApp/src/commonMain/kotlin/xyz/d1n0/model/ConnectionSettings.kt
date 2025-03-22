package xyz.d1n0.model

import xyz.d1n0.constant.AutoSyncEnabled
import xyz.d1n0.constant.Command

data class ConnectionSettings(
    var enabled: AutoSyncEnabled,
    var syncOffsetMinute: Int,
	var connectionTimeoutMinute: Int,
) {
	companion object {
        // TODO: don't know what do these bytes mean yet
		val packetPrefix = byteArrayOf(0x0F, 0x0F, 0x0F, 0x06, 0x00, 0x50, 0x00, 0x04, 0x00, 0x01, 0x00)

		@OptIn(ExperimentalStdlibApi::class)
		fun fromPacket(packet: ByteArray): ConnectionSettings {
			require(packet.first() == Command.CONNECTION_SETTINGS.value.toByte()) {
				"Auto Sync Settings packet must starts with command code ${Command.CONNECTION_SETTINGS.value.toHexString(HexFormat.UpperCase)}"
			}
			require(packet.size == 15) {
				"Auto Sync Settings packet must be exactly 15 bytes long, e.g. 11 0F 0F 0F 06 00 50 00 04 00 01 00 00 20 03"
			}
			return ConnectionSettings(
                enabled = AutoSyncEnabled.fromValue(packet[12].toUByte().toInt()),
                syncOffsetMinute = packet[13].toUByte().toInt(),
				connectionTimeoutMinute = packet[14].toUByte().toInt(),
            )
		}
	}

	val packet: ByteArray
		get() = byteArrayOf(
			Command.CONNECTION_SETTINGS.value.toByte(),
			*packetPrefix,
			enabled.value.toByte(),
			syncOffsetMinute.toByte(),
			connectionTimeoutMinute.toByte(),
		)
}