package xyz.d1n0.lib.constant

import xyz.d1n0.lib.constant.ReminderBitmask.MONTHLY
import xyz.d1n0.lib.constant.ReminderBitmask.MULTIPLE_DAYS
import xyz.d1n0.lib.constant.ReminderBitmask.WEEKLY
import xyz.d1n0.lib.constant.ReminderBitmask.YEARLY

enum class ReminderRecurrence {
	ONCE, REPEAT_DAILY, REPEAT_WEEKLY, REPEAT_MONTHLY, REPEAT_YEARLY;

	companion object {
		fun fromByte(byte: Byte) = byte.toInt().let {
			when {
				it and MULTIPLE_DAYS == MULTIPLE_DAYS -> REPEAT_DAILY
				it and WEEKLY == WEEKLY -> REPEAT_WEEKLY
				it and YEARLY == YEARLY -> REPEAT_YEARLY
				it and MONTHLY == MONTHLY -> REPEAT_MONTHLY
				else -> ONCE
			}
		}
	}

	val byte: Byte
		get() = when (this) {
			ONCE -> 0
			REPEAT_DAILY -> MULTIPLE_DAYS.toByte()
			REPEAT_WEEKLY -> WEEKLY.toByte()
			REPEAT_MONTHLY -> MONTHLY.toByte()
			REPEAT_YEARLY -> YEARLY.toByte()
		}

	override fun toString(): String = when (this) {
		ONCE -> "Once"
		REPEAT_DAILY -> "Repeat"
		REPEAT_WEEKLY -> "Weekly"
		REPEAT_MONTHLY -> "Monthly"
		REPEAT_YEARLY -> "Yearly"
	}
}
