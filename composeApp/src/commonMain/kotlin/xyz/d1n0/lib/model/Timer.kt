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
	val status: TimerStatus,
) {
	companion object {
		/** Timer packet of the GMW-B5000 family. */
		const val CLASSIC_PACKET_SIZE = 8

		/**
		 * Timer packet of the GW-BX5600 and GA-B2100: the same fields followed by seven zero
		 * bytes. The official app writes this size to the GA-B2100 too.
		 */
		const val PADDED_PACKET_SIZE = 15

		@OptIn(ExperimentalStdlibApi::class)
		fun fromPacket(packet: ByteArray): Timer {
			require(packet.first() == OpCode.TIMER.byte) {
				"Timer packet must start with command code ${
					OpCode.TIMER.byte.toHexString(
						HexFormat.UpperCase
					)
				}"
			}
			require(packet.size == CLASSIC_PACKET_SIZE || packet.size == PADDED_PACKET_SIZE) {
				"Timer packet bytes must be 8 or 15 bytes long, e.g. 18 17 0F 1E 00 00 00 00"
			}
			return packet.drop(1).toByteArray().let {
				Timer(
					duration = Duration.fromByteArray(it.sliceArray(0..2)),
					// The 15-byte GW-BX5600 packet keeps the normal fields in the first
					// eight bytes and appends seven padding bytes.
					status = TimerStatus.fromByte(it[6]),
				)
			}
		}
	}

	init {
		require(duration < 1.days) { "Timer must not exceed 24 hours" }
	}

	/**
	 * @param size [CLASSIC_PACKET_SIZE], or [PADDED_PACKET_SIZE] for watches that reply with it,
	 *        see [TimerSettings.packetSize]. The extra bytes are zero.
	 * @throws IllegalArgumentException if [size] is neither.
	 */
	fun packet(size: Int = CLASSIC_PACKET_SIZE): ByteArray {
		require(size == CLASSIC_PACKET_SIZE || size == PADDED_PACKET_SIZE) {
			"Timer packet size must be $CLASSIC_PACKET_SIZE or $PADDED_PACKET_SIZE, got $size"
		}
		val durationBytes = duration.toByteArray()
		return ByteArray(size) {
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
