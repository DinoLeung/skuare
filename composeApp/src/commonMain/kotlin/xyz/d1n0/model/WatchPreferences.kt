package xyz.d1n0.model

import xyz.d1n0.constant.PreferencesBitMask

data class WatchPreferences(
    var is24HourTime: Boolean,
    var isToneMuted: Boolean,
    var autoBacklight: Boolean,
    var powerSaving: Boolean,
) {
    companion object {
        fun fromByte(byte: Byte) = WatchPreferences(
            is24HourTime = byte.toInt() and PreferencesBitMask.MILITARY_TIME != 0,
            isToneMuted = byte.toInt() and PreferencesBitMask.MUTE_TONE != 0,
            autoBacklight = byte.toInt() and PreferencesBitMask.AUTO_BACKLIGHT_OFF == 0,
            powerSaving = byte.toInt() and PreferencesBitMask.POWER_SAVING_OFF == 0,
        )
    }

    val byte: Byte
        get() {
            var bitmask = 0
            if (is24HourTime) bitmask = bitmask or PreferencesBitMask.MILITARY_TIME
            if (isToneMuted) bitmask = bitmask or PreferencesBitMask.MUTE_TONE
            if (!autoBacklight) bitmask = bitmask or PreferencesBitMask.AUTO_BACKLIGHT_OFF
            if (!powerSaving) bitmask = bitmask or PreferencesBitMask.POWER_SAVING_OFF
            return bitmask.toByte()
        }
}