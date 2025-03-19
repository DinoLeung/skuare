package xyz.d1n0.model

import xyz.d1n0.constant.AutoSyncEnabled
import xyz.d1n0.constant.Command

data class AutoSyncSettings(
    var enabled: AutoSyncEnabled,
    var syncOffsetMinute: Int,
) {
	companion object {
        // TODO: don't know what are these bytes means yet
		val packetPrefix = byteArrayOf(0x0F, 0x0F, 0x0F, 0x06, 0x00, 0x50, 0x00, 0x04, 0x00, 0x01, 0x00)
		val packetSuffix = byteArrayOf(0x03)

		@OptIn(ExperimentalStdlibApi::class)
		fun fromPacket(packet: ByteArray): AutoSyncSettings {
			require(packet.first() == Command.AUTO_SYNC_SETTINGS.value.toByte()) {
				"Auto Sync Settings packet must starts with command code ${Command.AUTO_SYNC_SETTINGS.value.toHexString(HexFormat.UpperCase)}"
			}
			require(packet.size == 15) {
				"Auto Sync Settings packet must be exactly 15 bytes long, e.g. 11 0F 0F 0F 06 00 50 00 04 00 01 00 00 20 03"
			}
			return AutoSyncSettings(
                enabled = AutoSyncEnabled.fromValue(packet.get(12).toInt()),
                syncOffsetMinute = packet.get(12).toInt(),
            )
		}
	}

	val packet: ByteArray
		get() = byteArrayOf(
			Command.AUTO_SYNC_SETTINGS.value.toByte(),
			*packetPrefix,
			enabled.value.toByte(),
			syncOffsetMinute.toByte(),
			*packetSuffix
		)
}