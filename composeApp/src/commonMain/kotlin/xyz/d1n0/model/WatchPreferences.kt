package xyz.d1n0.model

import xyz.d1n0.constant.PreferencesBitMask

class WatchPreferences {
    var is24HourTime: Boolean
    var isToneMuted: Boolean
    var autoBacklight: Boolean
    var powerSaving: Boolean

    constructor(bitMask: Byte) {
        val bitmaskInt = bitMask.toInt()
        is24HourTime = bitmaskInt and PreferencesBitMask.MASK_24_HOURS != 0
        isToneMuted = bitmaskInt and PreferencesBitMask.MASK_MUTE_TONE != 0
        autoBacklight = bitmaskInt and PreferencesBitMask.MASK_AUTO_BACKLIGHT_OFF == 0
        powerSaving = bitmaskInt and PreferencesBitMask.MASK_POWER_SAVING_OFF == 0
    }

    fun toBitMask(): Byte {
        var bitmask = 0
        if (is24HourTime) bitmask = bitmask or PreferencesBitMask.MASK_24_HOURS
        if (isToneMuted) bitmask = bitmask or PreferencesBitMask.MASK_MUTE_TONE
        if (!autoBacklight) bitmask = bitmask or PreferencesBitMask.MASK_AUTO_BACKLIGHT_OFF
        if (!powerSaving) bitmask = bitmask or PreferencesBitMask.MASK_POWER_SAVING_OFF
        return bitmask.toByte()
    }
}