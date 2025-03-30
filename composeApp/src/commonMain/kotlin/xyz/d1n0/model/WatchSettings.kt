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
			require(packet.first() == Command.WATCH_SETTINGS.byte) {
				"Settings packet must starts with command code ${Command.WATCH_SETTINGS.byte.toHexString(HexFormat.UpperCase)}"
			}
			require(packet.size == 12) {
				"Settings packet must be exactly 15 bytes long, e.g. 13 07 00 00 01 00 00 00 00 00 00 00"
			}
			return WatchSettings(
                preferences = WatchPreferences.fromByte(packet[1]),
                backlightDuration = BacklightDuration.fromByte(packet[2]),
                dateFormat = DateFormat.fromByte(packet[4]),
                weekdayLanguage = WeekdayLanguage.fromByte(packet[5]),
            )
		}
	}

    val packet: ByteArray
        get() = ByteArray(12) {
            when (it) {
                0 -> Command.WATCH_SETTINGS.byte
                1 -> preferences.byte
                2 -> backlightDuration.byte
                4 -> dateFormat.byte
                5 -> weekdayLanguage.byte
                else -> 0x00
            }
        }
}