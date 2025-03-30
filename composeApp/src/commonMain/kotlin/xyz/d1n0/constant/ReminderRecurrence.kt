package xyz.d1n0.constant

import xyz.d1n0.constant.ReminderBitmask.MULTIPLE_DAYS
import xyz.d1n0.constant.ReminderBitmask.WEEKLY
import xyz.d1n0.constant.ReminderBitmask.YEARLY
import xyz.d1n0.constant.ReminderBitmask.MONTHLY

enum class ReminderRecurrence {
    ONCE_SAME_DAY,
    ONCE_MULTIPLE_DAYS,
    REPEAT_WEEKLY,
    REPEAT_MONTHLY,
    REPEAT_YEARLY;

    companion object {
        fun fromByte(byte: Byte) =
            byte.toInt().let {
                when {
                    it and MULTIPLE_DAYS == MULTIPLE_DAYS -> ONCE_MULTIPLE_DAYS
                    it and WEEKLY == WEEKLY -> REPEAT_WEEKLY
                    it and YEARLY == YEARLY -> REPEAT_YEARLY
                    it and MONTHLY == MONTHLY -> REPEAT_MONTHLY
                    else -> ONCE_SAME_DAY
                }
            }
        }

    val byte: Byte
        get() = when(this) {
            ONCE_SAME_DAY -> 0
            ONCE_MULTIPLE_DAYS -> MULTIPLE_DAYS.toByte()
            REPEAT_WEEKLY -> WEEKLY.toByte()
            REPEAT_MONTHLY -> MONTHLY.toByte()
            REPEAT_YEARLY -> YEARLY.toByte()
        }
}
