package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.OpCode
import xyz.d1n0.lib.constant.TimerStatus
import xyz.d1n0.lib.helper.fromByteArray
import xyz.d1n0.lib.helper.toByteArray
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

// Writing timer will reset timer status, and timer status cannot be set
data class Timer(
	val duration: Duration,
	val status: TimerStatus = TimerStatus.NOT_STARTED,
) {
	companion object {

		@OptIn(ExperimentalStdlibApi::class)
		fun fromPacket(packet: ByteArray): Timer {
			require(packet.first() == OpCode.TIMER.byte) {
				"Timer packet must starts with command code ${
					OpCode.TIMER.byte.toHexString(
						HexFormat.UpperCase
					)
				}"
			}
			require(packet.size == 8) {
				"Timer packet bytes must be exactly 8 bytes long, e.g. 18 17 0F 1E 00 00 00 00"
			}
			return packet.drop(1).toByteArray().let {
					Timer(
						duration = Duration.fromByteArray(it.sliceArray(0..2)),
						status = TimerStatus.fromByte(it.last()),
					)
				}
		}
	}

	init {
		require(duration < 1.days) { "Timer must not exceed 24 hours" }
	}

	val packet: ByteArray
		get() {
			val durationBytes = duration.toByteArray()
			return ByteArray(8) {
				when (it) {
					0 -> OpCode.TIMER.byte
					1 -> durationBytes[0]
					2 -> durationBytes[1]
					3 -> durationBytes[2]
					7 -> status.byte
					else -> 0
				}
			}
		}
}
