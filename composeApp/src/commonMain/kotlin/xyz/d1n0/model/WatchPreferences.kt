package xyz.d1n0.model

import xyz.d1n0.constant.PreferencesBitmask

data class WatchPreferences(
    var is24HourTime: Boolean,
    var isToneMuted: Boolean,
    var autoBacklight: Boolean,
    var powerSaving: Boolean,
) {
    companion object {
        fun fromValue(value: Int) = WatchPreferences(
            is24HourTime = value and PreferencesBitmask.MILITARY_TIME != 0,
            isToneMuted = value and PreferencesBitmask.MUTE_TONE != 0,
            autoBacklight = value and PreferencesBitmask.AUTO_BACKLIGHT_OFF == 0,
            powerSaving = value and PreferencesBitmask.POWER_SAVING_OFF == 0,
        )
    }

    val value: Int
        get() {
            var bitmask = 0
            if (is24HourTime) bitmask = bitmask or PreferencesBitmask.MILITARY_TIME
            if (isToneMuted) bitmask = bitmask or PreferencesBitmask.MUTE_TONE
            if (!autoBacklight) bitmask = bitmask or PreferencesBitmask.AUTO_BACKLIGHT_OFF
            if (!powerSaving) bitmask = bitmask or PreferencesBitmask.POWER_SAVING_OFF
            return bitmask
        }
}