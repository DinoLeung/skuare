package xyz.d1n0.lib.model

import xyz.d1n0.constant.TimerStatus
import xyz.d1n0.lib.helper.fromByteArray
import xyz.d1n0.lib.helper.toByteArray
import kotlin.time.Duration

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
                status = TimerStatus.fromByte(bytes.last()),
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
                    6 -> status.byte
                    else -> 0
                }
            }
        }
}
