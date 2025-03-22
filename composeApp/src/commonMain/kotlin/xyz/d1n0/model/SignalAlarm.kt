package xyz.d1n0.model

import xyz.d1n0.constant.AlarmBitMask

data class SignalAlarm(
    var enable: Boolean,
    var offsetMinute: Int,
) {
    companion object {
        fun fromBytes(bytes: ByteArray): SignalAlarm {
            require(bytes.size == 2) {
                "Signal alarm bytes must be exactly 2 bytes long, e.g. 80 00"
            }
            return SignalAlarm(
                enable = bytes[0].toInt() and AlarmBitMask.MASK_HOURLY_SIG != 0,
                offsetMinute = bytes[1].toUByte().toInt()
            )
        }
    }
    val bytes: ByteArray
        get() = byteArrayOf(
            (if (enable) AlarmBitMask.MASK_HOURLY_SIG else 0).toByte(),
            offsetMinute.toByte(),
        )
}
