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
        @OptIn(ExperimentalStdlibApi::class)
        fun fromPacket(packet: ByteArray): Timer {
            require(packet.first() == Command.TIMER.value.toByte()) {
                "Timer packet must starts with command code ${Command.TIMER.value.toHexString(HexFormat.UpperCase)}"
            }
            require(packet.size == 8) {
                "Timer packet bytes must be exactly 8 bytes long, e.g. 18 17 0F 1E 00 00 00 00"
            }
            return Timer(
                duration = packet[1].toUByte().toInt().hours
                        + packet[2].toUByte().toInt().minutes
                        + packet[3].toUByte().toInt().seconds,
                status = TimerStatus.fromValue(packet.last().toInt(),
                )
            )
        }
    }

    val packet: ByteArray
        get() = duration.toComponents { hours, minutes, seconds, _ ->
            ByteArray(8) {
                when (it) {
                    0 -> Command.TIMER.value.toByte()
                    1 -> hours.toByte()
                    2 -> minutes.toByte()
                    3 -> seconds.toByte()
                    7 -> status.value.toByte()
                    else -> 0
                }
            }
        }
}
