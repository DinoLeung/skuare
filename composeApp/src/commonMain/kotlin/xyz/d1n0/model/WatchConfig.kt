package xyz.d1n0.model

import xyz.d1n0.constant.*

class WatchConfig {
    lateinit var preferences: WatchPreferences
    lateinit var backlightDuration: BacklightDuration
    lateinit var dateFormat: DateFormat
    lateinit var weekdayLanguage: WeekdayLanguage

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


