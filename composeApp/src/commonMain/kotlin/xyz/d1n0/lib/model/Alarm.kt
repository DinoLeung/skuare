package xyz.d1n0.lib.model

import kotlinx.datetime.LocalTime
import xyz.d1n0.lib.constant.AlarmBitMask
import xyz.d1n0.lib.helper.fromByteArray
import xyz.d1n0.lib.helper.toByteArray

data class Alarm(
	val enable: Boolean,
	val time: LocalTime,
) {
	companion object {
		fun fromBytes(bytes: ByteArray): Alarm {
			require(bytes.size == 4) {
				"Alarm bytes must be exactly 4 bytes long, e.g. 40 00 17 3B"
			}
			return Alarm(
				enable = bytes[0].toInt() and AlarmBitMask.ENABLE != 0,
				time = LocalTime.fromByteArray(bytes.sliceArray(2..3)),
			)
		}
	}

	val bytes: ByteArray
		get() {
			val timeBytes = time.toByteArray()
			return ByteArray(4) {
				when (it) {
					0 -> (if (enable) AlarmBitMask.ENABLE else 0).toByte()
					2 -> timeBytes[0]
					3 -> timeBytes[1]
					else -> 0x00
				}
			}
		}
}
