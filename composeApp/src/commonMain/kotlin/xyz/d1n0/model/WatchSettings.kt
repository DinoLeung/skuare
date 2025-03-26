package xyz.d1n0.model

import xyz.d1n0.constant.BacklightDuration
import xyz.d1n0.constant.Command
import xyz.d1n0.constant.DateFormat
import xyz.d1n0.constant.WeekdayLanguage

data class WatchSettings(
    var preferences: WatchPreferences,
    var backlightDuration: BacklightDuration,
    var dateFormat: DateFormat,
    var weekdayLanguage: WeekdayLanguage,
) {
	companion object {
		@OptIn(ExperimentalStdlibApi::class)
        fun fromPacket(packet: ByteArray): WatchSettings {
			require(packet.first() == Command.WATCH_SETTINGS.value.toByte()) {
				"Settings packet must starts with command code ${Command.WATCH_SETTINGS.value.toHexString(HexFormat.UpperCase)}"
			}
			require(packet.size == 12) {
				"Settings packet must be exactly 15 bytes long, e.g. 13 07 00 00 01 00 00 00 00 00 00 00"
			}
			return WatchSettings(
                preferences = WatchPreferences.fromValue(packet[1].toInt()),
                backlightDuration = BacklightDuration.fromValue(packet[2].toInt()),
                dateFormat = DateFormat.fromValue(packet[4].toInt()),
                weekdayLanguage = WeekdayLanguage.fromValue(packet[5].toInt()),
            )
		}
	}

    val packet: ByteArray
        get() = ByteArray(12) {
            when (it) {
                0 -> Command.WATCH_SETTINGS.value.toByte()
                1 -> preferences.value.toByte()
                2 -> backlightDuration.value.toByte()
                4 -> dateFormat.value.toByte()
                5 -> weekdayLanguage.value.toByte()
                else -> 0x00
            }
        }
}