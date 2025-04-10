package xyz.d1n0.lib.model

import xyz.d1n0.constant.PreferencesBitmask

data class WatchPreferences(
    var is24HourTime: Boolean,
    var isToneMuted: Boolean,
    var autoBacklight: Boolean,
    var powerSaving: Boolean,
) {
    companion object {
        fun fromByte(byte: Byte) = WatchPreferences(
            is24HourTime = byte.toInt() and PreferencesBitmask.MILITARY_TIME != 0,
            isToneMuted = byte.toInt() and PreferencesBitmask.MUTE_TONE != 0,
            autoBacklight = byte.toInt() and PreferencesBitmask.AUTO_BACKLIGHT_OFF == 0,
            powerSaving = byte.toInt() and PreferencesBitmask.POWER_SAVING_OFF == 0,
        )
    }

    val byte: Byte
        get() = listOf(
            is24HourTime to PreferencesBitmask.MILITARY_TIME,
            isToneMuted to PreferencesBitmask.MUTE_TONE,
            (!autoBacklight) to PreferencesBitmask.AUTO_BACKLIGHT_OFF,
            (!powerSaving) to PreferencesBitmask.POWER_SAVING_OFF,
        )
            .filter { (flag, _) -> flag }
            .fold(0) { acc, (_, mask) -> acc or mask }
            .toByte()
}