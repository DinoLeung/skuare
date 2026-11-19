package xyz.d1n0.lib.model

import xyz.d1n0.lib.helper.fromCasioByteArray
import xyz.d1n0.lib.helper.toCasioByteArray

data class ReminderTitle(val value: String) {
	init {
		if (value.length > MAX_LENGTH) {
			throw TextTooLongException(MAX_LENGTH, "Reminder title must not exceed $MAX_LENGTH characters")
		}
	}

	companion object {
		const val MAX_LENGTH = 18

		fun fromBytes(bytes: ByteArray): ReminderTitle {
			require(bytes.size == 18) {
				"Reminder title bytes must be exactly 18 bytes long, e.g. 8D 8C 90 91 92 93 80 87 82 8B 2D 2C 00 00 00 00 00 00"
			}
			return ReminderTitle(value = String.fromCasioByteArray(bytes))
		}
	}

	val bytes: ByteArray
		get() = value.toCasioByteArray().let {
			it + ByteArray(18 - it.size) { 0x00.toByte() }
		}
}
