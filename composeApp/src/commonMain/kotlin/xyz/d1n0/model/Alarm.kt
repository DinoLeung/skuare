package xyz.d1n0.model

import kotlinx.datetime.LocalTime
import xyz.d1n0.constant.AlarmBitMask

data class Alarm(
    var enable: Boolean,
    var time: LocalTime,
) {
    companion object {
        fun fromBytes(bytes: ByteArray): Alarm {
            require(bytes.size == 4) {
                "Alarm bytes must be exactly 4 bytes long, e.g. 40 00 17 3B"
            }
            return Alarm(
                enable = bytes[0].toInt() and AlarmBitMask.ENABLE != 0,
                time = LocalTime(
                    hour = bytes[2].toUByte().toInt(),
                    minute = bytes[3].toUByte().toInt(),
                )
            )
        }
    }

    val bytes: ByteArray
        get() = ByteArray(4) {
            when (it) {
                0 -> (if (enable) AlarmBitMask.ENABLE else 0).toByte()
                2 -> time.hour.toByte()
                3 -> time.minute.toByte()
                else -> 0x00
            }
        }
}
