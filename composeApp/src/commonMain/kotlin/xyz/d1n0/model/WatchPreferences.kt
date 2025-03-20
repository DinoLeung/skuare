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
            is24HourTime = byte.toInt() and PreferencesBitMask.MASK_24_HOURS != 0,
            isToneMuted = byte.toInt() and PreferencesBitMask.MASK_MUTE_TONE != 0,
            autoBacklight = byte.toInt() and PreferencesBitMask.MASK_AUTO_BACKLIGHT_OFF == 0,
            powerSaving = byte.toInt() and PreferencesBitMask.MASK_POWER_SAVING_OFF == 0,
        )
    }

    val byte: Byte
        get() {
            var bitmask = 0
            if (is24HourTime) bitmask = bitmask or PreferencesBitMask.MASK_24_HOURS
            if (isToneMuted) bitmask = bitmask or PreferencesBitMask.MASK_MUTE_TONE
            if (!autoBacklight) bitmask = bitmask or PreferencesBitMask.MASK_AUTO_BACKLIGHT_OFF
            if (!powerSaving) bitmask = bitmask or PreferencesBitMask.MASK_POWER_SAVING_OFF
            return bitmask.toByte()
        }
}