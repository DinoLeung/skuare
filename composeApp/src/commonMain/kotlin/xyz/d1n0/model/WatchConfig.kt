package xyz.d1n0.model

import xyz.d1n0.constant.BacklightDuration
import xyz.d1n0.constant.Command
import xyz.d1n0.constant.DateFormat
import xyz.d1n0.constant.WeekdayLanguage

enum class AutoSyncEnabled(val value: Int) {
    ENABLED(0x00),
    DISABLED(0x80);

    companion object {
        fun fromValue(value: Int) =
            AutoSyncEnabled.entries.firstOrNull { it.value == value } ?: error("Unknown backlight duration value: $value")
    }
}

// TODO: make it a data class
class AutoSyncConfig {
    companion object {
        // prefix 0F0F0F0600500004000100
        // suffix 03
        val packetPrefix = byteArrayOf(0x0F, 0x0F, 0x0F, 0x06, 0x00, 0x50, 0x00, 0x04, 0x00, 0x01, 0x00)
        val packetSuffix = byteArrayOf(0x03)
    }
    lateinit var enabled: AutoSyncEnabled
    var syncOffsetMinute: Int = 0


}

class WatchConfig {
    lateinit var preferences: WatchPreferences
    lateinit var backlightDuration: BacklightDuration
    lateinit var dateFormat: DateFormat
    lateinit var weekdayLanguage: WeekdayLanguage
    lateinit var autoSync: AutoSyncConfig

    @OptIn(ExperimentalStdlibApi::class)
    fun parseSettingsPacket(settingsPacket: ByteArray) {
        require(settingsPacket.first() == Command.WATCH_SETTINGS.value.toByte()) {
            "Settings packet must starts with command code ${Command.WATCH_SETTINGS.value.toHexString(HexFormat.UpperCase)}"
        }
        require(settingsPacket.size == 12) {
            "Settings packet must be exactly 15 bytes long, e.g. 13 07 00 00 01 00 00 00 00 00 00 00"
        }
        preferences = WatchPreferences(settingsPacket.get(1))
        backlightDuration = BacklightDuration.fromValue(settingsPacket.get(2).toInt())
        dateFormat = DateFormat.fromValue(settingsPacket.get(4).toInt())
        weekdayLanguage = WeekdayLanguage.fromValue(settingsPacket.get(5).toInt())
    }

    private fun isSettingsInitialized() = ::preferences.isInitialized && ::backlightDuration.isInitialized && ::dateFormat.isInitialized && ::weekdayLanguage.isInitialized && ::weekdayLanguage.isInitialized

    val settingsPacket: ByteArray
        get() {
            require(isSettingsInitialized()) {
                "Settings must be initialized"
            }
            return ByteArray(12) { index ->
                when (index) {
                    0 -> Command.WATCH_SETTINGS.value.toByte()
                    1 -> preferences.toBitMask()
                    2 -> backlightDuration.value.toByte()
                    4 -> dateFormat.value.toByte()
                    5 -> weekdayLanguage.value.toByte()
                    else -> 0x00
                }
            }
        }
}


