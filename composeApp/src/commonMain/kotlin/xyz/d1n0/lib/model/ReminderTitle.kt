package xyz.d1n0.lib.model

import xyz.d1n0.lib.helper.fromCasioByteArray
import xyz.d1n0.lib.helper.toCasioByteArray

data class ReminderTitle(
	val value: String,
) {
	companion object {
		fun fromBytes(bytes: ByteArray): ReminderTitle {
			require(bytes.size == 18) {
				"Reminder Name bytes must be 9 bytes long, e.g. 8D 8C 90 91 92 93 80 87 82 8B 2D 2C 00 00 00 00 00 00"
			}
			return ReminderTitle(value = String.fromCasioByteArray(bytes))
		}
	}

	val bytes: ByteArray
		get() = value.toCasioByteArray()

}
