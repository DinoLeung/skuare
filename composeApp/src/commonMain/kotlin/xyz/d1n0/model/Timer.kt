package xyz.d1n0.model

import xyz.d1n0.constant.Command
import xyz.d1n0.constant.TimerStatus
import xyz.d1n0.helper.fromByteArray
import xyz.d1n0.helper.toByteArray
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
                duration = Duration.fromByteArray(bytes.sliceArray(0..2)),
                status = TimerStatus.fromValue(bytes.last().toInt(),
                )
            )
        }
    }

    val bytes: ByteArray
        get() {
            val durationBytes = duration.toByteArray()
            return ByteArray(7) {
                when (it) {
                    0 -> durationBytes[0]
                    1 -> durationBytes[1]
                    2 -> durationBytes[2]
                    6 -> status.value.toByte()
                    else -> 0
                }
            }
        }
}
