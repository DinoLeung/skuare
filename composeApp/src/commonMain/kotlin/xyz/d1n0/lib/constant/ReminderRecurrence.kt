package xyz.d1n0.lib.constant

import xyz.d1n0.lib.constant.ReminderBitmask.MONTHLY
import xyz.d1n0.lib.constant.ReminderBitmask.MULTIPLE_DAYS
import xyz.d1n0.lib.constant.ReminderBitmask.WEEKLY
import xyz.d1n0.lib.constant.ReminderBitmask.YEARLY

enum class ReminderRecurrence {
	ONCE_SAME_DAY, REPEAT_DAYS, REPEAT_WEEKLY, REPEAT_MONTHLY, REPEAT_YEARLY;

	companion object {
		fun fromByte(byte: Byte) = byte.toInt().let {
			when {
				it and MULTIPLE_DAYS == MULTIPLE_DAYS -> REPEAT_DAYS
				it and WEEKLY == WEEKLY -> REPEAT_WEEKLY
				it and YEARLY == YEARLY -> REPEAT_YEARLY
				it and MONTHLY == MONTHLY -> REPEAT_MONTHLY
				else -> ONCE_SAME_DAY
			}
		}
	}

	val byte: Byte
		get() = when (this) {
			ONCE_SAME_DAY -> 0
			REPEAT_DAYS -> MULTIPLE_DAYS.toByte()
			REPEAT_WEEKLY -> WEEKLY.toByte()
			REPEAT_MONTHLY -> MONTHLY.toByte()
			REPEAT_YEARLY -> YEARLY.toByte()
		}

	override fun toString(): String = when (this) {
		ONCE_SAME_DAY -> "Once"
		REPEAT_DAYS -> "Repeat"
		REPEAT_WEEKLY -> "Weekly"
		REPEAT_MONTHLY -> "Monthly"
		REPEAT_YEARLY -> "Yearly"
	}
}
