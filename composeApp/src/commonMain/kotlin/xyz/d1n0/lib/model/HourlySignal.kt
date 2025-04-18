package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.AlarmBitMask

data class HourlySignal(
    var enable: Boolean,
) {
    companion object {
        fun fromByte(byte: Byte): HourlySignal {
            return HourlySignal(
                enable = byte.toInt() and AlarmBitMask.HOURLY_SIG != 0,
            )
        }
    }
    val byte: Byte
        get() = (if (enable) AlarmBitMask.HOURLY_SIG else 0).toByte()
}
