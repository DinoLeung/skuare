package xyz.d1n0.model

import xyz.d1n0.constant.Command
import xyz.d1n0.constant.TimerStatus
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// Writing timer will reset timer status, and timer status cannot be set
data class Timer(
    val duration: Duration,
    val status: TimerStatus
) {
    companion object {
        fun fromBytes(bytes: ByteArray): Timer {
            require(bytes.size == 7) {
                "Timer bytes must be exactly 7 bytes long, e.g. 17 0F 1E 00 00 00 00"
            }
            return Timer(
                duration = bytes[0].toUByte().toInt().hours
                        + bytes[1].toUByte().toInt().minutes
                        + bytes[2].toUByte().toInt().seconds,
                status = TimerStatus.fromValue(bytes.last().toInt(),
                )
            )
        }
    }

    val bytes: ByteArray
        get() = duration.toComponents { hours, minutes, seconds, _ ->
            ByteArray(7) {
                when (it) {
                    0 -> hours.toByte()
                    1 -> minutes.toByte()
                    2 -> seconds.toByte()
                    6 -> status.value.toByte()
                    else -> 0
                }
            }
        }
}
